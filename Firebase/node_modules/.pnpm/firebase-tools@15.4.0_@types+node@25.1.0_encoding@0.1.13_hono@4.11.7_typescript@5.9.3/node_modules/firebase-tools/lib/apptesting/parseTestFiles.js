"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.parseTestFiles = parseTestFiles;
const fsutils_1 = require("../fsutils");
const path_1 = require("path");
const logger_1 = require("../logger");
const types_1 = require("./types");
const utils_1 = require("../utils");
const error_1 = require("../error");
function createFilter(pattern) {
    const regex = pattern ? new RegExp(pattern) : undefined;
    return (s) => !regex || regex.test(s);
}
async function parseTestFiles(dir, targetUri, filePattern, namePattern) {
    try {
        new URL(targetUri);
    }
    catch (ex) {
        const errMsg = "Invalid URL" + (targetUri.startsWith("http") ? "" : " (must include protocol)");
        throw new error_1.FirebaseError(errMsg, { original: (0, error_1.getError)(ex) });
    }
    const fileFilterFn = createFilter(filePattern);
    const nameFilterFn = createFilter(namePattern);
    async function parseTestFilesRecursive(testDir) {
        const items = (0, fsutils_1.listFiles)(testDir);
        const results = [];
        for (const item of items) {
            const path = (0, path_1.join)(testDir, item);
            if ((0, fsutils_1.dirExistsSync)(path)) {
                results.push(...(await parseTestFilesRecursive(path)));
            }
            else if (fileFilterFn(path) && (0, fsutils_1.fileExistsSync)(path)) {
                try {
                    const file = await (0, utils_1.readFileFromDirectory)(testDir, item);
                    const parsedFile = (0, utils_1.wrappedSafeLoad)(file.source);
                    const tests = parsedFile.tests;
                    const defaultConfig = parsedFile.defaultConfig;
                    if (!tests || !tests.length) {
                        logger_1.logger.info(`No tests found in ${path}. Ignoring.`);
                        continue;
                    }
                    for (const rawTestDef of parsedFile.tests) {
                        if (!nameFilterFn(rawTestDef.testName))
                            continue;
                        const testDef = toTestDef(rawTestDef, targetUri, defaultConfig);
                        results.push(testDef);
                    }
                }
                catch (ex) {
                    const errMsg = (0, error_1.getErrMsg)(ex);
                    const errDetails = errMsg ? `Error details: \n${errMsg}` : "";
                    logger_1.logger.info(`Unable to parse test file ${path}. Ignoring.${errDetails}`);
                    continue;
                }
            }
        }
        return results;
    }
    return parseTestFilesRecursive(dir);
}
function toTestDef(testDef, targetUri, defaultConfig) {
    const steps = testDef.steps ?? [];
    const route = testDef.testConfig?.route ?? defaultConfig?.route ?? "";
    const browsers = testDef.testConfig?.browsers ??
        defaultConfig?.browsers ?? [types_1.Browser.CHROME];
    return {
        testCase: {
            startUri: targetUri + route,
            displayName: testDef.testName,
            instructions: { steps },
        },
        testExecution: browsers.map((browser) => ({ config: { browser } })),
    };
}

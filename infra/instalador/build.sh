#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
JDK_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
# Una compilación nueva también retira clases y recursos eliminados de esta edición.
rm -rf -- target/classes target/test-classes
mkdir -p target/classes target/test-classes
find src/main/java -name '*.java' -print > target/sources.txt
"${JDK_BIN}javac" -J-Xmx256m --release 17 --add-modules jdk.httpserver -encoding UTF-8 -d target/classes @target/sources.txt
cp -R src/main/resources/. target/classes/
"${JDK_BIN}jar" -J-Xmx128m --create --file target/instalador.jar --main-class ar.com.lamontana.instalador.Main -C target/classes .
if [[ ${1:-} == test || ${1:-} == test-docker ]]; then
  find src/test/java -name '*.java' -print > target/tests.txt
  "${JDK_BIN}javac" -J-Xmx256m --release 17 --add-modules jdk.httpserver -encoding UTF-8 -cp target/classes -d target/test-classes @target/tests.txt
  "${JDK_BIN}java" -Xmx256m -ea --add-modules jdk.httpserver -cp target/classes:target/test-classes ar.com.lamontana.instalador.InstaladorTest
  "${JDK_BIN}java" -Xmx256m -ea --add-modules jdk.httpserver -cp target/classes:target/test-classes ar.com.lamontana.instalador.CarpetasTest
  "${JDK_BIN}java" -Xmx256m -ea --add-modules jdk.httpserver -cp target/classes:target/test-classes ar.com.lamontana.instalador.FirmasAntivirusTest
  if [[ ${1:-} == test-docker ]]; then
    "${JDK_BIN}java" -Xmx256m -ea --add-modules jdk.httpserver -cp target/classes:target/test-classes ar.com.lamontana.instalador.InstaladorDockerTest
  fi
fi

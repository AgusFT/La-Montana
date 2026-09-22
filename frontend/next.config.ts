import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  // Keep the compiler API for TypeScript 5; builds still check all types.
  experimental: { useTypeScriptCli: false },
};

export default nextConfig;

import type { NextConfig } from "next"
import { PHASE_DEVELOPMENT_SERVER } from "next/constants"

const nextConfig = (phase: string): NextConfig => {
    const isDev = phase === PHASE_DEVELOPMENT_SERVER

    return {
        ...(isDev ? {} : { output: "export" }),

        ...(isDev ? {
            async rewrites() {
                return [
                    {
                        source: "/api/:path*",
                        destination: "http://localhost:8080/api/:path*",
                    },
                ]
            },
        } : {}),
    }
}

export default nextConfig

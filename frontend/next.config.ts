import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  /* config options here */
  compress: false,
  output: 'standalone', // Enable standalone build for Docker
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'goorm-opener.s3.ap-northeast-2.amazonaws.com',
      },
    ],
  },
  async rewrites() {
    if (process.env.NODE_ENV !== 'development') return []
    const origin = process.env.BACKEND_ORIGIN
    if (!origin) return []

    return [
      {
        source: '/api/:path*',
        destination: `${origin}/api/:path*`,
      },
    ]
  },
}

export default nextConfig

import type { NextConfig } from "next";
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./i18n/request.ts');

const serverActionAllowedOrigins = process.env.NEXT_SERVER_ACTION_ALLOWED_ORIGINS
    ?.split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);

const nextConfig: NextConfig = {
  /* config options here */
  outputFileTracingRoot: process.cwd(),
  reactCompiler: true,
  experimental: {
    serverActions: {
      allowedOrigins: serverActionAllowedOrigins && serverActionAllowedOrigins.length > 0
          ? serverActionAllowedOrigins
          : ['localhost:4054', '127.0.0.1:4054'],
    },
  },
};

export default withNextIntl(nextConfig);
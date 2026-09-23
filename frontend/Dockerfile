# region ENCABEZADO · Dockerfile
# ========================================================================
# ARCHIVO: Dockerfile
# ========================================================================
# FUNCIÓN
# Construye el frontend con Node.js y pnpm y prepara una imagen standalone de Next.js, con usuario
# sin privilegios y comprobación de salud.
#
# ------------------------------------------------------------------------
# CONTENIDO DECLARADO
# - base: Node.js, directorio de trabajo y versión de pnpm.
# - dependencies: instalación con lockfile congelado.
# - builder: compilación de Next.js y preparación de PDF.js.
# - runner: copia de la aplicación standalone, estáticos y recursos públicos.
# - USER, EXPOSE, HEALTHCHECK y CMD: usuario, puerto, salud e inicio.
#
# MÉTODOS / FUNCIONES: no declara; archivo de configuración.
# ========================================================================
# endregion

FROM node:22-alpine AS base
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm install --global pnpm@11.19.0

FROM base AS dependencies
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

FROM base AS builder
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .
RUN pnpm build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 HOSTNAME=0.0.0.0 PORT=3000
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 --ingroup nodejs nextjs
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/public ./public
USER nextjs
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=4s --start-period=20s --retries=3 CMD node -e 'fetch("http://127.0.0.1:3000/health", {signal: AbortSignal.timeout(3000)}).then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))'
CMD ["node", "server.js"]

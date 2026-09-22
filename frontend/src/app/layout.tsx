import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "La Montaña · Identidad inicial",
  description: "Instalación, registro de clientes y acceso a La Montaña 0.1.",
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="es-AR"><body>{children}</body></html>;
}

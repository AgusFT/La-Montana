import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "La Montaña · Impresiones",
  description: "Cotización, producción y entrega de pedidos de impresión.",
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="es-AR"><body>{children}</body></html>;
}

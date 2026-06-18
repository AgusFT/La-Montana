import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "La Montaña Impresiones",
  description: "Panel web para gestionar pedidos de impresión.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}

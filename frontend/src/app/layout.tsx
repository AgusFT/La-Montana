import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "La Montaña · Base técnica",
  description: "Estado de la primera etapa de La Montaña 0.1.",
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="es-AR"><body>{children}</body></html>;
}

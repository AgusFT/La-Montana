import { ReactNode } from "react";

import { Sidebar } from "@/components/navigation/DashboardSidebar";
import { BrandLockup } from "@/components/brand/BrandLockup";

interface ClienteLayoutProps {
  children: ReactNode;
}

export function ClienteLayout({
  children,
}: ClienteLayoutProps) {
  return (
    <main className="dashboard-shell app-screen">
      <Sidebar />

      <section className="dashboard-content">
        <header className="topbar">
          <BrandLockup compact />

          <div className="topbar-actions">
            <a>Contacto</a>

            <span
              className="avatar"
              aria-label="Usuario Alejandro"
            >
              A
            </span>

            <span>Alejandro</span>
          </div>
        </header>

        {children}
      </section>
    </main>
  );
}
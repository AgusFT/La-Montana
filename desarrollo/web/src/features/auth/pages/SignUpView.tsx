"use client";

import { BrandLockup } from "@/components/brand/BrandLockup";
import Image from "next/image";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export function SignUpView() {
  const router = useRouter();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    // TODO: Integrar con Supabase Auth
    router.push("/dashboard");
  }

  return (
    <main className="login-shell">
      <header className="login-header">
        <div className="login-header-inner">
          <BrandLockup />

          <nav className="login-nav" aria-label="Navegación pública">
            <a href="#contacto">Contacto</a>
            <a href="#cuenta">Cuenta</a>
          </nav>
        </div>
      </header>

      <section className="login-main" aria-labelledby="signup-title">
        <div className="mountain-mark" aria-hidden="true" />

        <form className="login-card panel-card" onSubmit={handleSubmit}>
          <Image
            className="login-card-logo"
            src="/logo.jpg"
            alt="La Montaña Impresiones"
            width={120}
            height={120}
          />

          <p className="eyebrow">Portal cliente</p>

          <h1 id="signup-title">Crear cuenta</h1>

          <p className="muted">
            Registrate para solicitar presupuestos, cargar archivos y realizar
            el seguimiento de tus pedidos.
          </p>

          <div className="field">
            <label htmlFor="firstName">Nombre</label>
            <input
              id="firstName"
              type="text"
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
              placeholder="Tu nombre"
            />
          </div>

          <div className="field">
            <label htmlFor="lastName">Apellido</label>
            <input
              id="lastName"
              type="text"
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
              placeholder="Tu apellido"
            />
          </div>

          <div className="field">
            <label htmlFor="phone">Teléfono</label>
            <input
              id="phone"
              type="tel"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="11 1234 5678"
            />
          </div>

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="tu@email.com"
            />
          </div>

          <div className="field">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Elegí una contraseña"
            />
          </div>

          <div className="field">
            <label htmlFor="confirmPassword">
              Confirmar contraseña
            </label>

            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              placeholder="Repetí la contraseña"
            />
          </div>

          <div className="login-actions">
            <button className="primary-button" type="submit">
              Crear cuenta
            </button>

            <button
              className="secondary-button"
              type="button"
              onClick={() => router.push("/login")}
            >
              Ya tengo cuenta
            </button>
          </div>
        </form>
      </section>

      <footer className="login-footer" id="contacto">
        <div className="login-footer-inner">
          <div className="footer-feature">
            <span className="metric-dot" style={{ background: "#3498db" }}>
              S
            </span>

            <p>
              <strong>Seguridad</strong>
              <span>Protegemos tus archivos y datos de contacto.</span>
            </p>
          </div>

          <div className="footer-feature">
            <span className="metric-dot" style={{ background: "#27ae60" }}>
              A
            </span>

            <p>
              <strong>Archivos</strong>
              <span>Historial y pedidos siempre disponibles.</span>
            </p>
          </div>

          <div className="footer-feature">
            <span className="metric-dot" style={{ background: "#e67e22" }}>
              T
            </span>

            <p>
              <strong>Trazabilidad</strong>
              <span>Seguimiento claro desde revisión hasta entrega.</span>
            </p>
          </div>
        </div>
      </footer>
    </main>
  );
}
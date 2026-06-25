"use client";

import { BrandLockup } from "@/components/brand/BrandLockup";
import { obtenerPerfilAutenticado } from "@/lib/auth/profile";
import { crearClienteSupabaseBrowser } from "@/lib/supabase/client";
import Image from "next/image";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";


export function LoginView() {
  const router = useRouter();
  
  const [email, setEmail] = useState("cliente@cliente.com");
  const [password, setPassword] = useState("cliente");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!email.trim() || !password) {
      setError("Ingresá email y contraseña.");
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      const supabase = crearClienteSupabaseBrowser();
      const { error: signInError } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password,
      });

      if (signInError) {
        setError("No pudimos iniciar sesión con esos datos.");
        return;
      }

      const perfil = await obtenerPerfilAutenticado(supabase);

      if (perfil?.rolCodigo !== "cliente") {
        await supabase.auth.signOut();
        setError("Esta cuenta no tiene acceso al Portal Cliente.");
        return;
      }

      router.replace("/dashboard");
      router.refresh();
    } catch {
      setError("No pudimos conectar con Supabase.");
    } finally {
      setIsSubmitting(false);
    }
  }

    function goToSignUp() {
    router.push("/signup");
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

      <section className="login-main" aria-labelledby="login-title">
        <div className="mountain-mark" aria-hidden="true" />
        <form className="login-card panel-card" onSubmit={handleSubmit}>
          <Image
            className="login-card-logo"
            src="/logo.jpg"
            alt="La Montaña Impresiones"
            width={120}
            height={120} />
          <p className="eyebrow">Portal cliente</p>
          <h1 id="login-title">Ingresá a tus pedidos</h1>
          <p className="muted">
            Consultá cotizaciones, subí archivos y seguí el avance de cada
            trabajo desde un solo lugar.
          </p>

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="tu@email.com" />
          </div>

          <div className="field">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Tu contraseña" />
          </div>

          {error ? <p className="form-error">{error}</p> : null}

          <div className="login-actions">
            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Ingresando..." : "Iniciar sesión"}
            </button>
            <button className="secondary-button" type="button" onClick={goToSignUp}>
              Crear cuenta
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

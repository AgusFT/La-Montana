import {getSetupState} from "@/lib/identity-server";
import {MountainMark} from "@/components/mountain-brand";
import "./web.css";
export const dynamic="force-dynamic";
export default async function WebsitePage(){
 const setup=await getSetupState();
 return <div className="website"><header className="website-header"><a href="/web" className="website-brand"><span className="website-logo-placeholder" aria-hidden="true">◇</span><span>Nombre de la imprenta<small>Identidad sin configurar</small></span></a><nav aria-label="Página de la imprenta"><a href="/acceso">Iniciar sesión</a><a className="website-button" href={setup?.requierePropietario?"/acceso":"/registro"}>Crear cuenta</a></nav></header>
 <main><div className="website-notice"><strong>Sitio sin configurar</strong><p>Este es el espacio público de la imprenta. Su propietario todavía no publicó contenido; los recuadros indican dónde aparecerá.</p></div>{!setup&&<p className="website-error" role="alert">No pudimos comprobar la instalación. <a href="/web">Volver a intentar</a></p>}
 <section className="website-hero"><div><span className="website-eyebrow">PORTADA PENDIENTE</span><h1>El lugar para presentar<br/>tu imprenta.</h1><p className="website-placeholder-text">Título y descripción de la portada sin configurar.</p><div className="website-placeholder-line"/><div className="website-placeholder-line short"/></div><div className="website-image-placeholder"><span aria-hidden="true">▧</span><p>Imagen de portada</p><small>Sin configurar</small></div></section>
 <section className="website-section"><span className="website-eyebrow">CATÁLOGO PÚBLICO</span><h2>Productos y servicios</h2><p>Las fichas que publique el propietario aparecerán aquí.</p><div className="website-cards">{[1,2,3].map(n=><article className="website-placeholder-card" key={n}><div className="website-image-placeholder"><span aria-hidden="true">▧</span><small>Imagen pendiente</small></div><h3>Ficha sin configurar</h3><div className="website-placeholder-line"/><div className="website-placeholder-line short"/></article>)}</div></section>
 <section className="website-section website-contact"><div><span className="website-eyebrow">CONTACTO</span><h2>Datos de la imprenta</h2><p>Sin datos de contacto publicados.</p></div><div><div className="website-placeholder-line"/><div className="website-placeholder-line short"/></div></section></main>
 <footer className="website-footer"><span><MountainMark/> Software La Montaña · Demo local</span><a href="/">Panel de inicio de la demo</a></footer></div>;
}

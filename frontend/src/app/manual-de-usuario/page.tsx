import Image from "next/image";
import Link from "next/link";
import { getSession } from "@/lib/identity-server";
import { sessionHome } from "@/lib/roles";
import { redirect } from "next/navigation";
import { inicioDemo, disponibilidad } from "./edicion-demo";

import { Indice } from "./Indice";
import { administracion } from "./administracion";
import { cliente } from "./cliente";
import { operacion } from "./operacion";
import { orientacion, continuaciones } from "./orientacion";
import { imagenes } from "./imagenes";
import styles from "./manual.module.css";

export const metadata = {
  title: "Manual de usuario de La Montaña",
  description: "Cómo configurar la imprenta, cotizar un trabajo, producirlo, cobrarlo y entregarlo en La Montaña. Guía paso a paso con capturas de la aplicación.",
};

const capitulos = [inicioDemo, orientacion, ...cliente, ...operacion, ...administracion];
const grupos = capitulos.map(({ id, titulo, temas }) => ({ id, titulo, temas: temas.map(({ id, titulo, paraQue }) => ({ id, titulo, paraQue })) }));
const titulos = new Map(capitulos.flatMap(c => c.temas.map(t => [t.id, t.titulo] as const)));
const revisiones = capitulos.flatMap(c => c.temas.filter(t => t.revision));
const entradas = [
  { rol: "Soy cliente", texto: "Preparar un PDF y hacer un pedido", id: "cli-archivos" },
  { rol: "Trabajo en la imprenta", texto: "Encontrar el próximo trabajo", id: "op-bandejas" },
  { rol: "Administro la imprenta", texto: "Configurar la imprenta y su oferta", id: "admin-borrador" },
];

export default async function ManualPrincipal() {
  const sesion = await getSession();
  if (sesion.state !== "authenticated") redirect("/acceso");
  return (
    <main className={styles.manual} id="contenido">
      <Link className={styles.regreso} href={sessionHome(sesion.profile)}>← Volver a mi panel</Link>
      <header className={styles.heading}><h1>Manual de usuario</h1></header>
      <div className={styles.columnas}>
        <Indice grupos={grupos} />
        <article className={styles.articulo}>
          <header className={styles.portada}>
            <h2>La Montaña</h2>
            <p>Seguí un trabajo desde que el cliente prepara el PDF hasta que la imprenta lo cobra y lo entrega. El manual y sus capturas están instalados en este equipo. Cada tema indica qué rol lo usa y cuándo corresponde a la edición completa.</p>
            <div className={styles.entradas}>
              {entradas.map(({ rol, texto, id }) => (
                <a key={id} href={`#${id}`}><strong>{rol}</strong><span>{texto}</span><span aria-hidden="true">↘</span></a>
              ))}
            </div>
          </header>
          {capitulos.map(capitulo => (
            <section key={capitulo.id} className={styles.capitulo} aria-labelledby={capitulo.id}>
              <h2 id={capitulo.id} data-capitulo={capitulo.id} tabIndex={-1}>{capitulo.titulo}</h2>
              {capitulo.temas.map(tema => (
                <section key={tema.id} className={styles.tema} aria-labelledby={tema.id}>
                  <h3 id={tema.id} data-capitulo={capitulo.id} tabIndex={-1}>{tema.titulo}</h3>
                  <p className={styles.alcance}>{disponibilidad(capitulo.id, tema.id)}</p>
                  <p>{tema.paraQue}</p>
                  <ol>{tema.pasos.map((paso, i) => <li key={i}>{paso}</li>)}</ol>
                  {tema.resultado && <p className={styles.resultado}><strong>Resultado. </strong>{tema.resultado}</p>}
                  {(imagenes[tema.id] ?? []).map((imagen, index, capturas) => (
                    <figure className={styles.figura} key={imagen.archivo} aria-labelledby={tema.id}
                      style={{ width: Math.min(imagen.ancho, Math.round(imagen.ancho * 550 / imagen.alto)) }}>
                      <a href={`/manual/la-montana/${imagen.archivo}`} target="_blank" rel="noopener noreferrer"
                        aria-label={`Ampliar captura${capturas.length > 1 ? ` ${index + 1} de ${capturas.length}` : ""} de «${tema.titulo}» (abre otra pestaña)`}>
                        <Image src={`/manual/la-montana/${imagen.archivo}`} width={imagen.ancho} height={imagen.alto}
                          alt="" unoptimized />
                        <span aria-hidden="true">Ampliar ↗</span>
                      </a>
                      <figcaption>{imagen.pie}</figcaption>
                    </figure>
                  ))}
                  {tema.nota && <p className={styles.nota}>{tema.nota}</p>}
                  {tema.revision && <aside className={styles.revision} id={`revision-${tema.id}`} tabIndex={-1}><strong>Para revisión de desarrollo</strong><p>{tema.revision}</p></aside>}
                  {continuaciones[tema.id] && <div className={styles.siguientes}><strong>Seguí con</strong>
                    <ul>{continuaciones[tema.id].map(id => <li key={id}><a href={`#${id}`}>{titulos.get(id)}</a></li>)}</ul>
                  </div>}
                </section>
              ))}
            </section>
          ))}
          <section className={styles.capitulo} aria-labelledby="revisiones">
            <h2 id="revisiones" data-capitulo="revisiones" tabIndex={-1}>Puntos para revisión de desarrollo</h2>
            <p>Estos temas tienen una diferencia de comportamiento o una comprobación pendiente. En cada nota se indica qué puede hacer quien usa la aplicación.</p>
            <ul className={styles.enlacesRevision}>
              {revisiones.map(tema => <li key={tema.id}><a href={`#revision-${tema.id}`}>{tema.titulo}</a></li>)}
            </ul>
          </section>
          <a href="#contenido" className={styles.volver}>Volver al inicio del manual ↑</a>
        </article>
      </div>
    </main>
  );
}

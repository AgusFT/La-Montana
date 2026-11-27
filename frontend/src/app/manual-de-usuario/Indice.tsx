"use client";

import { useEffect, useState } from "react";
import styles from "./manual.module.css";

type Entrada = { id: string; titulo: string };
type Grupo = Entrada & { temas: (Entrada & { paraQue: string })[] };

function normalizar(texto: string) {
  return texto.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("es");
}

export function Indice({ grupos }: { grupos: Grupo[] }) {
  const [consulta, buscar] = useState("");
  const [abierto, abrir] = useState(false);
  const [expandido, expandir] = useState(grupos[0].id);
  const [actual, marcar] = useState("");
  const palabras = normalizar(consulta.trim()).split(/\s+/).filter(Boolean);
  const filtrando = palabras.length > 0;
  const visibles = grupos.map(grupo => ({
    ...grupo,
    temas: grupo.temas.filter(tema => {
      const texto = normalizar([grupo.titulo, tema.titulo, tema.paraQue].join(" "));
      return palabras.every(palabra => {
        const raiz = palabra.length > 5 ? palabra.replace(/(?:es|s)$/, "") : palabra;
        return texto.includes(raiz);
      });
    }),
  })).filter(grupo => grupo.temas.length > 0);

  useEffect(() => {
    const titulos = Array.from(document.querySelectorAll<HTMLElement>("[data-capitulo]"));
    let frame = 0;
    let ultimoGrupo = "";
    function actualizar() {
      frame = 0;
      const margen = window.matchMedia("(max-width: 800px)").matches ? 108 : 68;
      let visible: HTMLElement | undefined;
      for (const titulo of titulos) {
        if (titulo.getBoundingClientRect().top > margen) break;
        visible = titulo;
      }
      marcar(visible?.id ?? "");
      const grupo = visible?.dataset.capitulo;
      if (grupo && grupo !== ultimoGrupo) expandir(grupo);
      ultimoGrupo = grupo ?? "";
    }
    function mover() { if (!frame) frame = requestAnimationFrame(actualizar); }
    actualizar();
    window.addEventListener("scroll", mover, { passive: true });
    window.addEventListener("resize", mover);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("scroll", mover);
      window.removeEventListener("resize", mover);
    };
  }, []);

  return (
    <aside className={styles.rail}>
      <nav aria-label="Índice del manual" className={styles.indice} onKeyDown={event => {
        if (event.key === "Escape" && abierto) {
          abrir(false);
          document.getElementById("abrir-indice")?.focus();
        }
      }}>
        <h2 className={styles.indiceTitulo}>Índice del manual</h2>
        <button id="abrir-indice" type="button" className={styles.indiceMovil} aria-expanded={abierto}
          aria-controls="temario" onClick={() => abrir(!abierto)}>Índice del manual <span aria-hidden="true">{abierto ? "−" : "+"}</span></button>
        <div id="temario" className={`${styles.temario} ${abierto ? styles.visible : ""}`}>
          <label className={styles.buscar}>
            <span>Buscar tema</span>
            <input type="search" value={consulta} onChange={e => buscar(e.target.value)} placeholder="Ej. retiro" />
          </label>
          {visibles.length === 0 && <p role="status" className={styles.sinTemas}>No hay coincidencias. Probá con una palabra, como «retiro».</p>}
          <div className={styles.grupos}>
            {visibles.map(grupo => {
              const abiertoGrupo = filtrando || expandido === grupo.id;
              return (
                <div className={styles.grupo} key={grupo.id}>
                  {filtrando ? <p className={styles.tituloGrupo}>{grupo.titulo}</p> :
                    <button type="button" className={styles.tituloGrupo} aria-controls={`indice-${grupo.id}`} aria-expanded={abiertoGrupo}
                      onClick={() => expandir(abiertoGrupo ? "" : grupo.id)}>
                      {grupo.titulo}<span aria-hidden="true">{abiertoGrupo ? "−" : "+"}</span>
                    </button>}
                  <ul id={`indice-${grupo.id}`} hidden={!abiertoGrupo}>
                    {grupo.temas.map(t => <li key={t.id}><a href={`#${t.id}`}
                      aria-current={actual === t.id ? "location" : undefined} onClick={() => abrir(false)}>{t.titulo}</a></li>)}
                  </ul>
                </div>
              );
            })}
          </div>
          {!filtrando && <a className={styles.indiceRevision} href="#revisiones" aria-current={actual === "revisiones" ? "location" : undefined} onClick={() => abrir(false)}>Puntos para revisión de desarrollo</a>}
        </div>
      </nav>
    </aside>
  );
}

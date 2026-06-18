# La Montaña — Sistema de Diseño

![La Montaña](https://img.shields.io/badge/Version-1.0-blue) ![Status](https://img.shields.io/badge/Status-Ready-success)

Sistema de diseño completo para **La Montaña**, una imprenta moderna que combina tecnología de vanguardia con precios accesibles, dirigida especialmente a profesionales en formación.

A continuación la demo de paleta de colores.
Demo: https://genius-taper-17568747.figma.site/
---

## 📚 Documentación

### 1. **Guidelines.md** — Documentación Completa del Sistema
Documento maestro con todas las guías del sistema de diseño:
- Esencia de marca y dirección visual
- Paleta de colores completa con uso semántico
- Sistema tipográfico (Roboto Slab + Inter + JetBrains Mono)
- Escala de espaciado y convenciones
- Patrones de componentes (botones, cards, inputs, badges)
- Principios de layout y grid system
- Guías de imagery y fotografía
- Animaciones y motion
- Accesibilidad (WCAG AA)
- Voice & tone
- Checklist de implementación

**[Ver Guidelines.md →](./Guidelines.md)**

### 2. **DESIGN-REFERENCE.md** — Referencia Visual Rápida
Guía visual condensada con:
- Tablas de colores con hexadecimales
- Jerarquía tipográfica completa
- Componentes con código de ejemplo
- Sistema de espaciado
- Borders, radius y sombras
- Breakpoints y grid
- Ejemplos de uso práctico
- Checklist de implementación

**[Ver DESIGN-REFERENCE.md →](./DESIGN-REFERENCE.md)**

---

## 🎨 Paleta de Colores

### Primarios
- **Mountain Slate** ` #2C3E50 ` — Texto principal
- **Mountain Blue** ` #3498DB ` — CTAs secundarios
- **Summit White** ` #CFCFCF ` — Fondo principal
- **Mist** ` #95A5A6 ` — Fondos alternos 1
- **Celeste** ` '#5892B9' ` — Fondo alternativo 2

### Acentos
- **Sunset** ` #E67E22 ` — CTA principal ⭐
- **Earth** ` #8B7355 ` — Acento cálido
- **Forest** ` #27AE60 ` — Success

---

## 🔤 Tipografía

### Familias
- **Display/Headings**: Roboto Slab (400, 600, 700)
- **Body/UI**: Inter (400, 500, 600)
- **Monospace**: JetBrains Mono (400, 500, 600) — para precios y specs

### Importación
Las fuentes se importan automáticamente desde Google Fonts en `src/styles/fonts.css`

---

## 🧩 Componentes Clave

### Botones
- **CTA Principal**: Brutalist (sin border-radius), color Sunset
- **Secundario**: Outline, color Mountain Blue
- **Ghost**: Texto con underline hover

### Cards
- Border sutil, border-radius 12px
- Shadow mínimo, aumenta en hover
- Precios en monospace color Sunset

### Inputs
- Background Snow, border Mist
- Focus con border blue y shadow sutil

---

## 📐 Sistema de Espaciado

Basado en múltiplos de **4px**:
- `--spacing-4` (16px) — Base
- `--spacing-6` (24px) — Cards
- `--spacing-8` (32px) — Entre secciones
- `--spacing-12` (48px) — Bloques grandes
- `--spacing-16` (64px) — Hero sections

---

## 🎯 Filosofía de Diseño

### Minimalist + Brutalist
- **Minimalist**: Espacios generosos, jerarquía clara, tipografía prominente
- **Brutalist**: Toques en elementos clave (botones principales sin radius) para modernidad
- **Mobile-first**: Diseñar primero para móvil, escalar a desktop
- **Accesible**: WCAG AA compliance en todos los elementos

### Voz y Tono
- Directo y claro (sin jerga innecesaria)
- Cercano pero profesional
- Honesto (no prometer lo que no se puede cumplir)
- Empoderador ("Hazlo posible con La Montaña")

---

## 🚀 Implementación

### Archivos del Sistema

```
A definir.

/workspaces/default/code/
├── Guidelines.md              # Documentación completa
├── DESIGN-REFERENCE.md        # Referencia visual rápida
├── README.md                  # Este archivo
├── src/
│   ├── styles/
│   │   ├── 
│   │   
│   └── app/
│       └── 
```

### Uso de Variables CSS

Todos los tokens están disponibles como variables CSS:

```css
/* Colores */
background-color: var(--color-mountain-blue);
color: var(--color-sunset);

/* Espaciado */
padding: var(--spacing-6);
margin-bottom: var(--spacing-12);

/* Borders y Radius */
border: var(--border-width) solid var(--border-color);
border-radius: var(--radius-lg);

/* Sombras */
box-shadow: var(--shadow-card);

/* Transiciones */
transition: var(--transition-base);
```

### Ejemplo de Componente

```tsx
function PriceCard({ title, price }: { title: string; price: string }) {
  return (
    <div
      style={{
        backgroundColor: 'white',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: 'var(--spacing-6)',
        boxShadow: 'var(--shadow-card)'
      }}
    >
      <h4>{title}</h4>
      <p style={{
        fontFamily: "'JetBrains Mono', monospace",
        color: 'var(--color-sunset)',
        fontWeight: 600
      }}>
        {price}
      </p>
    </div>
  );
}
```

---

## ✅ Checklist Rápido

Antes de publicar cualquier componente:

- [ ] Usa variables CSS (no valores hardcoded)
- [ ] Tipografía correcta (Roboto Slab títulos, Inter cuerpo)
- [ ] Sistema de espaciado (múltiplos de 4px)
- [ ] Contraste WCAG AA (4.5:1 mínimo)
- [ ] Hover/focus states en interactivos
- [ ] Responsive en todos los breakpoints
- [ ] Touch targets 44x44px mínimo

---

## 📖 Guías de Inicio Rápido

### Para Diseñadores
1. Lee **Guidelines.md** completo
2. Usa **DESIGN-REFERENCE.md** como cheatsheet
3. Respeta la paleta de colores y tipografía
4. Prototipa con los componentes base antes de crear nuevos

### Para Desarrolladores
1. Revisa **DESIGN-REFERENCE.md** para referencia rápida
2. Importa tokens desde `src/styles/theme.css`
3. Usa variables CSS para todos los valores
4. Consulta `src/app/App.tsx` para ejemplos de implementación
5. Verifica el checklist antes de hacer commit

---

## 📱 Demo en Vivo

Para ver el sistema de diseño en acción, ejecuta la aplicación:

```bash
# El servidor de desarrollo ya está corriendo
# Abre el preview en la interfaz
```

La demo muestra:
- Paleta de colores completa
- Jerarquía tipográfica
- Variantes de botones
- Cards de servicios
- Y más componentes...

---

## 🤝 Contribuir

Al agregar nuevos componentes o variantes:
1. Asegúrate de usar los tokens existentes
2. Documenta el nuevo componente en `DESIGN-REFERENCE.md`
3. Agrega ejemplos de uso
4. Verifica accesibilidad (contraste, keyboard navigation)
5. Prueba en móvil y desktop

---

## 📞 Contacto

**Proyecto**: La Montaña
**Versión**: 1.0  
**Fecha**: Mayo 2026  
**Estatus**: ⚒️ Sistema en desarrollo

---

## 📝 Notas de Versión

### Decision aplicada - Login y Dashboard web inicial (Junio 2026)

La primera implementacion en `desarrollo/web` toma como referencia directa el prototipo exportado desde Figma ubicado en `desarrollo/prototipo-figma` y las capturas de `docs/diseño/Front/ux-ui/vistas-web-mockups/cliente`.

Decisiones tomadas:
- Se conserva la estructura funcional del prototipo: login, dashboard cliente, pedido actual, timeline de estados, tabla de pedidos, resumen general, punto de entrega y CTA para crear pedido.
- Se evita copiar literalmente todos los estilos del prototipo para alinear la web con la guia visual del sistema: estetica minimalista con acentos brutalist.
- Los colores se centralizan como tokens CSS en `src/app/globals.css`: `--color-mountain-slate`, `--color-mountain-ink`, `--color-mountain-blue`, `--color-sunset`, `--color-earth`, `--color-forest`, `--color-mist`, `--color-snow` y `--color-border`.
- La accion principal usa `--color-sunset` y boton sin border-radius para sostener el criterio brutalist definido en la guia.
- Las acciones secundarias usan outline azul con `--color-mountain-blue`, manteniendo jerarquia clara sin competir con la accion primaria.
- Se declaran stacks tipograficos para `Roboto Slab` en titulos y marca, `Inter` para interfaz y `JetBrains Mono` para precios. En esta primera version se usan como tokens CSS con fallbacks locales para que el build funcione sin depender de descargas externas.
- Las cards mantienen borde sutil, radio moderado y sombra baja. El objetivo es que el panel se sienta profesional y utilitario, no como landing decorativa.
- El recurso visual de montaña se usa como fondo abstracto liviano en login y bienvenida del dashboard para mantener identidad sin depender de imagenes pesadas o genericas.
- El logo real de La Montaña se copia a `desarrollo/web/public/logo.jpg` y se usa como identificador visual principal.

Justificacion:
El prototipo resuelve bien los flujos internos del cliente, por eso se toma como base UX. La implementacion web ajusta la estetica para acercarla al sistema de diseño documentado: mayor limpieza, tokens reutilizables, jerarquia de botones consistente y menos dependencia de estilos hardcodeados. Esta decision permite evolucionar el dashboard, el flujo de cotizacion y futuras pantallas sin romper coherencia visual.

### v1.0 (Mayo 2026)
- ✅ Sistema de colores completo
- ✅ Tipografía (Roboto Slab + Inter + JetBrains Mono)
- ✅ Componentes base (botones, cards, inputs, badges)
- ✅ Sistema de espaciado y layout
- ✅ Documentación completa
- ✅ Demo funcional
- ✅ Tokens CSS implementados

---

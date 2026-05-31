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
- **Mountain Slate** `#2C3E50` — Texto principal
- **Mountain Blue** `#3498DB` — CTAs secundarios
- **Summit White** `#FFFFFF` — Fondo principal
- **Snow** `#F8F9FA` — Fondos alternos

### Acentos
- **Sunset** `#E67E22` — CTA principal ⭐
- **Earth** `#8B7355` — Acento cálido
- **Forest** `#27AE60` — Success
- **Mist** `#95A5A6` — Texto secundario

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

### v1.0 (Mayo 2026)
- ✅ Sistema de colores completo
- ✅ Tipografía (Roboto Slab + Inter + JetBrains Mono)
- ✅ Componentes base (botones, cards, inputs, badges)
- ✅ Sistema de espaciado y layout
- ✅ Documentación completa
- ✅ Demo funcional
- ✅ Tokens CSS implementados

---


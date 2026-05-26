# La Montaña — Referencia Visual del Sistema de Diseño

Este documento complementa `Guidelines.md` y sirve como referencia rápida de todos los elementos visuales del sistema de diseño.

---

## Paleta de Colores

### Colores Primarios

| Color | Hex | Variable CSS | Uso |
|-------|-----|--------------|-----|
| ![Mountain Slate](https://via.placeholder.com/80x40/2C3E50/2C3E50.png) **Mountain Slate** | `#2C3E50` | `--color-mountain-slate` | Texto principal, encabezados |
| ![Mountain Blue](https://via.placeholder.com/80x40/3498DB/3498DB.png) **Mountain Blue** | `#3498DB` | `--color-mountain-blue` | CTA secundario, enlaces |
| ![Summit White](https://via.placeholder.com/80x40/FFFFFF/FFFFFF.png) **Summit White** | `#FFFFFF` | `--color-summit-white` | Fondo principal |
| ![Snow](https://via.placeholder.com/80x40/F8F9FA/F8F9FA.png) **Snow** | `#F8F9FA` | `--color-snow` | Fondos alternos, secciones |

### Colores de Acento

| Color | Hex | Variable CSS | Uso |
|-------|-----|--------------|-----|
| ![Sunset](https://via.placeholder.com/80x40/E67E22/E67E22.png) **Sunset** | `#E67E22` | `--color-sunset` | CTA principal, precios, acciones importantes |
| ![Earth](https://via.placeholder.com/80x40/8B7355/8B7355.png) **Earth** | `#8B7355` | `--color-earth` | Acento cálido, elementos destacados |
| ![Forest](https://via.placeholder.com/80x40/27AE60/27AE60.png) **Forest** | `#27AE60` | `--color-forest` | Success, confirmaciones |
| ![Mist](https://via.placeholder.com/80x40/95A5A6/95A5A6.png) **Mist** | `#95A5A6` | `--color-mist` | Texto secundario, borders |

### Colores Funcionales

| Color | Hex | Variable CSS | Uso |
|-------|-----|--------------|-----|
| ![Error](https://via.placeholder.com/80x40/E74C3C/E74C3C.png) **Error** | `#E74C3C` | `--color-error` | Errores, alertas críticas |
| ![Warning](https://via.placeholder.com/80x40/F39C12/F39C12.png) **Warning** | `#F39C12` | `--color-warning` | Advertencias |
| ![Success](https://via.placeholder.com/80x40/27AE60/27AE60.png) **Success** | `#27AE60` | `--color-success` | Confirmaciones exitosas |
| ![Info](https://via.placeholder.com/80x40/3498DB/3498DB.png) **Info** | `#3498DB` | `--color-info` | Información neutral |

---

## Tipografía

### Familias Tipográficas

```css
/* Display/Headings */
font-family: 'Roboto Slab', serif;

/* Body/UI */
font-family: 'Inter', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;

/* Monospace (precios, specs) */
font-family: 'JetBrains Mono', 'Courier New', monospace;
```

### Jerarquía de Títulos

| Elemento | Tamaño | Peso | Familia | Ejemplo |
|----------|--------|------|---------|---------|
| **H1** | 36px (2.25rem) | 700 | Roboto Slab | Tecnología de Vanguardia |
| **H2** | 30px (1.875rem) | 700 | Roboto Slab | Precios Accesibles |
| **H3** | 24px (1.5rem) | 700 | Roboto Slab | Calidad Profesional |
| **H4** | 20px (1.25rem) | 600 | Roboto Slab | Servicios Destacados |
| **H5** | 18px (1.125rem) | 600 | Roboto Slab | Subtítulos |
| **H6** | 16px (1rem) | 600 | Roboto Slab | Micro títulos |

### Texto de Cuerpo

| Tipo | Tamaño | Peso | Familia | Uso |
|------|--------|------|---------|-----|
| **Body Large** | 18px (1.125rem) | 400 | Inter | Texto destacado |
| **Body** | 16px (1rem) | 400 | Inter | Texto principal |
| **Body Small** | 14px (0.875rem) | 400 | Inter | Texto secundario |
| **Caption** | 12px (0.75rem) | 400 | Inter | Labels, metadatos |

---

## Componentes

### Botones

#### CTA Principal (Brutalist)
```tsx
<button
  style={{
    backgroundColor: 'var(--color-sunset)',
    color: 'white',
    padding: '0.75rem 1.5rem',
    fontWeight: 600,
    borderRadius: 0,
    border: 'none',
    cursor: 'pointer',
    transition: 'transform 0.2s ease-out'
  }}
>
  Cotizar Ahora
</button>
```
**Características:**
- Sin border-radius (brutalist)
- Color: Sunset (#E67E22)
- Hover: Transform translateY(-1px)
- Uso: Acciones principales

#### Botón Secundario
```tsx
<button
  style={{
    backgroundColor: 'transparent',
    color: 'var(--color-mountain-blue)',
    padding: '0.75rem 1.5rem',
    fontWeight: 600,
    border: '2px solid var(--color-mountain-blue)',
    borderRadius: 'var(--radius-md)',
    cursor: 'pointer'
  }}
>
  Ver Servicios
</button>
```
**Características:**
- Outline con border 2px
- Color: Mountain Blue (#3498DB)
- Border-radius: 8px (--radius-md)
- Hover: Fondo blue, texto blanco

#### Botón Ghost/Text
```tsx
<button
  style={{
    backgroundColor: 'transparent',
    color: 'var(--color-mountain-slate)',
    padding: '0.75rem 1.5rem',
    fontWeight: 600,
    border: 'none',
    cursor: 'pointer',
    textDecoration: 'underline'
  }}
>
  Más información
</button>
```
**Características:**
- Transparente
- Underline on hover
- Uso: Acciones terciarias

---

### Cards

#### Service Card (Card de Servicio)
```tsx
<div
  style={{
    backgroundColor: 'white',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '1.5rem',
    boxShadow: '0 1px 3px rgb(0 0 0 / 0.05)',
    transition: 'box-shadow 0.2s ease-out'
  }}
>
  <h4>Título del Servicio</h4>
  <p style={{ color: 'var(--color-mist)' }}>
    Descripción del servicio
  </p>
  <p style={{
    fontFamily: "'JetBrains Mono', monospace",
    color: 'var(--color-sunset)',
    fontWeight: 600
  }}>
    Desde $50
  </p>
</div>
```

**Características:**
- Border: 1px solid, color mist con opacidad
- Border-radius: 12px (--radius-lg)
- Padding: 24px (1.5rem)
- Shadow: Sutil, aumenta en hover
- Precio: Monospace, color Sunset

---

### Inputs y Formularios

#### Input de Texto
```tsx
<input
  type="text"
  placeholder="Tu nombre"
  style={{
    backgroundColor: 'var(--color-snow)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-md)',
    padding: '0.75rem 1rem',
    fontSize: '1rem',
    width: '100%'
  }}
/>
```

**Características:**
- Background: Snow (#F8F9FA)
- Border: 1px con color mist
- Border-radius: 8px
- Focus: Border blue, shadow sutil

---

### Badges y Tags

```tsx
<span
  style={{
    backgroundColor: 'var(--color-snow)',
    color: 'var(--color-mountain-slate)',
    padding: '0.25rem 0.75rem',
    borderRadius: 'var(--radius-sm)',
    fontSize: '0.75rem',
    fontWeight: 500
  }}
>
  Nuevo
</span>
```

**Características:**
- Background: Snow
- Padding: 4px 12px
- Border-radius: 4px (--radius-sm)
- Font-size: 12px
- Font-weight: 500

---

## Espaciado

### Sistema de Espaciado (múltiplos de 4px)

| Token | Valor | Rem | Uso Común |
|-------|-------|-----|-----------|
| `--spacing-1` | 4px | 0.25rem | Gaps mínimos |
| `--spacing-2` | 8px | 0.5rem | Padding interno pequeño |
| `--spacing-3` | 12px | 0.75rem | Padding botones |
| `--spacing-4` | 16px | 1rem | Padding base |
| `--spacing-6` | 24px | 1.5rem | Padding cards |
| `--spacing-8` | 32px | 2rem | Gaps entre secciones |
| `--spacing-12` | 48px | 3rem | Separación de bloques |
| `--spacing-16` | 64px | 4rem | Márgenes grandes |
| `--spacing-24` | 96px | 6rem | Hero sections |

### Convenciones de Uso

- **Componente padding**: 16-24px (--spacing-4 a --spacing-6)
- **Gap entre secciones**: 48-64px (--spacing-12 a --spacing-16)
- **Márgenes de página**: 24px móvil, 48px desktop
- **Gap entre cards**: 32px (--spacing-8)

---

## Borders y Radius

### Border Widths
```css
--border-width: 1px;          /* Standard */
--border-width-thick: 2px;    /* Énfasis */
```

### Border Radius
```css
--radius-sm: 4px;    /* Tags, badges */
--radius-md: 8px;    /* Botones, inputs */
--radius-lg: 12px;   /* Cards */
--radius-xl: 16px;   /* Modals, large cards */
```

### Border Color
```css
--border-color: rgb(149 165 166 / 0.2);  /* Mist con 20% opacidad */
```

---

## Sombras

```css
/* Card default */
box-shadow: 0 1px 3px rgb(0 0 0 / 0.05);

/* Card hover */
box-shadow: 0 4px 12px rgb(0 0 0 / 0.1);

/* Modal */
box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
```

---

## Breakpoints

```css
--breakpoint-sm: 640px;   /* Mobile landscape */
--breakpoint-md: 768px;   /* Tablet portrait */
--breakpoint-lg: 1024px;  /* Tablet landscape */
--breakpoint-xl: 1280px;  /* Desktop */
--breakpoint-2xl: 1536px; /* Large desktop */
```

### Grid System
- **Desktop**: 12 columnas, max-width 1280px
- **Tablet**: 8 columnas
- **Mobile**: 4 columnas
- **Gutter**: 24px móvil, 32px desktop

---

## Animaciones y Transiciones

### Duraciones
```css
--transition-fast: 150ms ease-out;    /* Hover effects */
--transition-base: 200ms ease-out;    /* Standard */
--transition-slow: 300ms ease-out;    /* Page transitions */
```

### Uso
```css
/* Hover en botones e interactivos */
transition: all 0.2s ease-out;

/* Transform para elevación */
transition: transform 0.2s ease-out;
transform: translateY(-1px);
```

---

## Ejemplos de Uso

### Hero Section
```tsx
<section style={{
  padding: '6rem 1.5rem',
  backgroundColor: 'var(--color-snow)',
  textAlign: 'center'
}}>
  <h1 style={{ marginBottom: '1.5rem' }}>
    Tecnología de Vanguardia
  </h1>
  <p style={{
    fontSize: 'var(--text-lg)',
    color: 'var(--color-mist)',
    marginBottom: '2rem',
    maxWidth: '65ch',
    marginLeft: 'auto',
    marginRight: 'auto'
  }}>
    Impresión profesional con precios accesibles para
    profesionales en formación.
  </p>
  <button style={{
    backgroundColor: 'var(--color-sunset)',
    color: 'white',
    padding: '0.75rem 1.5rem',
    fontWeight: 600,
    border: 'none',
    cursor: 'pointer'
  }}>
    Comenzar Ahora
  </button>
</section>
```

### Grid de Servicios
```tsx
<div style={{
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
  gap: '2rem'
}}>
  {/* Service cards aquí */}
</div>
```

### Pricing Display
```tsx
<p style={{
  fontFamily: "'JetBrains Mono', monospace",
  fontSize: 'var(--text-2xl)',
  fontWeight: 600,
  color: 'var(--color-sunset)'
}}>
  Desde $50
</p>
```

---

## Checklist de Implementación

Al crear nuevos componentes, verificar:

- [ ] Usa variables CSS para todos los colores
- [ ] Aplica el sistema de espaciado (múltiplos de 4px)
- [ ] Tipografía correcta: Roboto Slab para títulos, Inter para cuerpo
- [ ] Contraste de color cumple WCAG AA (mínimo 4.5:1)
- [ ] Elementos interactivos tienen hover/focus states
- [ ] Botones principales usan estilo brutalist (sin border-radius)
- [ ] Cards usan shadow sutil con hover effect
- [ ] Responsive en todos los breakpoints
- [ ] Transiciones suaves (150-300ms)
- [ ] Touch targets mínimo 44x44px

---

## Recursos

- **Documentación completa**: Ver `Guidelines.md`
- **Tokens CSS**: Ver `src/styles/theme.css`
- **Importación de fuentes**: Ver `src/styles/fonts.css`
- **Implementación demo**: Ver `src/app/App.tsx`

---

**Última actualización**: Mayo 2026  
**Versión**: 1.0  
**Creado para**: La Montaña — Imprenta Moderna

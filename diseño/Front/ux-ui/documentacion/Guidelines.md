# La Montaña — Design System Guidelines

## Brand Essence

**La Montaña** es una imprenta moderna que democratiza el acceso a servicios de impresión de alta calidad. Dirigida a profesionales en formación, combina tecnología de vanguardia con precios accesibles. El diseño debe transmitir:

- **Confianza profesional**: Calidad sin compromisos
- **Modernidad accesible**: Tecnología avanzada, precios justos
- **Solidez y estabilidad**: Como una montaña, firme y confiable
- **Claridad**: Sin pretensiones, directo al punto

## Visual Direction

**Estética primaria**: Minimalist con elementos brutalist  
Un enfoque limpio y directo que permite que el contenido respire. Tipografía prominente, espaciado generoso, jerarquía clara. Toques brutalist en elementos específicos (botones, etiquetas) para transmitir modernidad sin sacrificar usabilidad.

## Color Palette

### Primary Colors

```css
--color-mountain-slate: #2C3E50;     /* Primary dark - como roca de montaña */
--color-mountain-blue: #3498DB;      /* Primary bright - cielo de montaña */
--color-summit-white: #FFFFFF;       /* Canvas principal */
--color-snow: #F8F9FA;               /* Backgrounds sutiles */
```

### Secondary & Accent Colors

```css
--color-earth: #8B7355;              /* Accent cálido - tierra */
--color-forest: #27AE60;             /* Success/accent natural */
--color-sunset: #E67E22;             /* CTA principal - energía y acción */
--color-mist: #95A5A6;               /* Text secondary/borders */
```

### Functional Colors

```css
--color-error: #E74C3C;
--color-warning: #F39C12;
--color-success: #27AE60;
--color-info: #3498DB;
```

### Semantic Application

- **Backgrounds**: `--color-summit-white` para contenido principal, `--color-snow` para secciones alternas
- **Text**: `--color-mountain-slate` para texto principal, `--color-mist` para texto secundario
- **CTAs primarios**: `--color-sunset` - destaca sin ser agresivo
- **CTAs secundarios**: `--color-mountain-blue`
- **Acentos**: `--color-earth` para elementos destacados no-interactivos
- **Borders**: `--color-mist` con opacidad 20-40%

## Typography

### Font Families

**Display/Headings**: **Slab Serif**  
Referencia al mundo de la imprenta tradicional, transmite solidez y profesionalismo.

```css
--font-display: 'Roboto Slab', serif;
```

**Body/UI**: **Sans-serif moderna**  
Claridad y legibilidad contemporánea.

```css
--font-body: 'Inter', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
```

**Monospace** (para specs técnicas, precios):

```css
--font-mono: 'Jetbrains Mono', 'Courier New', monospace;
```

### Type Scale

```css
--text-xs: 0.75rem;      /* 12px - labels pequeños */
--text-sm: 0.875rem;     /* 14px - secondary text */
--text-base: 1rem;       /* 16px - body */
--text-lg: 1.125rem;     /* 18px - prominent body */
--text-xl: 1.25rem;      /* 20px - h4 */
--text-2xl: 1.5rem;      /* 24px - h3 */
--text-3xl: 1.875rem;    /* 30px - h2 */
--text-4xl: 2.25rem;     /* 36px - h1 */
--text-5xl: 3rem;        /* 48px - display */
--text-6xl: 3.75rem;     /* 60px - hero */
```

### Font Weights

```css
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

### Usage Guidelines

- **H1-H3**: Use `--font-display` con `--font-bold`
- **H4-H6**: Use `--font-display` con `--font-semibold`
- **Body**: Use `--font-body` con `--font-normal`
- **Emphasis**: Use `--font-medium` en lugar de bold para texto body
- **Prices/specs**: Use `--font-mono` con `--font-semibold`

## Spacing Scale

Sistema de espaciado basado en múltiplos de 4px para consistencia.

```css
--space-1: 0.25rem;   /* 4px */
--space-2: 0.5rem;    /* 8px */
--space-3: 0.75rem;   /* 12px */
--space-4: 1rem;      /* 16px */
--space-5: 1.25rem;   /* 20px */
--space-6: 1.5rem;    /* 24px */
--space-8: 2rem;      /* 32px */
--space-10: 2.5rem;   /* 40px */
--space-12: 3rem;     /* 48px */
--space-16: 4rem;     /* 64px */
--space-20: 5rem;     /* 80px */
--space-24: 6rem;     /* 96px */
```

### Spacing Conventions

- **Component padding interno**: `--space-4` to `--space-6`
- **Sección gaps**: `--space-12` to `--space-16`
- **Page margins**: `--space-6` (mobile), `--space-12` (desktop)
- **Card spacing**: `--space-6` interno, `--space-8` entre cards

## Border & Radius

```css
--border-width: 1px;
--border-width-thick: 2px;
--border-color: rgb(149 165 166 / 0.2);  /* mist with opacity */

--radius-sm: 0.25rem;    /* 4px - tags, badges */
--radius-md: 0.5rem;     /* 8px - buttons, inputs */
--radius-lg: 0.75rem;    /* 12px - cards */
--radius-xl: 1rem;       /* 16px - modals, large cards */
```

### Border Usage

- **Brutalist elements**: Sin border-radius (buttons con acción importante)
- **Standard UI**: `--radius-md`
- **Content cards**: `--radius-lg`
- **Dividers**: `--border-color` sutil

## Component Patterns

### Buttons

**Primary CTA** (brutalist accent):
- Background: `--color-sunset`
- Text: `white`
- Font: `--font-body`, `--font-semibold`
- Padding: `--space-3` `--space-6`
- Border-radius: `0` (brutalist)
- Hover: Darken 10%
- Transform: `translateY(-1px)` on hover

**Secondary Button**:
- Background: transparent
- Border: 2px solid `--color-mountain-blue`
- Text: `--color-mountain-blue`
- Padding: `--space-3` `--space-6`
- Border-radius: `--radius-md`
- Hover: Background `--color-mountain-blue`, Text white

**Ghost/Text Button**:
- Background: transparent
- Text: `--color-mountain-slate`
- Underline on hover

### Cards

```
Background: white
Border: 1px solid --border-color
Radius: --radius-lg
Padding: --space-6
Shadow: 0 1px 3px rgb(0 0 0 / 0.05)
Hover shadow: 0 4px 12px rgb(0 0 0 / 0.1)
```

### Input Fields

```
Background: white
Border: 1px solid --border-color
Radius: --radius-md
Padding: --space-3 --space-4
Focus: Border --color-mountain-blue, subtle shadow
```

### Badges/Tags

```
Background: --color-snow
Text: --color-mountain-slate
Font-size: --text-xs
Font-weight: --font-medium
Padding: --space-1 --space-3
Radius: --radius-sm
```

## Layout Principles

### Grid System

- **Desktop**: 12-column grid, max-width 1280px
- **Tablet**: 8-column grid
- **Mobile**: 4-column grid
- **Gutter**: `--space-6` (mobile), `--space-8` (desktop)

### Whitespace Philosophy

**Generous breathing room** — La Montaña no necesita abarrotar para impresionar. Deja que los elementos respiren:

- Mínimo `--space-12` entre secciones principales
- Usa `--space-16` a `--space-24` para separar bloques de contenido importantes
- Headers con padding vertical `--space-8` a `--space-12`

### Content Width

- **Text content**: max-width 65ch (óptimo para legibilidad)
- **Cards en grid**: min-width 280px, max-width 400px
- **Forms**: max-width 600px para mejor UX

## Imagery Guidelines

### Photography Style

- **Realista y honesto**: No stock photos genéricas
- **Trabajo en acción**: Manos trabajando, máquinas imprimiendo, productos finales
- **Luz natural**: Preferir luz natural o suave
- **Color grading**: Tonos cálidos sutiles que complementen la paleta

### Icons

- **Style**: Outline icons, 2px stroke
- **Size**: 20px o 24px base
- **Color**: Heredar del texto o usar `--color-mountain-blue` para interactivos

### Illustrations (si se usan)

- **Style**: Líneas simples, geométricas
- **Palette**: Usar solo colores del sistema
- **Purpose**: Complementar, nunca decorar sin propósito

## Animation & Motion

### Principles

- **Subtle y funcional**: Movimiento con propósito
- **Rápido y responsivo**: Duraciones 150-300ms
- **Natural**: Easing `ease-out` para entradas, `ease-in` para salidas

### Common Animations

```css
/* Hover transitions */
transition: all 0.2s ease-out;

/* Fade in */
animation: fadeIn 0.3s ease-out;

/* Slide up */
animation: slideUp 0.3s ease-out;
```

### Motion Budget

- **Hover effects**: Obligatorio para interactivos
- **Page transitions**: Fade simple
- **Microinteractions**: Sutil (botones, toggles)
- **NO**: Animaciones decorativas automáticas o loops

## Accessibility

### Color Contrast

- Todos los textos cumplen WCAG AA mínimo
- Textos principales: Contraste ratio ≥ 4.5:1
- Textos grandes (≥18px): Contraste ratio ≥ 3:1

### Interactive Elements

- Mínimo touch target: 44x44px
- Focus states visibles en todos los interactivos
- Navegación por teclado soportada

### Content

- Alt text descriptivo en imágenes
- Labels en inputs
- Estructura semántica (h1-h6 en orden)

## Responsive Breakpoints

```css
--breakpoint-sm: 640px;   /* Mobile landscape */
--breakpoint-md: 768px;   /* Tablet portrait */
--breakpoint-lg: 1024px;  /* Tablet landscape */
--breakpoint-xl: 1280px;  /* Desktop */
--breakpoint-2xl: 1536px; /* Large desktop */
```

### Mobile-First Approach

Diseñar primero para mobile, luego escalar hacia desktop.

## Voice & Tone

### Messaging Principles

- **Directo y claro**: Sin jerga innecesaria
- **Cercano pero profesional**: Tú/usted según contexto
- **Honesto**: No prometer lo que no podemos cumplir
- **Empoderador**: "Hazlo posible con La Montaña" no "Nosotros hacemos todo por ti"

### Microcopy Examples

- **CTAs**: "Ver precios", "Cotizar ahora", "Comenzar proyecto"
- **Empty states**: "Aún no hay proyectos. ¡Crea tu primer trabajo!"
- **Errors**: "No pudimos procesar tu solicitud. Intenta de nuevo."
- **Success**: "¡Listo! Tu cotización está en camino."

---

## Implementation Checklist

Cuando implementes componentes basados en estas guidelines:

- [ ] Usa variables CSS para todos los valores (colores, spacing, etc.)
- [ ] Verifica contraste de colores
- [ ] Aplica el sistema de espaciado consistentemente
- [ ] Usa las familias tipográficas correctas según el elemento
- [ ] Asegura que elementos interactivos tengan hover/focus states
- [ ] Prueba responsive en los breakpoints definidos
- [ ] Mantén el balance entre minimalist y brutalist según el componente
- [ ] Cada decisión debe preguntarse: "¿Esto sirve al profesional en formación?"

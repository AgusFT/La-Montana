
# Arquitectura Frontend - La Montaña

## Objetivo

Este directorio contiene todo el código fuente del frontend web de La Montaña.

La aplicación está desarrollada utilizando:

- Next.js (App Router)
- React
- TypeScript

La organización del código sigue una estructura basada en responsabilidades y dominios de negocio.

La aplicación es única para todos los usuarios del sistema:

- Cliente
- Empleado
- Administrador

Las funcionalidades visibles para cada usuario se determinan mediante autenticación, roles y permisos obtenidos desde Supabase.

---

## Principios Arquitectónicos

### 1. Las rutas viven en `app`

La carpeta `app` define las URLs y la navegación de la aplicación.

No debe contener lógica de negocio compleja.

---

### 2. La lógica de negocio vive en `features`

La carpeta `features` agrupa funcionalidades por dominio de negocio.

Ejemplos:

- pedidos
- auth
- usuarios
- produccion

---

### 3. Los componentes reutilizables viven en `components`

Los componentes de esta carpeta son compartidos entre distintos módulos.

No deberían depender de reglas específicas del negocio.

---

### 4. Los layouts viven en `layouts`

Los layouts representan estructuras visuales reutilizables.

Ejemplos:

- DashboardLayout
- DashboardSidebar
- DashboardTopbar

---

### 5. La infraestructura vive en `lib`

Todo acceso a servicios externos debe centralizarse en esta carpeta.

Ejemplos:

- Supabase
- Storage
- Auth
- Realtime

---

## Estructura General

```txt
src/
│
├── app/
├── components/
├── features/
├── layouts/
├── hooks/
├── lib/
├── types/
├── constants/
├── mocks/
└── styles/
```

---

## Qué NO hacer

### No colocar lógica de negocio dentro de `app`

Incorrecto:

```tsx
app/dashboard/page.tsx
```

con cientos de líneas de lógica.

---

### No duplicar funcionalidades por rol

Incorrecto:

```txt
cliente/pedidos
empleado/pedidos
admin/pedidos
```

La funcionalidad debe organizarse por dominio de negocio.

---

### No acceder a Supabase desde componentes visuales

Incorrecto:

```tsx
OrdersTable.tsx
```

realizando consultas directas.

La comunicación con servicios externos debe centralizarse en `lib` y `features`.

---

## Objetivo de la estructura

Mantener un frontend:

- mantenible
- escalable
- desacoplado
- fácil de navegar
- alineado con la arquitectura general del proyecto



src/
│
├── app/
│   ├── login/
│   │   └── page.tsx
│   │
│   ├── dashboard/
│   │   └── page.tsx
│   │
│   ├── layout.tsx
│   ├── page.tsx
│   └── globals.css
│
├── components/
│   │
│   ├── brand/
│   │   └── BrandLockup.tsx
│   │
│   └── ui/
│       ├── Button.tsx
│       ├── Badge.tsx
│       └── Card.tsx
│
├── layouts/
│   ├── DashboardSidebar.tsx
│   ├── DashboardTopbar.tsx
│   └── DashboardLayout.tsx
│
├── features/
│   │
│   ├── auth/
│   │   └── components/
│   │       └── LoginForm.tsx
│   │
│   ├── dashboard/
│   │   ├── pages/
│   │   │   └── DashboardPage.tsx
│   │   │
│   │   └── components/
│   │       ├── WelcomePanel.tsx
│   │       ├── SummaryPanel.tsx
│   │       └── CurrentOrderCard.tsx
│   │
│   └── pedidos/
│       ├── components/
│       │   ├── OrdersTable.tsx
│       │   └── OrderTimeline.tsx
│       │
│       └── mocks/
│           └── pedidos.ts
│
├── types/
├── constants/
└── styles/
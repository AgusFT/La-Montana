# App

## Objetivo

La carpeta `app` define las rutas de la aplicación utilizando Next.js App Router.

Cada subcarpeta representa una URL.

---

## Ejemplo

```txt
app/
│
├── login/
│   └── page.tsx
│
├── dashboard/
│   └── page.tsx
│
└── pedidos/
    └── page.tsx
```

---

## Responsabilidades

- Definir rutas
- Definir layouts de navegación
- Definir loading states
- Definir error boundaries

---

## No debe contener

- lógica de negocio compleja
- consultas directas a Supabase
- componentes grandes

---

## Recomendación

Mantener los archivos `page.tsx` lo más pequeños posible.

Ejemplo:

```tsx
export default function Page() {
  return <DashboardPage />;
}
```

La implementación real debe vivir en `features`.
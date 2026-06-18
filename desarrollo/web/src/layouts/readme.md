# Layouts

## Objetivo

Contener estructuras visuales reutilizables.

Un layout define cómo se organizan los componentes dentro de una pantalla.

---

## Ejemplos

```txt
layouts/
│
├── DashboardLayout.tsx
├── DashboardSidebar.tsx
├── DashboardTopbar.tsx
└── PublicLayout.tsx
```

---

## Responsabilidades

- navegación
- sidebar
- topbar
- footer
- organización visual

---

## No debe contener

- consultas a la base de datos
- reglas de negocio
- lógica específica de pedidos
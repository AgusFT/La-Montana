# Components

## Objetivo

Contener componentes reutilizables compartidos por toda la aplicación.

Estos componentes representan piezas visuales genéricas.

---

## Ejemplos

```txt
components/
│
├── ui/
│   ├── Button.tsx
│   ├── Input.tsx
│   ├── Badge.tsx
│   └── Card.tsx
│
├── brand/
│   └── BrandLockup.tsx
│
└── tables/
```

---

## Características

Un componente de esta carpeta:

- puede reutilizarse en múltiples módulos
- no debería conocer reglas de negocio
- no debería depender de un rol específico

---

## Correcto

```txt
Button
Input
Card
Avatar
```

---

## Incorrecto

```txt
PedidoAprobadoButton
ClientePedidoCard
AdministradorSidebar
```

Esos componentes pertenecen a un dominio específico y deberían vivir en `features`.
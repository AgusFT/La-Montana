# Lib

## Objetivo

Centralizar la integración con infraestructura y servicios externos.

---

## Ejemplos

```txt
lib/
│
├── supabase/
├── auth/
├── storage/
└── realtime/
```

---

## Responsabilidades

- inicialización de clientes
- acceso a APIs
- configuración de SDKs
- helpers de infraestructura

---

## Ejemplo futuro

```txt
lib/supabase/client.ts
lib/supabase/server.ts
```

---

## No debe contener

- componentes React
- pantallas
- layouts
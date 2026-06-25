# Features

## Objetivo

Agrupar funcionalidades por dominio de negocio.

Esta carpeta contiene la mayor parte de la lógica funcional del sistema.

---

## Ejemplos

```txt
features/
│
├── auth/
├── pedidos/
├── usuarios/
├── produccion/
├── cotizaciones/
├── entregas/
└── cobros/
```

---

## Filosofía

La organización se realiza por funcionalidad.

No por tecnología.

No por rol.

---

## Correcto

```txt
features/pedidos
features/auth
features/usuarios
```

---

## Incorrecto

```txt
cliente/pedidos
empleado/pedidos
admin/pedidos
```

---

## Estructura sugerida

```txt
pedidos/
│
├── components/
├── hooks/
├── services/
├── types/
└── utils/
```

---

## Beneficios

- menor duplicación
- mejor mantenibilidad
- mayor reutilización
- escalabilidad futura
export const ORDER_STATUS_STEPS = [
  {
    id: "pedido_recibido",
    label: "Pedido Recibido",
  },
  {
    id: "pendiente_revision",
    label: "En Revisión",
  },
  {
    id: "produccion",
    label: "Producción",
  },
  {
    id: "control_de_calidad",
    label: "Control de Calidad",
  },
  {
    id: "listo_para_entregar",
    label: "Listo para Entregar",
  },
  {
    id: "en_viaje",
    label: "En Viaje",
  },
  {
    id: "entregado",
    label: "Entregado",
  },
] as const;
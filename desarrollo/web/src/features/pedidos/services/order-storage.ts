// Capa para prototipo que será reemplazada por supabase  

import { Order } from "../types/order";

const STORAGE_KEY = "orders";


// obtengo todos los pedidos
export function getOrders(): Order[] {
  const storedOrders = localStorage.getItem(STORAGE_KEY);

  if (!storedOrders) {
    return [];
  }

  return JSON.parse(storedOrders);
}

// guardo pedido
export function saveOrder(
  order: Order
): void {
  const orders = getOrders();

  orders.push(order);

  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(orders)
  );
}

// buscar por ID
export function getOrderById(
  orderId: string
): Order | undefined {
  return getOrders().find(
    (order) => order.id === orderId
  );
}



// obtener el ultimo pedido. (para la vista de pedido actual)
const ACTIVE_STATUSES: Order["status"][] = [
  "pendiente_revision",
  "corregir",
  "aprobado",
  "produccion",
  "control_de_calidad",
  "listo_para_entregar",
  "en_viaje",
];

function isActiveOrder(
  order: Order
): boolean {
  return ACTIVE_STATUSES.includes(
    order.status
  );
}

export function getLastActiveOrder():
    | Order
  | undefined {

  const orders = getOrders();

  return [...orders]
    .reverse()
    .find(isActiveOrder);
}

// actualizar pedido
export function updateOrder(
  updatedOrder: Order
): void {
  const orders = getOrders();

  const updatedOrders =
    orders.map((order) =>
      order.id === updatedOrder.id
        ? updatedOrder
        : order
    );

  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(updatedOrders)
  );
}

// cancelar ultimo pedido cancelable

// Estados posibles que el usuario podria cancelar
const CANCELABLE_STATUSES: Order["status"][] = [
  "pendiente_revision",
  "corregir", // este estado lo asignaria el admin 
];

function isOrderCancelable(
  order: Order
): boolean {
  return CANCELABLE_STATUSES.includes(
    order.status
  );
}

// Para boton que cancela el ultimo pedido  
export function cancelLastCancelableOrder(): boolean {
  // obtengo todos los pedidos
  const orders = getOrders();

  // traigo el ultimo pedido con estado pendiente_revision
  const orderToCancel = [...orders]
    .reverse()
    .find(isOrderCancelable); // que busque directamente si el pedido es cancelable

  if (!orderToCancel) {
    return false;
  }

  orderToCancel.status = "cancelado";

  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify(orders)
  );

  return true;
}


// Limpiar Storage
export function clearOrders(): void {
  localStorage.removeItem(
    STORAGE_KEY
  );
}


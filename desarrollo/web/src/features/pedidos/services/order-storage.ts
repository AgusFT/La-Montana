// Capa para prototipo que será reemplazada por supabase  

import { Order } from "../types/order";

const STORAGE_KEY = "orders";


// obtengo todos los pedidos
export function getOrders(): Order[] {
  const storedOrders =
    localStorage.getItem(STORAGE_KEY);

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

// Limpiar Storage
export function clearOrders(): void {
  localStorage.removeItem(
    STORAGE_KEY
  );
}
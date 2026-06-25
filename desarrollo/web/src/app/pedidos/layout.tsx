"use client";

import { CreateOrderProvider } from "@/features/pedidos/context/CreateOrderContext";


// Se crea este layout para poder utilizar el useContext,. y guardar entre las paginas de /pedidos el pedido. 
export default function PedidosLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <CreateOrderProvider>
      {children}
    </CreateOrderProvider>
  );
}
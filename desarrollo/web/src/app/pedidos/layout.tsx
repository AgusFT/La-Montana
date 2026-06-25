import { CreateOrderProvider } from "@/features/pedidos/context/CreateOrderContext";
import { exigirClienteAutenticado } from "@/lib/auth/require-client";


// Se crea este layout para poder utilizar el useContext,. y guardar entre las paginas de /pedidos el pedido. 
export default async function PedidosLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  await exigirClienteAutenticado();

  return (
    <CreateOrderProvider>
      {children}
    </CreateOrderProvider>
  );
}

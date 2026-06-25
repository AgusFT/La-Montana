import { Order } from "../../types/order";

interface CurrentOrderHeaderProps {
  code: Order["code"];
  statusLabel: Order["statusLabel"];
}

export function CurrentOrderHeader({
  code,
  statusLabel,
}: CurrentOrderHeaderProps) {
  return (
    <header className="current-order">
      <div>
        <h1>Pedido Actual</h1>

        <p className="current-order-subtitle">
          {code} - Estado visible: {statusLabel}
        </p>
      </div>
    </header>
  );
}

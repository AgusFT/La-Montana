
interface CreateOrderActionsProps {
 disabled: boolean;
  onSubmit: () => void;
}

export function CreateOrderActions({
  disabled,
  onSubmit,
}: CreateOrderActionsProps) {
  return (
    <section className="create-order-actions">
      <button
        type="button"
        disabled={disabled}
        className="primary-button submit-order-button"
        onClick={onSubmit}
      >
        Cotizar pedido
      </button>

      <span className="form-security-note">
        🔒 Tus datos están protegidos y solo se usan para procesar tu pedido.
      </span>

      <div className="contact-confirmation">
        <span>
         👥 Nos pondremos en contacto cuando tu pedido haya sido confirmado.
        </span>
      </div>
    </section>
  );
}
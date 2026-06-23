interface OrderStatusStepProps {
  label: string;

  variant:
    | "completed"
    | "current"
    | "pending";
}

export function OrderStatusStep({
  label,
  variant,
}: OrderStatusStepProps) {
  return (
    <div className="status-step">

      <div
        className={`
          status-circle
          status-circle--${variant}
        `}
      />

      <span className="status-label">
        {label}
      </span>

    </div>
  );
}
// SummaryHeader
//  Título
// Subtítulo
// Botón Volver

// Detalles de Pedido
// Revisá el resumen de tu pedido antes de crearlo.
//                          [Volver]


"use client";

import { ArrowLeft } from "lucide-react";

interface SummaryHeaderProps {
  onBack: () => void;
}

export function SummaryHeader({ onBack }: SummaryHeaderProps) {
  return (
    <div className="summary-header">
      <div>
        <h1>Detalles de Pedido</h1>

        <p className="summary-subtitle">
          Revisá el resumen de tu pedido antes de crearlo.
        </p>
      </div>

      <button
        type="button"
        className="secondary-button"
        onClick={onBack}
      >
        <ArrowLeft size={18} />
        Volver
      </button>
    </div>
  );
}
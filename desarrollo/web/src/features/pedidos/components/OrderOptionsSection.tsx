import { CreateOrderForm } from "../types/create-order";

interface OrderOptionsSectionProps {
  form: CreateOrderForm;

  onChange: (
    field: keyof CreateOrderForm,
    value: unknown
  ) => void;
}

export function OrderOptionsSection({
    form,
    onChange,
    }:OrderOptionsSectionProps){
        return(
            <section className="job-details-card panel-card">
                <div className="section-title">
                    <span className="step-badge">3</span>
                    <h2>Opcionales</h2>
                </div>
                
                <div className="order-options">
                    <label className="checkbox-option">
                    <input
                        type="checkbox"
                        checked={form.doubleSided}
                        onChange={(e) =>
                        onChange("doubleSided", e.target.checked)
                        }
                    />

                    <span>Doble faz</span>
                    </label>

                    <label className="checkbox-option">
                    <input
                        type="checkbox"
                        checked={form.bound}
                        onChange={(e) =>
                        onChange("bound", e.target.checked)
                        }
                    />

                    <span>Encuadernado</span>
                    </label>

                    <label className="checkbox-option">
                    <input
                        type="checkbox"
                        checked={form.spiralBound}
                        onChange={(e) =>
                        onChange("spiralBound", e.target.checked)
                        }
                    />

                    <span>Anillado</span>
                    </label>
                </div>

                <p className="section-help">
                    Podrás confirmar estos detalles durante la revisión administrativa.
                </p>
            </section>
        )
    }

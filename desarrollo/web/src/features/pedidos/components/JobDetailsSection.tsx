import { CreateOrderForm } from "../types/create-order";

interface JobDetailsSectionProps {
  form: CreateOrderForm;

  onChange: (
    field: keyof CreateOrderForm,
    value: unknown
  ) => void;
}

export function JobDetailsSection({
  form,
  onChange,
}: JobDetailsSectionProps) {
 return (
    <section className="job-details-card panel-card">
      <div className="section-title">
        <span className="step-badge">2</span>
        <h2>Detalles del trabajo</h2>
      </div>

      <div className="form-group">
        <label className="form-label">
          Número de páginas
        </label>

        <input
          className="form-input"
          value={
            form.pages ??
            "Se completará al cargar el archivo"
          }
          disabled
          readOnly
        />
      </div>

      <div className="form-group">
        <label className="form-label">
          Tamaño de hoja
        </label>

        <select
          className="form-select"
          value={form.paperSize}
          onChange={(e) =>
            onChange("paperSize", e.target.value)
          }
        >
          <option value="A4">A4</option>
          <option value="A3">A3</option>
          <option value="OFICIO">Oficio</option>
        </select>
      </div>

      <div className="form-group">
        <label className="form-label">
          Cantidad de copias
        </label>

        <div className="copies-control">
          <button
            type="button"
            onClick={() =>
              onChange(
                "copies",
                Math.max(1, form.copies - 1)
              )
            }
          >
            -
          </button>

          <span>{form.copies}</span>

          <button
            type="button"
            onClick={() =>
              onChange(
                "copies",
                form.copies + 1
              )
            }
          >
            +
          </button>
        </div>
      </div>

      <div className="form-group">
        <label className="form-label">
          Tipo de impresión
        </label>

        <label>
          <input
            type="radio"
            name="printType"
            checked={form.printType === "byn"}
            onChange={() =>
              onChange("printType", "byn")
            }
          />
          Blanco y negro
        </label>

        <label>
          <input
            type="radio"
            name="printType"
            checked={form.printType === "color"}
            onChange={() =>
              onChange("printType", "color")
            }
          />
          Color
        </label>
      </div>
    </section>
  );
}
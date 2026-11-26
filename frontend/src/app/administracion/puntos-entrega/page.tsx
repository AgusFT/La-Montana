import { DemoModulePreview } from "@/components/demo-module-preview";

const pointName = "Punto Centro";
const states = ["Sin definir", "Habilitado", "Deshabilitado"] as const;

export default function PointsPage() {
  return <DemoModulePreview area="puntos" title="Disponibilidad de puntos" description="Habilitación temporal de los puntos de entrega">
    <p className="admin-info">Disponibilidad de los puntos de la configuración activa y los nuevos puntos del cambio pendiente, si existen. Esto no cambia las condiciones versionadas.</p>
    <p className="admin-note">Los cambios son inmediatos, no requieren motivo ni fecha de fin y no crean otra versión de configuración. La disponibilidad se comprueba al cotizar, confirmar y aceptar una reprogramación. Deshabilitar un punto no cancela ni cambia automáticamente pedidos ya confirmados.</p>
    <div className="point-list-layout">
      <section>
        <div className="resource-toolbar availability-toolbar">
          <div className="catalog-field point-search"><label htmlFor="availability-search">Buscar punto o dirección</label><input id="availability-search"/></div>
          <div className="catalog-field"><label htmlFor="availability-filter">Estado temporal</label>
            <select id="availability-filter"><option>Todos los estados</option>{states.map(state => <option key={state}>{state}</option>)}</select>
          </div>
        </div>
        <article className="admin-card point-row availability-row">
          <div className="point-location"><span className="point-location-icon" aria-hidden="true">⌖</span>
            <div><h2>{pointName}</h2><p>Pasaje de las Tintas 120</p><small>CENTRO · America/Argentina/Buenos_Aires</small></div>
          </div>
          <div><span className="availability-state state-sin_definir">Sin definir</span><p className="admin-note">El administrador aún no eligió un estado.</p></div>
          <div className="availability-actions">
            <button type="button" className="admin-button">Habilitar {pointName}</button>
            <button type="button" className="admin-button secondary">Deshabilitar {pointName}</button>
          </div>
        </article>
      </section>
      <aside className="admin-card point-summary"><h2>Resumen de disponibilidad</h2>
        <dl>{states.map(state => <div key={state}><dt>{state}</dt><dd>{state === "Sin definir" ? 1 : 0}</dd></div>)}</dl>
        <p className="admin-note">Habilitado indica una decisión temporal. También se necesitan configuración activa, origen, franjas y cupos válidos para ofrecer el punto.</p>
      </aside>
    </div>
    <div className="configuration-actions">
      <button type="button" className="admin-button secondary">Actualizar disponibilidad</button>
      <button type="button" className="admin-button secondary">Editar definiciones en el configurador</button>
      <a className="admin-button secondary" href="/administracion">Volver al dashboard</a>
    </div>
    <details className="edit-details"><summary>Definición del punto de muestra</summary>
      <section className="admin-form admin-card point-editor"><h3>Definición del punto</h3>
        <div className="admin-form-grid">
          {Object.entries({"Código del punto":"CENTRO", "Nombre del punto":pointName, "Calle del punto":"Pasaje de las Tintas",
            "Número del punto":"120", "Localidad del punto":"Ciudad de Buenos Aires", "Provincia del punto":"CABA",
            "Código postal del punto":"1407", "Zona horaria del punto":"America/Argentina/Buenos_Aires"}).map(([label, value]) =>
              <label key={label}>{label}<input defaultValue={value}/></label>)}
        </div>
        <label className="point-references">Referencias para llegar<textarea rows={2}/></label>
        <section className="point-origins"><h3>Sucursales de origen y franjas</h3><div className="admin-form-grid">
          <label>Sucursal de origen<select><option>Elegir sucursal</option></select></label>
          <label>Costo de retiro (ARS)<input type="number"/></label><label>Día<select><option>Elegir día</option></select></label>
          <label>Apertura<input type="time"/></label><label>Cierre<input type="time"/></label><label>Cupo de pedidos<input type="number"/></label>
        </div></section>
        <div className="configuration-actions"><button type="button" className="admin-button secondary">Agregar franja</button>
          <button type="button" className="admin-button">Guardar punto</button></div>
      </section>
    </details>
  </DemoModulePreview>;
}

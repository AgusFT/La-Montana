import { DemoModulePreview } from "@/components/demo-module-preview";

export default function PointsPage() {
  return <DemoModulePreview area="puntos" title="Puntos de entrega" description="Gestión de puntos de retiro y disponibilidad para la aplicación móvil.">
    <section className="admin-card"><h2>Disponibilidad de puntos</h2>
      <div className="admin-form-grid"><label>Buscar punto<input type="search" placeholder="Nombre, dirección o código"/></label>
        <label>Estado<select><option>Todos los estados</option><option>Sin definir</option><option>Habilitado</option><option>Deshabilitado</option></select></label></div>
      <div className="configuration-actions"><button type="button" className="admin-button">Habilitar punto</button>
        <button type="button" className="admin-button secondary">Deshabilitar punto</button></div>
    </section>
    <section className="admin-card"><h2>Definición del punto</h2><div className="admin-form-grid">
      {["Código", "Nombre", "Calle", "Número", "Localidad", "Provincia", "Código postal", "Zona horaria", "Referencias para llegar"].map(label =>
        <label key={label}>{label}<input/></label>)}
    </div><h3>Sucursales de origen y franjas</h3><div className="admin-form-grid">
      <label>Sucursal de origen<select><option>Elegir sucursal</option></select></label>
      <label>Costo de retiro (ARS)<input type="number"/></label><label>Día<select><option>Elegir día</option></select></label>
      <label>Apertura<input type="time"/></label><label>Cierre<input type="time"/></label><label>Cupo de pedidos<input type="number"/></label>
    </div><div className="configuration-actions"><button type="button" className="admin-button secondary">Agregar franja</button>
      <button type="button" className="admin-button">Guardar punto</button></div></section>
  </DemoModulePreview>;
}

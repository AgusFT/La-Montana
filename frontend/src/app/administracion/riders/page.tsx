import { DemoModulePreview } from "@/components/demo-module-preview";

export default function RidersPage() {
  return <DemoModulePreview area="riders" title="Riders" description="Repartidores y asignación de entregas de la aplicación móvil.">
    <section className="admin-card"><h2>Asignación de entregas</h2>
      <div className="admin-form-grid"><label>Repartidor<select><option>Elegir rider</option></select></label>
        <label>Pedido<select><option>Elegir pedido</option></select></label></div>
      <div className="configuration-actions"><button type="button" className="admin-button">Asignar entrega</button></div>
    </section>
  </DemoModulePreview>;
}

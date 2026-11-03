import {notFound} from "next/navigation";
import "./pruebas.css";

export const dynamic="force-dynamic";

const users=[
  ["Administrador","admin@prueba.local","Dashboard global, configuración, sucursales y empleados"],
  ["Producción","produccion@prueba.local","Revisión, producción, calidad y entrega"],
  ["Caja","caja@prueba.local","Cobros, acreditaciones y devoluciones"],
  ["Rider Oeste","rider.oeste@prueba.local","Recorrido asignado, direcciones y pedidos bajo su custodia"],
  ["Rider Centro","rider.centro@prueba.local","Segundo recorrido independiente para comprobar el aislamiento"],
  ["Punto Caballito","punto.caballito.retiro","Pedidos, recepción, entrega y finanzas sólo de Caballito"],
  ["Punto Palermo","punto.palermo.retiro","Pedidos, recepción, entrega y finanzas sólo de Palermo"],
  ["Cliente 1","cliente1@prueba.local","Historial de seis meses y pedidos actuales"],
  ["Cliente 2","cliente2@prueba.local","Pedidos, pagos y destinos de retiro"],
  ["Cliente 3","cliente3@prueba.local","Seguimiento, archivos y correcciones"],
  ["Cliente 4","cliente4@prueba.local","Compras anteriores, entregas y cancelaciones"],
];

export default function TestEnvironmentPage(){
  if(process.env.TEST_ENVIRONMENT!=="true")notFound();
  const password=process.env.TEST_USER_PASSWORD||"MontanaPrueba2026!";
  return <main className="test-home">
    <div className="test-home__header"><span>SEIS MESES DE ACTIVIDAD SIMULADA</span><h1>Un día en La Montaña</h1><p>Entrá como administrador, empleado o cliente y explorá una imprenta con historia: pedidos anteriores, trabajo en curso, entregas y movimientos de caja. Todos los datos, documentos y pagos son ficticios.</p><a href="/acceso">Ingresar a la aplicación →</a></div>
    <section><h2>Cuentas disponibles</h2><p className="test-password"><strong>Contraseña común:</strong> <code>{password}</code></p><div className="test-account-grid">{users.map(([role,email,purpose])=><article key={email}><span>{role}</span><h3>{email}</h3><p>{purpose}</p></article>)}</div></section>
    <section className="test-account-grid" aria-label="Datos del escenario">
      <article><span>Configuración</span><h2>3 históricas + 1 activa</h2><p>Versiones completas con cambios de modelo, seña, tarifas, preparación y entrega. Abrí el historial y compará sus módulos.</p></article>
      <article><span>Catálogo comercial</span><h2>27 variantes de papel</h2><p>A3, A4, A5, A6, Carta, Folio y Legal. Papel común de 75, 80 y 90 g/m² e ilustración de 130, 170 y 250 g/m² en A3 y A4. Cada revisión contiene 54 tarifas BN/color, simple y doble faz.</p></article>
      <article><span>Pedidos y reparto</span><h2>Más de 120 pedidos</h2><p>Seis meses de compras, recorridos históricos y dos riders con viajes actuales. Incluye custodia, llegada a kioscos, entrega, incidencias y PDF de muestra.</p></article>
      <article><span>Caja</span><h2>Cobros y reintegros</h2><p>Efectivo, transferencias acreditadas, anticipos, saldos y devoluciones vinculados con sus pedidos. También hay una transferencia informada pendiente de verificación.</p></article>
    </section>
    <section className="test-data"><h2>Recorrido sugerido</h2><ol><li>Como administrador, revisá Entregas pendientes, seleccioná pedidos preparados y asigná un rider al iniciar el reparto.</li><li>Ingresá como Rider Oeste o Rider Centro para comprobar que cada uno ve únicamente su recorrido, destinos y pedidos.</li><li>Como punto, recibí los pedidos que trae el rider, buscá al cliente y entregá con código. Consultá cobros e historial.</li><li>En configuración, compará V1, V2 y V3 con V4; en caja, abrí un cobro, un anticipo y una devolución.</li><li>Como cliente, consultá tus pedidos, PDF, pagos y timeline. Después creá un pedido nuevo para continuar la experiencia.</li></ol><p>Casa Matriz Floresta, sucursales Medicina y La Plata; puntos Caballito y Palermo como destinos independientes; envíos a domicilio y recorridos agrupados. El escenario se conserva al reiniciar.</p></section>
  </main>;
}

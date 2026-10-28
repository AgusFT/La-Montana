import {notFound} from "next/navigation";
import "./pruebas.css";

export const dynamic="force-dynamic";

const users=[
  ["Administrador","admin@prueba.local","Dashboard global, configuración, sucursales y empleados"],
  ["Producción","produccion@prueba.local","Revisión, producción, calidad y entrega"],
  ["Caja","caja@prueba.local","Cobros, acreditaciones y devoluciones"],
  ["Cliente 1","cliente1@prueba.local","Crear y seguir pedidos"],
  ["Cliente 2","cliente2@prueba.local","Probar otro historial y destino"],
  ["Cliente 3","cliente3@prueba.local","Pruebas simultáneas"],
  ["Cliente 4","cliente4@prueba.local","Pruebas de cancelación y correcciones"],
];

export default function TestEnvironmentPage(){
  if(process.env.TEST_ENVIRONMENT!=="true")notFound();
  const password=process.env.TEST_USER_PASSWORD||"MontanaPrueba2026!";
  return <main className="test-home"><div className="test-home__header"><span>ENTORNO AISLADO</span><h1>La Montaña · Pruebas funcionales</h1><p>Estos datos son ficticios. Podés modificarlos y reiniciar el escenario sin afectar la instalación normal.</p><a href="/acceso">Ingresar a la aplicación →</a></div><section><h2>Cuentas disponibles</h2><p className="test-password"><strong>Contraseña común:</strong> <code>{password}</code></p><div className="test-account-grid">{users.map(([role,email,purpose])=><article key={email}><span>{role}</span><h3>{email}</h3><p>{purpose}</p></article>)}</div></section><section className="test-data"><h2>Escenario precargado</h2><ul><li>Casa Matriz Floresta y sucursales Medicina y La Plata.</li><li>Puntos de entrega Caballito y Palermo, separados de las sucursales.</li><li>Catálogo A4, papeles, impresión color/BN, anillado y abrochado.</li><li>Configuración operativa activa, impresora de prueba, horarios, retiro y transferencia.</li></ul><p>Los pedidos no se inventan: ingresá como cliente y crealos mediante el flujo real para probar carga, cotización, confirmación, revisión y seguimiento.</p></section></main>;
}

import type { Capitulo } from "./tipos";

export const inicioDemo: Capitulo = {
  id: "demo-inicio", titulo: "Usar la demo en este equipo",
  temas: [
    {id:"demo-instalar", titulo:"Elegir la carpeta y abrir la aplicación",
      paraQue:"La Montaña Demo usa tu navegador y guarda el trabajo en tu equipo.",
      pasos:["Abrí el instalador. Revisá la explicación de los componentes y del lugar donde se guardan los datos.",
        "En una instalación nueva, conservá la carpeta propuesta o usá Examinar para elegir otra. Si querés abrir la demo desde el escritorio, seleccioná el acceso directo.",
        "Al iniciar, mantené abierto el panel mientras prepara los servicios. Cuando abre el navegador, queda minimizado. Podés volver a abrirlo desde la barra de tareas."],
      resultado:"Vas a ver el selector de Administrador, Empleado y Cliente. Para apagar la demo, volvé al panel y confirmá el cierre."},
    {id:"demo-recorrido", titulo:"Retomar el tutorial y consultar el manual",
      paraQue:"Seguí el mismo pedido entre tres roles o consultá directamente un tema.",
      pasos:["Elegí un rol y pulsá Iniciar Tutorial al principio del menú. El índice de la izquierda muestra el paso actual y qué necesitás para avanzar.",
        "Completá los campos que señala la guía. Confirmá el cambio de rol cuando el siguiente paso corresponda a otra persona.",
        "Podés pausar, salir y volver a la demo. Retomá el tutorial para seguir con el pedido guardado. Si un PDF todavía no llegó a cargarse, elegilo otra vez."],
      resultado:"El pedido y las operaciones confirmadas siguen disponibles en los módulos de la imprenta. Podés volver a este manual desde cualquiera de los tres roles."},
  ],
};

export function disponibilidad(capitulo: string, tema: string) {
  if (capitulo === "admin-puesta-en-marcha") return "Edición completa. Para instalar la demo consultá «Usar la demo en este equipo».";
  if (capitulo === "admin-web" || capitulo === "admin-red" || ["admin-empleado","admin-soporte","op-reparto","op-domicilio","op-punto","op-finanzas-punto","op-conciliar-puntos","op-acuerdos"].includes(tema))
    return "Referencia de la edición completa. En la demo se aplican los permisos y opciones que muestra el menú.";
  if (capitulo.startsWith("admin-")) return "Recorrido de Administración.";
  if (capitulo.startsWith("op-")) return "Recorrido del personal de la imprenta según sus permisos. En la demo la impresión es simulada.";
  if (capitulo.startsWith("cli-")) return "Recorrido del cliente.";
  return "Disponible para Administrador, Empleado y Cliente.";
}

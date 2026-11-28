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
    {id:"demo-recorrido", titulo:"Elegir, pausar, retomar o reiniciar un tutorial",
      paraQue:"Practicá un rol o seguí un pedido que pasa por toda la imprenta.",
      pasos:["Entrá con un rol y pulsá «Empezar tutorial» al principio del menú. Buscá la tarjeta de Administrador, Cliente o Flujo completo y pulsá «Iniciar». Si ya practicaste ese recorrido, retomás donde lo dejaste.",
        "En el índice elegí un paso y leé el cuadro que apunta al control. Podés usar «Minimizar» para despejar la pantalla y volver a mostrar la explicación cuando la necesites.",
        "Para interrumpir un ejercicio usá «Pausar». Al volver, seleccioná el mismo recorrido. Los campos guardados y el PDF ya cargado siguen disponibles. Si solo habías elegido el archivo en tu equipo, volvé a seleccionarlo.",
        "Para empezar de nuevo, pulsá «Reiniciar datos» en la tarjeta del recorrido o «Reiniciar tutorial» dentro del tutorial. Comprobá el nombre y confirmá. Volvés al primer paso con los campos de práctica vacíos. Los otros tutoriales, los pedidos, los cobros y las tarifas guardadas se conservan. Si hay una preparación abierta, podés elegir volver a usarla."],
      resultado:"Podés abrir este manual con cualquiera de los tres roles y buscar un tema específico."},
    {id:"demo-administrador", titulo:"Tutorial del lado administrador",
      paraQue:"Aprendé a relacionar los precios y las opciones de trabajo con las cotizaciones de la imprenta.",
      pasos:["Empezá en el tablero y consultá qué pedidos se encuentran en cada estado.",
        "En «Servicios y precios» revisá el papel y el servicio, editá una tarifa y aplicala al borrador. Comprobá qué precio se usaría para el trabajo.",
        "Recorré las fases del Configurador y revisá la simulación. Podés completar el ejercicio en borrador. Si decidís publicar, revisá el alcance y confirmá el cambio porque las próximas cotizaciones usarán la nueva configuración.",
        "Explorá las opciones disponibles de la página web local, las sucursales, la operación y los cupos. Los candados del menú identifican los límites de la demo."]},
    {id:"demo-cliente", titulo:"Tutorial del lado cliente",
      paraQue:"Conocé el precio y las condiciones del trabajo antes de comprometer un pedido.",
      pasos:["Abrí el recorrido del cliente. Elegí el afiche de práctica o un PDF de tu equipo. Si ya tenés una preparación abierta, revisá qué archivo contiene y elegí si querés usarlo.",
        "Configurá las opciones de impresión, revisá las páginas y comprobá las condiciones de entrega y pago.",
        "Al terminar podés conservar la preparación o confirmar el pedido. La confirmación ocupa una de las veinte plazas de la demo. En «Mis pedidos» podés seguir sus etapas y consultar archivos y pagos."]},
    {id:"demo-flujo-completo", titulo:"Tutorial de flujo completo",
      paraQue:"Seguí el mismo trabajo desde el precio por hoja hasta la entrega y el cierre.",
      pasos:["Como Administrador, consultá la tarifa vigente y confirmá la lectura. Para seguir, el cuadro señala «Cambiar rol» y ofrece el botón para continuar como Cliente. Usá el botón del cuadro.",
        "Prepará el PDF, cotizá y confirmá el pedido como Cliente. Seguí las indicaciones de cambio de rol para registrar el cobro y acompañar la revisión, la impresión simulada y el control de calidad.",
        "En la entrega obtené el código como Cliente. Volvé a Empleado desde la guía, validá el código, registrá la recepción y cerrá el pedido."],
      resultado:"Las operaciones confirmadas quedan en el pedido para que puedas volver a consultarlas."},
  ],
};

export function disponibilidad(capitulo: string, tema: string) {
  if (capitulo === "admin-puesta-en-marcha") return "Edición completa. Para instalar la demo consultá «Usar la demo en este equipo».";
  if (capitulo === "admin-web") return "La demo permite preparar y publicar la página en este equipo. Consultá la disponibilidad del alojamiento externo con YG Soluciones.";
  if (capitulo === "admin-red" || ["admin-empleado","admin-soporte","op-reparto","op-domicilio","op-punto","op-finanzas-punto","op-conciliar-puntos","op-acuerdos"].includes(tema))
    return "Referencia de la edición completa. En la demo se aplican los permisos y opciones que muestra el menú.";
  if (capitulo.startsWith("admin-")) return "Recorrido de Administración.";
  if (capitulo.startsWith("op-")) return "Recorrido del personal de la imprenta según sus permisos. En la demo la impresión es simulada.";
  if (capitulo.startsWith("cli-")) return "Recorrido del cliente.";
  return "Disponible para Administrador, Empleado y Cliente.";
}

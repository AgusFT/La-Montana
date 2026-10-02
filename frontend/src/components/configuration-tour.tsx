"use client";

import {GuidedTour,type TourStep} from "./guided-tour";

type Props={phase:number;owner:string;firstSetup:boolean;ready:boolean;hasDraft:boolean;hasScheduled:boolean;hasActive:boolean;conditional:boolean;printerEditor?:boolean};

const phaseTours:Record<number,{steps:TourStep[];completion:{title:string;description:string}}>= {
  3:{steps:[
    {target:"config-tour-payments",title:"Elegí los medios de pago",description:"Habilitá los medios que puede usar el cliente. La condición elegida en la fase 2 puede exigir acreditación previa o limitar el efectivo."},
    {target:"config-tour-deposit",title:"Definí la regla de seña",description:"Según el modelo, podés exigir una seña y configurar su condición, porcentaje o importe. Si elegiste pago previo total, la seña no se aplica."},
    {target:"config-tour-payments-actions",title:"Guardá las reglas",description:"Guardar conserva estas decisiones en el borrador. El próximo paso se habilita cuando los requisitos quedan completos; todavía no cambia la configuración activa."},
  ],completion:{title:"Pagos preparados",description:"Revisá las condiciones antes de continuar. La simulación es opcional y no registra cobros ni pedidos."}},
  4:{steps:[
    {target:"config-tour-printers",title:"Declarar impresoras",description:"Registrá las impresoras y sus capacidades reales. La compatibilidad se comprueba contra las sucursales, formatos y servicios habilitados."},
    {target:"config-tour-assignment",title:"Elegí la asignación",description:"Definí el método de asignación y los servicios ofrecidos por sucursal. La asignación automática sigue en construcción; elegí una opción disponible."},
    {target:"config-tour-resources-actions",title:"Continuá cuando esté guardado",description:"Los cambios de recursos se guardan en el borrador. Revisá que haya equipos operativos y asignaciones compatibles antes de seguir."},
  ],completion:{title:"Recursos revisados",description:"Las impresoras y servicios declarados se volverán a comprobar en la revisión integral y al aplicar la configuración."}},
  5:{steps:[
    {target:"config-tour-schedule",title:"Definí cuándo produce la sucursal",description:"Este calendario parte del horario guardado en la ficha de la sucursal y puede ajustarse en el borrador. Define las horas operativas que se cuentan para preparar el pedido. Se pueden recibir pedidos fuera de horario si el sistema está habilitado; el cálculo comienza o continúa en la próxima apertura."},
    {target:"config-tour-pickup",title:"Definí cuándo se retiran los pedidos",description:"Las franjas de retiro indican cuándo el cliente puede retirar un pedido listo en esta sucursal; no son otro horario de producción. Podés atender retiros fuera del horario de producción si hay personal para entregar pedidos listos. Por ejemplo: lunes producción de 09:00 a 18:00 y retiro de 18:00 a 20:00; miércoles sin producción y retiro de 11:00 a 13:00. Verás el aviso Producción cerrada · retiros habilitados. Estas franjas no suman horas de preparación. El cupo cuenta pedidos completos; la disponibilidad se confirma al crear el pedido."},
    {target:"config-tour-times",title:"Indicá los tiempos",description:"Cargá la preparación y el traslado estimados. Estas cifras ayudan a calcular fechas, pero no prometen tiempos de transportistas externos."},
    {target:"config-tour-delivery",title:"Elegí las modalidades",description:"Habilitá las entregas que vas a ofrecer. Configurá puntos o zonas solo si elegiste esas modalidades."},
    {target:"config-tour-delivery-validation",title:"Comprobá los mínimos",description:"Guardá la fase y consultá la validación. Las simulaciones son opcionales; para seguir sí deben estar completos los requisitos de horarios y entrega."},
  ],completion:{title:"Entrega preparada",description:"Al continuar se revisará el borrador completo con los recursos y la referencia comercial vigente."}},
  6:{steps:[
    {target:"config-tour-review-summary",title:"Leé el resumen general",description:"Acá se reúnen las decisiones de las fases anteriores y los servicios y precios vigentes. Los precios tienen su propio ciclo de configuración."},
    {target:"config-tour-conflicts",title:"Resolvé los conflictos",description:"Los bloqueos impiden aplicar la versión. Corregí cada fase indicada y volvé a actualizar la revisión. Las advertencias deben revisarse expresamente."},
    {target:"config-tour-review-actions",title:"Continuá a la aplicación",description:"La simulación de un recorrido es opcional. Continuar a la fase 7 todavía no activa nada: allí elegirás el momento y autorizarás la operación."},
  ],completion:{title:"Revisión entendida",description:"Cuando no haya bloqueos y hayas revisado las advertencias, podrás decidir si activás ahora o programás la nueva versión."}},
  7:{steps:[
    {target:"config-tour-activation-facts",title:"Revisá la versión antes de aplicarla",description:"Comprobá qué versión vas a publicar y si existen bloqueos. Activar ahora reemplaza la vigente; programar cierra este borrador y conserva la actual hasta la fecha elegida."},
    {target:"config-tour-phase-help",title:"La aplicación exige seguridad",description:"Después de revisar el momento y el motivo, se pedirá tu contraseña y un código de un solo uso por correo. El servidor vuelve a validar las condiciones antes de aplicar."},
  ],completion:{title:"Aplicación bajo tu control",description:"Solo la confirmación autorizada cambia el estado. Los pedidos ya aceptados conservan sus condiciones anteriores."}},
  8:{steps:[
    {target:"config-tour-phase-content",title:"Consultá el historial",description:"Las versiones y los intentos se conservan para comparar qué cambió y cuándo. Consultar el detalle no modifica la versión vigente."},
    {target:"config-tour-phase-help",title:"Usá una versión como base",description:"Esa acción crea un nuevo borrador. Antes de aplicarlo, repasá las fases 2 a 6 y comprobá nuevamente impresoras, módulos y condiciones actuales."},
  ],completion:{title:"Historial disponible",description:"Podés volver a la configuración actual desde esta pantalla. Recuperar una versión nunca la activa por sí solo."}},
};

export function ConfigurationTour({phase,owner,firstSetup,ready,hasDraft,hasScheduled,hasActive,conditional,printerEditor=false}:Props){
  let steps:TourStep[],completion:{title:string;description:string};
  if(phase===1){
    steps=[
      {target:"config-tour-current",title:"Esta es la configuración vigente",description:hasActive?"La versión activa rige para los nuevos pedidos. Los pedidos ya confirmados conservan sus condiciones; una cotización pendiente se revalida al confirmarla.":"Todavía no hay una versión operativa activa. Completá y aplicá el recorrido para comenzar a usarla."},
      {target:"config-tour-scheduled",title:"Revisá si hay una activación programada",description:hasScheduled?"La versión programada está cerrada y no admite edición ni un segundo borrador. La versión vigente sigue funcionando hasta que se complete la activación.":"No hay una versión esperando una fecha futura. Al terminar la revisión podrás activar en el momento o programar el cambio."},
      {target:"config-tour-draft",title:"Comprobá el estado de edición",description:hasDraft?"Hay un único borrador en curso. Podés retomarlo y guardarlo por fases sin alterar la versión vigente.":hasScheduled?"Con una versión programada no podés empezar otra edición. Primero resolvé esa programación.":"No hay un borrador abierto. Podés comenzar una configuración nueva."},
      {target:hasScheduled?"config-tour-schedule-action":hasDraft?"config-tour-resume":"config-tour-start",title:hasScheduled?"Gestioná la programación":hasDraft?"Retomá el borrador":"Comenzá con un borrador",description:hasScheduled?"Consultá la fecha y las acciones disponibles para esta programación. Cancelarla requiere la autorización correspondiente.":hasDraft?"Continuá en la fase pendiente; el avance guardado permanece hasta que lo apliques o canceles.":"Crear borrador prepara una versión vacía y abre el modelo operativo en la fase 2. Guardar una fase no activa el sistema."},
      {target:"config-tour-phases",title:"Avanzá fase por fase",description:"El menú lateral muestra las ocho fases. Durante la instalación se habilitan en orden; el historial conserva lo que se aplicó o canceló."},
    ];
    completion={title:"Ya conocés el punto de partida",description:"Entrá a la edición cuando quieras. Cada fase tiene su propia guía en el botón ? y podés volver a abrirla sin modificar tus datos."};
  }else if(phase===2){
    steps=[
      {target:"config-tour-model",title:"Elegí el modelo operativo",description:"Control manual exige revisión y decisión humana para todos los pedidos. Control condicional aplica una condición admitida junto con las validaciones técnicas. La automatización certificada aún no está disponible."},
      ...(conditional?[{target:"config-tour-criterion",title:"Definí la condición",description:"Elegí la condición de aprobación que corresponda. Los importes, medios y porcentajes se completan en la fase 3; el sistema no asigna valores por defecto."}]:[]),
      {target:"config-tour-model-actions",title:"Guardá antes de continuar",description:"La selección queda en el borrador. Si después cambiás el modelo o la condición, deberás revisar nuevamente sus reglas de pago."},
    ];
    completion={title:"Modelo preparado",description:"El siguiente paso define los medios de pago y la seña compatibles con esta decisión. Guardar no activa la configuración."};
  }else if(phase===4&&printerEditor){
    steps=[
      {target:"config-tour-printer-details",title:"Identificá la impresora y sus capacidades",description:"Ingresá el nombre, la sucursal y las capacidades reales de color, doble faz y cantidad de hojas. Al editar una impresora existente, su sucursal e identidad se conservan. Estos datos pertenecen al borrador y todavía no cambian la configuración activa."},
      {target:"config-tour-printer-formats",title:"Seleccioná los formatos compatibles",description:"Marcá solamente los tamaños de papel que esta impresora admite. Necesitás al menos un formato. Seleccionar todos no certifica la compatibilidad física del equipo."},
      {target:"config-tour-printer-actions",title:"Guardá y volvé a la lista",description:"Guardar impresora conserva sus datos en el borrador. Después configurá la asignación y los servicios de la sucursal. Volver a la lista no guarda los cambios del formulario."},
    ];
    completion={title:"Impresora preparada para guardar",description:"La guía no guarda ni activa el equipo. Revisá el formulario y usá Guardar impresora cuando sus datos estén completos."};
  }else{
    const tour=phaseTours[phase];if(!tour)return null;
    steps=tour.steps;completion=tour.completion;
  }
  const context=phase===4&&printerEditor?":printer-editor":"";
  const revision=phase===5?"v3":"v1";
  return <GuidedTour key={`${owner}:${phase}${context}`} steps={steps} completion={completion} storageKey={`lamontana:configuration-tour:phase-${phase}${context}:${revision}:${owner}`} autoStart={firstSetup&&ready&&phase<=7} helpLabel={`Guía de la fase ${phase}${context?" · formulario de impresora":""}`}/>;
}

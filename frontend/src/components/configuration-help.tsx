//#region ENCABEZADO · src/components/configuration-help.tsx
/*
 * ========================================================================
 * ARCHIVO: src/components/configuration-help.tsx
 * ========================================================================
 * FUNCIÓN
 * Reúne las explicaciones pedagógicas de las fases del configurador operativo y las presenta junto
 * al paso correspondiente.
 *
 * ------------------------------------------------------------------------
 * COMPONENTES, FUNCIONES Y MÉTODOS DECLARADOS
 * Incluye funciones nombradas, auxiliares anidadas y callbacks asignados a un nombre. Ámbito ::
 * firma identifica funciones internas; el retorno se muestra cuando está declarado explícitamente.
 * - [export] ConfigurationHelp({phase}: {phase:number})
 *   Componente de interfaz.
 *
 * ------------------------------------------------------------------------
 * TIPOS DECLARADOS
 * - No declara tipos con nombre.
 *
 * ------------------------------------------------------------------------
 * VALORES DE MÓDULO Y REEXPORTACIONES
 * - help [const].
 * ========================================================================
 */
//#endregion

const help=[
  ["Primero prepará un borrador","Un borrador es una configuración de trabajo guardada: podés completarla en varias sesiones sin cambiar cómo funciona la imprenta. Solo la activación del paso 7 aplica sus reglas a los nuevos pedidos.","Creá las sucursales y cargá servicios, formatos, papeles y precios desde el menú. Después creá un borrador o retomá el existente. Si ya hay una versión activa, seguirá funcionando mientras preparás la siguiente."],
  ["Decidí quién aprueba los pedidos","Elegí si un responsable revisa cada pedido o si la aprobación depende de una condición. Esta decisión determina qué reglas de pago completarás después.","Seleccioná una opción según el funcionamiento real de tu negocio y guardala. Guardar permite continuar al paso 3, pero todavía no cambia la operación."],
  ["Definí cuándo y cuánto debe pagar el cliente","La seña es un adelanto opcional del total. Elegí si querés usarla. El umbral por monto determina la revisión humana aunque no cobres seña. Los importes y porcentajes deben ser decisiones de tu imprenta.","Completá y guardá las opciones; después probá un importe en el simulador. La simulación es una prueba: no cobra ni crea pedidos. Después configurá los recursos."],
  ["Relacioná servicios, sucursales e impresoras","La asignación decide dónde se podrá producir cada trabajo. Antes necesitás sucursales, servicios y formatos cargados en sus secciones del menú.","Registrá las impresoras con su capacidad real, asigná los servicios por sucursal y elegí el método de asignación. Guardá cada edición antes de continuar a horarios y entrega."],
  ["Indicá cuándo y cómo se entregan los trabajos","Los horarios, tiempos de preparación y modalidades permiten calcular fechas y capacidad. Los puntos de retiro y las zonas de envío solo son necesarios para las modalidades que ofrezcas.","Completá los horarios y elegí las modalidades. Los botones para abrir puntos o zonas guardan primero tus cambios pendientes; sus formularios también se guardan en este borrador. Usá la simulación y luego revisá el resumen."],
  ["Revisá el conjunto antes de aplicarlo","Esta revisión comprueba que las decisiones de los pasos anteriores sean compatibles. Cada observación indica qué falta y dónde corregirlo.","Resolvé los errores, revisá las advertencias y probá la simulación. La revisión no activa nada: cuando esté lista, continuá a aplicación y seguridad."],
  ["Aplicá las reglas con una autorización explícita","Podés activar ahora o programar una fecha. La contraseña y el código recibido por correo autorizan esta operación concreta; guardar los pasos anteriores no la autoriza.","Revisá la versión y el momento de aplicación. Si querés salir durante una autorización, usá la opción de cancelar o volver del formulario para revocarla. Confirmá el resultado antes de seguir."],
  ["Consultá qué cambió y recuperá una base","El historial conserva versiones y resultados. Usar una versión como base crea otro borrador: no reemplaza automáticamente la configuración activa.","Compará versiones para entender sus diferencias. Si creás una copia, revisá y guardá nuevamente sus pasos antes de activarla. Una reversión también requiere revisión y autorización."],
];
export function ConfigurationHelp({phase}:{phase:number}){
  const [title,purpose,next]=help[phase-1]??help[0];
  return <section className="configuration-help" aria-label="Guía de este paso"><h2>{title}</h2><p>{purpose}</p><p><strong>Cómo seguir:</strong> {next}</p></section>;
}

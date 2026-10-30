# Aviso de transferencia del cliente

En el pedido actual, «Informar transferencia» aparece en verde junto a «Cancelar pedido». La cancelación conserva sus condiciones; el aviso también puede estar disponible después de comenzar la producción si el servidor lo permite. Los movimientos, saldos y comprobantes anteriores siguen en «Ver pagos y comprobantes».

El botón abre una ventana con el importe sugerido y editable: seña o pago previo pendiente según la oferta y la configuración vigente, o saldo cuando esos requisitos ya están cubiertos. Se usan centavos exactos y se admite coma o punto decimal. Otro aviso pendiente no se cuenta como dinero acreditado.

Se puede confirmar sin referencia bancaria, descripción ni comprobante. La aplicación genera un identificador de aviso web y un texto descriptivo para el registro existente; no inventa una referencia bancaria. El servidor conserva el usuario y la fecha del evento. Informar no recibe ni aplica dinero: la imprenta debe verificar la transferencia.

El comprobante es opcional, privado y PDF, hasta 10 MiB. Se adjunta al aviso recién creado mediante los endpoints existentes, con los mismos permisos e inspección de archivos. También puede agregarse después, sin descripción obligatoria. Si el archivo se rechaza, el aviso se conserva y la interfaz informa el resultado del archivo por separado.

Los reintentos conservan la operación del aviso, la operación del comprobante y el identificador de archivo. Si el aviso ya se guardó, una carga fallida no vuelve a informar el importe. Ante una respuesta incierta se bloquea la salida y se ofrece recuperar la misma operación. Una falla confirmada permite finalizar el aviso y retomar el comprobante desde los pagos.

Validación: `pnpm typecheck`, `pnpm build`, `pnpm test:client-transfer` y `pnpm test:payment-receipt`. Las pruebas cubren cálculo de importes, ausencia de acreditación automática, asociación del comprobante, respuestas perdidas en las tres escrituras, validación de archivos y rechazo del PDF. La comprobación visual usa componentes reales con API simulada; no sustituye una prueba integrada de almacenamiento, antivirus y base de datos.

# Boticario

Sistema de gestion para farmacia: inventario por unidad base y presentaciones,
lotes con FEFO, punto de venta con lector de codigo de barras, compras,
clientes/proveedores, inyectologia, auditoria y dashboard.

Este repositorio se construye **por fases** (ver "Estado del proyecto" abajo).
La arquitectura y el modelo de datos completos se definieron antes de escribir
codigo; si necesitas el detalle de diseno (ERD, flujos de compra/venta/
inyectologia, estrategia de concurrencia y auditoria), pide el documento de
Fase 1 — este README solo cubre como levantar y navegar el codigo.

## Estructura del repositorio

```
FarmaApp/
├── backend/     # API REST — Java 21 + Spring Boot 3 + PostgreSQL
├── frontend/    # SPA — Angular 18 + PrimeNG
└── docker-compose.yml
```

## Requisitos

- Docker y Docker Compose (para levantar todo con un solo comando), **o**
- JDK 21 + PostgreSQL 16 (para correr el backend directamente desde IntelliJ)
- Node.js 20+ y npm (para correr el frontend con `ng serve`)

## Opcion 1 — Levantar todo con Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Esto levanta:

| Servicio  | URL                              |
|-----------|-----------------------------------|
| Frontend  | http://localhost:4200             |
| Backend   | http://localhost:8080/api         |
| Swagger   | http://localhost:8080/docs        |
| PostgreSQL| localhost:5432                    |

## Opcion 2 — Desarrollo desde IntelliJ (backend) + `ng serve` (frontend)

1. **Base de datos**: la forma mas simple es levantar solo Postgres con Docker:
   ```bash
   docker compose up db
   ```
2. **Backend**: abre la carpeta `backend/` como proyecto Maven en IntelliJ
   (`File > Open`, selecciona `backend/pom.xml`). IntelliJ resuelve las
   dependencias automaticamente. Verifica que el SDK del proyecto sea JDK 21
   (`File > Project Structure > Project`).
   Configura las variables de entorno de la Run Configuration de
   `BoticarioApplication` con los valores de `.env.example` (al menos
   `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`) o simplemente deja los
   valores por defecto de `application.yml`, que ya apuntan a
   `localhost:5432/boticario`.
   Ejecuta `BoticarioApplication`. Flyway crea el esquema y los datos semilla
   automaticamente en el primer arranque.
3. **Frontend**: en una terminal aparte:
   ```bash
   cd frontend
   npm install
   npm start
   ```
   Se sirve en http://localhost:4200 y ya apunta a `http://localhost:8080/api`
   (`src/environments/environment.ts`).

## Usuario administrador inicial (solo desarrollo)

Al arrancar el backend por primera vez (tabla `users` vacia), se crea
automaticamente:

```
usuario:     admin
contrasena:  Admin123!
```

Cambia estas credenciales (variables `SEED_ADMIN_USERNAME` /
`SEED_ADMIN_PASSWORD` / `SEED_ADMIN_EMAIL`, o el usuario mismo una vez dentro)
antes de usar el sistema fuera de un ambiente de desarrollo local.

## Variables de entorno

Ver `.env.example` para la lista completa con su valor por defecto. Ninguna
credencial ni secreto esta hardcodeado en el codigo fuente.

## Pruebas

```bash
cd backend
mvn test
```

Las pruebas de integracion (`*IT.java`) usan Testcontainers y levantan un
PostgreSQL real en Docker — Docker debe estar corriendo para ejecutarlas.

## Estado del proyecto (desarrollo por fases)

- [x] **Fase 1** — Arquitectura, modelo de datos y flujos (aprobada).
- [x] **Fase 2** — Backend base, Docker Compose, seguridad JWT, modulo de
      usuarios/roles/permisos, login funcional de punta a punta con el
      frontend (pantalla de login + dashboard placeholder protegido).
- [x] **Fase 3** — Catalogo: productos, categorias, laboratorios, principios
      activos, unidades, presentaciones con factor de conversion y codigos
      de barras (uno por presentacion). Historial de precios. Pantalla de
      Productos en el frontend (listado con busqueda/filtros, alta, detalle
      con gestion de presentaciones y precios).
- [x] **Fase 4** — Inventario: stock por producto en unidad base, lotes con
      vencimiento, kardex inmutable, ajustes manuales (positivos y
      negativos/merma/vencimiento con lote obligatorio), alertas de stock
      bajo y de vencimiento, motor FEFO (`StockAllocationService`) listo
      para que compras/ventas/inyectologia lo consuman en fases siguientes,
      con bloqueo pesimista para concurrencia. Pantalla de Inventario en el
      frontend (alertas, kardex por producto, ajuste manual).
- [x] **Fase 5** — Compras: proveedores (dato maestro minimo, se amplia en
      la Fase 7), ciclo de vida BORRADOR -> RECIBIDA/ANULADA, conversion
      automatica de la presentacion comprada a unidad base, integracion con
      StockAllocationService al recibir (abre/suma lotes, deja el
      movimiento COMPRA en el kardex, actualiza el costo de compra
      vigente), y devolucion a proveedor (parcial o total, contra el lote
      exacto que la compra origino). Pantallas de Proveedores y Compras en
      el frontend (listado, alta con items dinamicos y conversion de
      unidades visible antes de guardar, detalle con recibir/anular/
      devolver).
- [x] **Fase 6** — POS / ventas: registro atomico (venta + descuento de
      stock en una sola transaccion, sin estado borrador), consumo FEFO
      via `StockAllocationService`, escaneo de codigo de barras o busqueda
      manual de producto/presentacion, pagos divididos (varias formas de
      pago por venta, efectivo cubre el vuelto, tarjeta/transferencia
      nunca superan el total), descuento por item autorizado solo con el
      permiso `SALE_APPLY_DISCOUNT` (CAJERO vende pero no puede aplicar
      descuento), advertencia (no bloqueo) cuando la venta incluye
      productos que requieren receta, y anulacion que repone exactamente
      los lotes consumidos (`DEVOLUCION_VENTA`) sin reescribir el kardex
      original. Pantallas de Punto de venta (POS), listado de Ventas y
      detalle con anulacion en el frontend.
- [x] **Fase 7** — Clientes y proveedores: dato maestro de clientes
      (documento opcional pero unico cuando se informa, contacto), venta
      ligada a un cliente real desde el POS (customerId, con snapshot
      inmutable del nombre) sin perder la venta de mostrador anonima que
      ya existia, historial de compras por cliente, y enriquecimiento del
      dato maestro de proveedor (persona de contacto, dias de credito
      pactados informativos, notas). Deliberadamente sin fiado/credito ni
      cuentas por pagar todavia — ver la seccion de esta fase para el
      detalle de esa decision. Pantallas de Clientes (listado + detalle
      con historial) y selector de cliente en el POS en el frontend.
- [x] **Fase 8** — Inyectologia: registro de servicios de aplicacion
      (codigo propio INY-000001, paciente ligado a un cliente real con
      snapshot del nombre o nombre libre si no se registra), cobro con
      tarifa simple (un valor + una forma de pago), anulacion con permiso
      separado de quien registra (INYECTOLOGIA puede aplicar pero no
      anular su propio cobro). Decision confirmada: el medicamento
      siempre lo trae el paciente — este modulo NUNCA descuenta
      inventario; si el medicamento se compra en la misma farmacia, eso
      es una venta normal del POS que se puede anotar (opcional) en el
      registro. Pantallas de listado, registro y detalle en el frontend.
- [x] **Fase 9** — Dashboard y reportes: resumen de ventas (total,
      cantidad de ventas, ticket promedio, descuentos, desglose por forma
      de pago) excluyendo siempre las ventas anuladas, ranking de
      productos mas vendidos por ingresos, y resumen de alertas de
      inventario (stock bajo, lotes vencidos, lotes por vencer) —
      reutilizando integramente la logica ya existente de Ventas e
      Inventario, sin nuevas tablas ni migraciones. Selector de rango
      (hoy / ultimos 7 dias / ultimos 30 dias / personalizado) en el
      frontend. Deliberadamente solo en pantalla, sin exportacion a
      Excel/CSV todavia.
- [x] **Fase 10** — Auditoria completa (aspecto AOP sobre 29 metodos de
      escritura en los 9 modulos de negocio, usando la tabla audit_logs que
      existia desde la Fase 1 pero nunca se habia usado), endurecimiento de
      seguridad (cabeceras HTTP, logging real de errores no controlados, y
      la correccion de un hallazgo critico: faltaba @EnableMethodSecurity,
      asi que ningun @PreAuthorize se aplicaba desde la Fase 2 — ver la
      seccion de esta fase para el detalle), pruebas adicionales (matriz de
      autorizacion por rol, ademas de las pruebas de auditoria y
      seguridad), y esta actualizacion final de documentacion. Cierra el
      roadmap de 10 fases.

## Catalogo (Fase 3) — como probarlo

Un producto de ejemplo ("Ibuprofeno 400 mg") se carga automaticamente por
migracion, con sus 3 presentaciones y codigos de barras — util para probar
el flujo de escaneo sin dar de alta nada primero:

| Presentacion | Codigo de barras | Factor | Precio    |
|--------------|-------------------|--------|-----------|
| Pastilla     | 770000000011      | 1      | $500      |
| Tableta (10) | 770000000028      | 10     | $4.500    |
| Caja (100)   | 770000000035       | 100    | $40.000   |

Prueba rapida contra el backend (con `admin`/`Admin123!` ya logueado):

```
GET /api/products/barcode/770000000028
```

Debe responder con `conversionFactor: 10` — es exactamente el caso "escanear
la tableta agrega 10 pastillas al carrito" que se valida en `ProductCatalogIT`.

Pendientes de tu confirmacion (no bloquean el trabajo actual): si aplica un
rol de "regente de farmacia" y si `requires_prescription` debe bloquear la
venta en caja o solo advertir — esto se resuelve naturalmente en la Fase 6
(POS), cuando se construya la pantalla de venta.

## Inventario (Fase 4) — como probarlo

El producto demo "Ibuprofeno 400 mg" (ver seccion de Catalogo) ya trae
saldo inicial cargado por migracion, repartido en dos lotes con
vencimientos distintos para poder ver FEFO de inmediato:

| Lote         | Vence            | Cantidad (pastillas) |
|--------------|------------------|-----------------------|
| LOTE-DEMO-A  | hoy + 6 meses    | 150                   |
| LOTE-DEMO-B  | hoy + 18 meses   | 150                   |

Stock total esperado: **300 pastillas**. La ventana de alerta de
vencimiento por defecto es de **60 dias** (`business_settings.expiration_alert_days`),
configurable desde `GET/PUT /api/settings` (requiere permiso `SETTINGS_MANAGE`).

Endpoints principales (todos requieren sesion; los de ajuste requieren el
permiso `INVENTORY_ADJUST`, tipicamente ADMIN o FARMACEUTICO):

```
GET  /api/inventory/products/{productId}/stock
GET  /api/inventory/products/{productId}/lots
GET  /api/inventory/products/{productId}/kardex
GET  /api/inventory/alerts/low-stock
GET  /api/inventory/alerts/expiring-lots
GET  /api/inventory/alerts/expired-lots
POST /api/inventory/adjustments
```

Prueba rapida (con `admin`/`Admin123!`): consultar el stock del producto
demo y confirmar que suma los dos lotes:

```
GET /api/inventory/products/{id}/stock   →  quantityBase: 300
```

Regla clave a validar manualmente si quieres: un ajuste `NEGATIVO`, `MERMA`
o `VENCIMIENTO` **sin** `lotId` en el body debe rechazarse con
`409 BUSINESS_RULE_VIOLATION` — a diferencia de una venta futura, un ajuste
manual nunca elige el lote por FEFO, porque quien ajusta ya sabe (por
conteo fisico) cual lote esta afectando.

Desde el frontend: menu **Inventario** → pestañas de alertas (stock bajo,
por vencer, vencidos); desde el detalle de un producto, boton
**"Ver inventario"** lleva al kardex de ese producto, con el boton
**"Ajustar inventario"** (visible solo con el permiso `INVENTORY_ADJUST`).

Los dos casos de prueba obligatorios de concurrencia y FEFO (casos 4 y 5
del encargo) se validan en
`backend/src/test/java/com/boticario/modules/inventory/StockAllocationConcurrencyIT.java`
directamente contra `StockAllocationService`, simulando N operaciones
concurrentes reales (cada una en su propia transaccion) sobre el mismo
producto — confirma que el bloqueo pesimista sobre `inventory_stock` no
pierde actualizaciones y que el stock jamas queda negativo, incluso bajo
sobredemanda simultanea.

## Compras (Fase 5) — como probarlo

Un proveedor de ejemplo ("Distribuidora Central S.A.S.") se carga por
migracion para poder registrar una compra de prueba sin dar de alta nada
primero.

Endpoints principales (las mutaciones requieren el permiso
`PURCHASE_MANAGE`, que ya tienen ADMIN y FARMACEUTICO):

```
GET  /api/suppliers
POST /api/suppliers
GET  /api/purchases
POST /api/purchases
PUT  /api/purchases/{id}                      (solo mientras esta en BORRADOR)
POST /api/purchases/{id}/receive
POST /api/purchases/{id}/cancel               (solo mientras esta en BORRADOR)
POST /api/purchases/{id}/items/{itemId}/return
```

Flujo tipico: crear una compra en `BORRADOR` con uno o mas items (cada item
apunta a una **presentacion** concreta del producto — ej. "Caja x100" — no
al producto directamente); mientras este en borrador se puede editar
libremente. Al llamar a `receive`, cada item:

1. Se convierte de la presentacion comprada a unidad base (una caja de
   factor 100 entra como 100 unidades al inventario).
2. Abre o suma a un lote (obligatorio indicar `lotCode`/`expirationDate`
   si el producto controla lote/vencimiento — igual regla que los ajustes
   manuales de la Fase 4: el sistema nunca adivina el lote).
3. Deja su propio movimiento `COMPRA` en el kardex y actualiza el costo de
   compra vigente de esa presentacion.

Una compra ya `RECIBIDA` no se anula ni se edita — para revertir stock que
ya entro se usa la devolucion a proveedor
(`POST /api/purchases/{id}/items/{itemId}/return`), que descuenta
exactamente del lote que esa compra origino y queda registrada como
`DEVOLUCION_COMPRA` en el kardex, nunca como si la compra no hubiera
existido.

Los casos de prueba cubiertos en
`backend/src/test/java/com/boticario/modules/purchasing/PurchaseFlowIT.java`:
conversion de unidades + actualizacion de inventario y costo al recibir,
rechazo de una segunda recepcion sobre la misma compra, devolucion parcial
respetando el limite disponible, anulacion de un borrador sin tocar
inventario, rechazo de recepcion sin lote para un producto que lo controla,
y rechazo de una compra contra un proveedor inactivo.

Desde el frontend: menu **Proveedores** para el dato maestro, y
**Compras** para el listado, alta (con busqueda de producto, seleccion de
presentacion y vista previa de la conversion a unidad base antes de
guardar) y detalle con las acciones de recibir/anular/editar/devolver
segun el estado de la compra.

## Ventas / POS (Fase 6) — como probarlo

A diferencia de una compra, una venta no tiene estado borrador: se crea y
descuenta el stock en una unica transaccion. Si algo falla a mitad de
camino (stock insuficiente en cualquier item, pagos que no cuadran con el
total), toda la venta se revierte — nunca queda una venta a medias ni
stock descontado sin una venta que lo respalde.

Endpoints principales:

```
GET  /api/sales                       (abierto a cualquier usuario autenticado)
GET  /api/sales/{id}                  (abierto a cualquier usuario autenticado)
POST /api/sales                       (requiere SALE_REGISTER)
POST /api/sales/{id}/cancel           (requiere SALE_CANCEL)
```

`SALE_REGISTER` lo tienen CAJERO, FARMACEUTICO y ADMIN — cualquiera puede
cobrar. `SALE_CANCEL` **no** lo tiene CAJERO, solo FARMACEUTICO y ADMIN:
un cajero puede vender pero no anular una venta ya cobrada.

Reglas de negocio confirmadas para esta fase:

1. **Descuento por item**: el campo `discountPercentage` de cada item se
   valida dentro de `SaleService`, no en el controller, porque es una
   regla por item y no por endpoint. Si un usuario sin
   `SALE_APPLY_DISCOUNT` (por ejemplo CAJERO) envia un descuento mayor a
   0%, la venta se rechaza (409) — pero ese mismo usuario puede vender sin
   descuento sin ningun problema. El frontend ya deshabilita el campo de
   descuento en el POS si el usuario no tiene el permiso, pero la
   validacion real esta en el backend.
2. **Receta medica**: si algun item de la venta corresponde a un producto
   que requiere receta, la venta **no se bloquea** — se registra
   normalmente y queda marcada con `hasPrescriptionItems = true`, que el
   frontend muestra como una advertencia visual (en el POS antes de
   cobrar, y en el detalle de la venta despues). Esto se decidio
   explicitamente para esta fase; un control mas estricto (exigir el
   numero de receta, por ejemplo) puede agregarse en una fase posterior
   sin romper este contrato.
3. **Pagos divididos**: una venta puede tener varias formas de pago
   (`EFECTIVO`, `TARJETA`, `TRANSFERENCIA`). Las formas distintas de
   efectivo nunca pueden sumar mas que el total (no existe "vuelto" en
   tarjeta); el efectivo cubre lo que falte y cualquier excedente queda
   como `changeAmount` (vuelto) en la venta.
4. **Consumo de inventario (FEFO)**: cada item se descuenta con
   `StockAllocationService.decreaseStockFefo`, que puede tomar el item de
   varios lotes distintos empezando por el que vence primero. Por eso una
   venta puede terminar con mas de un lote por item — el detalle de la
   venta lista exactamente que lotes y cuanto se tomo de cada uno.
5. **Anulacion**: `POST /api/sales/{id}/cancel` repone el stock
   exactamente a los mismos lotes de donde salio (no a "cualquier lote
   disponible"), dejando un movimiento `DEVOLUCION_VENTA` por cada
   asignacion original en el kardex. La venta y sus movimientos originales
   nunca se reescriben — el kardex queda inmutable y se puede reconstruir
   la venta completa leyendo sus movimientos.

Los casos de prueba cubiertos en
`backend/src/test/java/com/boticario/modules/sales/SaleFlowIT.java`:
venta con descuento que consume FEFO entre dos lotes distintos y calcula
el vuelto correctamente, un cajero que no puede aplicar descuento pero si
puede vender sin el (y que no puede anular), anulacion que repone
exactamente los lotes consumidos, rechazo por stock insuficiente y por
pagos que no cuadran con el total, y ventas concurrentes sobre el mismo
producto que nunca dejan el stock en negativo (bloqueo pesimista, igual
mecanismo que en Inventario/Compras).

Desde el frontend: menu **Punto de venta** para cobrar (escaneo de codigo
de barras o busqueda manual de producto, carrito editable, pagos
divididos con calculo de vuelto en vivo, ticket de confirmacion al
cobrar), y **Ventas** para el historial (busqueda, filtro por estado,
detalle con anulacion segun permisos).

## Clientes y proveedores (Fase 7) — como probarlo

Esta fase deliberadamente se quedo corta en un punto: **no hay fiado ni
cuentas por pagar**. Se evaluo con el usuario y se decidio dejarlo para
una fase aparte porque implica reglas de negocio completas por si solas
(limite de credito, abonos parciales, mora, saldo por factura). Lo que si
entra en esta fase:

Endpoints principales de clientes (las mutaciones requieren
`CUSTOMER_MANAGE`, que tienen ADMIN, FARMACEUTICO y CAJERO — es solo dato
de contacto, bajo riesgo):

```
GET   /api/customers
POST  /api/customers
PUT   /api/customers/{id}
PATCH /api/customers/{id}/status
```

Reglas de negocio confirmadas:

1. **Documento opcional pero unico**: muchos clientes de mostrador solo
   dan su nombre. Si se informa un numero de documento, el tipo
   (`CC`/`CE`/`TI`/`PASAPORTE`/`NIT`) es obligatorio — el sistema nunca
   adivina, igual criterio que lotes/vencimientos en Inventario. Dos
   clientes no pueden compartir el mismo numero de documento.
2. **La venta de mostrador anonima sigue existiendo**: `POST /api/sales`
   sigue aceptando `customerName` como texto libre sin tocar el modulo de
   clientes (comportamiento identico a la Fase 6). Si en cambio se envia
   `customerId`, este tiene prioridad: se valida que el cliente exista y
   este activo, y su nombre se copia (snapshot) a `customerName` en la
   venta — si el cliente cambia de nombre despues, las ventas ya
   registradas conservan el nombre que tenia en ese momento (mismo
   criterio de inmutabilidad que el resto del kardex).
3. **Historial de compras**: `GET /api/sales?customerId={id}` devuelve
   las ventas de ese cliente (mismo endpoint de siempre, ahora con un
   filtro mas) — el frontend lo usa en el detalle del cliente.
4. **Proveedores enriquecidos**: `contactName`, `paymentTermsDays`
   (dias de credito pactados) y `notes` se agregaron a `Supplier` /
   `PUT /api/suppliers/{id}`. `paymentTermsDays` es puramente informativo:
   no se valida contra ninguna compra ni se lleva saldo pendiente.

Los casos de prueba cubiertos en
`backend/src/test/java/com/boticario/modules/customers/CustomerFlowIT.java`:
alta y busqueda por nombre/documento, rechazo de documento duplicado y de
numero sin tipo de documento, una venta ligada a un cliente que aparece en
su historial con el nombre correctamente copiado, rechazo de una venta
contra un cliente inactivo, y actualizacion de un proveedor con los
nuevos campos de contacto.

Desde el frontend: menu **Clientes** para el listado (alta rapida) y el
detalle (edicion, activar/desactivar, historial de compras con enlace a
cada venta); en el **Punto de venta**, el campo de cliente ahora busca
clientes registrados por nombre o documento, con un campo de texto libre
como alternativa para no obligar a registrar a nadie; el detalle de una
venta enlaza al cliente cuando la venta esta ligada a uno.

## Inyectologia (Fase 8) — como probarlo

La decision de negocio mas importante de esta fase, confirmada con el
usuario antes de construirla: **el medicamento siempre lo trae el
paciente**. Este modulo nunca escribe en `inventory_stock` ni
`inventory_lots` — no hay FEFO, no hay lotes que validar, no hay reversion
de stock al anular. Si el medicamento se vende en la misma farmacia, eso
se resuelve con una venta normal del POS (Fase 6); este modulo solo deja
un campo opcional para anotar el numero de esa venta, sin fusionar los dos
flujos.

Endpoints principales:

```
GET  /api/injections                    (abierto a cualquier usuario autenticado)
GET  /api/injections/{id}               (abierto a cualquier usuario autenticado)
POST /api/injections                    (requiere INJECTION_REGISTER)
POST /api/injections/{id}/cancel        (requiere INJECTION_CANCEL)
```

`INJECTION_REGISTER` lo tienen INYECTOLOGIA, FARMACEUTICO y ADMIN.
`INJECTION_CANCEL` **no** lo tiene INYECTOLOGIA — igual criterio que
CAJERO/SALE_CANCEL en la Fase 6: quien aplica el servicio no deberia poder
anular su propio cobro.

Reglas de negocio confirmadas:

1. **Paciente**: puede ligarse a un cliente real (`customerId`, con
   snapshot inmutable del nombre — igual criterio que `customerName` en
   Sale) o registrarse con un nombre libre. A diferencia de una venta, un
   servicio medico **no puede quedar completamente anonimo**: si no se
   selecciona un cliente, el nombre del paciente es obligatorio.
2. **Medicamento en texto libre**: `medicationName`, `dose` y `lotCode`
   no referencian el catalogo — es informacion que se registra tal cual
   la trae el paciente, sin validarla contra nada.
3. **Cobro simple**: un valor (`fee`) y una sola forma de pago
   (`EFECTIVO`/`TARJETA`/`TRANSFERENCIA`) por registro — no el mecanismo
   de pagos divididos de Sale, porque es una tarifa de servicio, no una
   compra con varios articulos.
4. **Venta relacionada opcional**: `relatedSaleId` solo se valida en
   cuanto a que la venta exista (404 si no) — no se cruza con el cliente
   ni con el estado de esa venta, es una simple anotacion de trazabilidad.
5. **Anulacion**: revierte el cobro (`status = ANULADA`) pero el registro
   original nunca se borra ni se reescribe — mismo criterio de
   inmutabilidad que el resto del sistema.

Los casos de prueba cubiertos en
`backend/src/test/java/com/boticario/modules/injections/InjectionFlowIT.java`:
registro con cliente registrado (copia el nombre) y con paciente de texto
libre, rechazo cuando no hay ni cliente ni nombre, rechazo contra un
cliente inactivo, rechazo de una referencia a una venta inexistente, y la
regla de que INYECTOLOGIA puede registrar pero no anular su propio cobro.

Desde el frontend: menu **Inyectologia** para el listado (con filtro por
estado) y el registro de un nuevo servicio (buscador de cliente registrado
o nombre libre, datos del medicamento, tarifa y forma de pago); el detalle
enlaza al cliente (si esta ligado) y a la venta relacionada (si se anoto
una), y permite anular segun el permiso.

## Dashboard y reportes (Fase 9) — como probarlo

Este modulo es deliberadamente de solo lectura: no agrega tablas ni
migraciones nuevas, y reutiliza integramente los datos que Ventas e
Inventario ya registran. `reports` no tiene capa `domain` propia, solo
`application` (agregacion) y `api` (DTOs y controller).

Endpoints principales (abiertos a cualquier usuario autenticado, sin
permiso especial — son solo lectura de datos que cada rol ya puede ver en
sus propias pantallas):

```
GET /api/reports/sales-summary?from={ISO}&to={ISO}
GET /api/reports/top-products?from={ISO}&to={ISO}&limit={n}
GET /api/reports/inventory-alerts-summary
```

Reglas de negocio confirmadas:

1. **Resumen de ventas**: `totalVentas`, `cantidadVentas`, `ticketPromedio`
   y `totalDescuentos` se calculan **excluyendo siempre** las ventas con
   estado `ANULADA` — una venta anulada no debe inflar ni distorsionar
   los numeros del periodo. El desglose por forma de pago
   (`PaymentBreakdownDTO`) suma los pagos reales registrados en cada
   venta valida, no un promedio ni una estimacion.
2. **Productos mas vendidos**: ranking por ingresos (`SUM(lineTotal)`),
   no por cantidad — el criterio de negocio es "que genero mas dinero",
   no "que se vendio mas unidades". El limite (`limit`) se aplica con
   `Pageable`, no cargando todo y recortando en memoria.
3. **Alertas de inventario**: son los mismos tres conteos que ya se ven en
   la pantalla de Inventario > Alertas (stock bajo, lotes vencidos, lotes
   por vencer) — el dashboard no inventa una nueva regla de "que es una
   alerta", solo cuenta cuantas hay ahora mismo.
4. **Rango de fechas invertido**: si `from` es posterior a `to`, la
   peticion se rechaza (400) — el sistema nunca adivina cual de las dos
   fechas quiso decir el usuario.

Los casos de prueba cubiertos en
`backend/src/test/java/com/boticario/modules/reports/ReportFlowIT.java`:
el resumen de ventas excluye las anuladas y calcula bien el ticket
promedio y las formas de pago, el ranking de productos ordena por
ingresos y respeta el limite, se rechaza un rango de fechas invertido, y
el resumen de alertas de inventario responde con contadores no negativos.

Desde el frontend: pantalla **Dashboard** (pagina de inicio tras iniciar
sesion) con un selector de rango (Hoy / Ultimos 7 dias / Ultimos 30 dias /
Personalizado, este ultimo con selector de rango de fechas), tarjetas con
el resumen de ventas y su desglose por forma de pago, tarjetas de alertas
de inventario que enlazan directamente a la pantalla de Inventario >
Alertas, y una tabla con el ranking de productos mas vendidos del
periodo seleccionado.

## Auditoria, seguridad y cierre del roadmap (Fase 10) — como probarlo

### Hallazgo critico corregido: @EnableMethodSecurity

Al construir la matriz de autorizacion por rol de esta fase se descubrio
que **faltaba `@EnableMethodSecurity` en SecurityConfig**. Sin esa
anotacion, Spring Security nunca evalua las expresiones `@PreAuthorize` —
es decir, TODAS las anotaciones `@PreAuthorize` escritas desde la Fase 2
en adelante (`PRODUCT_MANAGE`, `SALE_CANCEL`, `USER_MANAGE`,
`INJECTION_CANCEL`, etc.) eran letra muerta: la unica regla real que se
aplicaba en el filtro HTTP era "cualquier usuario autenticado puede
llamar cualquier endpoint" (`anyRequest().authenticated()`). Un CAJERO
podia crear usuarios, un INYECTOLOGIA podia anular ventas ajenas, etc.
Se corrigio agregando `@EnableMethodSecurity` a `SecurityConfig`. Este
hallazgo es el motivo mas concreto por el que valia la pena construir
`AuthorizationMatrixIT`: es exactamente el tipo de regresion silenciosa
que ninguna prueba de una sola fase iba a detectar por si sola.

### Auditoria (aspecto AOP)

La tabla `audit_logs` existe desde la V1 de la Fase 1 pero nunca se habia
usado. Ahora `AuditAspect` (en `common/audit`) intercepta cualquier
metodo de un servicio de aplicacion anotado con `@Audited` y deja una
fila con usuario, accion, entidad, id de entidad, y un antes/despues.
Se anotaron 29 metodos — todo endpoint de escritura expuesto por un
controller en los 9 modulos de negocio (catalogo, inventario, compras,
ventas, clientes, inyectologia, identidad y configuracion).

Decisiones de diseno importantes:

1. **Solo se audita lo que realmente tuvo exito**: el aspecto usa
   `@AfterReturning`, nunca `@Around` — una accion que lanza una
   excepcion (regla de negocio, duplicado, etc.) jamas deja una fila de
   auditoria a medias.
2. **"Antes" y "despues" no son una foto de la entidad en base de
   datos**: "antes" son los argumentos de la llamada (la solicitud/
   intencion del usuario) y "despues" es el valor de retorno (el
   resultado tal como lo ve el cliente de la API). Capturar un diff real
   de entidad antes/despues de forma generica para 29 metodos en 9
   modulos distintos habria requerido cargar cada entidad dos veces con
   un mapeo propio por modulo, dejando de ser una unica pieza de
   infraestructura transversal. Se documenta esto explicitamente para no
   insinuar una garantia que no existe.
3. **La auditoria nunca puede tumbar una transaccion de negocio**: si hay
   una transaccion Spring activa, el guardado del registro se difiere a
   `afterCommit` (nunca se audita algo que en realidad se revirtio), y
   cualquier error al construir o guardar la fila se atrapa y solo deja
   un warning en el log.

Consulta: `GET /api/audit-logs` (filtros: `entityName`, `action`,
`username`, `from`, `to`, paginado), protegido por `AUDIT_VIEW` — permiso
sembrado desde la Fase 1 pero nunca usado hasta ahora, y que solo tiene
ADMIN. Pantalla **Auditoria** en el frontend (enlace de navegacion oculto
para cualquier otro rol) con filtros y detalle de la solicitud/resultado
en formato JSON legible.

### Endurecimiento de seguridad

1. Cabeceras HTTP explicitas en `SecurityConfig` (`X-Content-Type-Options:
   nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security` con
   `includeSubDomains` y un año de vigencia) — varias ya las agregaba
   Spring Security por defecto, pero se dejan explicitas para que la
   postura de seguridad sea auditable leyendo el archivo, sin depender de
   conocer los defaults del framework.
2. `server.forward-headers-strategy: framework` en `application.yml`
   para que, detras de un proxy inverso que termine TLS en produccion
   (nginx/traefik en el docker-compose), Spring reconozca correctamente
   `https` y HSTS funcione.
3. `GlobalExceptionHandler.handleUnexpected` ahora deja un `log.error`
   real con el stack trace completo — antes el comentario decia que el
   detalle quedaba en el log del servidor, pero no habia ninguna linea de
   log: un error 500 real era indiagnosticable.

### Pruebas adicionales

- `AuditFlowIT`: una escritura exitosa queda auditada con el resultado
  correcto; una escritura que falla (duplicado) nunca deja una fila a
  medias; un cambio de estado audita la solicitud como "antes"; y
  `GET /api/audit-logs` exige `AUDIT_VIEW`.
- `SecurityHeadersIT`: las cabeceras configuradas llegan en la respuesta
  real (incluyendo HSTS en una request marcada como segura), y un cuerpo
  JSON malformado nunca filtra un stack trace ni un nombre de clase Java
  en la respuesta.
- `AuthorizationMatrixIT`: 26 casos parametrizados que cubren, para cada
  rol del sistema (ADMIN, FARMACEUTICO, CAJERO, INYECTOLOGIA), si puede o
  no puede llamar un endpoint representativo de cada permiso de escritura
  principal (USER_MANAGE, PRODUCT_MANAGE, CUSTOMER_MANAGE, SALE_CANCEL,
  INJECTION_CANCEL, PURCHASE_MANAGE, INVENTORY_ADJUST, SETTINGS_MANAGE,
  AUDIT_VIEW). Es la prueba que habria detectado de inmediato el hallazgo
  de `@EnableMethodSecurity` si hubiera existido antes.

### Cierre del roadmap

Con esta fase se completan las 10 fases planeadas para Boticario/FarmaApp:
arquitectura y modelo de datos, backend base y seguridad, catalogo,
inventario, compras, POS/ventas, clientes y proveedores, inyectologia,
dashboard y reportes, y finalmente auditoria + endurecimiento de
seguridad + pruebas adicionales. El sistema cubre las funciones centrales
de una farmacia pequeña/mediana con trazabilidad, control de permisos por
rol, y un historial de auditoria consultable — quedan fuera del alcance
(documentado explicitamente en cada fase donde aplico) cosas como fiado/
credito a clientes, multi-sucursal, y exportacion a Excel/CSV de los
reportes, que quedan como posibles extensiones futuras.

# Spec — Simple Stock Flow

**Qué debe hacer el sistema y por qué. Ninguna decisión técnica vive en este documento**
(esas están en `plan.md`). Un lector de negocio debe poder leerlo entero y objetar.

- **Origen:** `../enunciado/prueba-inventarios.docx` — prueba técnica "Construcción de Sistema End-to-End".
- **Fecha:** 2026-09-19

---

## 1. Problema

Un negocio necesita saber **qué tiene para vender y qué ha vendido**. Hoy no puede responder
ninguna de las dos preguntas sin contarlo a mano, y el recuento envejece en cuanto se registra
una venta.

El sistema debe sostener tres afirmaciones, y todo lo demás es accesorio:

1. El stock que muestra el catálogo es el stock que hay.
2. Una venta registrada no se puede alterar después.
3. El reporte de un período cerrado dice hoy lo mismo que dirá dentro de un año.

---

## 2. Actores

| Actor | Quién es | Qué puede hacer |
|---|---|---|
| **Vendedor** (`seller`) | Operador de mostrador | Consultar el catálogo, registrar ventas, consultar ventas y el reporte |
| **Administrador** (`admin`) | Responsable del catálogo | Todo lo del vendedor, más crear, editar y dar de baja productos, subir imágenes y dar de alta usuarios |
| **Anónimo** | Nadie autenticado | Únicamente autenticarse y consultar el estado de salud del servicio |

**No existe el comprador como actor del sistema.** La venta registra quién la tomó, no a quién
se le vendió. Es una decisión de alcance, no un olvido: no hay requisito de cliente final, y
añadirlo ampliaría la superficie de datos personales sin que nadie lo haya pedido.

---

## 3. Historias de usuario

Cada historia lleva criterios de aceptación verificables. "Verificable" significa que se puede
escribir el test antes que el código.

### HU-01 — Consultar el catálogo

> Como **vendedor**, quiero buscar productos por nombre y filtrar por categoría, para encontrar
> rápido lo que el cliente pide.

- **CA-01.1** Dado un catálogo con productos, cuando consulto sin filtros, entonces recibo una
  página de resultados con el total de elementos y el total de páginas.
- **CA-01.2** Dado un texto de búsqueda, entonces solo recibo productos cuyo nombre lo contiene,
  **sin distinguir mayúsculas**.
- **CA-01.3** Dado un filtro de categoría, entonces solo recibo productos de esa categoría.
- **CA-01.4** Dado un producto dado de baja, entonces **no aparece nunca** en los resultados.
- **CA-01.5** Dado un tamaño de página mayor que el máximo permitido, entonces el sistema aplica
  el máximo en lugar de rechazar la petición.

### HU-02 — Mantener el catálogo

> Como **administrador**, quiero crear, editar y dar de baja productos, para que el catálogo
> refleje lo que el negocio vende.

- **CA-02.1** Dado un producto nuevo con nombre, precio, stock y categoría, cuando lo creo,
  entonces queda disponible para la venta y recibo su identificador.
- **CA-02.2** Dado un precio menor o igual a cero, entonces la creación **se rechaza** con un
  mensaje que explica la regla.
- **CA-02.3** Dado un stock inicial negativo, entonces la creación se rechaza.
- **CA-02.4** Dada una categoría inexistente, entonces la creación se rechaza.
- **CA-02.5** Dado un producto **que ya se vendió**, cuando lo doy de baja, entonces desaparece
  del catálogo **y las ventas que lo contienen siguen intactas y consultables**.
- **CA-02.6** Dado un producto dado de baja, entonces **no se puede vender** aunque alguien
  conozca su identificador.
- **CA-02.7** Dado un vendedor, cuando intenta crear, editar o dar de baja, entonces la
  operación se rechaza por falta de permiso.

### HU-03 — Imagen del producto

> Como **administrador**, quiero asociar una imagen a un producto, para que el catálogo sea
> reconocible de un vistazo.

- **CA-03.1** Dada una imagen subida, entonces el producto la muestra en las consultas
  posteriores.
- **CA-03.2** Dado un producto sin imagen, entonces el catálogo lo devuelve sin imagen, **sin
  error y sin una dirección rota**.
- **CA-03.3** Dado un producto con imagen, cuando lo doy de baja, entonces **no queda una
  referencia apuntando a un binario que ya no existe**.

### HU-04 — Registrar una venta

> Como **vendedor**, quiero registrar una venta con varias líneas, para descontar el stock y
> dejar constancia de lo vendido.

- **CA-04.1** Dada una venta con líneas válidas, cuando la registro, entonces el stock de cada
  producto **baja exactamente** en la cantidad vendida y recibo el identificador de la venta.
- **CA-04.2** Dada una cantidad mayor que el stock disponible, entonces **la venta completa se
  rechaza** y **ningún** producto queda descontado.
- **CA-04.3** Dada una venta sin líneas, entonces se rechaza.
- **CA-04.4** Dado el mismo producto repetido en dos líneas, entonces se rechaza.
- **CA-04.5** Dada una cantidad menor o igual a cero, entonces se rechaza.
- **CA-04.6** Dadas **dos ventas simultáneas** del último ejemplar, entonces una de las dos se
  registra y la otra recibe un conflicto. **El stock nunca queda negativo.**
- **CA-04.7** Registrada la venta, cuando después cambia el nombre o el precio del producto,
  entonces **la venta sigue mostrando el nombre y el precio del momento en que se vendió**.

### HU-05 — Consultar ventas

> Como **vendedor**, quiero consultar una venta y listar las de un período, para resolver dudas
> sobre lo registrado.

- **CA-05.1** Dado el identificador de una venta, entonces obtengo su fecha, quién la registró,
  sus líneas y el total.
- **CA-05.2** Dado un rango de fechas, entonces obtengo una página de ventas de ese rango.
- **CA-05.3** Dado un rango cuya fecha final es anterior a la inicial, entonces se rechaza.
- **CA-05.4** El total de la venta **siempre cuadra con la suma de sus líneas**, porque se
  calcula a partir de ellas.
- **CA-05.5** Registrada una venta, **no existe forma de modificarla ni de anularla**: ni una
  operación del agregado, ni un método del repositorio, ni un verbo del API. Es la afirmación 2
  de §1 y se verifica sobre la **forma** del sistema, no sobre una llamada concreta (RN-07).

### HU-06 — Reporte de ventas por rango de fechas

> Como **responsable**, quiero saber cuántas unidades y cuánto dinero generó cada producto en un
> período, para decidir qué reponer.

- **CA-06.1** Dado un rango, entonces obtengo **una fila por producto y etiqueta de categoría
  congelada**, con unidades vendidas e importe, más el número de ventas y el total general del
  período. Sin recategorizaciones dentro del rango —el caso normal— eso es exactamente una fila por
  producto. **Con** ellas son dos, y es deliberado: colapsarlas exigiría decidir que dos etiquetas
  son la misma cosa, y esa decisión le tocaba a quien renombró, no al reporte
  ([`data-model.md`](data-model.md) §11.1).
- **CA-06.2** Dado un rango sin ventas, entonces obtengo un reporte vacío, **no un error**.
- **CA-06.3** Dado un producto que se dio de baja después de haberse vendido, entonces **sigue
  apareciendo** en el reporte del período en que se vendió.
- **CA-06.4** Dado un reporte de un período ya transcurrido, entonces **repetir la consulta
  devuelve exactamente lo mismo**, aunque el catálogo haya cambiado entretanto.
- **CA-06.5** La agregación se resuelve **en la base de datos**. Traer las ventas a memoria para
  sumarlas es un defecto, no una alternativa.

### HU-07 — Autenticación y protección

> Como **negocio**, quiero que solo el personal autorizado use el sistema, para que el
> inventario y las ventas no los toque cualquiera.

- **CA-07.1** Dadas credenciales válidas, entonces recibo un token con su vencimiento, mi
  usuario y mi rol. **Nunca el hash de la clave.**
- **CA-07.2** Dadas credenciales inválidas, entonces recibo un rechazo que **no revela** si
  falló el usuario o la contraseña.
- **CA-07.3** Dada una petición sin token a un endpoint protegido, entonces se rechaza.
- **CA-07.4** Dado un token de vendedor en una operación de administrador, entonces se rechaza
  por permiso, no por autenticación.
- **CA-07.5** El sistema **nunca** almacena ni registra la contraseña en claro.
- **CA-07.6** Dos usuarios no pueden compartir nombre de usuario, **ni siquiera si se registran
  a la vez**.
- **CA-07.7** Dado un rol que no es administrador ni vendedor, entonces el alta del usuario **se
  rechaza**. No hay un tercer rol, ni un rol vacío, ni un rol por omisión que se cuele (RN-11).

### HU-08 — Operar el sistema

> Como **evaluador**, quiero levantar el sistema completo con un comando y entender qué hace,
> para poder juzgarlo sin adivinar.

- **CA-08.1** Dado el repositorio clonado, un solo comando levanta base de datos, API y portal.
- **CA-08.2** En el primer arranque, el esquema se crea solo, **sin ejecutar SQL a mano**.
- **CA-08.3** Existen categorías desde el arranque: sin ellas **no se puede crear ni un
  producto**, y el CRUD no se podría ni ejercer.
- **CA-08.4** El README explica qué es cada repositorio, cómo se levanta y **qué falta**.

---

## 4. Reglas de negocio

Se numeran para poder citarlas desde un test o desde una revisión.

**Una regla que nadie prueba no es una regla: es una intención.** Por eso cada fila declara cuatro
cosas y no una — dónde vive, **quién la hace cumplir**, **qué prueba la cubre** y **qué criterio de
aceptación la ejerce**—, y donde no hay prueba lo dice con esas palabras en vez de dejar la celda
en blanco. Los nombres de test son literales: se pueden buscar en
`simple-stock-flow-api/tests`. Verificado fichero a fichero el **2026-09-19**.

Las tres columnas de la derecha se leen así:

- **Quién la hace cumplir** — *agregado* (una guarda en C#), *motor* (una restricción de
  PostgreSQL), *los dos*, o **nadie**.
- **Prueba que la cubre** — el nombre del método de test. `D` = `Domain.UnitTests`,
  `A` = `Application.UnitTests`, `I` = `Adapters.IntegrationTests`.
- **Criterio que la ejerce** — el criterio de aceptación de §3 que se apoya en la regla.

| # | Regla | Dónde vive | Quién la hace cumplir | Prueba que la cubre | Criterio que la ejerce |
|---|---|---|---|---|---|
| RN-01 | El stock de un producto **nunca** es negativo | Agregado `Product` | **Los dos** — `Product.Withdraw` y el `CHECK ck_product_stock_non_negative` | `D·Withdraw_never_leaves_the_stock_negative` · `A·Fails_without_committing_when_the_stock_falls_short` · `I·The_engine_refuses_a_negative_stock_written_around_the_adapter` · `I·A_lost_update_cannot_resurrect_stock_that_was_already_sold` | CA-04.1, CA-04.2, CA-04.6 |
| RN-02 | El precio de un producto es **mayor que cero** | Agregado `Product` | **Solo el agregado** — `Product.ChangePrice`, por donde pasa también la creación. `Money` admite importe 0, así que un precio 0 escrito por otra vía entra sin resistencia. El `CHECK` es T-20 punto 4 | `D·ChangePrice_rejects_a_price_of_zero` | CA-02.2 |
| RN-03 | La cantidad vendida es **mayor que cero** | Value object `Quantity` | **Solo el agregado** — el constructor de `Quantity`. El `CHECK` es T-20 punto 5 | **Ninguna. Nunca se afirma que `Quantity` rechace 0 ni un negativo:** `Domain.UnitTests` no tiene fichero de pruebas para `Quantity`. La cubre **T-21** | CA-04.5, **hoy sin test que lo verifique** |
| RN-04 | Una venta tiene **al menos una** línea | Agregado `Sale` | **Solo el agregado** — `Sale.EnsureConfirmable`, invocado por `PlaceSaleService` | `D·EnsureConfirmable_rejects_a_sale_with_no_lines` · `A·Rejects_a_sale_with_no_lines` | CA-04.3 |
| RN-05 | Un producto **no se repite** dentro de una misma venta | Agregado `Sale` | **Solo el agregado** — `Sale.AddItem`. El único compuesto `(sale_id, product_id)` es T-20 punto 3 | `D·AddItem_rejects_the_same_product_twice` · `A·Rejects_a_repeated_line` | CA-04.4 |
| RN-06 | El precio y el nombre de la línea quedan **congelados** al vender | Agregado `Sale` | **Solo el agregado** — `SaleItem` copia nombre y precio al construirse y no expone setters públicos | `D·AddItem_takes_the_stock_and_freezes_the_price` · `I·Sale_items_rebuild_quantity_and_the_price_frozen_at_sale_time` | CA-04.7 |
| RN-07 | Una venta registrada **no se modifica ni se anula**. No existe operación que lo permita | Modelo entero | **Nadie: la sostiene una ausencia.** Hoy es cierta —`Sale` no expone quitar ni reemplazar líneas, `Items` es `AsReadOnly()`, `ISaleRepository` solo tiene `Find`/`Search`/`ListByRange`/`Add`, y `api/sales` solo publica `POST` y dos `GET` (verificado)—, pero nada la defiende | **Ninguna.** Una pasada futura puede añadir un `Sale.RemoveItem`, un `ISaleRepository.Update` o un `DELETE /api/sales/{id}` **sin que falle un solo test**. La cubre **T-22** | CA-05.5, **criterio nuevo y hoy sin test** |
| RN-08 | Un producto vendido **no se elimina**: se da de baja y desaparece del catálogo | Catálogo y persistencia | **Nadie, en ninguna de las tres capas.** `Product` no tiene marca de baja; `IProductRepository.Remove` es un borrado duro y el `TODO` de `ProductCatalogService.DeleteAsync` dice literalmente que lo llame; y **no existe clave foránea de `sale_items` hacia `products`**, así que un `DELETE` manual pasaría en silencio. La baja lógica es T-09 y la barrera del motor es T-20 punto 2 | **Ninguna** | CA-02.5, CA-02.6, CA-01.4, CA-06.3 — **los cuatro sin test** |
| RN-09 | Todos los importes de una venta están **en la misma moneda** | Agregado `Sale` | **Parcial.** `Money.operator +` rechaza sumar dos monedas; `Sale.AddItem` **no comprueba nada** al añadir la línea. La guarda del agregado es T-05 | `D·Refuses_to_operate_on_two_currencies` prueba el **value object**, no el agregado. La guarda de `Sale` sigue sin prueba | Ninguno. La regla no tiene criterio propio: §6 declara el alcance monomoneda |
| RN-10 | El nombre de usuario es **único** y se normaliza a minúsculas | Identidad y persistencia | **Los dos** — `User.NormalizeUsername` más el índice único de `users.username`. El `CHECK` que exige la forma normalizada en el motor es T-20 punto 8 | `A·Refuses_a_username_that_is_already_taken` · `A·Looks_the_user_up_by_the_same_name_the_domain_stores` · `I·Two_writers_that_both_saw_the_username_free_still_leave_a_single_row` · `I·The_writer_that_loses_the_race_gets_a_business_rule_not_a_database_error` · `I·Registering_a_username_that_already_exists_is_refused_before_touching_the_engine` | CA-07.6 |
| RN-11 | El rol pertenece a un conjunto cerrado: administrador o vendedor | Agregado `User` | **Solo el agregado** — el constructor de `User` vía `Roles.IsValid`. El `CHECK` es T-20 punto 7 | **Ninguna. `Roles.IsValid` nunca se ejerce en un test:** `Domain.UnitTests` no tiene fichero de pruebas para `User`. La cubre **T-21** | CA-07.7, **criterio nuevo y hoy sin test**. CA-07.4 ejerce el rol en el borde HTTP, no el conjunto cerrado |
| RN-12 | El total de una venta es **siempre** la suma de sus líneas | Agregado `Sale` | **Solo el agregado, y por construcción:** `Sale.Total` se calcula desde las líneas y la configuración de persistencia lo marca `Ignore` — **no hay columna de total que pueda discrepar** (verificado) | `D·Total_adds_up_every_line` | CA-05.4 |

### 4.1 · Las cuatro reglas que hoy nadie prueba

RN-03, RN-07, RN-08 y RN-11. No comparten causa y no se cierran igual:

| Regla | Por qué no hay prueba | Quién la cierra |
|---|---|---|
| **RN-03** | El dominio tiene **siete tipos y solo tres ficheros de test** —`MoneyTests`, `ProductTests`, `SaleTests`—. `Quantity` no tiene ninguno | **T-21** |
| **RN-11** | La misma causa: `User` tampoco tiene fichero de test | **T-21** |
| **RN-07** | Es una regla **estructural**: no se prueba ejercitando una operación, sino comprobando que **no existe ninguna** que la viole. **Afirmada el 2026-09-21 en las tres superficies**: el agregado (`SaleImmutabilityTests`), la ruta HTTP (`SalesEndpointsRefuseMutationTests`) y el puerto (`SaleWriteSurfaceTests`) | **T-22** ✅ |
| **RN-08** | No hay prueba porque **no hay implementación**: la baja lógica entera está sin construir | **T-09**, con la barrera del motor en **T-20** |

> **RN-09 está satisfecha por construcción, no por una regla activa:** el sistema es monomoneda
> por cómo se persiste el importe (`plan.md` D-05), no porque el agregado lo compruebe. La guarda
> sigue pendiente en `tasks.md` como T-05, para cerrar la puerta donde corresponde.

> **Las reglas que solo viven en el agregado no protegen los datos, solo la aplicación.** Cinco de
> ellas —RN-02, RN-03, RN-11, la normalización de RN-10 y el nombre no vacío de la categoría— las
> salta cualquier `INSERT` por `psql`. Bajarlas al motor es **T-20**, y ahí está enumerada cada una
> con su definición exacta.

---

## 5. Requisitos no funcionales

| # | Requisito | Criterio de aceptación |
|---|---|---|
| RNF-01 | **Integridad bajo concurrencia** | Dos ventas simultáneas del último ejemplar no dejan stock negativo (CA-04.6) |
| RNF-02 | **Fidelidad histórica** | Un reporte de un período cerrado no cambia porque el catálogo cambie (CA-06.4) |
| RNF-03 | **Paginación** | Toda lista que pueda crecer sin límite viene paginada, con un tope de tamaño de página |
| RNF-04 | **Arranque reproducible** | Desde cero a sistema usable con un comando, sin pasos manuales |
| RNF-05 | **Trazabilidad** | Registro estructurado de cada petición, con correlación, sin datos personales ni secretos |
| RNF-06 | **Portabilidad del almacenamiento de binarios** | Cambiar disco local por un servicio de objetos es un adapter nuevo y una línea de composición |
| RNF-07 | **Pruebas** | Invariantes con test unitario; casos de uso con puertos en doble; persistencia contra motor real |
| RNF-08 | **Confidencialidad y retención** · **alcance añadido** | Cada atributo tiene una clasificación declarada y un manejo exigido: qué puede salir en una respuesta, qué puede aparecer en un registro y qué no se borra nunca. Ver [`data-model.md`](data-model.md) §7. **El enunciado no lo pide:** es el único requisito del spec que no sale del reto, y se conserva a sabiendas porque el sistema guarda el nombre de quien vende. Decisión del propietario, 2026-09-19 |

---

## 6. Fuera de alcance

No se modela, **porque nadie lo pidió**: clientes o compradores, devoluciones y anulaciones,
pagos y medios de pago, envíos, descuentos, impuestos, permisos más finos que los dos roles,
notificaciones, gestión de usuarios más allá del alta, y CRUD de categorías (son datos de
referencia sembrados).

**El producto tiene cinco atributos y no seis.** Nombre, precio, stock, categoría e imagen. Quedan
fuera **descripción, código de referencia y SKU**, y no por olvido: es la decisión DP-03 de §7.0.
Los puntos suspensivos del enunciado —*«nombre, precio, stock, categoria, imagen ...»*— se leen
como «y nada más», no como una invitación.

**Multimoneda operativa** queda fuera: la estructura la admite, pero el sistema opera en una
sola moneda y RN-09 lo exige por venta.

### El «Delete» del enunciado se cumple como baja lógica

El enunciado pide un CRUD, y la **D** de CRUD es *Delete*. Este sistema **no borra productos**:
los da de baja (ADR-003, T-09). Es una reinterpretación consciente y se declara aquí para que
nadie la lea como un requisito incumplido.

**Por qué.** Un producto vendido está referenciado por líneas de venta que congelan su nombre y su
precio. Borrarlo de verdad deja dos únicas salidas, y las dos son peores: romper el histórico de
ventas —lo que RN-07 prohíbe—, o fallar con un error que la persona usuaria **no puede resolver**,
porque no puede deshacer una venta ya registrada.

**Qué ve quien usa el sistema.** Exactamente un *Delete*: la acción se llama **Eliminar**, el
producto desaparece del catálogo, deja de poder venderse y no vuelve a aparecer en ninguna
búsqueda. Lo que cambia es lo que ocurre por debajo: la fila sobrevive para que su histórico
siga siendo consultable.

**Dónde se explica al evaluador:** en el README del servicio, no solo aquí. Una decisión que
contradice la letra del enunciado y solo vive en el spec es una decisión que nadie encontrará.

---

## 7. Decisiones de negocio

Marcadas explícitamente porque un spec que las esconde produce código que las decide en
silencio. Tres están **cerradas**; una sigue abierta y dice por qué.

### 7.0 · Decididas

Las tomó el propietario del proyecto el **2026-09-19**. **No se vuelven a abrir.** Cada una trae
lo único que le importa a quien implementa: **qué cambia en el código**.

| # | Pregunta | Decisión | Consecuencia para el código |
|---|---|---|---|
| **DP-01** | Si un producto se **renombra** entre dos ventas del mismo rango, ¿qué nombre muestra el reporte? | **El nombre congelado más reciente del rango.** El razonamiento que descartó las otras salidas está en §7.1, ya no como pregunta sino como el **porqué** de esta decisión | **T-08.** La consulta agregada agrupa por identificador de producto y toma el `product_name` de la **línea de la venta más reciente del rango**, con una función de ventana resuelta **en el motor**. La cláusula de ordenación se escribe con nombre, no disimulada: es el punto exacto donde se leería la decisión. El nombre vivo del catálogo **no se consulta nunca** para el reporte |
| **DP-02** | ¿El reporte debe poder desglosarse **por vendedor**? | **No.** Solo por producto. Evita cruzar datos personales del operador sin que nadie lo haya pedido | `SalesReportRow` queda **cerrada** en producto, categoría, unidades e importe — que es lo que ya tiene (verificado), y ahora por decisión y no por omisión. Ningún endpoint de reporte acepta filtro ni agrupación por vendedor. **T-12 no crea índice sobre `sold_by_user_id`**, y ese es el motivo. La autoría se sigue guardando: se registra, no se explota |
| **DP-03** | ¿El catálogo necesita descripción, código o referencia? | **No.** Solo los cinco atributos que el enunciado enumera: **nombre, precio, stock, categoría, imagen.** Los puntos suspensivos del enunciado se leen como *«y nada más»* | `CreateProductCommand`, `UpdateProductCommand` y `ProductView` quedan **cerrados** con esos atributos — que es lo que hoy tienen (verificado). Ninguna migración añade columnas a `products` salvo la `deleted_at` de T-09, que no es un atributo de negocio sino la marca de baja. **El formulario del portal no crece.** Si alguien echa de menos un campo, la respuesta está escrita aquí |

**El criterio que gobierna las tres es el mismo:** *entendimiento de negocio alineado únicamente
al reto*. Lo que el enunciado no pide, no entra — ni aunque sea barato, ni aunque sea habitual.

### 7.1 · DP-01 — por qué esa y no otra

**Este apartado ya no plantea una pregunta: justifica la decisión de §7.0.** Se conserva entero
porque el razonamiento es lo que hace que la decisión no se reabra por costumbre.

De las tres salidas posibles, **dos ya estaban prohibidas** por criterios escritos antes:

- **El nombre vivo del catálogo** queda descartado por CA-06.4 y RNF-02 —renombrar cambiaría un
  reporte ya cerrado— y por CA-06.3, porque obligaría a saltarse el filtro global de bajas.
- **Una fila por cada nombre** queda descartada por CA-06.1, que pide *una fila por producto*.

Sobrevive **un nombre congelado del rango**, y el desempate era lo único que faltaba:

| Desempate | A favor | Coste |
|---|---|---|
| **El más reciente del rango** ✅ **elegido** | Es el nombre con el que el responsable reconoce el producto hoy | El nombre depende del corte de fechas: el mismo producto puede salir con nombres distintos en dos rangos que se solapan. **Coste aceptado a sabiendas** |
| El primero del rango | Estable hacia atrás: ampliar el rango por la derecha nunca cambia el nombre de una fila ya vista | Es el nombre más viejo — justo el que alguien quiso cambiar |
| El de más unidades vendidas | El nombre bajo el que el producto de verdad se movió en el período | Puede empatar, y entonces hay que desempatar el desempate |

**Las tres cuestan lo mismo de implementar** —una función de ventana en la consulta agregada—, así
que la decisión no se tomó por precio sino por a quién sirve el reporte: al responsable que decide
qué reponer, y que reconoce el producto por su nombre de hoy. **Y por lo mismo, cambiarla algún día
sería una cláusula de ordenación, no una migración.**

### 7.2 · Cerrada el 2026-09-20

| # | Pregunta | Por qué sigue abierta | Quién decide |
|---|---|---|---|
| **DP-04** | ¿Un administrador puede crear otros administradores? | **CERRADA el 2026-09-20: no.** Un administrador da de alta **vendedores**; el rol `admin` lo provisiona el despliegue al arrancar, desde el entorno. Una sola vía de escalada de privilegios, y la controla quien despliega. Estaba bloqueada por el defecto A-1 —que el alta fuera anónima—, ya cerrado | **La restricción es estructural, no una comprobación.** `IAuthenticate.RegisterAsync` rechaza `admin` con 422; crear el primero vive en un puerto aparte, `IProvisionAdministrator`, que **no está inyectado en el adaptador HTTP**. El portal ofrece `/vendedores/nuevo` solo al administrador, y **sin selector de rol**: no hay nada que elegir |

---

## 8. Criterio de terminado

**Un criterio de terminado que nadie ejecutó no es un criterio: es una lista de deseos.** Estos
pasos se ejecutan en orden, sobre un equipo limpio con Docker y nada más, y se deja constancia de
lo que cada uno devolvió. Es literalmente el guion de **T-18**.

Cada paso dice **qué se observa**, no qué se espera sentir.

| # | Paso | Se da por bueno cuando |
|---|---|---|
| **1** | **Clonar desde GitHub** los tres repositorios y seguir su README | El README basta: no hay ningún paso que esté solo en la cabeza de quien lo escribió. El enunciado pide *repositorio clonable y ejecutable*, así que el punto de partida es el clon, no la carpeta de trabajo |
| **2** | **Un comando levanta todo** y el portal responde | `docker compose up` deja los tres contenedores sanos y el portal sirve su primera pantalla. El esquema se crea solo, sin SQL a mano (CA-08.1, CA-08.2) |
| **3** | **Un endpoint protegido rechaza a quien no presenta token** | `GET /api/products` sin token devuelve 401 **antes** de que nadie se autentique. Es lo que el enunciado llama *endpoints protegidos* (CA-07.3) |
| **4** | **Un administrador entra**, crea un producto, le sube una imagen y lo ve en el catálogo | El producto aparece en la lista con su imagen, y solo con los cinco atributos de DP-03 (CA-02.1, CA-03.1) |
| **5** | **Un vendedor entra** y el catálogo lo deja mirar pero no tocar | Con token de vendedor, listar funciona y crear devuelve un rechazo **por permiso, no por autenticación** (CA-02.7, CA-07.4) |
| **6** | **El vendedor registra una venta** de ese producto y el stock baja exactamente | El stock del catálogo baja en la cantidad vendida, ni una unidad más (CA-04.1) |
| **7** | **El reporte del rango muestra esa venta** con sus unidades e importe | Una fila por producto, con unidades e importe, más el número de ventas y el total del período (CA-06.1) |
| **8** | **Dar de baja el producto lo saca del catálogo y el reporte lo sigue mostrando** | El producto desaparece de la lista, no se puede vender aunque se conozca su identificador, y su venta **sigue consultable** con el nombre y el precio del momento (CA-02.5, CA-02.6, CA-01.4, CA-06.3, CA-04.7) |
| **9** | **Repetir el paso 7 sobre el mismo rango devuelve exactamente lo mismo** | El reporte de un período cerrado no cambió porque el catálogo cambiara (CA-06.4, RNF-02) |
| **10** | **Las pruebas del servicio pasan enteras**, incluidas las de integración contra motor real | `dotnet test SimpleStockFlow.sln` en verde, sin excluir proyectos y sin un solo warning (`TreatWarningsAsErrors`) |
| **11** | **Las pruebas del portal pasan enteras** | `ng test --watch=false --browsers=ChromeHeadless` en verde. El enunciado pide *testing*, y el portal es la mitad de lo que entrega |
| **12** | **Las doce reglas de §4 tienen prueba**, y la tabla de §4 lo dice sin una sola celda que diga «ninguna» | Es el paso que esta versión del spec añade. Hoy **fallaría**: RN-03, RN-07, RN-08 y RN-11 no tienen ninguna. Las cierran T-21, T-22 y T-09 |
| **13** | **El README dice la verdad sobre todo lo anterior** | Ninguna afirmación del README es falsa, y lo que no corre está dicho en vez de omitido (CA-08.4, Artículo X) |
| **14** | **El análisis está entregable** | El enunciado pide *análisis/planteamiento lógico de la solución* como parte de la entrega, y eso son estos documentos: este spec, `plan.md`, `architecture.md`, `data-model.md`, `api-contract.md` y los ADR. Se da por bueno cuando ninguno contradice al código |

**Dos cosas que este criterio no puede comprobar solo, y por eso se nombran:**

- **La sustentación** que el enunciado pide es una defensa oral. No se verifica con un comando:
  se prepara. Lo que estos catorce pasos garantizan es que hay algo que sustentar y que decir la
  verdad sobre ello no obliga a ningún rodeo.
- **El paso 3 será mentira mientras el defecto A-1 siga abierto.** `POST /api/auth/register` es
  anónimo hoy, así que §2 —«el anónimo únicamente puede autenticarse»— es falso en el sistema que
  corre. Cerrar A-1 es requisito del paso 3, no un extra.

---

## 9. Firma

**Qué se acepta.** El entendimiento de negocio de Simple Stock Flow: el problema (§1), los tres actores
(§2), las ocho historias con sus criterios de aceptación (§3), las doce reglas de negocio con su
trazabilidad completa (§4), los ocho requisitos no funcionales (§5), el alcance excluido (§6), las
tres decisiones de negocio cerradas (§7.0) y el criterio de terminado de catorce pasos (§8).

**Contra qué se verificó.** Contra el enunciado `../enunciado/prueba-inventarios.docx` y contra el código que
corre, no contra la memoria de nadie:

- Los nombres de test de §4 salen de leer `simple-stock-flow-api/tests` fichero a fichero.
- «Ninguna prueba» en RN-03 y RN-11 se afirma tras comprobar que `Domain.UnitTests` contiene
  exactamente tres ficheros —`MoneyTests`, `ProductTests`, `SaleTests`— para **siete** tipos de
  dominio: `Quantity`, `Category` y `User` no tienen ninguno.
- «Nadie la hace cumplir» en RN-08 se afirma tras comprobar las tres capas: `Product` no tiene
  marca de baja, `ProductCatalogService.DeleteAsync` sigue siendo un `NotImplementedException`
  cuyo `TODO` manda llamar a `IProductRepository.Remove`, y no existe clave foránea de
  `sale_items` hacia `products`.
- «Nadie la hace cumplir» en RN-07 se afirma tras comprobar que `api/sales` publica **un `POST` y
  dos `GET`**, y nada más.
- RN-12 se declara cierta por construcción tras comprobar que `SaleConfiguration` marca `Total`
  como `Ignore`: no hay columna que pueda discrepar de la suma.

**Fecha.** 2026-09-19.

**Qué queda explícitamente fuera de esta firma.**

- **El modelo de datos** — vive en [`data-model.md`](data-model.md) y se firma aparte.
- **El contrato de API** — vive en [`api-contract.md`](api-contract.md) y se firma aparte. Este
  documento no describe ni una ruta ni un cuerpo de respuesta, a propósito.
- **Las decisiones técnicas** — viven en [`plan.md`](plan.md) y en [`architecture.md`](architecture.md).
- **DP-04**, que sigue abierta y bloqueada tras el defecto A-1. **Es el único hueco de negocio que
  queda, y tiene dueño:** negocio decide, después de que alguien cierre A-1.
- **El estado de avance.** Este documento dice qué debe hacer el sistema, no cuánto está hecho.
  Eso vive en [`tasks.md`](tasks.md) y en `../traspaso/HANDOFF.md` §4.

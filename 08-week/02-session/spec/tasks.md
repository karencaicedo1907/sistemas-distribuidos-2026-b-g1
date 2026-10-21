# Tareas — Simple Stock Flow

Unidades ejecutables derivadas de `plan.md`, ordenadas por dependencia real. Cada una trae
**cómo se sabe que está hecha**, y ese criterio es una comprobación, no una opinión.

**El estado verificado del sistema —tests, contenedores, endpoints— vive en
`../traspaso/HANDOFF.md` §4.** Aquí, solo qué tarea está hecha y por qué evidencia.

---

## Progreso

**20 de 22 hechas (91 %), verificado el 2026-09-21.** Cada fila trae la evidencia que lo sostiene;
los números del sistema —tests, contenedores, endpoints— viven en [`../traspaso/HANDOFF.md`](../traspaso/HANDOFF.md) §4.

| Tarea | Estado | Evidencia |
|---|---|---|
| **T-01** | ✅ **Hecha** | 3 tests de integración contra Postgres real: el importe, la cantidad y el precio congelado se reconstruyen. **El mapeo no era el riesgo que temíamos** |
| **T-02** | ✅ **Hecha** | Migración `InitialSchema` con las 5 categorías de referencia dentro, más 2 tests: migrar deja las categorías, y migrar dos veces no duplica |
| **T-03** | ✅ **Hecha** | `simple-stock-flow-infra` con compose, override de desarrollo, `.env.example`, README y `verify.sh`. **El guion pasó de 0 de 10 a todo en verde**, escrito antes que el compose |
| **T-04** | ✅ **Hecha** | `ProductCatalogService` entero, incluido `GET /api/categories`, que era la única brecha del contrato con el portal. Verificado por HTTP: catálogo **200** paginado y categorías **200** |
| **T-06** | ✅ **Hecha** | Autenticar, registrar y crear el administrador al arrancar. **13 tests nuevos** — 10 de aplicación con los puertos en doble y 3 de integración contra Postgres real. Probada de punta a punta: `POST /api/auth/login` devuelve **200** con los cuatro campos del contrato y sin el hash; el token abre un endpoint protegido |
| **T-07** | ✅ **Hecha** | `GetSaleService`. `GET /api/sales` y `GET /api/sales/{id}` responden **200** con la forma exacta de la ficha, y el orden por fecha descendente está verificado |
| **T-08** | ✅ **Hecha** | `SalesReportService` con puerto de lectura propio y agregación **en el motor** (CA-06.5). **Con una salvedad declarada**: `categoryName` sale vacío porque lo congela T-11, que no se ha ejecutado |
| **T-09** | ✅ **Hecha** | Baja lógica con `deleted_at` como propiedad sombra y filtro global. Siete tests escritos **antes**, que fallaron con `23503` — la clave foránea de T-20 — y hoy pasan. Dar de baja un producto vendido responde **204** y la venta anterior sigue consultable |
| **T-10** | ✅ **Hecha** | Token `xmin` como propiedad sombra, reintento del caso de uso completo con descarte previo, y traducción a **409** en el filtro. **13 tests nuevos** — 4 de la política de reintento, 3 del filtro, 3 del caso de uso y 3 de integración contra Postgres real |
| **T-05** | ✅ **Hecha** | RN-09 se hace cumplir en `Product`, en las dos puertas. Dos pruebas escritas antes y vistas fallar con «no exception was thrown». **Su criterio original se reescribió**: pedía un test de venta con monedas mezcladas, que es **inalcanzable por construcción** —ningún producto puede tener otra moneda— y por tanto no era una tarea pendiente sino una puerta sin salida. No se tocó columna, ni persistencia, ni D-05 |
| **T-11** | ✅ **Hecha** | La línea de venta congela la categoría. Dos pruebas escritas **antes**, vistas fallar —`""` en vez de la etiqueta, y **una fila en vez de dos**— y hoy en verde. La migración añade la columna **nulable**, rellena las filas ya escritas con la categoría vigente de su producto —declarado como aproximación, porque nadie guardó la de entonces— y solo entonces la pone `NOT NULL`, **sin defecto**. La consulta **agrupa POR la etiqueta congelada**: ni ventana ni desempate |
| **T-12** | ✅ **Hecha** | `sold_by` → `sold_by_username`, más `sold_by_user_id` `NOT NULL` sin defecto y `FK_sale_user_sold_by_user_id` en **RESTRICT** (`confdeltype = r`). Migración en tres pasos: las 18 ventas existentes resolvieron a su usuario real, **0 con el GUID cero**. **Dos desviaciones declaradas**: el índice de la clave foránea existe —EF lo crea por convención y quitarlo desincroniza el snapshot— y el enunciado decía «`sale` tiene 0 filas», caducado |
| **T-13** | ✅ **Hecha** | Los dos que faltaban, **parciales sobre las filas activas** porque toda lectura del catálogo lleva el filtro global: `IX_product_category_id_name_active` (btree sobre `category_id, name`) e `IX_product_name_trigram_active` (GIN `gin_trgm_ops`, para el «contiene» de CA-01.2), más la extensión `pg_trgm`. Tres pruebas de motor escritas antes y vistas fallar en `<null>` |
| **T-14** | ✅ **Hecha** | **La correlación ya estaba** y no hizo falta código: el formato compacto emite `@tr` y `@sp` en cada línea de petición. Lo que faltaba era **la auditoría**, y ahora es guarda: `verify.sh` §6c cuenta las líneas correlacionadas y busca la clave enviada, el prefijo del hash y un JWT en el flujo real de logs. Las dos comprobaciones pasaron de entrada —son guarda, no motor— y se probaron contra un flujo sucio sintético |
| **T-21** | ✅ **Hecha** | **Ya estaba hecha y el enunciado no lo sabía.** `QuantityTests` cubre 0, −1 y `int.MinValue` y conserva 1, 7 y `int.MaxValue`; `UserTests` cubre el rol fuera del conjunto cerrado, los dos válidos, el rol por omisión y la normalización del nombre. RN-03 y RN-11 tienen dueño |
| **T-22** | ✅ **Hecha** | **Ya estaba hecha y el enunciado no lo sabía.** Las tres superficies que T-22 enumera están afirmadas: el agregado (`SaleImmutabilityTests`, 5 pruebas), la ruta HTTP (`SalesEndpointsRefuseMutationTests`: ningún verbo que reescriba o borre) y el puerto (`SaleWriteSurfaceTests`: el de salida solo deja añadir, el de entrada solo lee) |
| **T-15** | ✅ **Hecha** | Su criterio era que un evaluador entrara, creara un producto, vendiera y viera el reporte **sin abrir Swagger**. Recorrido entero el 2026-09-20. Se añadieron además las miniaturas de producto, que nunca se habían pintado, y la configuración regional `es-CO` |
| **T-16** | ✅ **Hecha** | Los tres README reescritos contra las cinco preguntas del artículo XIII, con cada comando ejecutado antes de escribirlo. **Nueve afirmaciones falsas** eliminadas, entre ellas la migración inicial "pendiente" (hay tres), "cinco páginas" (son seis), carpetas `components/` y resolvers que ya no existen, y Swagger en un puerto que `dotnet run` nunca usa. Los tres responden ya a **dónde están los datos** |
| **T-19** | ✅ **Hecha** | 63 comentarios traducidos y **17 borrados** en el servicio y el portal, 16 métodos de test renombrados al inglés, 8 separadores decorativos sustituidos por métodos con nombre (`Program.cs` pasa de 129 a 41 líneas). Cero comentarios en español en ambos repos |
| **T-20** | ✅ **Hecha** | Las invariantes bajaron al motor: **6 restricciones `CHECK` y 3 claves foráneas**, contadas contra `pg_constraint`. Incluye `FK_sale_item_product_product_id`, la barrera en que ADR-003 se apoyaba y que no existía |
| **T-18** | Pendiente | Verificación de punta a punta: el guion de §8 de [`spec.md`](spec.md), ejecutado y con constancia de lo que devolvió cada paso |
| **T-17** | Pendiente | Git y publicación. **Depende de T-18**: la regla del propietario es verificar primero y commitear después |

**Las cuatro reglas de negocio sin prueba tienen dueño desde esta pasada.** RN-03 y RN-11 son
**T-21**; RN-07 es **T-22**; RN-08 es **T-09**, con la barrera del motor en **T-20**. La
trazabilidad regla por regla —quién la hace cumplir, qué test la cubre, qué criterio la ejerce—
vive en [`spec.md` §4](spec.md), y es el único sitio donde vive.

Los defectos que aparecieron construyendo las tareas ya hechas —y por qué el compilador no ve
ninguno— están inventariados en `../traspaso/HANDOFF-TECNICO.md` §6.

---

## Fase 0 — Cimientos. Bloquean todo lo demás

### T-01 · Probar que los value objects se materializan

**Antes que cualquier repositorio.** Test de integración contra Postgres real: persistir y releer
un producto y una venta, y comprobar que `Money` y `Quantity` se reconstruyen sin lanzar.

- **Por qué primero:** los constructores validan y el ORM materializa por conversor. Si esto
  falla, falla toda la persistencia, y descubrirlo después de escribir cuatro repositorios
  cuesta cuatro veces más (D-07).
- **Hecho cuando:** el test pasa contra el contenedor, y falla si se le quita el conversor.

### T-02 · Migración inicial y categorías semilla

Generar la migración desde las configuraciones existentes. Sembrar las categorías en la misma
migración, con identificadores fijos.

- **Depende de:** T-01.
- **Por qué las categorías no son opcionales:** el repositorio de categorías es de solo lectura y
  la categoría del producto es obligatoria. **Sin semilla no se puede crear ni un producto** y el
  CRUD del enunciado no se puede ni ejercer (CA-08.3).
- **Hecho cuando:** sobre una base vacía, arrancar el servicio crea el esquema y deja categorías
  consultables. Sin ejecutar SQL a mano (CA-08.2).
- **El esquema que esta tarea creó ya no se llama igual.** Las cinco tablas pasaron a **singular**
  —`category`, `product`, `sale`, `sale_item`, `user`— en la migración
  `20260919215344_RenameTablesToSingular`, que arrastra los nombres derivados y el único `CHECK`
  escrito a mano. **Ninguna columna se renombró.** La convención y su porqué están en
  [`data-model.md` §0](data-model.md); la lista completa de migraciones, en su §3.2. Aquí no se
  repite.

### T-03 · Repositorio de infraestructura

`simple-stock-flow-infra` con el compose que levanta Postgres, la API y el portal.

- **Depende de:** T-02.
- **Límite duro:** **ni una línea de DDL** (artículo V). El contenedor crea la base vacía; el
  esquema lo pone el servicio. Lo que el repositorio no lleva nunca, en
  [`architecture.md` §8](architecture.md).
- **Contrato que esta tarea impone a T-06:** las credenciales del primer administrador llegan por
  `Bootstrap__AdminUsername` y `Bootstrap__AdminPassword`. Hoy el servicio **no las lee**.
- **Hecho cuando:** sobre un equipo limpio con Docker y nada más, un solo comando levanta los tres
  servicios; el portal responde en el puerto publicado; la API crea el esquema sola en el primer
  arranque; reiniciar **conserva** datos y binarios; y **arrancar sin `Jwt__SigningKey` falla con
  un error explícito**, no con una clave de relleno.

---

## Fase 1 — Cerrar los casos de uso. Test primero, siempre

### T-04 · `ProductCatalogService`

Los **siete** métodos: crear, actualizar, dar de baja, obtener, listar paginado, adjuntar imagen
y **listar las categorías de referencia**.

- **Depende de:** T-02.
- **Cubre:** CA-02.1 a CA-02.4, CA-01.1 a CA-01.3, CA-01.5, CA-03.1, CA-03.2.
- **El séptimo método no estaba ni en el código ni en el contrato.** `GET /api/categories` lo
  **invoca ya el portal**, y sin él no se puede pintar el selector de categoría de ningún
  formulario de producto — y sin categoría no se crea un producto (D-10). Sale de
  `ICategoryRepository.ListAsync`, que **ya existe**: falta el método en el puerto entrante y un
  controlador de tres líneas. Forma y alcance en
  [`architecture.md` §3.1](architecture.md) — solo lectura, sin paginar.
- **Cuidado con el orden al dar de baja:** anular la clave de imagen y confirmar **antes** de
  borrar el binario (D-08). Al revés queda una imagen rota permanente (CA-03.3).
- **Hecho cuando:** cada criterio de aceptación tiene su test en `Application.UnitTests` con los
  puertos en doble, y el servicio no contiene ni un `if` de negocio (Artículo VI).

### T-05 · Guarda de moneda en `Sale.AddItem`

Rechazar la línea cuyo producto no comparta moneda con la primera línea de la venta.

- **Depende de:** nada.
- **Cubre:** RN-09.
- **Criterio reescrito el 2026-09-21, porque el original no se podía satisfacer nunca.** Pedía
  *«un test de dominio que construye la venta mixta y espera el rechazo»*. Esa venta **no se puede
  construir**: ningún producto puede tener otra moneda, así que el test no describía una tarea
  pendiente sino una puerta sin salida. Un «hecho cuando» que nadie puede cumplir no es un
  criterio, es un pendiente eterno heredado de un enunciado mal escrito.
- **Dónde se hace cumplir RN-09, que es donde sí es alcanzable:** en `Product`, la única puerta por
  la que una moneda ajena puede entrar. `ChangePrice(Money)` la descartaba **en silencio**
  —`price.Amount`, y la moneda se perdía—, así que un `Money` en USD se guardaba como COP sin que
  nadie se enterara. Ahora se rechaza en voz alta, y en **las dos** puertas: `ChangePrice(Money)` y
  `Product.Create(name, Money, …)`, que se saltaba la guarda entera sacando el importe antes.
- **Hecho cuando:** existen pruebas de dominio que construyen un producto en otra moneda y esperan
  el rechazo, por las dos puertas. *(`Refuses_a_price_in_a_currency_the_system_does_not_keep` y
  `Changing_the_price_to_another_currency_is_refused_the_same_way`.)*

### T-06 · `AuthenticationService` y arranque del administrador ✅

Autenticar, registrar, y crear el usuario administrador inicial al arrancar si no existe.

- **Depende de:** T-02.
- **Cubre:** CA-07.1, CA-07.2, CA-07.5, CA-07.6.
- **Reglas duras:** credenciales del administrador desde el entorno, **nunca versionadas**
  (Artículo IX). El rechazo de credenciales **no distingue** usuario inexistente de contraseña
  incorrecta. El resultado de autenticar **jamás** incluye el hash.
- **Hecho cuando:** los tests cubren los cuatro criterios y un test de integración confirma que
  dos altas concurrentes del mismo usuario dejan una sola fila.
- **Lo que esta tarea NO cubrió, dicho para que no se dé por hecho:** **CA-07.7** —el rol en
  conjunto cerrado, RN-11— no tiene prueba. `Roles.IsValid` se invoca en el constructor de `User`
  y **ningún test lo ejerce**. Es de **T-21**, no de aquí.

### T-07 · `GetSaleService`

Obtener una venta con sus líneas y listar por rango paginado.

- **Depende de:** T-02.
- **Cubre:** CA-05.1 a CA-05.4. **CA-05.5 no**: la inmutabilidad de la venta es estructural y la
  prueba **T-22**.
- **Hecho cuando:** el total devuelto se calcula desde las líneas y hay un test que lo prueba
  contra la suma, no contra un valor fijo.
- **Límite que esta tarea no cruza:** el puerto `IGetSale` es de **solo lectura** y se queda así.
  Ni un método que actualice, ni uno que anule — RN-07 (`spec.md` §4).

### T-08 · `SalesReportService` y su puerto de lectura

Puerto outbound de solo lectura que devuelve filas ya agregadas, e implementación que agrupa en
el motor.

- **Depende de:** T-02. **DP-01 ya está decidida**, no pendiente
  ([`spec.md` §7.0](spec.md)).
- **Cubre:** CA-06.1, CA-06.2, CA-06.5.
- **Prohibido:** cargar las ventas del rango y sumarlas en memoria. Es el defecto que el propio
  `TODO` del servicio advierte, y lo que el enunciado evalúa con más peso.
- **DP-01, decidida el 2026-09-19 — ya no es provisional:** agrupar por identificador de producto
  y tomar el nombre de la **línea de la venta más reciente del rango**, con función de ventana en
  la consulta agregada. **Escrito como tal en el código**, no disimulado: la cláusula de ordenación
  es el punto donde se lee la decisión. El nombre vivo del catálogo no se consulta nunca aquí. El
  porqué está en [`spec.md` §7.1](spec.md).
- **DP-02, decidida el 2026-09-19:** el reporte **no** se desglosa por vendedor. La firma del
  puerto de lectura no acepta vendedor ni como filtro ni como agrupación, y `SalesReportRow` no
  lleva ese campo.
- **Hecho cuando:** un test de integración compara el resultado contra un conjunto de ventas
  conocido, otro confirma que el reporte vacío devuelve vacío y no error, y **un tercero vende,
  renombra el producto y comprueba que el reporte muestra el nombre más reciente del rango** —que
  es la única forma de que DP-01 deje de ser una frase en un documento.

---

## Fase 2 — Robustez. Lo que separa una prueba aprobada de una prueba buena

### T-20 · Las invariantes que `plan.md` promete y el motor no tiene

**El número es el siguiente libre; el sitio es este.** Depende solo de T-02, no bloquea a nadie, y
aun así va la primera de la fase por la razón del apartado *Por qué ahora*.

`plan.md` §2 describe un esquema con restricciones que **el motor no tiene**. Contrastado contra
`pg_constraint`, lo único que existe hoy son las cuatro claves primarias, los dos únicos de
`category.name` y `user.username`, dos claves foráneas y **un solo `CHECK`**:
`ck_product_stock_non_negative`. Todo lo demás de §2 es una promesa del documento.

**El criterio que lo justifica, en una frase.** ADR-002 estableció para `stock >= 0` que *"si la
restricción salta, algo escribió fuera del adapter"*. Ese criterio **no se aplicó a ninguna otra
invariante**. Una invariante que solo vive en C# protege a la aplicación, no a los datos: cualquier
`psql`, cualquier script de migración y cualquier servicio futuro la saltan sin enterarse.

**Por qué ahora y no después.** `product`, `sale` y `sale_item` tienen **0 filas** (verificado).
Todo esto se pone hoy con `ALTER TABLE`, sin migración de datos y sin riesgo. **Con la primera venta
real, arreglar la nulabilidad de `sale_item.sale_id` deja de ser gratis**: habría que decidir qué
hacer con las líneas huérfanas antes de poder imponer `NOT NULL`, y en una tabla que por política
nunca se edita (`plan.md` §4.1) eso no tiene ninguna respuesta buena. **Esa ventana es el argumento
de prioridad, y se cierra sola.**

**Lo que hay que poner, sin margen de interpretación:**

| # | Objeto | Definición exacta |
|---|---|---|
| 1 | `sale_item.sale_id` | **`SET NOT NULL`**. La clave foránea `FK_sale_item_sale_sale_id` conserva su `ON DELETE CASCADE` intacto |
| 2 | `FK_sale_item_product_product_id` | `sale_item.product_id → product.id` · **`ON DELETE RESTRICT`** · `ON UPDATE NO ACTION`. El índice que sirve al lado referenciante, `IX_sale_item_product_id`, **ya existe**: no hay que crearlo |
| 3 | `IX_sale_item_sale_id_product_id` | **Único** sobre `(sale_id, product_id)`, **incluyendo `quantity` y `unit_price`** — las dos columnas que `plan.md` §3.2 nombra, y ninguna más. En la **misma** migración se borra `IX_sale_item_sale_id`, al que el compuesto deja redundante |
| 4 | `ck_product_price_positive` | `CHECK (price > 0)` |
| 5 | `ck_sale_item_quantity_positive` | `CHECK (quantity > 0)` |
| 6 | `ck_category_name_not_blank` | `CHECK (btrim(name) <> '')` — «no vacío» incluye la cadena de solo espacios |
| 7 | `ck_user_role_allowed` | `CHECK (role IN ('admin', 'seller'))` — los dos únicos valores de `Roles` en el dominio, verificados en el código |
| 8 | `ck_user_username_normalized` | `CHECK (username = lower(btrim(username)))` — **minúsculas y sin espacios en los extremos**, que es exactamente lo que hace `User.NormalizeUsername` (`Trim().ToLowerInvariant()`). Comprobar solo las minúsculas dejaría pasar `' ana '`, que el dominio guardaría como `ana`: la restricción y el código dirían cosas distintas |

**Orden obligado dentro de la tarea: 1 antes que 3.** Una columna nulable deja inservible el
compuesto único, porque en SQL dos nulos nunca chocan entre sí: la unicidad no protegería nada.

**Nombres.** Los `CHECK` se escriben a mano, en `snake_case`, con el patrón `ck_{tabla}_{regla}`,
igual que el `ck_product_stock_non_negative` que ya existe. Lo que genera EF conserva su estilo
`PK_` / `IX_` / `FK_`. Hoy conviven los dos estilos sin que nadie lo hubiera puesto por escrito.

**La clave foránea del punto 2 no es una decisión nueva.** **ADR-003 ya la decidió** y se apoya en
ella de forma explícita: la baja lógica funciona porque *"la clave foránea hacia el producto queda
como barrera de última instancia para que un borrado manual falle ruidosamente"*. Nunca se
implementó. Dicho de otro modo: **ADR-003 está apoyado sobre una barrera que no existe**, y esta
tarea la construye.

**Nota fina sobre `price > 0`:** `Money` admite importe 0. La única guarda del sistema vive en
`Product.ChangePrice` —por la que pasa también la creación—, así que un precio 0 escrito por
cualquier otra vía entra sin resistencia.

**Las filas que ya existen no estorban, y se ha comprobado.** `category` tiene sus 5 filas semilla
con nombre no vacío, y `user` tiene 1 —el administrador de arranque de T-06— con `role = 'admin'` y
`username = 'admin'`, ya en minúsculas. **Las ocho restricciones se validan contra los datos
actuales sin rechazar ni una fila.**

- **Depende de:** T-02. Nada más.
- **Cubre:** la brecha entre `plan.md` §2 y el motor. No añade ningún criterio de aceptación nuevo:
  baja al motor lo que el documento ya prometía.
- **Hecho cuando:** por cada una de las ocho, un `INSERT` o `UPDATE` escrito **a mano en `psql`**
  que la viole **falla**, y se deja constancia del comando y del mensaje de error. Es la
  comprobación correcta porque **hoy las ocho pasarían sin protestar**: ahí está exactamente la
  tarea. Un `CHECK` que nadie intentó violar no está probado, está escrito.

### T-09 · Baja lógica

Propiedad sombra `deleted_at`, filtro global de consulta, y los tres contratos de repositorio de
D-03. La baja pasa a ser un `UPDATE`; el `DELETE` desaparece.

- **Depende de:** T-04.
- **Cubre:** CA-02.5, CA-02.6, CA-01.4, RN-08.

**Lo que se añade al esquema, sin margen de interpretación:**

| Qué | Definición exacta |
|---|---|
| Columna | `sales.product.deleted_at` · **`timestamptz`, nulable** · **sin `DEFAULT`** |
| Significado | `NULL` = producto activo. No nula = instante de la baja, en UTC |
| Claves foráneas | **Ninguna se añade ni se cambia.** `FK_product_category_category_id` sigue `ON DELETE RESTRICT` |
| Índices | **Ninguno aquí.** Los dos parciales que usan este predicado son T-13, y ese orden es deliberado |

El tipo no es una elección de esta tarea: **toda marca de tiempo del sistema es `timestamptz`**, sin
excepción, y el motor corre en UTC. La nulabilidad tampoco: ADR-003 descarta la marca booleana
precisamente porque *"la fecha nula funciona como predicado de índice parcial"*, y una columna no
nula con valor centinela no serviría para eso.

La migración corre sobre `product` **vacía** —0 filas, verificado—: la columna entra sin relleno y
sin valor por defecto temporal.

**Unicidad de `product.name`: no se crea, y es una decisión, no un olvido.** ADR-003 exige que
*"cualquier restricción de unicidad que se añada sobre el producto debe ser parcial sobre los
activos"*. Es una regla **condicional**, y hoy no hay ninguna unicidad sobre `product.name`: el
único índice sobre esa columna es `IX_product_name`, que **no es único** (verificado). Ningún
criterio de aceptación pide que dos productos no puedan llamarse igual, así que crearla sería
inventar una regla de negocio que nadie pidió. Queda escrito para que no se vuelva a preguntar:

- **T-09 no crea unicidad sobre `product.name`.** La exigencia de ADR-003 no aplica porque no hay
  nada a lo que aplicarse.
- **Si alguna vez se añade**, la forma obligada es un índice único **parcial**, con el predicado de
  activos, nunca uno total: si no, un producto retirado reservaría su nombre para siempre. Ese día
  la regla de ADR-003 entra en vigor sin discusión.

- **El detalle que decide si funciona:** el filtro debe aplicarse **también** al cargar productos
  para vender. Sin eso **se puede vender un producto retirado**, que es el defecto que la baja
  lógica venía a evitar.
- **Hecho cuando:** hay un test de integración que da de baja un producto vendido y comprueba las
  tres cosas: desaparece del catálogo, no se puede vender, y su venta sigue consultable.
- **Y una cuarta, que es la que convierte RN-08 en una regla y no en una intención.** Hoy
  **RN-08 no tiene ni una prueba** ([`spec.md` §4](spec.md)), y esta tarea es su único dueño. El
  test debe afirmar que **el producto vendido sigue existiendo como fila** después de la baja —no
  solo que no se lista—, porque «se da de baja» y «se borra y la consulta no falla» son
  indistinguibles desde el catálogo y radicalmente distintos para el histórico. La forma
  concreta: dar de baja, consultar la venta, y comprobar que la línea conserva el nombre y el
  precio congelados.
- **Lo que esta tarea borra del código, y hay que comprobar que desaparece:** `IProductRepository`
  pierde `Remove(Product)`. Mientras ese método exista, el borrado duro sigue siendo alcanzable
  desde el caso de uso —es lo que el `TODO` de `ProductCatalogService.DeleteAsync` manda llamar
  hoy— y RN-08 dependería de que nadie lo invoque. **Una regla que depende de la buena voluntad
  del que escribe el siguiente método no está cerrada.** La barrera del motor, la clave foránea
  `ON DELETE RESTRICT` de `sale_item` hacia `product`, la pone **T-20 punto 2**: las dos son
  necesarias y ninguna sustituye a la otra.

### T-10 · Concurrencia optimista sobre el stock

Token de versión sobre la columna de sistema, reintento acotado del caso de uso completo, y
conflicto hacia el cliente cuando se agotan los intentos. Restricción `stock >= 0` en la base
como última barrera.

- **Depende de:** T-02.
- **Cubre:** CA-04.6, RNF-01.
- **Dos errores que anulan la tarea:** reintentar solo la escritura en vez del caso de uso
  entero, y dejar que la excepción de concurrencia del ORM salga del adapter (D-04).
- **Hecho cuando:** un test lanza dos ventas simultáneas del último ejemplar: una confirma, la
  otra recibe conflicto, y el stock final no es negativo.
- **Hecha.** Dos cosas que el plan no anticipaba y que quien mantenga esto debe saber:
  1. **`UseXminAsConcurrencyToken()` está obsoleto en Npgsql 8** y genera lo mismo, así que no
     resolvía nada. Se usó la propiedad sombra, que es la forma vigente. Las trampas de la
     migración están en `../traspaso/HANDOFF-TECNICO.md` §4.3.
  2. **El reintento vive en el caso de uso, no en un decorador de composición** — el plan lo
     dejaba abierto. El puerto de unidad de trabajo ganó `DiscardChanges()`, y cada intento
     **empieza descartándose**. Sin eso el segundo intento arrastra el descuento del primero y
     descuenta dos veces: la forma exacta del error que ADR-002 advierte.

### T-11 · Congelar la categoría en la línea de venta

Añadir el nombre de categoría a `SaleItem`, pasar la categoría a `Sale.AddItem`, y resolverla en
el caso de uso cargando las categorías sembradas.

- **Depende de:** T-08.
- **Cubre:** CA-06.3, CA-06.4, RNF-02.

**Lo que se añade al esquema, sin margen de interpretación:**

| Qué | Definición exacta |
|---|---|
| Columna | `sales.sale_item.category_name` · **`varchar(120)`, `NOT NULL`** · **sin `DEFAULT`** |
| Por qué 120 | Es una **copia congelada** de `category.name`, que es `varchar(120)` (verificado). Un límite menor truncaría al copiar; uno mayor admitiría lo que el origen no puede producir |
| Claves foráneas | **Ninguna, y es el punto entero de la tarea.** Si apuntara a `category`, renombrar una categoría reescribiría el histórico, que es justo lo que esto evita |
| Índices | Ninguno. Ningún patrón de acceso de `plan.md` §3.1 filtra ni ordena por el nombre de categoría de la línea |

**`NOT NULL` es gratis hoy y deja de serlo mañana.** `sale_item` tiene **0 filas** (verificado): la
columna entra directamente no nula, sin relleno y sin valor por defecto temporal. **Con la primera
venta real esa ventana se cierra.** A partir de ahí la migración tendría que rellenar las líneas ya
escritas con la categoría **vigente** del producto —que no es necesariamente la que tenía cuando se
vendió—, y el dato congelado nacería falso; la alternativa sería admitir nulos para siempre y
partir el reporte en dos casos. Ejecutar esta tarea antes de la primera venta no es una preferencia
de orden: es la diferencia entre un histórico fiel y uno reconstruido a ojo.

- **Coste declarado:** cambia la firma de un método del agregado y añade una dependencia al
  camino de escritura. Se paga porque sin ello **recategorizar un producto reescribe reportes de
  períodos ya cerrados**.
- **Hecho cuando:** el test de fidelidad pasa: vender, renombrar, recategorizar, dar de baja, y
  el reporte del período no cambia.

### T-12 · Autoría de la venta con integridad referencial

Añadir el identificador de usuario a la venta, con clave foránea restrictiva, conservando el
nombre de usuario como atributo congelado del evento.

- **Depende de:** T-06.

**Lo que cambia en el esquema, que son dos cosas y no una:**

| Qué | Definición exacta |
|---|---|
| **Renombrado** | `sales.sale.sold_by` → **`sold_by_username`**. Tipo y nulabilidad **no cambian**: sigue `varchar(120)`, `NOT NULL` |
| **Columna nueva** | `sales.sale.sold_by_user_id` · **`uuid`, `NOT NULL`** · **sin `DEFAULT`** |
| **Clave foránea** | `sold_by_user_id → sales.user(id)` · **`ON DELETE RESTRICT`** · `ON UPDATE NO ACTION` |
| **Índice sobre la columna nueva** | **Ninguno, a propósito.** `plan.md` §3.3 lo descarta: ningún patrón de acceso lo usa, y la única consulta que lo justificaría —ventas por operador— cruza datos personales y está sin decidir (DP-02) |

**Sí, se renombra, y se alinea el código con la documentación y no al revés.** `plan.md` §2 y §4 ya
llaman `sold_by_username` a esta columna; el motor la llama `sold_by` (verificado). El nombre largo
es el correcto **en cuanto exista `sold_by_user_id` al lado**: con los dos campos juntos, `sold_by`
no dice cuál de los dos es. Efecto colateral que ya se estaba pagando: §4 clasifica como dato
personal una columna cuyo nombre no existe en ninguna parte del sistema.

**El renombrado no toca el contrato de API.** El campo que viaja al cliente es **`SaleView.SoldBy`,
y no cambia**: ni su nombre, ni su tipo, ni su presencia. El cambio se agota entre la entidad y la
tabla. Ningún consumidor del portal se entera, y no hay nada que versionar.

**`NOT NULL` también es gratis hoy.** `sale` tiene **0 filas** (verificado), así que la columna
entra no nula sin relleno. Con la primera venta habría que inventar un autor para las filas
anteriores o admitir nulos para siempre, y un autor inventado en un registro contable es peor que
no tener el campo.

**Por qué `RESTRICT` y no otra cosa:** ya está decidido en `plan.md` §2 y §4.1 — *"la autoría de una
venta no puede quedar huérfana"*. `CASCADE` borraría ventas al borrar un usuario, que contradice la
retención indefinida de §4.1, y `SET NULL` es incompatible con `NOT NULL`. Hoy **no existe ningún
puerto que borre usuarios**; `RESTRICT` es la barrera que hace que, el día que se añada uno, falle
ruidosamente en vez de callar.

- **Por qué los dos campos:** el identificador da integridad y sobrevive a un renombrado; el
  nombre da fidelidad a lo que se registró. Ninguno sustituye al otro.
- **Coste:** toca el agregado, el comando y el controller. **Es la tarea más prescindible de esta
  fase**: si hay que recortar alcance, se recorta esta y se dice por qué.
- **Hecho cuando:** no se puede insertar una venta con un autor inexistente, y existe el test que
  lo comprueba.

### T-13 · Índices según los patrones de acceso

**Faltan tres, no cinco.** El enunciado anterior de esta tarea era incorrecto: quien lo ejecutara al
pie de la letra intentaría crear índices que ya existen. Contrastado índice a índice contra el
motor, esto es lo que hay y lo que toca hacer:

| Índice de `plan.md` §3.2 | Estado real | Qué hace esta tarea |
|---|---|---|
| `product (category_id, name)`, **parcial sobre activos** | **No existe** | **Lo crea** |
| `product (name)` con **trigramas**, parcial sobre activos | **No existe** | **Lo crea**, junto con la extensión que lo hace posible |
| `sale_item (sale_id, product_id)` **único**, con cantidad e importe incluidos | **No existe** | **Aquí no se crea.** Es a la vez la restricción de unicidad y el índice, y lo crea **T-20** en su forma definitiva. Crearlo dos veces no tendría sentido |
| `sale (sold_at)` | **Ya existe** — `IX_sale_sold_at` | **Nada.** No se toca |
| `sale_item (product_id)` | **Ya existe** — `IX_sale_item_product_id` | **Nada.** No se toca |
| `category (name)` único · `user (username)` único | **Ya existen** | **Nada** |

**Sobre el `DESC` de `sale (sold_at)`:** en un índice de **una sola columna** el sentido es
cosmético — Postgres lo recorre hacia atrás sin coste añadido. El argumento que `plan.md` §3.2 da
para pedirlo descendente **no aplica a este caso**, y recrear el índice solo para invertirlo no
compra nada. Se deja como está.

**Lo que esta tarea borra**, que es tan parte del trabajo como lo que crea:

| Índice | Por qué se va |
|---|---|
| `IX_product_name` | El compuesto parcial lo cubre. **Se sustituye, no se suma** |
| `IX_product_category_id` | Igual. Cada índice de más encarece toda escritura, para siempre |
| `IX_sale_item_sale_id` | **Redundante** en cuanto exista el compuesto único (`plan.md` §3.3). **Lo borra T-20**, en la misma migración que crea el compuesto: dejarlo para esta tarea abriría una ventana en la que conviven los dos. Se dice aquí para que nadie lo busque en esta lista y concluya que se queda |

**La extensión `pg_trgm` la instala esta migración**, y no estaba dicho en ninguna parte:

- **No puede ir en `db/init/`** del repositorio de infraestructura: ADR-001 prohíbe ahí **toda línea
  de DDL**, y `CREATE EXTENSION` lo es.
- Va en la **misma migración de EF Core** que el índice de trigramas, ejecutada antes de crearlo.
- **No hace falta superusuario:** en PostgreSQL 16 `pg_trgm` es una extensión *trusted* (verificado
  en el motor), así que el propietario de la base la instala sin privilegios extra.
- Si la extensión se objeta, **el índice de trigramas es el primero que se cae** (`plan.md` §3.2) y
  el resto de la tarea sigue en pie sin tocarse.

- **Depende de:** T-09 (los parciales necesitan la columna de baja).
- **Requisito que los hace servir de algo:** el predicado de baja lógica debe aparecer
  **literalmente** en la consulta, o el motor no usará el índice parcial.
- **Hecho cuando:** los índices existen en la migración y ninguno indexa un booleano ni una
  enumeración de dos valores. **Y una comprobación más, porque el error probable es sumar en vez de
  sustituir:** tras aplicar la migración, `\d sales.product` muestra los dos parciales nuevos y
  **no** muestra `IX_product_name` ni `IX_product_category_id`.

### T-21 · Las invariantes de dominio que nunca se afirmaron

**El dominio tiene siete tipos y tres ficheros de test.** `MoneyTests`, `ProductTests` y
`SaleTests`. **`Quantity`, `Category` y `User` no tienen ninguno** (verificado el 2026-09-19), y
en los dos primeros huecos viven **RN-03 y RN-11**: las dos únicas reglas de negocio de §4 que
hoy no tiene nadie por dueño.

Esta tarea es su dueño. Son dos ficheros nuevos en `Domain.UnitTests`:

| Fichero | Qué afirma | Regla |
|---|---|---|
| `QuantityTests` | Construir `Quantity` con **0** lanza. Construirla con un **negativo** lanza. Construirla con 1 no lanza y conserva el valor | **RN-03** |
| `UserTests` | `User.Create` con un rol que no es `admin` ni `seller` lanza — **ese es el único punto donde `Roles.IsValid` se ejerce**. Y, de paso, que `NormalizeUsername` guarda `«  Ana  »` como `ana`, que hoy solo se prueba **de refilón**, a través de `Looks_the_user_up_by_the_same_name_the_domain_stores` en la capa de aplicación | **RN-11**, y el lado de minúsculas de **RN-10** |

- **Depende de:** nada. Es la tarea más barata del plan entero y cierra dos de las cuatro reglas
  sin prueba.
- **Cubre:** RN-03, RN-11, y directamente CA-04.5 y CA-07.7.
- **Qué no hace:** **no baja nada al motor.** `ck_sale_item_quantity_positive` y
  `ck_user_role_allowed` son **T-20**, puntos 5 y 7. Las dos capas hacen falta: el test de
  dominio prueba que la aplicación respeta la regla, el `CHECK` impide que un `psql` la salte.
- **`Category` se queda sin fichero, y es una decisión.** Su única guarda —nombre no vacío— no es
  una regla de §4: las categorías son datos de referencia sembrados y su CRUD está **fuera de
  alcance** ([`spec.md` §6](spec.md)). La invariante sí baja al motor en T-20 punto 6, que es
  donde importa, porque el riesgo real es un `INSERT` a mano y no un formulario que no existe.
- **Hecho cuando:** los dos ficheros existen, y **cada test falla si se le quita la guarda que
  afirma**. Un test que pasa con y sin la guarda no prueba la guarda: prueba el compilador.

### T-22 · Probar que una venta no se puede tocar

**RN-07 es la afirmación 2 de `spec.md` §1** —*«una venta registrada no se puede alterar
después»*— y hoy **no tiene ni una prueba**. Es cierta, y esa es justamente la trampa: se cumple
por **ausencia**, y una ausencia no protesta cuando desaparece.

Verificado hoy, la venta es inmutable porque:

- `Sale` no expone quitar ni reemplazar líneas —solo `Open`, `AddItem` y `EnsureConfirmable`—, y
  `Items` es un `AsReadOnly()` sobre la lista interna.
- `SaleItem` no tiene un solo setter público.
- `ISaleRepository` tiene exactamente cuatro métodos: `FindAsync`, `SearchAsync`,
  `ListByRangeAsync` y `AddAsync`. **Ninguno actualiza ni borra** — y el contraste está a la
  vista: `IProductRepository` sí tiene `Remove`.
- `api/sales` publica **un `POST` y dos `GET`**. No hay `PUT`, ni `PATCH`, ni `DELETE`.

**Nada de eso está afirmado en ningún sitio.** Una pasada futura puede añadir un
`Sale.RemoveItem` «solo para un caso», un `ISaleRepository.UpdateAsync` que el ORM agradezca, o un
`DELETE /api/sales/{id}` para limpiar pruebas, **y el conjunto de tests seguiría entero en verde**.

**Cómo se prueba una regla que se cumple por ausencia.** No ejercitando una operación, sino
comprobando la **forma** del sistema. Tres afirmaciones, y las tres se pueden escribir hoy:

| # | Qué se comprueba | Cómo |
|---|---|---|
| 1 | El agregado no deja mutar una venta ya formada | La colección que devuelve `Sale.Items` **rechaza** que le añadan o le quiten un elemento. Es una afirmación de comportamiento, no de reflexión |
| 2 | El agregado no expone una puerta nueva | Por **reflexión** sobre `Sale` y `SaleItem`: el conjunto de miembros públicos que modifican estado es exactamente el declarado —`Open`, `AddItem`, `EnsureConfirmable`— y **ninguna propiedad tiene setter público**. Si alguien añade un método, el test falla y **obliga a justificarlo aquí**, que es el punto entero |
| 3 | La venta no se puede alterar desde fuera del agregado | Por **reflexión** sobre `ISaleRepository`: sus miembros son los cuatro nombrados y ninguno más. Y sobre `SalesController`: ningún método lleva `HttpPut`, `HttpPatch` ni `HttpDelete` |

- **Depende de:** nada. Se puede escribir hoy y **hoy pasa en verde** — que es lo correcto: esta
  tarea no arregla un defecto, **levanta una alarma sobre una propiedad que ya es cierta** para
  que deje de serlo con ruido en vez de en silencio.
- **Cubre:** RN-07, CA-05.5.
- **El riesgo de escribirlo mal, dicho una vez:** un test de reflexión que solo cuente métodos es
  frágil sin ser útil —se rompe al renombrar—. Los tres puntos afirman **capacidad de mutación**,
  no nombres: un método de lectura nuevo no debe romper nada; uno que escriba, sí.
- **Hecho cuando:** los tres pasan, y **se ha comprobado que fallan**: añadiendo temporalmente un
  `RemoveItem` al agregado y un `DELETE` al controlador, y viéndolos ponerse en rojo antes de
  deshacerlo. Un test estructural que nadie vio fallar no prueba la estructura.

---

## Fase 3 — Entrega

### T-14 · Registro estructurado

**Está casi hecho, y conviene no rehacerlo:** el arranque ya configura Serilog con formato JSON
compacto en consola y registro por petición. Lo que falta es poco y concreto:

- Añadir el **identificador de correlación** a cada línea.
- **Auditar que no se filtra nada**: ni token, ni hash, ni nombre de usuario en claro donde no
  corresponda (`plan.md` §4).
- **Cubre:** RNF-05.
- **Hecho cuando:** una petición fallida se sigue de punta a punta por su correlación, y una
  búsqueda de los campos sensibles en la salida de logs no devuelve nada.

### T-15 · Portal: CRUD y reporte contra la API real

El front ya tiene dominio, puertos, casos de uso y repositorios HTTP. Falta cerrar el circuito:
pantallas de catálogo con alta, edición y baja; carrito de venta; y reporte por rango.

- **Depende de:** T-04, T-07, T-08, T-06.
- **Hecho cuando:** un evaluador entra, crea un producto, vende, ve el stock bajar y ve la venta
  en el reporte, **sin abrir Swagger**.

### T-16 · Los tres README ✅

Uno por repositorio: qué es, cómo se levanta, cómo se prueba, **y qué falta**.

- **Cubre:** CA-08.4, Artículo X.
- **Hecho cuando:** ninguna afirmación del README es falsa. Si algo no corre, el README lo dice
  en vez de omitirlo.

### T-17 · Git y publicación

Commits con historia legible en los tres repositorios, y publicación en GitHub. Hoy **no hay ni
un commit**: todo el trabajo existe solo en disco.

- **Depende de:** **T-18**. Regla del propietario del proyecto: **no se commitea nada hasta tener
  una versión funcional probada.** El orden es verificar primero y commitear después, no al revés.
- **Consecuencia asumida:** hasta entonces el trabajo vive sin red. Un borrado accidental no tiene
  vuelta atrás, y eso es una decisión tomada a sabiendas, no un olvido.
- **Hecho cuando:** los tres repositorios clonan y levantan desde cero siguiendo su README, que
  es literalmente lo que el enunciado pide entregar.

### T-18 · Verificación de punta a punta

Ejecutar el criterio de terminado de `spec.md` §8, los siete pasos, sobre un equipo limpio.

- **Depende de:** todo lo anterior.
- **Hecho cuando:** los siete pasos pasan y se deja constancia de qué se ejecutó. Un criterio de
  terminado que nadie ejecutó no es un criterio: es una lista de deseos.

---

### T-19 · Idioma y comentarios ✅

Alinear el código con los artículos XI y XII.

- **Depende de:** nada. **Sigue aplicándose a cada archivo que se toque**: la tarea se cerró una
  vez, pero la regla es permanente.
- **Hecho cuando:** no queda un comentario en español, ni un separador decorativo, ni un comentario
  que se pueda borrar sin perder información.
- **Hecha.** Tres cosas que conviene saber:
  1. **Lo que más valió fue borrar, no traducir.** 17 comentarios se fueron enteros porque repetían
     la firma que tenían debajo (`/** Adjunta el JWT a cada request */` sobre `authInterceptor`).
     Traducir un comentario inútil solo lo hace inútil en otro idioma.
  2. **Los letreros decorativos no se traducen: se sustituyen por métodos con nombre.** Las cinco
     secciones de `Program.cs` salieron a `HostConfiguration.cs` —`AddJwtAuthentication`,
     `AddPortalCors`, `AddSwaggerWithBearerAuth`, `ApplyMigrationsAsync`, `UseMediaFiles`— y el
     archivo pasó de 129 a 41 líneas. `AddHexagon` quedó en tres llamadas encadenadas.
  3. **La frontera del artículo XI tenía un hueco, y el primer parche lo empeoró.** No decía
     dónde caen los diagnósticos de arranque; se resolvió declarándolos en español, y el efecto
     fue dejar archivos **mezclados** —`verify.sh` con comentarios en inglés y sus treinta y tres
     mensajes en español—. La excepción se retiró: ahora una sola pregunta, «¿quién lo lee?»,
     decide sin discutir. **Una frontera con excepciones no es una frontera.**

**Brecha que esta pasada cerró y nadie había mirado:** los nombres de método de las pruebas.
`Domain.UnitTests` y `PlaceSaleServiceTests` los tenían en español mientras el resto de
`Application.UnitTests` y los de integración ya estaban en inglés. Son identificadores, no texto:
16 renombrados.

---

## Orden recomendado

```
T-01 ─► T-02 ─► T-03
         │
         ├─► T-20  (las invariantes que faltan — cuanto antes, ver abajo)
         ├─► T-04 ─► T-09 ─► T-13
         ├─► T-06 ─► T-12
         ├─► T-07
         └─► T-08 ─► T-11
T-05  (independiente, en cualquier momento)
T-10  (tras T-02, en paralelo con la fase 1)
T-21  (independiente — RN-03 y RN-11. La más barata del plan)
T-22  (independiente — RN-07. Se escribe hoy y hoy pasa: esa es la idea)

Fase 3: T-14 · T-15 · T-16 ─► T-18 ─► T-17
                                      └─ verificar primero, commitear después

T-19  ✅ hecha una vez; la regla sigue vigente en cada archivo que se toque
```

**Sobre T-20 y por qué va primera de su rama sin bloquear a nadie.** Su coste **crece con la primera
venta** y el de las demás no: mientras `product`, `sale` y `sale_item` estén vacías es un
`ALTER TABLE` y nada más. T-20 y T-13 son ramas **independientes**: T-13 ya no crea el único
compuesto de `sale_item` —lo crea T-20, que es donde vive la restricción— y los parciales de T-13
solo necesitan la columna de T-09. Si ambas se hacen, **T-20 antes**, para que `IX_sale_item_sale_id`
no sobreviva a la migración que lo vuelve redundante.

**Si hay que recortar alcance**, el orden de sacrificio es: T-12, luego T-14, luego T-13. **No se
recortan** T-09, T-10 ni T-11: son las tres que sostienen las tres afirmaciones del problema en
`spec.md` §1, y sin ellas el sistema parece funcionar y no funciona. **Tampoco se recorta T-20**:
es la más barata de todas hoy y la más cara de todas después.

**T-21 y T-22 tampoco se recortan, y cuestan un rato.** Entre las dos son unos pocos ficheros de
test y **ni una línea de producción**. Sin ellas, cuatro de las doce reglas de `spec.md` §4 siguen siendo
intenciones: RN-03 y RN-11 porque nadie las afirma, RN-07 porque se cumple por ausencia y nadie
vigila esa ausencia. **Recortar la prueba de una regla no recorta trabajo: recorta la regla.**

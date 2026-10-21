# Contrato de API — Simple Stock Flow

> **Qué es este documento.** La forma exacta de cada petición y de cada respuesta de la API, con
> **todas** las decisiones de comportamiento tomadas. Sustituye a la lista de huecos abiertos que
> vivía en [`architecture.md`](architecture.md) §3.1.1: aquí no queda ninguno abierto.
>
> **Para qué sirve.** Para que T-04, T-06, T-07 y T-08 se puedan implementar **sin inventarse nada**.
> Si al escribir código hay que elegir entre dos formas de responder y este documento no lo dice,
> eso es un fallo de este documento, no una libertad del implementador.
>
> **Verificado el 2026-09-19 y corregido el 2026-09-20** contra el sistema levantado. Cada
> afirmación se sondeó con `curl`. **Ya no queda nada «no observable»:** las quince operaciones
> responden, ninguna devuelve 500, y lo que cambió desde la firma está en el
> [anexo B](#anexo-b--registro-de-deuda-declarada), fila por fila.

---

## 1. Convenciones comunes

Todo lo de esta sección aplica a **todos** los endpoints y no se repite en cada ficha.

| Aspecto | Valor |
|---|---|
| **Base directa** | `http://localhost:5000` — el puerto del servicio, solo en desarrollo |
| **Base a través del portal** | `http://localhost:8080` — nginx proxea `/api/` y `/media/` hacia el servicio. **Con una excepción medida: ver [E-15](#e-15--get-mediakey)** |
| **Formato** | JSON, `UTF-8` |
| **Nombres de campo** | **camelCase**, en petición y respuesta. No hay ninguna opción de serialización configurada en el proyecto: es el comportamiento por omisión de ASP.NET Core, verificado en la respuesta de `login` (`accessToken`, `expiresAt`) |
| **Autenticación** | `Authorization: Bearer <jwt>`. El token lo emite `POST /api/auth/login` y vive **60 minutos** (`Jwt:LifetimeMinutes`) |
| **Reloj admitido** | `ClockSkew` de **30 segundos** en la validación del token |
| **Identificadores** | `uuid` en texto, forma canónica con guiones, en minúsculas |
| **Importes** | JSON `number` con **dos decimales**, redondeo *half away from zero* (`Money`) |
| **Moneda** | Siempre `"COP"`. **Nunca `null`, nunca cadena vacía** — ver [D-C10](#d-c10--currency-nunca-viaja-nulo) |
| **Fechas de respuesta** | ISO 8601 con desplazamiento explícito, tal como las emite `DateTimeOffset`: `2026-09-19T22:34:36.6266451+00:00` |
| **CORS** | Solo los orígenes de `Cors:Origins`. En el compose el portal no cruza origen: nginx proxea |

### 1.1 Roles

Dos, y solo dos: **`admin`** y **`seller`** (`Roles.IsValid`). Cualquier otro valor lo rechaza el
dominio. "Autenticado" en las fichas significa *cualquiera de los dos*.

### 1.2 Paginación

Las dos colecciones paginadas —productos y ventas— comparten petición y respuesta.

**Parámetros de consulta:**

| Parámetro | Tipo | Obligatorio | Por omisión | Límites |
|---|---|---|---|---|
| `page` | entero | no | `1` | Menor que 1 → **se sirve 1** |
| `size` | entero | no | `20` | Ausente o menor que 1 → **20**. Mayor que 100 → **100** ([D-C5](#d-c5--un-size-por-encima-del-máximo-se-recorta-al-máximo)) |

Un `page` o un `size` que **no sean enteros** son un error de enlace: **400** con la forma de
[§2.2](#22-el-400--errors-sin-detail). Verificado: `?size=abc` → 400 (sonda 34).

**Respuesta — `PagedResult<T>`:**

| Campo | Tipo | Nulo | Qué es |
|---|---|---|---|
| `items` | `T[]` | no | La página. **Vacía es `[]`**, nunca `null` |
| `page` | `number` | no | La página **servida**, ya recortada |
| `size` | `number` | no | El tamaño **servido**, ya recortado — no el pedido |
| `total` | `number` | no | Total de elementos que casan con el filtro, **no** de la página |
| `totalPages` | `number` | no | `ceil(total / size)`; `0` si `size` es `0` — ver [D-C11](#d-c11--totalpages-es-la-parte-más-frágil-del-contrato) |

### 1.3 Rangos de fecha

Los usan `GET /api/sales` y `GET /api/reports/sales`, con las mismas reglas.

| Parámetro | Tipo | Obligatorio | Por omisión | Formato aceptado |
|---|---|---|---|---|
| `from` | instante | **sí** ([D-C4](#d-c4--from-y-to-son-obligatorios-de-verdad)) | — | **ISO 8601 con desplazamiento explícito** ([D-C3](#d-c3--solo-iso-8601-con-desplazamiento-explícito)) |
| `to` | instante | **sí** | — | igual |

- **Inclusividad: `from <= sold_at < to`.** El extremo inicial entra, el final **no**
  ([D-C2](#d-c2--from-inclusivo-to-exclusivo)).
- `to` anterior a `from` → **422**, `detail` = `"La fecha final no puede ser anterior a la inicial."`
  (`DateRange`, verificado en la sonda 11).
- `from == to` es un rango **vacío válido**, no un error: `DateRange` solo rechaza `to < from`.

---

## 2. Las tres formas del cuerpo de error

El cuerpo del error **no es uniforme hoy**. Las tres formas están medidas, y la decisión de qué
hacer con ellas está tomada en [D-C9](#d-c9--el-400-debe-llevar-detail).

### 2.1 El 422 y el 409 — `problem+json` con `detail`

Los emite `ExceptionTranslationFilter`, el **único** sitio donde una excepción se vuelve un código.

```json
{"title":"Regla de negocio violada","status":422,"detail":"La venta debe tener al menos un ítem."}
```

```json
{"title":"Conflicto con otra operación simultánea","status":409,"detail":"…"}
```

`Content-Type: application/problem+json; charset=utf-8`. **`detail` lleva el mensaje de dominio**,
en español, y es lo que la persona usuaria acaba leyendo.

### 2.2 El 400 — `errors`, sin `detail`

Lo emite la validación de modelo de ASP.NET Core, no el filtro.

```json
{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1",
 "title":"One or more validation errors occurred.",
 "status":400,
 "errors":{"Password":["The Password field is required."]},
 "traceId":"00-4bfcb5287103095ff8bda19dd280d169-5b6fa4dd7683d5d4-00"}
```

También es `application/problem+json`, **pero no trae `detail`** y sus textos están **en inglés**.

> **Consecuencia medida, no hipotética.** `error.interceptor.ts` enumera 0, 401, 403, 404, 409 y
> 422; para **todo lo demás —el 400 incluido—** lee `error.error?.detail`, que en un 400 no existe.
> El backend dice exactamente qué campo falló y **la persona usuaria ve *«Ocurrió un error
> inesperado.»***. El caso más visible es el formulario de producto: un precio no numérico
> devuelve `errors: {"$.price": […]}` y la pantalla no nombra el precio.

### 2.3 El 401, el 403 y el 404 — sin cuerpo

`Content-Length: 0`. No hay JSON que leer. El 401 añade `WWW-Authenticate: Bearer`, y
`Bearer error="invalid_token"` cuando el token existe pero no vale (sondas 2 y 20).

## 3. Las once decisiones, cerradas

Las ocho que `architecture.md` §3.1.1 dejaba abiertas como **C-1 … C-8**, más tres que el contrato
necesita y nadie había escrito. Ninguna vuelve a abrirse aquí.

| # | Antes | Ahora |
|---|---|---|
| [D-C1](#d-c1--get-apicategories-existe-devuelve-un-array-plano-ordenado-por-nombre) | C-1 · `GET /api/categories` no existe | **200 con un array plano, ordenado por nombre, vacío es `[]`** |
| [D-C2](#d-c2--from-inclusivo-to-exclusivo) | C-2 · inclusividad sin decidir | **`from` inclusivo, `to` exclusivo** |
| [D-C3](#d-c3--solo-iso-8601-con-desplazamiento-explícito) | C-3 · formato de fecha sin acotar | **ISO 8601 con desplazamiento obligatorio** |
| [D-C4](#d-c4--from-y-to-son-obligatorios-de-verdad) | C-4 · ausencia silenciosa | **Obligatorios de verdad: la ausencia es 400** |
| [D-C5](#d-c5--un-size-por-encima-del-máximo-se-recorta-al-máximo) | C-5 · `size` grande cae a 20 | **Se recorta a 100 y la respuesta devuelve `size: 100`** |
| [D-C6](#d-c6--register-deja-de-anunciar-location) | C-6 · `Location` a una ruta inexistente | **Se retira la cabecera `Location`** |
| [D-C7](#d-c7--un-id-mal-formado-es-404-y-se-documenta-como-tal) | C-7 · `{id}` mal formado da 404 | **Se documenta como 404; no se cambia** |
| [D-C8](#d-c8--el-403-se-mantiene-y-se-exige-la-prueba-que-lo-demuestre) | C-8 · el 403 nunca observado | **Se mantiene 403 vacío y se exige la prueba. Hueco declarado** |
| [D-C9](#d-c9--el-400-debe-llevar-detail) | — | **El 400 pasa a llevar `detail` además de `errors`** |
| [D-C10](#d-c10--currency-nunca-viaja-nulo) | — | **`currency` siempre `"COP"`, también en el reporte vacío** |
| [D-C11](#d-c11--totalpages-es-la-parte-más-frágil-del-contrato) | — | **`totalPages` es obligatorio y necesita un test de contrato** |

---

### D-C1 · `GET /api/categories` existe, devuelve un array plano ordenado por nombre

**Decisión.** `200 OK` con un **array JSON plano** —sin envoltorio de paginación— de objetos
`{id, name}`, **ordenado por `name` ascendente** con la intercalación de la base. Una lista vacía
es **`200 []`**, nunca 404 y nunca 204.

**Por qué.** El orden ya existía enterrado en la persistencia (`EfCategoryRepository.ListAsync`
hace `OrderBy(category => category.Name)`) y el portal lo pinta en un desplegable donde se ve:
declararlo lo convierte de accidente en obligación verificable.

**Consecuencias.**

- La intercalación de la base es **`en_US.utf8`** (verificado, sonda 28b), no ASCII: *Fontanería*
  va después de *Electricidad* y antes de *General*. Orden real hoy: `Electricidad`, `Fontanería`,
  `General`, `Herramientas`, `Pinturas` (sonda 26). Cambiar la intercalación de la base **cambiaría
  el contrato**.
- **204 queda prohibido** aunque no haya categorías: el portal hace `dtos.map(toCategory)` sobre el
  cuerpo, y un 204 sin cuerpo lo rompe.
- Sigue sin haber `POST`, `PUT` ni `DELETE` de categorías: [`architecture.md`](architecture.md) §3.1
  explica por qué, y D-10 lo cierra.

---

### D-C2 · `from` inclusivo, `to` exclusivo

**Decisión.** Una venta entra en el rango si **`from <= sold_at < to`**. Una venta cuyo `sold_at`
caiga **exactamente en `to` no entra**.

**Por qué.** Es la única de las dos opciones bajo la cual **dos reportes contiguos suman el total
del período**: `[a,b)` y `[b,c)` cubren `[a,c)` sin solaparse. Con `to` inclusivo, una venta en el
instante `b` se contaría en los dos y la suma de los parciales no cuadraría con el total.

**Consecuencias, que hay que escribir porque muerden.**

- `?from=2026-01-01T00:00:00Z&to=2026-01-31T00:00:00Z` **no incluye el 31 de enero**. Para «todo
  enero» se pide `to=2026-02-01T00:00:00Z`.
- **El portal hoy no lo hace.** `http-report.repository.ts` manda `range.to.toISOString()` tal cual:
  si la persona elige el 31 de enero en el selector, viaja `2026-01-31T00:00:00Z` y **se pierde
  todo el día 31**. Corregirlo es del portal —sumar un día al extremo, o llevarlo a fin de día—,
  no del backend. Queda como [hueco H-3](#5-huecos-declarados-con-su-dueño).
- El extremo inicial **sí** entra: una venta exactamente en `from` cuenta.

---

### D-C3 · Solo ISO 8601 con desplazamiento explícito

**Decisión.** `from` y `to` se aceptan **únicamente** con desplazamiento explícito: `Z` o `±HH:MM`.
Ejemplo válido: `2026-01-01T00:00:00Z`. Todo lo demás —`2026-01-01`, `2026-06-01T10:30`,
`01/06/2026`, un epoch— es **400** con la forma de §2.2 y `errors.from` / `errors.to`.

**Por qué.** Sin acotar, una fecha sin desplazamiento se interpreta con la zona del contenedor: el
mismo rango daría dos reportes distintos según dónde corra el proceso, y el reporte dejaría de ser
estable. Era una bomba de relojería que dependía de que nadie definiera `TZ`.

**Estado: cumplido.** Medido el 2026-09-20 — los cuatro casos responden **400** nombrando la causa:
`2026-09-01` sin desplazamiento, `01/06/2026`, `2026-09-01T10:30` y el rango ausente. Con `Z`
explícita, **200**.

---

### D-C4 · `from` y `to` son obligatorios de verdad

**Decisión.** **Obligatorios.** Su ausencia es **400** con la forma de §2.2. No hay rango por
omisión.

**Por qué.** No existe ningún rango por omisión defendible —¿el último mes?, ¿todo?— y cualquiera
que se eligiese devolvería un 200 que el consumidor interpretaría como *«no hubo ventas»*. Hoy la
ausencia enlaza a `DateTimeOffset.MinValue` **en silencio**: en cuanto T-07 y T-08 existan, omitir
los dos parámetros devolvería **200 con una página vacía**. Un reporte vacío por descuido es peor
que un error.

**Nota de honestidad: `../traspaso/HANDOFF.md` afirmaba que esto ya producía 400, y no es cierto.**

**Consecuencias, medidas el 2026-09-20 — la decisión está cumplida.**

| Petición | Antes de D-C4 | Hoy |
|---|---|---|
| `GET /api/reports/sales` | **500** — enlazaba `MinValue..MinValue` y moría en el caso de uso | **400**, `errors.from` y `errors.to` |
| `GET /api/reports/sales?from=2026-01-01` | **422** *«La fecha final no puede ser anterior a la inicial.»* — `to` caía a `MinValue` | **400**, `errors.to` |
| `GET /api/reports/sales?from=&to=` | **400** | **400**, igual |

Aquel 422 de la fila del medio era el síntoma más engañoso: acusaba a la fecha final de estar mal cuando
lo que pasa es que **no se envió**.

---

### D-C5 · Un `size` por encima del máximo se recorta al máximo

**Decisión.** Ya la tomó **CA-01.5** —*«el sistema aplica el máximo en lugar de rechazar la
petición»*— y el criterio gana. `size > 100` se sirve como **100**, y **la respuesta devuelve
`size: 100`**, no el valor pedido. `size` ausente o menor que 1 → **20**. `page` menor que 1 → **1**.

**Por qué el `size` de la respuesta es lo importante.** El portal pagina con el `size` que recibe y
con `totalPages`: devolver el `size` pedido mientras se sirven 100 elementos descuadraría el número
de páginas y el paginador saltaría filas.

**Estado.** Hoy `PageRequest` hace `Size is < 1 or > MaxSize ? 20 : Size`, es decir **cae a 20**, no
a 100, y trataba *«demasiado grande»* y *«ausente»* como el mismo caso. Era el defecto **A-3**, y
**está cerrado**: medido el 2026-09-20, `?size=999` devuelve `"size": 100` con la página servida, y
`?size=0` cae al valor por omisión.

| `size` pedido | `size` servido |
|---|---|
| ausente | 20 |
| `0` o negativo | 20 |
| `1` … `100` | el pedido |
| `101` o más | **100** |

---

### D-C6 · `register` deja de anunciar `Location`

**Decisión.** `POST /api/auth/register` responde **`201 Created` con `{"id": "<uuid>"}` y sin
cabecera `Location`**.

**Por qué.** La alternativa era exponer `GET /api/users/{id}`, y no se va a exponer: el enunciado no
pide consultar usuarios y **DP-02** ya evita construir lecturas que crucen datos personales del
operador. Anunciar una ubicación que devuelve 404 —verificado, sonda 17— es peor que no anunciar
ninguna: un cliente que siga la cabecera falla, y falla lejos de la causa.

**Estado: cumplido.** Medido el 2026-09-20 — el 201 emite cinco cabeceras y **ninguna es
`Location`**. Lo fija el test cuyo nombre lo dice entero:
`Register_with_an_administrator_token_creates_the_user_without_announcing_a_location`. El defecto
**A-4** queda cerrado.

---

### D-C7 · Un `{id}` mal formado es 404, y se documenta como tal

**Decisión.** **No se cambia.** Un identificador que no es un `uuid` devuelve **404 con el cuerpo
vacío**, igual que uno que no existe.

**Por qué.** La restricción de ruta `{id:guid}` hace que la ruta simplemente **no case**, así que
*«no es un identificador»* y *«no existe»* son indistinguibles desde fuera. Es lo habitual, no
filtra información, y distinguirlas obligaría a aceptar `{id}` libre y validar a mano en cada
acción — más código para menos seguridad.

**Verificado** en las tres formas: `GET /api/products/no-es-guid` → 404, `DELETE
/api/products/no-es-guid` → 404, `GET /api/sales/no-es-guid` → 404 (sondas 6, 36, 39).

**Consecuencia.** El portal muestra *«El recurso no existe.»* también ante un identificador con una
letra de más. Es aceptable y queda escrito para que nadie lo persiga como un fallo.

---

### D-C8 · El 403 se mantiene, y se exige la prueba que lo demuestre

**Decisión.** Un token de **`seller`** en una operación de **`admin`** responde **403 con el cuerpo
vacío**, distinguible del 401 (CA-07.4). La forma no cambia.

**Y está observado.** Medido el 2026-09-20 con un token de `seller` real: **403 con
`Content-Length: 0`** en las cinco operaciones de administrador —crear, reemplazar y dar de baja un
producto, subir su imagen, y dar de alta un usuario—, mientras el **mismo token** obtiene **200** en
las cuatro lecturas y llega hasta la regla de negocio en `POST /api/sales`. Lo fija el test
`A_seller_may_read_the_catalog_and_may_not_write_it`. El defecto **A-5** y el hueco **H-1** de este
contrato quedan **cerrados**.

---

### D-C9 · El 400 debe llevar `detail`

**Decisión.** El cuerpo canónico del error es `application/problem+json` **con `detail` siempre
presente**. El 400 pasa a llevar **`detail` además de `errors`**: `errors` se conserva tal cual para
quien lo lea, y `detail` resume en una frase **en español** qué campo falló. El 401, el 403 y el 404
**siguen sin cuerpo**.

**Por qué así y no al revés.** El interceptor del portal ya lee `detail` para todo lo no enumerado:
añadirlo en el backend arregla el mensaje **sin tocar el front y sin romper a nadie**. La
alternativa —enseñar al interceptor a leer `errors`— deja el problema para cualquier otro
consumidor futuro y obliga a traducir textos en inglés dentro de la capa de presentación.

**Por qué el 401, el 403 y el 404 se quedan vacíos.** Los emite el middleware de autenticación y el
enrutador, antes de llegar a ningún filtro; darles cuerpo obligaría a envolverlos sin ninguna
ganancia, porque el interceptor ya los enumera y les pone su propio texto.

**Estado.** Requiere personalizar la respuesta de validación de modelo. **No hay tarea que lo
lleve**: [hueco H-2](#5-huecos-declarados-con-su-dueño).

---

### D-C10 · `currency` nunca viaja nulo

**Decisión.** `currency` vale **siempre `"COP"`**, en `ProductView`, en `SaleView` y en
`SalesReport` — **incluido el reporte de un rango sin ventas**. Nunca `null`, nunca `""`.

**Por qué.** El front hace `Money.of(dto.grandTotal, dto.currency)` y `Money.of` ejecuta
`currency.toUpperCase()` **sin guarda**: un `null` lanza `TypeError` y **revienta la pantalla del
reporte**. **CA-06.2** exige que un rango sin ventas devuelva un reporte vacío *y no un error*, así
que un `currency` nulo incumpliría el criterio por la puerta de atrás, en el cliente.

**De dónde sale el valor.** De `Money.DefaultCurrency`, la constante del dominio. **No es una
columna**: el sistema es monomoneda por construcción (D-05) y la moneda no se persiste ni se acepta
en ninguna petición.

**Forma obligatoria del reporte vacío:**

```json
{"from":"2026-01-01T00:00:00+00:00","to":"2026-02-01T00:00:00+00:00",
 "salesCount":0,"grandTotal":0,"currency":"COP","rows":[]}
```

`rows` es `[]`, **nunca `null`**. `grandTotal` es `0`, no `null`. Lo mismo vale para `items` en
`PagedResult` y en `SaleView`.

---

### D-C11 · `totalPages` es la parte más frágil del contrato

**Qué es.** Una **propiedad calculada** de un `record`:

```csharp
public int TotalPages => Size == 0 ? 0 : (int)Math.Ceiling(Total / (double)Size);
```

**Decisión.** `totalPages` es **obligatorio** en toda respuesta paginada. El front lo exige
(`PagedResultDto.totalPages`, sin `?`) y `http-product.repository.ts` lo copia al modelo de dominio.

**Por qué hay que decirlo en voz alta.** Nadie lo declara en ningún sitio: se emite porque
`System.Text.Json` serializa las propiedades públicas de solo lectura **y porque no hay ninguna
opción de serialización configurada en el proyecto** — verificado: no existe una sola referencia a
`JsonSerializerOptions` en `src/`. Cualquiera de estas tres cosas lo borraría del JSON:

1. un `[JsonIgnore]` sobre la propiedad,
2. `IgnoreReadOnlyProperties = true` en las opciones del host,
3. proyectar la respuesta a otro tipo que no la tenga.

**Ninguna de las tres rompe la compilación ni un solo test.** El portal recibiría `undefined`, el
paginador dejaría de pintar páginas y nadie sabría por qué.

**Lo que el contrato exige.** Un **test de contrato** que afirme la presencia del campo en el JSON
serializado, no en el objeto. No existe: [hueco H-4](#5-huecos-declarados-con-su-dueño).


---

## 4. Fichas de endpoint

Quince endpoints. **Ninguna pregunta sin respuesta.**

Regla transversal que ahorra repetirla en cinco fichas: **las operaciones por identificador —`GET`,
`PUT`, `DELETE` y la subida de imagen— no distinguen un producto retirado de uno activo**. Solo el
listado del catálogo y la carga previa a vender filtran las bajas. Es el contrato de repositorios de
[ADR-003](adr/adr-003-baja-logica.md), y es lo que permite que una línea de venta histórica resuelva
su producto (CA-02.5).

---

### E-01 · `POST /api/auth/login`

| | |
|---|---|
| **Autorización** | **Anónimo** |
| **Cuerpo** | `application/json` |

**Petición**

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `username` | `string` | **sí** | Se **normaliza**: recorte de espacios y minúsculas (`User.NormalizeUsername`). `"  ADMIN  "` inicia sesión igual que `"admin"` — verificado, sonda 53 |
| `password` | `string` | **sí** | Nunca se registra ni se almacena en claro (CA-07.5) |

*Obligatorio* significa **no nulo**. Una cadena **vacía** sí pasa la validación de modelo y muere en
la regla de negocio: `""` → 422, `null` → 400 (sondas 54 y 55).

**200 OK — `AuthResult`**

| Campo | Tipo | Nulo | Qué es |
|---|---|---|---|
| `accessToken` | `string` | no | JWT firmado con HS256. Claims: `sub` (id), `unique_name` (usuario), `role`, `jti` |
| `expiresAt` | `string` | no | Instante de vencimiento, ISO 8601 con desplazamiento. `ahora + 60 min` |
| `username` | `string` | no | El nombre **normalizado**, no el enviado |
| `role` | `string` | no | `"admin"` o `"seller"` |

**Nunca incluye el hash de la clave** (CA-07.1).

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | Falta `username` o `password`, o el JSON no casa | §2.2, `errors.Username` / `errors.Password` |
| **422** | Credenciales inválidas | §2.1, `detail` = `"Usuario o contraseña incorrectos."` — **el mismo mensaje** tanto si el usuario no existe como si la clave falla (CA-07.2) |
| **405** | `GET` sobre esta ruta | vacío (sonda 41) |

---

### E-02 · `POST /api/auth/register`

| | |
|---|---|
| **Autorización** | **`admin`** |

**Solo se dan de alta vendedores** — decisión **DP-04**, tomada el 2026-09-20. El rol `admin` no se
crea desde aquí: lo provisiona el despliegue al arrancar, desde el entorno. La restricción no es una
comprobación del controlador: el puerto de registro **no sabe** crear administradores, y el que sí
sabe no está atado al adaptador HTTP.

**Petición**

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `username` | `string` | **sí** | Se normaliza igual que en login |
| `password` | `string` | **sí** | El hash lo calcula el adaptador; el dominio nunca ve la clave |
| `role` | `string` | **sí** | Solo `"seller"`. `"admin"` se rechaza con 422 (DP-04); cualquier otro valor, también |

**201 Created**

```json
{"id":"…"}
```

**Sin cabecera `Location`** ([D-C6](#d-c6--register-deja-de-anunciar-location)), verificado el
2026-09-20.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | Falta un campo o el JSON no casa | §2.2 |
| **401** | Sin token | vacío |
| **403** | Token de `seller` | vacío |
| **422** | `role` es `"admin"` (**DP-04**) | §2.1, `detail` = `"Solo se pueden dar de alta vendedores. El administrador lo crea el despliegue."` |
| **422** | El usuario ya existe | §2.1, `detail` = `"El usuario '<nombre>' ya existe."` |
| **422** | `role` fuera del conjunto | §2.1, `detail` = `"Rol no válido: '<rol>'."` |

> **El orden de comprobación importa y está medido, y DP-04 lo cambió.** `"admin"` se rechaza
> **antes** de mirar el nombre: sobre un usuario ya ocupado, el mensaje que llega es el de DP-04, no
> el del duplicado. Para cualquier **otro** rol inválido el orden sigue siendo el de siempre —gana el
> duplicado—, porque esa validación vive en el constructor de `User`, después de la búsqueda.
> Las dos ramas, medidas el 2026-09-20.

Dos altas concurrentes del mismo nombre dejan **una sola fila**: lo garantiza el índice único
`IX_user_username`, no la comprobación previa (CA-07.6).

---

### E-03 · `GET /api/products`

| | |
|---|---|
| **Autorización** | **Autenticado** (cualquier rol) |

**Parámetros de consulta**

| Parámetro | Tipo | Obligatorio | Por omisión | Notas |
|---|---|---|---|---|
| `search` | `string` | no | — sin filtro | Coincidencia **parcial** sobre el nombre, **sin distinguir mayúsculas** (CA-01.2) |
| `categoryId` | `uuid` | no | — sin filtro | Un valor que no es `uuid` → **400** |
| `page` | entero | no | `1` | [§1.2](#12-paginación) |
| `size` | entero | no | `20` | [§1.2](#12-paginación) y [D-C5](#d-c5--un-size-por-encima-del-máximo-se-recorta-al-máximo) |

**Orden de las filas, como contrato: por `name` ascendente.** Es el patrón de acceso Q1 de
[`plan.md`](plan.md) §3.1 y el índice parcial de T-13 está dimensionado para servirlo.

**Los productos dados de baja no aparecen nunca** (CA-01.4), ni siquiera filtrando por su categoría.

**200 OK — `PagedResult<ProductView>`**, con `items` de esta forma:

| Campo | Tipo | Nulo | Qué es |
|---|---|---|---|
| `id` | `string` (uuid) | no | |
| `name` | `string` | no | Recortado de espacios por el dominio |
| `price` | `number` | no | Dos decimales. **Siempre mayor que cero** |
| `currency` | `string` | no | `"COP"` ([D-C10](#d-c10--currency-nunca-viaja-nulo)) |
| `stock` | `number` | no | Entero, **nunca negativo** |
| `categoryId` | `string` (uuid) | no | |
| `categoryName` | `string` | no | El nombre **vivo** de la categoría. En el catálogo sí se lee vivo; en el reporte **no** (ADR-004) |
| `imageUrl` | `string` \| **`null`** | **sí** | Ruta **relativa** `"/media/<clave>"`. **`null` si el producto no tiene imagen** — nunca una cadena vacía ni una dirección rota (CA-03.2) |

**Atributos que el producto NO tiene, y no es un olvido:** no hay `description`, ni `sku`, ni código
de referencia. **DP-03**: solo los atributos que enumera el enunciado.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | `categoryId`, `page` o `size` no enlazan | §2.2 |
| **401** | Sin token o token inválido | vacío (sondas 2 y 20) |

`NotImplementedException` (sonda 4).

---

### E-04 · `GET /api/products/{id}`

| | |
|---|---|
| **Autorización** | **Autenticado** |
| **Ruta** | `{id}` con restricción `:guid` |

**200 OK — `ProductView`**, misma forma que los `items` de [E-03](#e-03--get-apiproducts).

**Devuelve también los productos dados de baja** (ADR-003: la carga por identificador no filtra).
Es lo que permite que el detalle de una venta antigua resuelva su producto.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **401** | Sin token | vacío |
| **404** | El producto no existe | vacío |
| **404** | **`{id}` no es un `uuid`** — la ruta no casa ([D-C7](#d-c7--un-id-mal-formado-es-404-y-se-documenta-como-tal)) | vacío (sonda 6) |


---

### E-05 · `POST /api/products`

| | |
|---|---|
| **Autorización** | **`admin`** |

**Petición**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `name` | `string` | **sí** | No vacío ni solo espacios. Se recorta |
| `price` | `number` | **sí** | **Mayor que cero**, estrictamente |
| `stock` | `number` | **sí** | Entero **no negativo**. Un decimal → 400 |
| `categoryId` | `uuid` | **sí** | Debe existir (CA-02.4) |

**No acepta `currency`** — el sistema es monomoneda (D-05). **No acepta `imageUrl` ni bytes de
imagen**: la imagen va por [E-08](#e-08--post-apiproductsidimage), en una petición aparte.

**201 Created**

```json
{"id":"…"}
```

con `Location: /api/products/{id}` — esta sí existe, la genera `CreatedAtAction`.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | Falta un campo, o un tipo no casa (`"price":"abc"`) | §2.2, con `errors["$.price"]` (sonda 23) |
| **401** | Sin token | vacío |
| **403** | Token de `seller` (CA-02.7) | vacío · [D-C8](#d-c8--el-403-se-mantiene-y-se-exige-la-prueba-que-lo-demuestre) |
| **422** | `name` vacío | §2.1, `"El nombre del producto es obligatorio."` |
| **422** | `price` menor o igual a cero (CA-02.2) | §2.1, `"El precio debe ser mayor a cero."` |
| **422** | `stock` negativo (CA-02.3) | §2.1, `"El stock inicial no puede ser negativo."` |
| **422** | `categoryId` es `00000000-…` | §2.1, `"La categoría es obligatoria."` |
| **422** | `categoryId` no existe (CA-02.4) | §2.1, `"La categoría <id> no existe."` — **mensaje prescrito por este contrato** y verificado el 2026-09-20: la comprobación existe todavía y T-04 debe escribirla con ese texto |

**Orden de comprobación**, para que el mensaje sea determinista: primero el enlace del JSON (400),
después nombre, precio y categoría en el orden del constructor de `Product`, y el stock **al final**
—`Product` valida `stock` después de `Rename`, `ChangePrice` y `SetCategory`—. La existencia de la
categoría se comprueba **antes** de construir el producto.


---

### E-06 · `PUT /api/products/{id}`

| | |
|---|---|
| **Autorización** | **`admin`** |

**Petición.** Idéntica a [E-05](#e-05--post-apiproducts) —`name`, `price`, `stock`, `categoryId`—,
**todos obligatorios**: es un reemplazo completo, no un parche. El `{id}` de la ruta manda; el
cuerpo **no lleva `id`**.

**204 No Content**, sin cuerpo.

**Errores.** Los mismos 422 de E-05, más:

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | El cuerpo no enlaza | §2.2 |
| **401** / **403** | Sin token / `seller` | vacío |
| **404** | No existe, o `{id}` no es `uuid` | vacío |
| **409** | Otra operación cambió la fila y se agotaron los **3 reintentos** de [ADR-002](adr/adr-002-concurrencia-optimista.md) | §2.1, `title` = `"Conflicto con otra operación simultánea"` |


---

### E-07 · `DELETE /api/products/{id}`

| | |
|---|---|
| **Autorización** | **`admin`** |

**204 No Content**, sin cuerpo.

**Es una baja lógica** ([ADR-003](adr/adr-003-baja-logica.md)): la fila no se borra. El producto
desaparece del catálogo y **no se puede vender** (CA-02.6), pero **las ventas que lo contienen
siguen intactas y consultables** (CA-02.5) y **sigue apareciendo en el reporte** del período en que
se vendió (CA-06.3).

**Orden obligatorio cuando el producto tiene imagen** (D-08, CA-03.3): anular la clave de imagen y
**confirmar** antes de borrar el binario. Al revés queda una referencia apuntando a un binario que
ya no existe.

**Repetir la baja.** Volver a dar de baja un producto ya retirado responde **204** otra vez: el
agregado no conoce la marca de baja —es una propiedad sombra— y no puede distinguir los dos casos.
Efecto colateral declarado: **se reescribe la fecha de baja**. Esa fecha no forma parte de ningún
contrato, así que no rompe nada; si alguna vez importa preservar la primera, es un ajuste de T-09.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **401** / **403** | Sin token / `seller` | vacío |
| **404** | Ningún producto con ese identificador, o `{id}` no es `uuid` | vacío (sonda 39) |
| **409** | Conflicto de concurrencia | §2.1 |


---

### E-08 · `POST /api/products/{id}/image`

| | |
|---|---|
| **Autorización** | **`admin`** |
| **Cuerpo** | `multipart/form-data` |

**Petición**

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `file` | fichero | **sí** | **El nombre del campo es exactamente `file`.** Coincide con el `form.append('file', …)` del portal |

**Tipos aceptados**, de `Storage:AllowedContentTypes`: **`image/jpeg`, `image/png`, `image/webp`**.
Cualquier otro se rechaza.

**El nombre del fichero que envía el cliente no se reutiliza nunca**: la clave es un `Guid` de 32
caracteres sin guiones más la extensión original. Evita el recorrido de rutas y las colisiones.

**200 OK**

```json
{"url":"/media/9f2c…a1.jpg"}
```

Ruta **relativa**, la misma que aparecerá después en `imageUrl` del producto (CA-03.1).

**Tamaño máximo.** **5 MB, y se aplica** — medido el 2026-09-20: un PNG de 8,28 MB responde 422 con
el texto de abajo, y el defecto **A-2** queda cerrado. Este contrato decide: **el máximo es 5 MB**, con **422** y
`detail = "La imagen supera el máximo de 5 MB."`. Con la salvedad física de que **por encima de
30 000 000 bytes (≈ 28,6 MB) la petición nunca llega al código**: la corta Kestrel y responde
**400 sin decir que es por tamaño**. Ese techo se hereda del framework y no está configurado en el
repositorio.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | Falta el campo `file` | §2.2, `errors.file` = `"The file field is required."` (sonda 25) |
| **400** | La petición supera el techo de Kestrel | §2.2, **sin mencionar el tamaño** |
| **401** / **403** | Sin token / `seller` | vacío |
| **404** | El producto no existe, o `{id}` no es `uuid` | vacío |
| **422** | Tipo de archivo no permitido | §2.1, `"Tipo de archivo no permitido: <contentType>."` |
| **422** | Supera los 5 MB | §2.1, `"La imagen supera el máximo de 5 MB."` |

> **Aviso para T-04.** Hoy `LocalFileStorage.SaveAsync` lanza `InvalidOperationException` para un
> tipo no permitido, y el filtro **no la traduce**: saldría un **500**. Para cumplir este contrato el
> rechazo tiene que llegar como regla de negocio. El mecanismo —validar en el servicio o traducir en
> el filtro— lo elige T-04; **el código de respuesta no**.


---

### E-09 · `GET /api/categories`

> **Existe y responde 200** desde T-04. La brecha que esta ficha vino a cerrar —el portal lo
> invocaba y no había endpoint— está cerrada: el selector de categoría de los formularios de
> producto se pinta con esta respuesta.

| | |
|---|---|
| **Autorización** | **Autenticado** (cualquier rol) |
| **Petición** | Sin cuerpo y **sin parámetros**. No se filtra, no se busca, no se pagina |

**200 OK — array plano**

```json
[{"id":"33333333-3333-4333-8333-333333333333","name":"Electricidad"},
 {"id":"44444444-4444-4444-8444-444444444444","name":"Fontanería"},
 {"id":"11111111-1111-4111-8111-111111111111","name":"General"},
 {"id":"22222222-2222-4222-8222-222222222222","name":"Herramientas"},
 {"id":"55555555-5555-4555-8555-555555555555","name":"Pinturas"}]
```

| Campo | Tipo | Nulo |
|---|---|---|
| `id` | `string` (uuid) | no |
| `name` | `string` | no |

- **Sin envoltorio de paginación**: son datos semilla de la migración inicial, no un catálogo que
  crezca (D-10).
- **Ordenado por `name` ascendente**, con la intercalación de la base
  ([D-C1](#d-c1--get-apicategories-existe-devuelve-un-array-plano-ordenado-por-nombre)).
- **Vacío es `200 []`**, nunca 204 ni 404.
- **Solo lectura.** No hay `POST`, `PUT` ni `DELETE` de categorías, y no es un olvido:
  [`architecture.md`](architecture.md) §3.1 lo razona.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **401** | Sin token o token inválido | vacío |

---

### E-10 · `POST /api/sales`

| | |
|---|---|
| **Autorización** | **Autenticado** (cualquier rol: vender es de `seller`) |

**Petición**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `lines` | array | **sí** | **Al menos un elemento** |
| `lines[].productId` | `uuid` | **sí** | Debe existir y **estar activo** |
| `lines[].quantity` | entero | **sí** | **Mayor que cero**. Un decimal → 400 (sonda 51) |

**El autor de la venta NO va en el cuerpo.** Sale del token: el controlador lee `ClaimTypes.Name` y
el generador emite `unique_name`. No hay campo `soldBy` en la petición, y enviarlo no hace nada.

> **Acoplamiento silencioso, heredado de [`../traspaso/HANDOFF.md`](../traspaso/HANDOFF.md) §7.** Funciona **solo
> porque el mapeo de claims está activo por omisión**. Con `MapInboundClaims = false`, cada venta
> quedaría registrada como `"desconocido"` y **nada fallaría**.

**201 Created**

```json
{"id":"…"}
```

con `Location: /api/sales/{id}`.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | Falta `lines`, o el JSON no enlaza | §2.2, `errors.Lines` = `"The Lines field is required."` (sonda 8) |
| **401** | Sin token | vacío |
| **422** | `lines` vacío (CA-04.3) | §2.1, `"La venta debe tener al menos un ítem."` (sonda 7) |
| **422** | Producto repetido en dos líneas (CA-04.4) | §2.1, `"La venta tiene productos repetidos."` (sonda 48) |
| **422** | El producto no existe o está dado de baja (CA-02.6) | §2.1, `"El producto <id> no existe."` (sonda 49) |
| **422** | `quantity` menor o igual a cero (CA-04.5) | §2.1, `"La cantidad debe ser mayor a cero."` |
| **422** | Stock insuficiente (CA-04.2) | §2.1, `"Stock insuficiente para '<nombre>': disponible <n>, solicitado <m>."` |
| **409** | Dos ventas simultáneas del último ejemplar, agotados los 3 reintentos (CA-04.6) | §2.1 |

**El orden de comprobación es contrato, y está medido.** Con varias cosas mal a la vez, el mensaje
que llega es el primero de esta lista:

1. `lines` vacío
2. productos repetidos
3. el producto no existe
4. `quantity` menor o igual a cero
5. stock insuficiente

> **Verificado, y contraintuitivo:** una línea con `quantity: 0` sobre un producto inexistente
> devuelve *«El producto … no existe.»*, **no** *«La cantidad debe ser mayor a cero.»* (sonda 50).
> Quien escriba los tests tiene que saberlo o los escribirá al revés.

**Todo o nada.** Si una sola línea falla, **ningún** producto queda descontado (CA-04.2): la venta y
los descuentos de stock se confirman en la misma unidad de trabajo.

---

### E-11 · `GET /api/sales`

| | |
|---|---|
| **Autorización** | **Autenticado** |

**Parámetros de consulta:** `from` y `to` **obligatorios**
([§1.3](#13-rangos-de-fecha)), más `page` y `size` ([§1.2](#12-paginación)).

**Orden de las filas, como contrato: por `soldAt` descendente** — la más reciente primero. Es el
patrón Q7 de [`plan.md`](plan.md) §3.1.

**200 OK — `PagedResult<SaleView>`**, con `items` de esta forma:

| Campo | Tipo | Nulo | Qué es |
|---|---|---|---|
| `id` | `string` (uuid) | no | |
| `soldAt` | `string` | no | ISO 8601 con desplazamiento |
| `soldBy` | `string` | no | El **nombre de usuario** normalizado, no un identificador |
| `total` | `number` | no | **Calculado desde las líneas**, nunca almacenado (CA-05.4) |
| `currency` | `string` | no | `"COP"` |
| `items` | `SaleItemView[]` | no | **`[]` si no hay, nunca `null`.** En la práctica nunca está vacío: una venta sin líneas no se persiste |

**`SaleItemView`**

| Campo | Tipo | Nulo | Qué es |
|---|---|---|---|
| `productId` | `string` (uuid) | no | |
| `productName` | `string` | no | **Congelado en el momento de la venta** (CA-04.7). Renombrar el producto después **no** lo cambia |
| `quantity` | `number` | no | Entero mayor que cero |
| `unitPrice` | `number` | no | **Congelado**: no sigue al catálogo |
| `subtotal` | `number` | no | `unitPrice × quantity`, calculado |

**El ítem no lleva `currency` propia**: el front usa la de la venta (`Money.of(item.unitPrice,
dto.currency)`). No la añadas: sería una segunda fuente para el mismo dato.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | Falta `from` o `to`, formato no aceptado, o `page`/`size` no enlazan | §2.2 |
| **401** | Sin token | vacío |
| **422** | `to` anterior a `from` (CA-05.3) | §2.1, `"La fecha final no puede ser anterior a la inicial."` |


---

### E-12 · `GET /api/sales/{id}`

| | |
|---|---|
| **Autorización** | **Autenticado** |

**200 OK — `SaleView`**, misma forma que los `items` de [E-11](#e-11--get-apisales) (CA-05.1).

**Una venta registrada no se modifica nunca**: no hay `PUT` ni `DELETE` de ventas, y eso es
deliberado.

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **401** | Sin token | vacío |
| **404** | La venta no existe, o `{id}` no es `uuid` | vacío (sonda 36) |


---

### E-13 · `GET /api/reports/sales`

| | |
|---|---|
| **Autorización** | **Autenticado** |

**Parámetros de consulta:** `from` y `to`, **obligatorios**, con las reglas de
[§1.3](#13-rangos-de-fecha). **No se pagina** y **no hay más parámetros**: en particular **no hay
filtro ni agrupación por vendedor** — **DP-02**, para no cruzar datos personales del operador.

**200 OK — `SalesReport`**

| Campo | Tipo | Nulo | Qué es |
|---|---|---|---|
| `from` | `string` | no | El extremo **recibido**, devuelto tal cual |
| `to` | `string` | no | Ídem |
| `salesCount` | `number` | no | Número de **ventas** del rango, no de líneas ni de unidades |
| `grandTotal` | `number` | no | Total del período. **`0` si no hay ventas**, nunca `null` |
| `currency` | `string` | no | `"COP"` **siempre**, también con el reporte vacío ([D-C10](#d-c10--currency-nunca-viaja-nulo)) |
| `rows` | `SalesReportRow[]` | no | **`[]` si no hay ventas**, nunca `null` (CA-06.2) |

**`SalesReportRow` — una fila por producto** (CA-06.1)

| Campo | Tipo | Nulo | Qué es |
|---|---|---|---|
| `productId` | `string` (uuid) | no | La clave de agrupación |
| `productName` | `string` | no | **El nombre congelado de la venta más reciente del rango** — **DP-01** |
| `categoryName` | `string` | no | El nombre **congelado** de la categoría en la línea (ADR-004, T-11). **No se consulta el catálogo vivo**, y desde el 2026-09-20 **la agregación agrupa POR este valor**: un producto recategorizado dentro del rango llega como **dos filas**, una por etiqueta. Nadie elige ganador, porque elegirlo dejaría que una venta nueva cambiara lo que un período cerrado ya dijo |
| `unitsSold` | `number` | no | Suma de cantidades |
| `revenue` | `number` | no | Suma de subtotales congelados |

**Orden de las filas, como contrato: por `revenue` descendente.** Es el patrón Q9 de
[`plan.md`](plan.md) §3.1, y es lo que hace útil el reporte: lo que más facturó, arriba.

**Invariantes del reporte que el contrato promete**

- **Estable.** Repetir la consulta de un período cerrado devuelve **exactamente lo mismo**, aunque
  el catálogo haya cambiado (CA-06.4, RNF-02). Se cumple porque nombre, precio y categoría están
  congelados en la línea, no leídos en vivo.
- **Incluye los productos retirados** que se vendieron dentro del rango (CA-06.3).
- **Agrega en el motor.** Traer las ventas a memoria para sumarlas es un defecto, no una
  alternativa (CA-06.5).
- **`grandTotal` cuadra con la suma de `revenue` de las filas.**
- **Dos rangos contiguos suman el total del período**, por [D-C2](#d-c2--from-inclusivo-to-exclusivo).

**Errores**

| Código | Cuándo | Cuerpo |
|---|---|---|
| **400** | Falta `from` o `to`, o el formato no se acepta | §2.2 (sondas 12, 32, 35) |
| **401** | Sin token | vacío |
| **422** | `to` anterior a `from` | §2.1, `"La fecha final no puede ser anterior a la inicial."` (sonda 11) |


---

### E-14 · `GET /health`

| | |
|---|---|
| **Autorización** | **Anónimo**, explícitamente (`.AllowAnonymous()`) |

**200 OK**

```json
{"status":"ok"}
```

`status` es `string` y su único valor es `"ok"`. **No hay ningún otro código**: si el proceso no
responde, no hay respuesta. No comprueba la base de datos — es prueba de vida del proceso, no de
sus dependencias. Verificado (sonda 1).

---

### E-15 · `GET /media/{key}`

| | |
|---|---|
| **Autorización** | **Anónimo, y no se puede proteger** |

**No lo sirve ningún controlador**: lo sirve `UseStaticFiles` sobre el volumen de binarios, bajo la
misma URL que `LocalFileStorage.ResolveUrl` publica dentro de `imageUrl`. Al no ser un endpoint
enrutado **no pasa por la autorización**.

- `{key}` es un `Guid` de 32 caracteres sin guiones más la extensión original. **No se puede
  adivinar ni enumerar**, pero **una URL filtrada sirve la imagen a cualquiera, para siempre**.
- **200** con los bytes y su `Content-Type`; **404 vacío** si la clave no existe (sonda 21).

> **Los cuatro tipos llegan por el portal, verificado el 2026-09-20.** Hubo un defecto —**A-6**— por
> el que la `location` de expresión regular de los estáticos ganaba al prefijo `/media/` y solo
> `.webp` alcanzaba el servicio. Se cerró con `location ^~ /media/`, y la prueba sigue siendo **la
> forma del 404**: una clave inexistente responde **404 con 0 bytes** —el de la API— y no la página
> HTML de 153 bytes de nginx. El hueco **H-5** queda cerrado.

---

## 5. Huecos declarados, con su dueño

Lo que este contrato **decide** está cerrado. Lo que sigue es lo que **falta hacer** para que el
código lo cumpla. Ninguno es una decisión pendiente: son tareas sin dueño asignado.

| # | Hueco | De dónde sale | Dueño |
|---|---|---|---|
| ~~**H-1**~~ | **CERRADO el 2026-09-20.** Hay vendedor en ejecución y el 403 está medido en las cinco operaciones de administrador. ~~No existía un usuario `seller` en ejecución ni un test de CA-07.4.~~ Mientras tanto el **403 es intención, no comportamiento observado** | [D-C8](#d-c8--el-403-se-mantiene-y-se-exige-la-prueba-que-lo-demuestre), defecto A-5 | **Propietario.** Hoy **ninguna tarea lo cubre**: T-06 está cerrada y solo crea el administrador. Encaja en T-18, que tendría que ganar ese paso |
| **H-2** | **El 400 no lleva `detail`**, y su `title` sale en inglés del marco. **El portal ya no dice «Ocurrió un error inesperado»**: su interceptor nombra el campo —*«Revisa estos datos: precio.»*—, así que el argumento con que se justificó D-C9 ya no aplica, pero **la decisión sí sigue sin cumplirse**. Además el cuerpo filtra el nombre del tipo interno de .NET | [D-C9](#d-c9--el-400-debe-llevar-detail) | **Propietario.** **Ninguna tarea lo lleva.** Es personalizar la respuesta de validación de modelo en el adaptador REST |
| ~~**H-3**~~ | **CERRADO.** El portal convierte el día elegido en el primer instante del siguiente y solo devuelve el inclusivo para mostrarlo. ~~Mandaba el extremo final sin ajustar a `to` exclusivo~~ y pierde el último día del rango que la persona elige | [D-C2](#d-c2--from-inclusivo-to-exclusivo) | **T-15** (portal contra la API real) |
| ~~**H-4**~~ | **CERRADO.** Dos tests de contrato sobre el JSON serializado lo fijan, uno por colección. ~~No había test que afirmara que `totalPages` viaja en el JSON.~~ Un `[JsonIgnore]` lo borraría sin romper compilación ni test | [D-C11](#d-c11--totalpages-es-la-parte-más-frágil-del-contrato) | **T-04 y T-07**, las primeras que devuelven un `PagedResult` |
| ~~**H-5**~~ | **CERRADO.** `location ^~ /media/`: los cuatro binarios reales responden 200 por el portal. ~~nginx respondía `/media/*.jpg` y `*.png` con su propio 404~~: dos de los tres tipos aceptados no se ven nunca | [E-15](#e-15--get-mediakey) | **T-03** (repositorio de infraestructura) o **T-15**. Ya tiene ficha: es el defecto **A-6** de [`../traspaso/HANDOFF-TECNICO.md`](../traspaso/HANDOFF-TECNICO.md) §6.1 |
| **H-6** | **`GET /media/{key}` es público y no se puede proteger** sin cambiar el mecanismo de servicio | [E-15](#e-15--get-mediakey) | **Propietario.** Decisión de riesgo aceptado, no de implementación: hoy es lo que hace falta para que la etiqueta `<img>` funcione sin cabeceras |

---

## 6. Qué exige este contrato del código

Mapa de decisión a tarea, para que nadie tenga que deducirlo.

| Decisión | Qué hay que escribir | Tarea |
|---|---|---|
| [D-C1](#d-c1--get-apicategories-existe-devuelve-un-array-plano-ordenado-por-nombre) | Método en el puerto entrante, `CategoryView`, controlador, y el `OrderBy` declarado como contrato en un test | **T-04** |
| [D-C2](#d-c2--from-inclusivo-to-exclusivo) | El predicado `from <= sold_at < to` en las dos consultas, con un test de frontera que meta una venta exactamente en `to` y compruebe que **no** entra | **T-07, T-08** |
| [D-C3](#d-c3--solo-iso-8601-con-desplazamiento-explícito) | Rechazo del formato sin desplazamiento, con test de `2026-01-01` → 400 y de `01/06/2026` → 400 | **T-07, T-08** |
| [D-C4](#d-c4--from-y-to-son-obligatorios-de-verdad) | `DateTimeOffset?` en los dos controladores y rechazo explícito de la ausencia | **T-07, T-08** |
| [D-C5](#d-c5--un-size-por-encima-del-máximo-se-recorta-al-máximo) | `Size > MaxSize ? MaxSize : (Size < 1 ? 20 : Size)` en `PageRequest`, con test | **T-04** (cierra A-3) |
| [D-C6](#d-c6--register-deja-de-anunciar-location) | Quitar la URL literal del `Created` | **T-06** (cierra A-4) |
| [D-C7](#d-c7--un-id-mal-formado-es-404-y-se-documenta-como-tal) | **Nada.** Se documenta y ya está | — |
| [D-C8](#d-c8--el-403-se-mantiene-y-se-exige-la-prueba-que-lo-demuestre) | Usuario `seller` en ejecución y test de CA-07.4 | **H-1, sin dueño** |
| [D-C9](#d-c9--el-400-debe-llevar-detail) | `detail` en la respuesta de validación de modelo | **H-2, sin dueño** |
| [D-C10](#d-c10--currency-nunca-viaja-nulo) | `Money.DefaultCurrency` en el reporte vacío, con test de rango sin ventas | **T-08** |
| [D-C11](#d-c11--totalpages-es-la-parte-más-frágil-del-contrato) | Test de contrato sobre el JSON serializado | **H-4** |
| **A-1** | Bajar el `[AllowAnonymous]` de la clase a la acción `Login` | **T-06**, gravedad crítica |
| **A-2** | Aplicar `Storage:MaxBytes` en la subida, o borrar la clave | **T-04** |

---

## 7. Firma

**Qué se acepta.** Los **quince endpoints** de §4, con la forma exacta de cada petición y de cada
respuesta, la obligatoriedad y el valor por omisión de cada parámetro, el formato de fecha, la
inclusividad del rango, el orden de las filas de las cuatro colecciones, **todos** los códigos de
error con su cuerpo concreto, y **las once decisiones de §3**, que cierran los ocho huecos
`C-1 … C-8` de [`architecture.md`](architecture.md) §3.1.1 y tres más que nadie había escrito.

**Contra qué se verificó.**

- **El sistema levantado**, el 2026-09-19: `db`, `service` y `portal`, los tres *healthy*. **55
  sondas con `curl`**, con la salida literal en el [anexo A](#anexo-a--salida-literal-de-las-sondas).
- **La base de datos**, por `psql`: categorías sembradas y su orden, usuarios y roles, intercalación.
- **El código como única fuente ejecutable**: los `record` de `src/application/Ports/Inbound/`, los
  cuatro controladores, `ExceptionTranslationFilter`, `PageRequest`, `PagedResult`, `DateRange`,
  `LocalFileStorage`, `HostConfiguration`, y del lado del portal `api.dto.ts`,
  `error.interceptor.ts`, `money.ts`, los cuatro repositorios HTTP y `nginx.conf`.
- **Los criterios de aceptación** de [`spec.md`](spec.md) y las decisiones de negocio **DP-01**,
  **DP-02** y **DP-03**, que este documento respeta y no reabre.

**Qué queda explícitamente fuera.**

- **El modelo de datos** —tablas, columnas, restricciones, índices, migraciones—: vive en
  [`plan.md`](plan.md) y en el documento de modelo de datos. Aquí solo aparece cuando se ve desde
  fuera, como el orden de las filas o la intercalación.
- **La arquitectura hexagonal, los puertos y los flujos**: [`architecture.md`](architecture.md).
- **Las fichas de los defectos A-1 a A-6** y su remedio:
  [`../traspaso/HANDOFF-TECNICO.md`](../traspaso/HANDOFF-TECNICO.md) §6.1. Aquí se citan, no se copian.
- **Los seis huecos H-1 a H-6** de §5: están **nombrados con su dueño**, no cerrados.
- **Cualquier cambio de código.** Este documento **no toca una sola línea**: especifica.

**Fecha:** 2026-09-19.

---

## Anexo A — Salida literal de las sondas

**Remedido entero el 2026-09-20.** La numeración y la pregunta de cada sonda se conservan, porque el
cuerpo del documento las cita («sonda 34»); lo que se regeneró es **la respuesta**. Catorce sondas
devolvían ya otra cosa, y su versión anterior describía un sistema que no existe desde que se
cerraron A-1, A-2, A-3, A-4, A-6 y las tareas T-04, T-07 y T-08.

Token de administrador obtenido de `simple-stock-flow-infra/.env` en una variable de entorno. **La
contraseña no aparece en ninguna salida**, y lo que cambia en cada ejecución —`traceId`, el token,
las marcas de tiempo— se pliega a `...` para que el anexo no envejezca por ruido.

**Tres sondas nuevas**, porque hay comportamiento que antes no existía: **16b** (`role: "admin"` →
422, DP-04), **44** (un vendedor sobre una operación de administrador → 403) y **40b** (Swagger a
través del portal).

**Lo único que aún se escribe al medir** es la sonda 22, que crea un producto; se borra al terminar
y el guion lo dice. Todo lo demás lee o es rechazado.

```console

### 1 health
HTTP/1.1 200 OK
Content-Type: application/json

{"status":"ok"}

### 2 GET /api/products sin token
HTTP/1.1 401 Unauthorized
Content-Length: 0
WWW-Authenticate: Bearer

### 3 GET /api/categories con token
HTTP/1.1 200 OK
Content-Type: application/json

[{"id":"33333333-3333-4333-8333-333333333333","name":"Electricidad"},{"id":"44444444-4444-4444-8444-444444444444","name":"Fontanería"},{"id":"11111111-1111-4111-8111-111111111111","name":"General"},{"id":"22222222-2222-4222-8222-222222222222","name":"Herramientas"},{"id":"55555555-5555-4555-8555-555...

### 4 GET /api/products con token
HTTP/1.1 200 OK
Content-Type: application/json

{"items":[{"id":"eace1e52-5417-4688-b855-05ece2b38064","name":"Alicate universal 8\"","price":32400.0,"currency":"COP","stock":21,"categoryId":"22222222-2222-4222-8222-222222222222","categoryName":"Herramientas","imageUrl":"/media/cb4cb61f1d6244d89b2cb89a2f09bec2.png"},{"id":"6cf87c59-ee63-4634-9717...

### 5 GET /api/products/{guid valido}
HTTP/1.1 200 OK
Content-Type: application/json

{"id":"eace1e52-5417-4688-b855-05ece2b38064","name":"Alicate universal 8\"","price":32400.0,"currency":"COP","stock":21,"categoryId":"22222222-2222-4222-8222-222222222222","categoryName":"Herramientas","imageUrl":"/media/cb4cb61f1d6244d89b2cb89a2f09bec2.png"}

### 6 GET /api/products/no-es-guid
HTTP/1.1 404 Not Found
Content-Length: 0

### 7 POST /api/sales con lines vacio
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"La venta debe tener al menos un ítem."}

### 8 POST /api/sales sin lines
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"Lines":["The Lines field is required."]},"traceId":"..."}

### 9 POST /api/sales con JSON roto
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"request":["The request field is required."],"$.lines":["Expected depth to be zero at the end of the JSON payload. There is an open JSON object or array that should b...

### 10 GET /api/reports/sales sin from ni to      -> 400, ya NO 500
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"to":["The to field is required."],"from":["The from field is required."]},"traceId":"..."}

### 11 GET /api/reports/sales?from=2026-12-01&to=2026-01-01
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"La fecha final no puede ser anterior a la inicial."}

### 12 GET /api/reports/sales?from=manzana&to=2026-01-01
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"from":["The value 'manzana' is not an ISO 8601 instant with an explicit offset."]},"traceId":"..."}

### 13 GET /api/reports/sales?from=2026-01-01&to=2026-12-31
HTTP/1.1 200 OK
Content-Type: application/json

{"from":"2026-01-01T00:00:00+00:00","to":"2026-12-31T00:00:00+00:00","salesCount":18,"grandTotal":2531700.0,"currency":"COP","rows":[{"productId":"c95743da-72c2-4039-8f67-c4bd63accf3e","productName":"Cable encauchetado 3x14, por metro","categoryName":"Electricidad","unitsSold":65,"revenue":416000.0}...

### 14 POST /api/auth/register SIN TOKEN, usuario ya existente   <-- A-1 CERRADO
HTTP/1.1 401 Unauthorized
Content-Length: 0
WWW-Authenticate: Bearer

### 15 POST /api/auth/register con token de admin, usuario existente
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"El usuario 'admin' ya existe."}

### 16 POST /api/auth/register con token de admin, role invalido "jefe"
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"El usuario 'admin' ya existe."}

### 16b POST /api/auth/register con role "admin"   <-- DP-04, nueva
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"Solo se pueden dar de alta vendedores. El administrador lo crea el despliegue."}

### 17 GET /api/users/{id}  (destino del Location que register ya NO envia)
HTTP/1.1 404 Not Found
Content-Length: 0

### 18 POST /api/auth/login con contrasena incorrecta
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"Usuario o contraseña incorrectos."}

### 19 POST /api/auth/login sin password
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"Password":["The Password field is required."]},"traceId":"..."}

### 20 GET /api/products con token invalido
HTTP/1.1 401 Unauthorized
Content-Length: 0
WWW-Authenticate: Bearer error="invalid_token"

### 21 GET /media/{clave inexistente} SIN token, contra el servicio
HTTP/1.1 404 Not Found
Content-Length: 0

### 22 POST /api/products como admin
HTTP/1.1 201 Created
Content-Type: application/json
Location: http://localhost:5000/api/products/b6b579a3-e85e-4031-944b-08968f47e29c

{"id":"b6b579a3-e85e-4031-944b-08968f47e29c"}

### 23 POST /api/products con {"price":"abc"}
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"request":["The request field is required."],"$.price":["The JSON value could not be converted to SimpleStockFlow.Adapters.Rest.Contracts.CreateProductRequest. Path: $.pr...

### 24 GET /api/sales?...&size=500   -> sirve el maximo, ya no 500
HTTP/1.1 200 OK
Content-Type: application/json

{"items":[{"id":"beaf2ef5-9093-4cef-8aba-10e3d41ebe8f","soldAt":"2026-09-20T14:00:00+00:00","soldBy":"admin","total":96300.0,"currency":"COP","items":[{"productId":"1d47eed9-a773-4017-9ec4-0b1f9bf205f0","productName":"Martillo de carpintero 16 oz","quantity":2,"unitPrice":38900.0,"subtotal":77800.0}...

### 25 POST /api/products/{id}/image sin el campo file
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"file":["The file field is required."]},"traceId":"..."}

### 34 page y size fuera de rango   -> el servicio ya responde
HTTP/1.1 200 OK
Content-Type: application/json

{"items":[{"id":"eace1e52-5417-4688-b855-05ece2b38064","name":"Alicate universal 8\"","price":32400.0,"currency":"COP","stock":21,"categoryId":"22222222-2222-4222-8222-222222222222","categoryName":"Herramientas","imageUrl":"/media/cb4cb61f1d6244d89b2cb89a2f09bec2.png"},{"id":"6cf87c59-ee63-4634-9717...

### 35 cuerpo completo de ?from=&to=
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"to":["The to field is required."],"from":["The from field is required."]},"traceId":"..."}

### 32 from y to con valor vacio (sales)
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"to":["The to field is required."],"from":["The from field is required."]},"traceId":"..."}

### 33 ausencia parcial
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"to":["The to field is required."]},"traceId":"..."}

### 30 fecha SIN desplazamiento   -> ya no cuela: 400
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"from":["The value '2026-06-01' is not an ISO 8601 instant with an explicit offset."]},"traceId":"..."}

### 31 "01/06/2026"   -> ya no se lee como 6 de enero: 400
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"from":["The value '01/06/2026' is not an ISO 8601 instant with an explicit offset."]},"traceId":"..."}

### 36 GET /api/sales/no-es-guid
HTTP/1.1 404 Not Found
Content-Length: 0

### 37 PUT /api/products/no-es-guid
HTTP/1.1 404 Not Found
Content-Length: 0

### 38 DELETE /api/products/no-es-guid
HTTP/1.1 404 Not Found
Content-Length: 0

### 39 POST /api/products/no-es-guid/image

### 41 metodo no permitido
HTTP/1.1 405 Method Not Allowed
Content-Length: 0
Allow: POST

### 44 seller sobre operacion de admin   -> 403, ya observado
HTTP/1.1 403 Forbidden
Content-Length: 0

### 48 POST /api/sales con el mismo producto dos veces
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"La venta tiene productos repetidos."}

### 49 POST /api/sales con un producto inexistente
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"El producto 00000000-0000-4000-8000-000000000000 no existe."}

### 50 POST /api/sales con quantity=0 sobre un producto inexistente
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"El producto 00000000-0000-4000-8000-000000000000 no existe."}

### 51 POST /api/sales con quantity=1.5
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"request":["The request field is required."],"$.lines[0].quantity":["The JSON value could not be converted to SimpleStockFlow.Adapters.Rest.Contracts.SaleLineRequest. Pat...

### 52 POST /api/sales con productId que no es uuid
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"request":["The request field is required."],"$.lines[0].productId":["The JSON value could not be converted to SimpleStockFlow.Adapters.Rest.Contracts.SaleLineRequest. Pa...

### 53 login con "  ADMIN  " y la contrasena correcta
HTTP/1.1 200 OK
Content-Type: application/json

{"accessToken":"...","expiresAt":"...","username":"admin","role":"admin"}

### 54 login con username vacio ""
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{"title":"Regla de negocio violada","status":422,"detail":"Usuario o contraseña incorrectos."}

### 55 login con username null
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"Username":["The Username field is required."]},"traceId":"..."}

--- por el portal, puerto 8080 ---

### 42 portal 8080: /media/{clave}.png real   -> llega a la API
HTTP/1.1 200 OK
Content-Type: image/png
Content-Length: 5319

�PNG


IHDRhh�����IDATx���yp��}��}WZ]H�sѱ��0� s��%D��g �c'�i��L�t�i;�����6�:��?�v:��ı�;��|`l��JB�	���X�oGc	���jW�����_�����w��y5���nѻ�p�[�1�@�p#�1�@�p#�1�@�p#�1�@�p#�1�@�p#�1�@�p#�1�@�p#�1�@�p#�1�...

### 43 portal 8080: /media/{clave inexistente}.jpg   -> 404 de la API, 0 bytes
HTTP/1.1 404 Not Found
Content-Length: 0

### 45 prueba de precedencia: /api/loquesea.jpg
HTTP/1.1 404 Not Found
Content-Type: text/html
Content-Length: 153

<html>
<head><title>404 Not Found</title></head>
<body>
<center><h1>404 Not Found</h1></center>
<hr><center>nginx/1.27.5</center>
</body>
</html>

### 46 control: /api/loquesea (sin extension)
HTTP/1.1 404 Not Found
Content-Length: 0

### 40 swagger en Production   -> 200, ya no 404
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8

{"openapi":"3.0.1","info":{"title":"Simple Stock Flow API","description":"Products and sales. Sign in through POST /api/auth/login, paste the accessToken into Authorize, and every other operation becomes executable from this page. Field names are camelCase, amounts carry two decimals, the currency is alw...

### 40b swagger por el portal
HTTP/1.1 200 OK
Content-Type: text/html;charset=utf-8

<!-- HTML for static distribution bundle build -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Simple Stock Flow API</title>
    <link rel="stylesheet" type="text/css" href="./swagger-ui.css">
    <link rel="stylesheet" type="text/css" href="./index.css">
    <link rel="...

### 26 psql: categorias, ordenadas por nombre
     name     
--------------
 Electricidad
 Fontanería
 General
 Herramientas
 Pinturas
(5 rows)


### 27 psql: usuarios y roles
   username    |  role  
---------------+--------
 admin         | admin
 vendedor.demo | seller
(2 rows)


### 28b psql: intercalacion de la base
 datcollate |  datctype  
------------+------------
 en_US.utf8 | en_US.utf8
(1 row)


### 29 zona horaria del contenedor del servicio
Mon Sep 21 HH:MM:SS UTC 2026

### 47 cuerpo del 404 de nginx, para comparar tamanos
/media/{inexistente}.jpg por el portal  -> %{size_download} bytes
/api/loquesea.jpg por el portal         -> %{size_download} bytes  (HTML propio de nginx)
```

## Anexo B — Registro de deuda declarada

**Este anexo es la fe de erratas del contrato.** No se vacía al pagar la deuda: una entrada saldada
sigue aquí, con la fecha y con qué se cambió, porque **un registro que se borra al cumplirse pierde
la memoria de que la mentira existió** — y era esa memoria la que impedía que volviera.

**Decisión del propietario, 2026-09-20: corregir las once y anexar la fe de erratas.** Las once
entradas están **saldadas**. El texto del contrato se corrigió en su sitio y las marcas de deuda que
avisaban al lector se retiraron, porque ya no hay de qué avisar.

> **Cómo se lee el estado.** **`Abierta`** significa que el texto sigue siendo falso y lleva una marca
> `⚠ Deuda declarada` justo donde el lector se topa con él. **`Saldada`** significa lo contrario: el
> texto ya es correcto y la marca **no** debe estar. `verify.sh` §9 comprueba esa correspondencia en
> los dos sentidos, y por eso no falla nunca por el hecho de haber deuda — solo por declararla mal.

**Once entradas. Once saldadas. Ninguna abierta.**

| # | Estado | Qué afirmaba | Qué se midió | Qué se cambió |
|---|---|---|---|---|
| **O-1** | Saldada el 2026-09-20 | E-02: `register` es **anónimo**, defecto A-1 crítico y abierto; el 401 y el 403 «hoy no ocurren» | Sin token → **401**; con `seller` → **403** | Fuera el bloque rojo y la fila de «autorización que tiene hoy». Los códigos 401 y 403 se declaran como lo que son. **Y entra DP-04**: `role: "admin"` → 422, con el orden de comprobación medido de nuevo |
| **O-2** | Saldada el 2026-09-20 | E-09: el endpoint **no existe**, responde 404 | **200**, con el array idéntico al ejemplo de la propia ficha | La cita de apertura ahora dice que existe y qué cierra |
| **O-3** | Saldada el 2026-09-20 | §2.4 y nueve notas `[no observable] — hoy 500` | **Ningún endpoint devuelve 500** | **§2.4 borrada entera**, las nueve notas y la de paginación también, y la convención `[no observable]` retirada de la cabecera del documento |
| **O-4** | Saldada el 2026-09-20 | D-C8 y H-1: no hay `seller` sembrado; el 403 es «intención, no comportamiento verificado» | **403 con `Content-Length: 0`** en las cinco operaciones de administrador; el mismo token lee con 200 | D-C8 pasa a «y está observado», con el test que lo fija. H-1 y el defecto A-5, cerrados |
| **O-5** | Saldada el 2026-09-20 | D-C5: `size` grande cae a 20 (A-3). D-C11 no observable. H-4: «no existe el test» | `size=999` → `"size": 100`; `size=0` → 20; `totalPages` presente | D-C5 reescrita en pasado, con la medición. H-4 cerrado, con los dos tests de contrato nombrados |
| **O-6** | Saldada el 2026-09-20 | E-15: `.jpg` y `.png` **no llegan** por el portal; solo `webp`; incumple CA-03.1 | Los binarios reales → **200** por el portal; clave inexistente → **404 con 0 bytes**, el de la API | El bloque rojo pasa a contar el defecto en pasado y **conserva la prueba**: la forma del 404 sigue siendo cómo se comprueba. H-5 cerrado |
| **O-7** | Saldada el 2026-09-20 | D-C6: `register` todavía envía `Location: /api/users/{id}` | El 201 emite cinco cabeceras y **ninguna es `Location`** | «Estado» pasa a «cumplido», con el test cuyo nombre lo dice entero. A-4 cerrado |
| **O-8** | Saldada el 2026-09-20 | D-C3: las fechas sin desplazamiento «cuelan». D-C4: la ausencia de rango da 500 o 422 | Los cuatro casos → **400**, nombrando la causa | D-C3 conserva **el porqué** —que era lo valioso— y cambia el estado. La tabla de D-C4 pasa de «Hoy / Con D-C4» a «Antes / Hoy» |
| **O-9** | Saldada el 2026-09-20 | H-2: la persona usuaria ve «Ocurrió un error inesperado» ante un precio no numérico | El portal dice **«Revisa estos datos: precio.»** | H-2 dice ahora que **el argumento con que se justificó D-C9 ya no aplica, pero la decisión sigue sin cumplirse** — y añade la fuga del tipo interno de .NET, que nadie había registrado |
| **O-10** | Saldada el 2026-09-20 | Anexo A, sonda 40: Swagger responde **404** en producción | **200** por `:5000` y por `:8080`, 15 operaciones descritas, página idéntica byte a byte | La sonda 40 se remidió y queda marcada como tal en el anexo |
| **O-11** | Saldada el 2026-09-20 | E-05 y E-08: tres comprobaciones «no existen todavía» — tamaño, tipo y existencia de categoría | Las tres hechas, **con el texto exacto prescrito**; 8,28 MB → 422 | Las tres advertencias retiradas y sustituidas por la medición. A-2 cerrado |

### Lo que este anexo NO cierra

- **El anexo A ya no mide el pasado: se remidió entero el 2026-09-20**, sonda por sonda,
  conservando numeración y pregunta. Y trae **tres sondas nuevas** para comportamiento que antes no
  existía. La deuda que este apartado declaraba pendiente **queda saldada en la misma pasada**.
- **El registro no comprueba que las once fueran todas.** Eso exige medir, y medir es lo que hizo la
  conciliación. Lo que `verify.sh` §9 comprueba es que lo declarado esté completo y bien formado.
- **D-C9 sigue incumplida** y no es deuda de este documento: es un defecto del código, **A-8**, con
  su sonda de cierre en [`../traspaso/HANDOFF-TECNICO.md`](../traspaso/HANDOFF-TECNICO.md) §6.1.

# Arquitectura — Simple Stock Flow

**Cómo están dispuestas las piezas y por dónde pasa cada cosa.** Las decisiones que sostienen esta
disposición viven en `plan.md` y en `adr/`; aquí está la forma, no el porqué.

- **Fecha:** 2026-09-19
- **Rige bajo:** `constitution.md`. Cada regla de dependencia de este documento es un artículo de
  esa constitución, y se verifica igual.

---

## 1. Topología: tres unidades desplegables

```
                    ┌─────────────────────────────┐
   navegador ──────►│  simple-stock-flow-portal   │  Angular · hexágono propio
                    │  nginx sirve el build        │
                    └──────────────┬──────────────┘
                                   │ JSON · token en cabecera
                                   ▼
                    ┌─────────────────────────────┐
                    │  simple-stock-flow-api      │  .NET 8 · hexagonal
                    │  ┌───────────────────────┐  │
                    │  │ adaptadores entrantes │  │  REST (hoy el único)
                    │  ├───────────────────────┤  │
                    │  │ aplicación + dominio  │  │  NÚCLEO
                    │  ├───────────────────────┤  │
                    │  │ adaptadores salientes │  │  persistencia · seguridad · binarios
                    │  └───────────────────────┘  │
                    └────────┬───────────┬────────┘
                             ▼           ▼
              ┌──────────────────┐  ┌──────────────────┐
              │ PostgreSQL       │  │ volumen binarios │
              │ simple-stock-    │  │ sustituible por  │
              │ flow-infra       │  │ almac. objetos   │
              └──────────────────┘  └──────────────────┘
```

| Repositorio | Responsable de | **No** responsable de |
|---|---|---|
| `simple-stock-flow-infra` | Contenedor del motor, red, volúmenes, orquestación del conjunto | **El esquema.** Ni una línea de DDL |
| `simple-stock-flow-api` | Reglas de negocio, API **y el esquema completo** vía migraciones | La orquestación del despliegue |
| `simple-stock-flow-portal` | Interfaz y sus propios casos de uso | El almacenamiento. Solo habla con la API |

La propiedad del esquema es la decisión estructural del conjunto: **ADR-001**.

---

## 2. El hexágono del servicio

```
     adaptador entrante                             adaptadores salientes
   (quién invoca al núcleo)                    (de quién depende el núcleo)

                                                    ┌───── persistencia (EF Core + Npgsql)
   REST ──► puertos entrantes ──► núcleo ──► puertos salientes ─┼───── binarios
            IPlaceSale            dominio +   IProductRepository└───── seguridad (JWT, hash)
            IManageProducts       aplicación  IFileStorage
            IGetSalesReport                   IClock
```

**Hoy REST es el único adaptador entrante**, y `adapters/inbound/` contiene una sola carpeta,
`api/`. Hubo dos proyectos más —mensajería y planificador— que registraban «inactivo» y no hacían
nada: declaraban una capacidad inexistente contra el artículo X y se borraron
([`../traspaso/HANDOFF.md`](../traspaso/HANDOFF.md) §4). El enunciado no los pide.

Lo que esa forma sigue garantizando es la regla: **un adaptador entrante nuevo no llamaría a
persistencia, sino al mismo puerto entrante que usa REST.** Es lo que hace del puerto una frontera
real y no un nombre — pero hasta que exista un segundo adaptador es una propiedad de diseño, no un
hecho observable.

Cómo se aplica SOLID aquí, principio por principio y con su verificación, está en el
**artículo IV** de [`constitution.md`](constitution.md). No se repite.

### 2.1 El portal repite el hexágono

```
src/app/
├─ domain/            modelos, value objects, políticas — sin Angular
├─ application/       ports/ · use-cases/ · state/
├─ infrastructure/    http/ · mappers/ · interceptors/ — implementan los puertos
└─ features/          pantallas por dominio funcional
```

Sus casos de uso no conocen el cliente HTTP. Cambiar el transporte es sustituir una clase de
`infrastructure/`. No es simetría decorativa: es lo que permite probar la política de carrito sin
levantar un servidor.

---

## 3. Interfaces

### 3.1 API

> **La forma exacta de cada petición y de cada respuesta está en
> [`api-contract.md`](api-contract.md), y solo ahí.** Este apartado se queda con lo que es
> arquitectura —qué endpoints hay, quién puede llamarlos, dónde se traduce un error y por qué no
> hay escritura de categorías—. Códigos, cuerpos, campos, tipos, nulabilidad, valores por omisión
> y orden de las filas **no se repiten aquí**: duplicarlos crearía la segunda fuente de verdad que
> el artículo XII prohíbe.

| Método | Ruta | Autorización | Caso de uso |
|---|---|---|---|
| `POST` | `/api/auth/login` | anónimo | autenticar |
| `POST` | `/api/auth/register` | **admin** ⚠ **hoy no lo exige** — ver A-1 | alta de usuario |
| `GET` | `/api/products` | autenticado | listar catálogo paginado |
| `GET` | `/api/products/{id}` | autenticado | obtener producto |
| `POST` `PUT` `DELETE` | `/api/products[/{id}]` | **admin** | mantener catálogo |
| `POST` | `/api/products/{id}/image` | **admin** | adjuntar imagen |
| `GET` | `/api/categories` | autenticado | listar categorías de referencia |
| `POST` | `/api/sales` | autenticado | registrar venta |
| `GET` | `/api/sales[/{id}]` | autenticado | consultar ventas |
| `GET` | `/api/reports/sales?from=&to=` | autenticado | reporte por rango |
| `GET` | `/media/{key}` | **anónimo** | servir la imagen de un producto |
| `GET` | `/health` | anónimo | — |

**La columna de autorización dice lo que la tabla *quiere*, y en una fila el código no lo cumple:**
`POST /api/auth/register` **acepta hoy peticiones sin ningún token** y permite crear un usuario con
rol `admin`. Es un defecto abierto de gravedad crítica, con su causa y su remedio en
[`../traspaso/HANDOFF-TECNICO.md`](../traspaso/HANDOFF-TECNICO.md) §6.1, A-1. La fila se deja como está porque **admin**
es lo correcto; el marcador señala que no es lo vigente.

**`GET /media/{key}` es público y sin token, y eso no estaba declarado en ninguna parte.** No lo
sirve un controlador: lo sirve `UseStaticFiles` sobre el volumen de binarios, bajo la misma URL que
`LocalFileStorage.ResolveUrl` publica dentro de `ProductView`. Al no ser un endpoint enrutado no
pasa por la autorización, así que **no hay forma de protegerlo sin cambiar el mecanismo**. Las
claves son `Guid` de 32 caracteres sin relación con el nombre original, luego no se pueden adivinar
ni enumerar, pero una URL filtrada sirve la imagen a cualquiera para siempre. Hoy es lo que hace
falta para que la etiqueta `<img>` del portal funcione sin cabeceras, y queda como riesgo aceptado
con dueño en [`api-contract.md`](api-contract.md) §5, `H-6`. Su ficha —incluido un defecto nuevo
del proxy del portal, que impide que `.jpg` y `.png` lleguen al servicio— está en
[`api-contract.md`](api-contract.md) E-15.

**Traducción de errores, en un único filtro del adaptador REST:**

| Origen | Respuesta | Motivo |
|---|---|---|
| Violación de invariante de negocio | **422** | La petición estaba bien formada; la regla es lo que falló |
| Contrato mal formado | 400 | — |
| Sin token o token inválido | 401 | Autenticación |
| Rol insuficiente | **403** | Autorización, distinguible de 401 (CA-07.4) |
| Recurso inexistente | 404 | — |
| Conflicto de concurrencia | **409** | Agotados los reintentos de ADR-002 |

> El filtro es **el único sitio** donde una excepción de dominio se convierte en código HTTP. Si
> aparece un `try/catch` de negocio en un controller, esa traducción ya está en dos sitios.

**El cuerpo del error tiene tres formas, y no es uniforme.** El filtro solo produce una de ellas;
las otras las pone ASP.NET Core. Las tres están medidas y reconciliadas en
[`api-contract.md`](api-contract.md) §2, junto con la decisión de qué hacer con el 400, que en su
forma actual deja al portal mostrando *"Ocurrió un error inesperado."* en vez del campo que falló.

**Categorías: lectura, y nada más.** `GET /api/categories` devuelve las categorías de referencia
**completas y sin paginar** —son datos semilla de la migración inicial, no un catálogo que crezca
(D-10)—. Su ficha completa, con el orden de las filas como parte del contrato, está en
[`api-contract.md`](api-contract.md) E-09.

**No hay `POST`, `PUT` ni `DELETE` sobre categorías, y no es un olvido.** El repositorio de
categorías es de solo lectura por decisión de D-10, y la línea de venta congela el nombre de la
categoría (ADR-004). Permitir renombrarlas obligaría a responder qué pasa con los nombres ya
congelados — una pregunta que hoy nadie ha hecho porque el enunciado no pide mantenerlas.

**No añade un puerto.** El método entra en el puerto de catálogo que ya existe —el mismo que
mantiene productos— y se resuelve con `ICategoryRepository.ListAsync`, que **ya está escrito**.
Falta el método del puerto entrante y el controlador que lo expone: el recuento de §3.2 no cambia.

**Dónde vive la forma exacta de cada respuesta.** En [`api-contract.md`](api-contract.md), una ficha
por endpoint. Su **fuente ejecutable** son los registros de `application/Ports/Inbound/`
—`ProductView`, `SaleView`, `SaleItemView`, `SalesReport`, `SalesReportRow`, `AuthResult`,
`PagedResult<T>`—, que están tipados, compilan y coinciden campo a campo con
`infrastructure/http/dto/api.dto.ts` del portal, el único consumidor. El contrato **no los copia**:
declara lo que los registros no pueden declarar por sí solos —obligatoriedad, valores por omisión,
nulabilidad, códigos de error, orden de las filas— y cita el registro como origen.

### 3.1.1 Los huecos del contrato: cerrados

**Este apartado listaba ocho decisiones de comportamiento sin tomar, `C-1` a `C-8`. Ya no hay
ninguna abierta.** Las ocho están decididas, con su motivo y su consecuencia, en
[`api-contract.md`](api-contract.md) §3 —`D-C1` a `D-C8`, en el mismo orden—, junto con tres más
que nadie había escrito: el cuerpo del 400, la moneda del reporte vacío y la fragilidad de
`totalPages`.

Los nombres y las formas de los DTO ya estaban reconciliados entre backend y portal
([`../traspaso/HANDOFF.md`](../traspaso/HANDOFF.md) §7); lo que faltaba era el **comportamiento**, y eso es lo que
cierra el contrato.

**Lo que sigue abierto no son decisiones, son tareas sin dueño**, y están nombradas una a una en
[`api-contract.md`](api-contract.md) §5, `H-1` a `H-6`. Los defectos de implementación
—`A-1` a `A-6`— siguen donde estaban, en
[`../traspaso/HANDOFF-TECNICO.md`](../traspaso/HANDOFF-TECNICO.md) §6.1; el contrato los cita. El que él descubrió,
el del proxy de `/media`, ya tiene ficha allí: es **A-6**.

### 3.2 Puertos

**Entrantes (5):** registrar venta · mantener productos · consultar ventas · reporte · autenticar.

**Salientes (10):** repositorios de producto, venta, categoría y usuario ·
almacenamiento de binarios · hash · generación de token · reloj · unidad de trabajo · **el puerto de lectura del reporte**, `ISalesReportQuery`, que existe desde T-11 y lo implementa `SqlSalesReportQuery`.

Ningún puerto habla el lenguaje de su adaptador: no aparece en sus firmas ningún tipo del ORM, del
servidor web ni del sistema de archivos. Es el artículo II de la constitución, y se comprueba con
una búsqueda de texto.

---

## 4. Flujos

### 4.1 Registrar una venta — donde se concentra el riesgo

```
REST ─► puerto de venta ─► servicio de venta
                              │
                              ├─ cargar productos por lote      ◄── filtra bajas (ADR-003)
                              ├─ cargar categorías               ◄── para congelar (ADR-004)
                              │
                              ├─ abrir venta con el reloj inyectado
                              ├─ por cada línea: añadir item
                              │      └─ retirar del stock  ◄── invariante stock ≥ 0
                              ├─ exigir al menos una línea
                              │
                              └─ confirmar unidad de trabajo
                                     ├─ éxito
                                     └─ conflicto de versión ─► reintento del caso de uso
                                                                completo (máx. 3) ─► 409
```

**Tres propiedades que este flujo debe garantizar**, y que los tests deben ejercer por separado:

1. **Atomicidad** — el descuento y la línea se confirman juntos o ninguno (CA-04.2).
2. **Aislamiento** — dos ventas simultáneas del último ejemplar no dejan stock negativo (CA-04.6).
3. **Congelación** — nombre, precio y categoría de la línea no siguen al catálogo (CA-04.7).

### 4.2 Reporte por rango

```
REST ─► puerto del reporte ─► servicio ─► puerto de lectura ─► agregación en el motor
                                                                agrupa por producto
                                                                suma unidades e importe
```

El servicio **no carga ventas**: recibe filas ya agregadas. Cargar el rango en memoria para sumarlo
es el defecto que CA-06.5 declara bloqueante, y el que el propio comentario del código advierte.

### 4.3 Arranque

```
docker compose up
   ├─ postgres   crea base vacía y usuario
   ├─ service    espera a que el motor responda, no a que el contenedor exista
   │               ├─ aplica migraciones pendientes     ◄── crea el esquema (CA-08.2)
   │               ├─ siembra categorías                ◄── sin esto no se crea un producto
   │               ├─ crea el administrador inicial si no existe, con credenciales de entorno
   │               └─ escucha
   └─ portal     nginx sirve el build y hace de proxy hacia la API
```

**"Espera a que el motor responda"** no es un detalle: un orden de arranque solo garantiza que el
contenedor exista, no que la base acepte conexiones. Es la causa más común de un primer arranque
que falla y un segundo que funciona.

---

## 5. Dónde se satisface cada requisito no funcional

| Requisito | Mecanismo | Dónde vive |
|---|---|---|
| **RNF-01** integridad bajo concurrencia | Control optimista con reintento acotado | Adaptador de persistencia — ADR-002 |
| **RNF-02** fidelidad histórica | Atributos congelados en la línea; sin operación de edición de venta | Dominio — ADR-004 |
| **RNF-03** paginación | Tamaño de página acotado, aplicado en el repositorio | Aplicación |
| **RNF-04** arranque reproducible | Migraciones y semilla en el arranque del servicio | Arranque — ADR-001 |
| **RNF-05** trazabilidad | Registro estructurado con correlación | Adaptador REST |
| **RNF-06** binarios intercambiables | Puerto con clave opaca; el dominio nunca ve rutas ni bytes | Aplicación |
| **RNF-07** pruebas en tres niveles | Dominio sin dobles · aplicación con dobles · integración contra motor real | `tests/` |

---

## 6. Riesgos arquitectónicos abiertos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Migraciones aplicadas en el arranque con varias réplicas | Carrera entre instancias | **Aceptado** con una sola instancia. Declarado en ADR-001: con réplicas, mover a un paso previo del despliegue |
| El filtro de bajas se olvida en la carga previa a vender | **Se vende un producto retirado** | Filtro global: lo seguro por omisión (ADR-003). Pendiente hasta T-09 |

---

## 7. Decisiones registradas

| ADR | Decisión |
|---|---|
| [**ADR-001**](adr/adr-001-propiedad-del-esquema.md) | El esquema pertenece al servicio; la infraestructura entrega un motor vacío |
| [**ADR-002**](adr/adr-002-concurrencia-optimista.md) | Concurrencia optimista sobre la columna de sistema, sin columna de usuario |
| [**ADR-003**](adr/adr-003-baja-logica.md) | Baja lógica como propiedad sombra con filtro global |
| [**ADR-004**](adr/adr-004-reporte-agregado-y-congelado.md) | El reporte agrega en el motor sobre atributos congelados |

---

## 8. Cómo se clonan y se levantan los tres repositorios

`simple-stock-flow-infra` ya existe y se explica a sí mismo (artículo XIII): su README dice cómo se
levanta, qué variable hace qué y dónde están los datos. **La configuración que consume la API
está en `../traspaso/HANDOFF-TECNICO.md` §4.5.** Aquí queda solo la decisión estructural
que ese repositorio no puede tomar por su cuenta, y los dos límites que nunca debe cruzar.

El conjunto son seis directorios hermanos, pero **solo tres se despliegan**: `infra`, `api` y
`portal`. Los otros tres —`docs`, `tools` y `page`— no entran en el compose y no hacen falta para
arrancar. `tools` es el que se movió el 2026-09-21: guarda el sembrador y las imágenes de
demostración, que antes vivían en `infra/demo/` y exigían Python donde la infraestructura promete
que basta con Docker.

**Los tres repositorios desplegables se clonan como hermanos**, y el compose apunta a `../` para construir. El
README del servicio declara que es **submódulo** del de infraestructura: **hoy no lo es, y no
puede serlo** —un submódulo apunta a un repositorio remoto y a un commit concreto, y aquí no hay
ni un commit—. **Hasta T-17 ese README miente y hay que corregirlo.**

Clonar como hermanos le ahorra además al evaluador la trampa clásica: clonar sin
`--recurse-submodules` deja las carpetas vacías y el build falla con un error que no dice por qué.
Cuando los repositorios estén publicados, convertirlos en submódulos es un cambio pequeño y aporta
el clonado único que el enunciado pide al hablar de *"repositorio clonable y ejecutable"* en
singular.

**Lo que este repositorio no lleva nunca:**

- **Ni una línea de DDL, ni datos semilla.** El esquema es del servicio (ADR-001). Un script de
  inicialización del contenedor además solo se ejecuta con el volumen vacío: la segunda versión
  del esquema no se aplicaría jamás, y el fallo sería silencioso.
- **Ningún secreto real.** Un `.env.example` con todas las claves y sin valores; el `.env` real,
  ignorado por git. Tampoco el puerto de la base publicado al host por defecto: se publica bajo un
  perfil de desarrollo, para inspeccionar con un cliente cuando haga falta.

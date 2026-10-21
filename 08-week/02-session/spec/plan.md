# Plan técnico — Simple Stock Flow

**Cómo se construye lo que `spec.md` pide.** Cada decisión trae la alternativa que se descartó y
el motivo, porque una decisión sin alternativa descartada no es una decisión: es una suposición
que nadie revisó.

- **Fecha:** 2026-09-19
- **Rige bajo:** `constitution.md` (innegociable) y `spec.md` (qué y por qué)
- **La forma del sistema** —topología, hexágono, interfaces, flujos— está en
  [`architecture.md`](architecture.md). **Las decisiones estructurales**, en [`adr/`](adr/).
- **El modelo de datos vive entero en [`data-model.md`](data-model.md)** y en ningún otro sitio:
  glosario, entidades e invariantes, columnas, restricciones, claves foráneas, índices, privacidad,
  auditoría y semilla. Aquí quedan las **decisiones técnicas** que lo produjeron (§1).

---

## 1. Decisiones técnicas

### D-01 · Motor: PostgreSQL

El enunciado admite SQLite, SQL Server o Postgres. Se elige **Postgres** por tres razones
concretas, no por preferencia:

- **Decimal exacto.** SQLite tiene afinidad de tipos, no tipos: una columna numérica no entera se
  almacena en punto flotante, y el importe del reporte —el entregable de más peso— dejaría de ser
  exacto.
- **Instante con zona.** El dominio usa instantes con desplazamiento; SQLite no tiene tipo fecha.
- **Control de concurrencia sin inventar columnas.** El testigo de versión del propio motor
  resuelve D-04 sin añadir nada al esquema ni al dominio.

SQL Server cumple todo técnicamente y se descarta solo por peso operativo: imagen de 1,5–2 GB y
licencia en un entregable cuyo extra declarado es Docker Compose. **Si el evaluador lo exigiera,
el plan se porta cambiando dos cosas:** el testigo de concurrencia y el índice de búsqueda por
texto.

### D-02 · Migraciones: EF Core, desde el adaptador de persistencia

```bash
dotnet ef migrations add <Nombre> \
  --project src/adapters/outbound/persistence \
  --startup-project src/bootstrap \
  --output-dir Migrations
```

El arranque aplica las migraciones pendientes. **Decisión completa y alternativa descartada:
[ADR-001](adr/adr-001-propiedad-del-esquema.md).**

**Por qué esto no viola la constitución:** el contexto del ORM y las migraciones viven en el
adaptador de persistencia. El dominio no las conoce, la aplicación tampoco, y ningún tipo del ORM
cruza un puerto. La dependencia sigue apuntando hacia dentro.

### D-03 · Baja lógica como propiedad sombra

La marca de baja **no existe en el agregado**: se declara en la configuración del ORM, con filtro
global. Consecuencias, que son requisitos y no sugerencias:

| Operación | Filtra bajas | Por qué |
|---|---|---|
| Buscar catálogo | **Sí** | Un producto dado de baja no se ofrece |
| Cargar varios por identificador, previo a vender | **Sí** | Sin esto **se puede vender un producto dado de baja** |
| Cargar uno por identificador | **No** | Una línea de venta histórica debe poder resolver su producto |

**Decisión completa: [ADR-003](adr/adr-003-baja-logica.md).**

### D-04 · Concurrencia de stock: optimista, sin columna de usuario

Entre leer el producto y confirmar la transacción cabe otra venta. Dos ventas simultáneas del
último ejemplar leerían el mismo stock y pasarían las dos.

Se reintenta **el caso de uso completo desde la lectura**, no solo la escritura: reintentar la
escritura reaplicaría un descuento calculado sobre un stock viejo. La restricción `stock >= 0` en
la base es la **última barrera**, no el mecanismo: si salta, algo escribió fuera del adaptador y
debe verse, no capturarse en silencio.

**Decisión completa, con cuatro alternativas descartadas:
[ADR-002](adr/adr-002-concurrencia-optimista.md).**

### D-05 · Moneda: el sistema es monomoneda, y ya está decidido en el mapeo

La persistencia guarda el importe en **una sola columna** y reconstruye el value object con la
moneda por defecto del sistema. No hay columna de moneda en ninguna tabla, y ningún contrato de
entrada permite elegir una. **El sistema es monomoneda por construcción.**

Se mantiene así porque es coherente con el alcance: la estructura del value object admite
multimoneda, pero nadie la pide. El día que se pida, el cambio es **una columna extra en el mapeo,
no un cambio de dominio** — que es exactamente el motivo de que el importe lleve su moneda dentro.

**Deuda barata que queda:** el agregado no comprueba la moneda al añadir una línea. Hoy no puede
fallar, pero la protección vive en el mapeo y no en la regla. La guarda cuesta tres líneas y
cierra la puerta donde corresponde (T-05).

**Alternativa descartada:** columna de moneda en la venta. Introduciría una dependencia funcional
de la venta sobre un atributo no clave —violación de tercera forma normal— para proteger algo que
la guarda protege mejor y más arriba.

### D-06 · Reporte: agregación en el motor por un puerto de lectura propio

El servicio del reporte no carga ventas: recibe filas ya agregadas de un puerto de solo lectura. Y
la línea de venta **congela también el nombre de la categoría**, para que recategorizar un producto
no reescriba reportes de períodos cerrados.

**Decisión completa, con su coste y la pregunta abierta que destapa:
[ADR-004](adr/adr-004-reporte-agregado-y-congelado.md).**

### D-07 · Value objects en el mapeo

El importe y la cantidad son estructuras de registro inmutables: no son entidades y no tienen tabla.

- El importe se convierte a **una sola columna** decimal (D-05).
- La cantidad se convierte a un entero.
- El total de la venta y el subtotal de la línea se **ignoran** en el mapeo: son cálculos
  (artículo VII).
- La colección de líneas se configura **por campo**, no por propiedad, para que el agregado
  conserve el control de su propia lista.

**Riesgo declarado:** los constructores lanzan ante datos inválidos, y el ORM materializa por
conversor. **Es el punto de mayor riesgo técnico del mapeo y se prueba primero** (T-01), antes de
escribir los repositorios.

### D-08 · Binarios fuera de la base

La base guarda una **clave opaca**, nunca la ruta ni los bytes.

**Orden obligatorio al dar de baja un producto con imagen:** anular la clave y confirmar la
transacción **primero**, borrar el binario **después**. Un binario huérfano es inofensivo; una
clave que apunta a un binario borrado es una imagen rota permanente. El almacenamiento no participa
en la transacción de la base, así que "en la misma transacción" no es alcanzable y no se promete.

### D-09 · Autenticación

Token firmado y autocontenido; sin estado de sesión, por tanto sin almacén de sesiones. El hash lo
produce un puerto: el dominio **nunca ve la clave en claro** y el resultado de autenticar **nunca
incluye el hash**. La clave de firma viene del entorno y no tiene valor por defecto (artículo IX).

### D-10 · Datos semilla

Las categorías **no son datos de ejemplo: son una dependencia funcional dura.** El repositorio de
categorías es de solo lectura y la categoría del producto es obligatoria, así que **sin categorías
sembradas no se puede crear ni un producto** y el CRUD del enunciado no se podría ejercer. Van en
la migración inicial, con identificadores fijos para que las pruebas puedan referenciarlas.

El **administrador inicial no se siembra desde SQL**: su hash solo puede producirlo el puerto de
hash. Sembrarlo desde SQL exigiría reimplementar una primitiva de seguridad —una segunda
implementación que puede divergir sin que nadie lo note— o versionar una credencial en el
repositorio. Se crea en el arranque, con credenciales de entorno.

---

## 2. Modelo físico del esquema

**Vive íntegro en [`data-model.md`](data-model.md) §3 y §4**, y solo allí: las 21 columnas con tipo,
nulabilidad y defecto, y cada restricción con la marca de **dónde vive hoy** —motor, solo dominio o
pendiente—. Las consultas que lo verifican contra el motor, con su salida, están en su §10.

---

## 3. Índices y patrones de acceso

**Vive íntegro en [`data-model.md`](data-model.md) §6.** Los diez patrones derivados de los puertos,
los índices que existen, los **tres** que faltan, los descartados con su motivo, y quién instala
`pg_trgm`.

---

## 4. Datos personales y retención

**Vive íntegro en [`data-model.md`](data-model.md) §7**, clasificado atributo por atributo y con su
política de retención. La decisión sobre columnas de auditoría —**el proyecto no las lleva**— está
en su §8, y la estrategia de semilla en su §9.

---

## 5. Estrategia de pruebas

Los tres niveles y lo que cada uno **no** puede usar son el artículo VIII de
[`constitution.md`](constitution.md). Lo que añade este plan es **qué toca cubrir en cada uno**:
dominio, cada regla de `spec.md` §4; aplicación, cada caso de uso; integración, el mapeo, los
filtros de baja lógica, la unicidad, la concurrencia y la consulta del reporte.

**Tres pruebas que no pueden faltar**, porque son las que atrapan los defectos caros de este
diseño:

1. **Materialización de los value objects** (D-07): leer de la base un producto y una venta y
   comprobar que se reconstruyen sin lanzar.
2. **Venta concurrente del último ejemplar** (CA-04.6): dos transacciones sobre el mismo producto;
   una confirma, la otra recibe conflicto, el stock no queda negativo.
3. **Fidelidad del reporte** (CA-06.4): vender, renombrar y recategorizar el producto, darlo de
   baja, y comprobar que el reporte del período **no cambia**.

---

## 6. Riesgos de implementación

> Los riesgos de la **forma** del sistema están en [`architecture.md`](architecture.md) §6. Aquí
> están los de construirlo.

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Se olvida filtrar bajas al cargar para vender | Se vende un producto retirado | Filtro global por omisión (D-03), más test de integración |
| El reporte se implementa cargando ventas en memoria | El entregable de más peso queda mal resuelto | CA-06.5 es criterio bloqueante, no preferencia |
| El índice parcial no se usa porque la consulta no lleva el predicado | Se paga el índice y no sirve | El predicado debe aparecer **literalmente** en el filtro ([`data-model.md`](data-model.md) §6.2) |
| Se ensancha un nombre sin ensanchar su copia congelada | La venta trunca el nombre **al registrarse**, y el histórico no se puede reparar | Regla de cambio: ambas columnas en la misma migración |
| El reporte se implementa sin aplicar DP-01 | Duplica filas tras un renombrado, en vez de mostrar el nombre congelado más reciente del rango | **DP-01 ya está decidida** (`spec.md` §7). La regla del desempate va en la consulta agregada, no en el servicio |
| La categoría congelada de T-11 hereda el mismo desempate y nadie lo decide | La agregación elige en silencio | Hueco **H-1** de [`data-model.md`](data-model.md) §11, con dueño |
| El código avanza sin actualizar estos documentos | El spec deja de describir el sistema | Artículo X: si discrepan, gana el código y el documento se corrige |

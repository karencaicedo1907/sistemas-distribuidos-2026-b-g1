# ADR-001 — El esquema pertenece al servicio; la infraestructura entrega un motor vacío

**Estado:** Aceptada · **Fecha:** 2026-09-19

## Contexto

El sistema se despliega como tres repositorios: infraestructura, servicio y portal. Alguien tiene
que crear las tablas, los índices y los datos de referencia, y hay dos sitios plausibles donde
alojar esa responsabilidad: el repositorio de infraestructura, junto al contenedor del motor, o
el repositorio del servicio, junto al modelo que las usa.

La decisión no es de comodidad: determina **qué commit explica el estado del esquema**.

## Decisión

Las migraciones se **generan desde el modelo del ORM** y viven en el adapter de persistencia del
servicio (`src/adapters/outbound/persistence/Migrations`). El servicio las aplica al arrancar.

El repositorio de infraestructura levanta el motor, la base vacía, el usuario, la red y el
volumen. **No contiene ni una línea de DDL.**

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| **SQL versionado con pares `up`/`down`**, ejecutado por un runner independiente en el repo de infraestructura | Es la opción correcta cuando varios servicios comparten la misma base o cuando el rol de administrador de base es distinto del de desarrollo. Aquí hay **un solo consumidor**, y el coste real sería mantener a mano un DDL que el ORM ya deriva del modelo, con la garantía añadida de que ambos diverjan |
| **Esquema creado por el contenedor** con scripts de inicialización | Solo se ejecutan cuando el volumen está vacío. La segunda versión del esquema no se aplicaría nunca, y el fallo sería silencioso |
| **Sincronización automática del modelo** sin migraciones | No deja historia, no es reversible y no se puede revisar en un cambio |

## Consecuencias

**Positivas**

- El commit que cambia una entidad trae la migración que la soporta. No se pueden separar.
- La infraestructura es sustituible: cualquier Postgres accesible sirve, incluido uno gestionado.
- Un entorno limpio queda completo ejecutando únicamente el servicio.

**Negativas, declaradas**

- Aplicar migraciones en el arranque **no es seguro con varias réplicas**: dos instancias
  arrancando a la vez compiten. Con réplicas, esto debe moverse a un paso previo del despliegue.
  Aceptado y declarado como riesgo en `plan.md`.
- El arranque del servicio depende de que el motor esté listo, no solo levantado. Requiere espera
  activa, no un simple orden de arranque.
- Quien revise el esquema tiene que leer código del ORM o la migración generada, no un `.sql`
  escrito a mano.

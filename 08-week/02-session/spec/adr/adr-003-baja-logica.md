# ADR-003 — Baja lógica de producto como propiedad sombra

**Estado:** Aceptada · **Fecha:** 2026-09-19

## Contexto

El enunciado pide CRUD de productos, la "D" incluida. Pero la línea de venta referencia al
producto, y el reporte histórico depende de que esa fila siga existiendo. Borrar físicamente un
producto vendido rompe el histórico; impedir el borrado deja al usuario con un botón que falla
justo para los productos que importan: los que se vendieron.

## Decisión

**Baja lógica.** Una marca de fecha de baja, nula mientras el producto está activo.

La marca **no existe en el agregado**: se declara como **propiedad sombra** en la configuración
del ORM, con **filtro global de consulta**. La operación de baja pasa a ser una actualización; el
borrado físico desaparece del camino de la aplicación, y la clave foránea hacia el producto queda
como barrera de última instancia para que un borrado manual falle ruidosamente.

**Contrato obligatorio de los repositorios:**

| Operación | Filtra bajas | Motivo |
|---|---|---|
| Buscar catálogo | **Sí** | Un producto retirado no se ofrece (CA-01.4) |
| Cargar varios por identificador, previo a vender | **Sí** | Sin esto **se puede vender un producto retirado** (CA-02.6) |
| Cargar uno por identificador | **No** | Una línea de venta histórica debe poder resolver su producto |

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| **Borrado físico con clave foránea restrictiva** | La "D" del CRUD fallaría exactamente para los productos con ventas, y el usuario recibiría un error que no puede resolver por sí mismo |
| **Marca booleana** en vez de fecha | El booleano no responde *cuándo* se dio de baja, y se indexa mal: dos valores efectivos. La fecha nula funciona como predicado de índice parcial |
| **Propiedad real en el agregado** | El dominio ganaría un concepto que es de persistencia. Es defendible verlo como regla de negocio —"un producto vendido se retira, no se elimina"—, pero el puerto ya expone una operación de borrado y añadirlo al agregado tendría mayor radio de cambio sin beneficio observable |

## Consecuencias

**Positivas**

- El dominio no se entera: `Product` no gana ni una propiedad.
- El filtro global hace que **lo seguro sea lo que ocurre por omisión** y la excepción tenga que
  escribirse a mano. La forma inversa —filtrar en cada consulta y confiar en no olvidarlo— es
  exactamente cómo se cuela la venta de un producto retirado.

**Negativas, declaradas**

- Las filas no se recuperan nunca: la tabla solo crece. Irrelevante al volumen previsto.
- Todo índice que sirva al catálogo debe llevar el predicado de baja, y **toda consulta que
  quiera usarlo debe llevarlo literalmente**, o el motor no lo usará.
- **Regla para el futuro:** cualquier restricción de unicidad que se añada sobre el producto debe
  ser parcial sobre los activos. Si no, un producto retirado reservaría su código para siempre.

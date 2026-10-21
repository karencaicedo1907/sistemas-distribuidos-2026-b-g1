# ADR-004 — El reporte agrega en el motor sobre atributos congelados

**Estado:** Aceptada, con una pregunta abierta · **Fecha:** 2026-09-19

## Contexto

El reporte de ventas por rango es el entregable funcional de más peso del enunciado. Necesita,
por producto: unidades, importe, nombre y categoría. Dos problemas:

1. El puerto de ventas existente devuelve **agregados de venta completos** para un rango, sin
   paginar. Usarlo obliga a cargar todas las ventas del período en memoria — exactamente lo que
   CA-06.5 declara defecto.
2. El nombre de la categoría **no es alcanzable** desde la venta: la línea congela el nombre del
   producto, pero no el de su categoría. Y el servicio del reporte no inyecta ningún repositorio
   de catálogo, así que hoy **no puede resolver ese campo de ninguna manera**.

## Decisión

**Dos cosas, y las dos son necesarias:**

1. **Un puerto outbound de solo lectura** propio del reporte, que devuelve las filas ya agregadas.
   El adapter lo implementa con una consulta que agrupa **en el motor**. El servicio de aplicación
   no ve una sola entidad de venta.
2. **La línea de venta congela también el nombre de la categoría**, igual que ya congela nombre y
   precio del producto.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| **Unir contra el catálogo vivo** para resolver la categoría | Recategorizar un producto **reescribiría reportes ya emitidos** de períodos cerrados. Viola CA-06.4 y RNF-02 |
| **Inyectar repositorios de catálogo en el servicio del reporte** | Le daría dos razones para cambiar y reintroduciría la lectura viva del punto anterior |
| **Guardar también el identificador de categoría** en la línea | Obligaría a unir contra la categoría para mostrar el nombre, que es la lectura viva que se quiere evitar. El reporte agrupa por producto, no por categoría |
| **Tabla de reporte precalculada** | Un agregado almacenado que puede desincronizarse, para un volumen que no lo necesita |

## Consecuencias

**Positivas**

- El reporte de un período cerrado es **estable por construcción**, no por disciplina.
- La consulta toca dos tablas en vez de cuatro.
- El servicio del reporte mantiene una sola responsabilidad.

**Negativas, declaradas**

- **Cambia la firma de un método del agregado**: añadir una línea pasa a requerir la categoría
  además del producto, y el caso de uso de venta gana una dependencia para resolverla. El coste
  no desaparece: se mueve del camino de lectura al de escritura, que es donde el dato se conoce.
- El nombre de la categoría se repite en cada línea. **No es desnormalización**: dos líneas del
  mismo producto pueden legítimamente tener categorías distintas si se recategorizó entre ambas
  ventas, luego no existe la dependencia funcional que haría transitiva la repetición.

## Pregunta abierta que esta decisión hace visible

**DP-01.** Con los nombres congelados, un producto **renombrado** entre dos ventas del mismo rango
produce dos valores distintos. Agrupar por producto y nombre devuelve **dos filas del mismo
producto**; agrupar solo por producto obliga a **elegir** un nombre. Ninguna de las dos opciones es
neutral, y la elección es de negocio.

**Mientras no se decida**, la implementación toma el nombre de la venta más reciente del rango y
lo deja escrito en el código como la decisión provisional que es — no disimulada dentro de una
consulta.

**Actualización — la pregunta es más pequeña de lo que este ADR creyó.** De las dos salidas que
el párrafo anterior presenta como equivalentes, **una ya estaba prohibida**: agrupar por producto
y nombre devuelve dos filas del mismo producto, y CA-06.1 pide *una fila por producto*. Unir
contra el catálogo vivo tampoco es una salida — CA-06.4 y RNF-02 lo cierran, que es justamente lo
que este ADR decidió. Queda por elegir **cuál** de los nombres congelados del rango, y eso no
bloquea la implementación: es una cláusula de ordenación. Análisis completo en
[`spec.md` §7.1](../spec.md).

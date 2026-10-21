# ADR-002 — Concurrencia optimista sobre el stock, sin columna de usuario

**Estado:** Aceptada · **Fecha:** 2026-09-19

## Contexto

`Product.Withdraw` garantiza que el stock no quede negativo **en memoria**. Entre que el caso de
uso lee los productos y confirma la transacción hay una ventana en la que otra venta puede
retirar del mismo producto: **ambas leen el mismo stock, ambas pasan la invariante, y la base
queda con stock negativo** aunque el dominio se haya cumplido en las dos.

CA-04.6 lo declara inaceptable: dos ventas simultáneas del último ejemplar deben resolverse con
una confirmación y un conflicto.

## Decisión

**Control optimista** con el token de versión que el propio motor mantiene por fila, expuesto
como **propiedad sombra**. Aislamiento por defecto del motor.

- Toda actualización de un producto condiciona la escritura a que la fila no haya cambiado desde
  que se leyó. Si cambió, la escritura no afecta a ninguna fila y se detecta.
- Ante conflicto se **reintenta el caso de uso completo desde la lectura**, tres veces con espera
  irregular. Agotados los intentos, la API responde con conflicto.
- Restricción `stock >= 0` en la base como **última barrera**: si salta, algo escribió fuera del
  adapter, y debe verse, no capturarse en silencio.
- **La excepción de concurrencia del ORM no sale del adapter.** Es un tipo de infraestructura; si
  llegara a la capa de aplicación, la regla de dependencia del patrón estaría rota.

## Alternativas consideradas

| Alternativa | Por qué no |
|---|---|
| **Columna de versión mantenida por la aplicación** | Exige que **todas** las rutas de escritura la incrementen. El día que una migración, una semilla o un adapter nuevo la olvide, el mecanismo queda desactivado en silencio y nadie se entera hasta que hay stock negativo |
| **Bloqueo pesimista** al leer los productos | El caso de uso lee N productos por identificador: bloquearlos durante toda la venta **serializa ventas que no comparten ningún producto**. Además convertiría un puerto de lectura en una lectura que bloquea, algo que su firma no expresa: el bloqueo se filtraría al contrato |
| **Aislamiento serializable** | Convierte el mismo conflicto en un fallo de serialización que también hay que reintentar — mismo coste — pero imponiendo bloqueo de predicados a **todas** las lecturas, incluidas las del reporte, que no tienen nada que evitar |
| **Decremento condicional en una sola sentencia** | Correcto y más barato, pero **mueve la decisión de negocio al SQL**: el dominio dejaría de ser la autoridad sobre su invariante. Se descarta por frontera, no por rendimiento. Es la alternativa a reconsiderar si el reintento resultara caro |

## Consecuencias

**Positivas**

- Superficie de esquema **cero**: no hay columna que el dominio pueda exponer por accidente.
- El motor mantiene el token en toda actualización, sin disciplina del programador.

**Negativas, declaradas**

- El token cambia ante **cualquier** actualización de la fila, incluido un renombrado o un cambio
  de imagen: produce conflictos que una versión ligada solo al stock no produciría. El reintento
  los absorbe.
- Es específico del motor elegido. Con otro motor el equivalente es una columna de versión nativa:
  mismo patrón, distinta materialización.
- Reintentar **solo la escritura** en vez del caso de uso completo reaplicaría un descuento
  calculado sobre un stock viejo. Es el error más probable al implementarlo, y por eso queda
  escrito aquí.

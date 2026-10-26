# Estados vacíos y acceso de alta en tablas

## Objetivo

Hacer más claro qué significa una tabla sin filas y ofrecer un acceso contextual para crear registros desde el final de los listados principales.

## Alcance

- Aplicar estados vacíos en español a las tablas de la aplicación, incluidas las listas secundarias y de consulta.
- Distinguir entre una lista que todavía no tiene registros y una lista cuyos registros no coinciden con la búsqueda o los filtros activos.
- Agregar una fila de alta debajo de los resultados de Rutas, Clientes, Inventario y Salidas.
- Mantener los flujos de alta existentes: formularios para catálogos y asistente para Salidas.

## Comportamiento

### Estado vacío

- Reemplazar el mensaje técnico predeterminado en inglés por un estado vacío amigable, con icono o ilustración, título breve y explicación en español.
- Adaptar título y explicación al contexto de cada tabla.
- Diferenciar “todavía no hay registros” de “no hay resultados que coincidan con la búsqueda o los filtros”.
- Cuando la tabla admite altas, incluir en el estado vacío una acción para abrir el flujo de alta existente. En tablas de consulta o vistas sin permiso de alta, mostrar solo información.
- Mantener el estado vacío propio de Checkpoints y el del dashboard dentro del patrón visual y lingüístico común.

### Fila de alta

- En las tablas principales de Rutas, Clientes, Inventario y Salidas, mostrar debajo de las filas una fila discreta y accionable con “+ Nuevo [elemento]”.
- La acción abre el formulario o asistente de alta existente; no inicia edición directa en una nueva fila.
- Mostrar la fila cuando la tabla tiene resultados y no hay criterios activos de búsqueda o filtro.
- Mantener la fila después del último registro también en listas largas; el contenedor de la pantalla permite desplazarse por el conjunto completo.
- Ocultarla cuando una búsqueda o filtro esté activo. Los botones de alta del encabezado continúan disponibles.
- No agregar esta fila a Checkpoints, al dashboard ni a Inventario de baja.
- Conservar los botones de alta existentes en el encabezado.
- Cuando no existan registros (en vez de estar vacía por filtros), usar el estado vacío con su acción de alta y ocultar la fila “+” para evitar duplicación.

## Criterios de aceptación

1. Ninguna tabla visible presenta el mensaje predeterminado “No content in table”.
2. El texto del estado vacío está en español y permite distinguir ausencia de registros de ausencia de coincidencias.
3. Las tablas sin alta disponible no ofrecen una acción de creación en el estado vacío.
4. Rutas, Clientes, Inventario y Salidas muestran “+ Nuevo …” debajo de sus resultados cuando corresponde.
5. La fila de alta abre el flujo existente y no crea ni guarda datos por sí misma.
6. La fila de alta no aparece durante búsquedas o filtros activos ni en Inventario de baja, Checkpoints o el dashboard.
7. Las acciones de alta del encabezado siguen disponibles.

## Fuera de alcance

- Cambiar formularios, validaciones, permisos o reglas de persistencia.
- Reemplazar las altas actuales por edición inline.
- Rediseñar la estructura o las acciones por fila de las tablas.

## Decisiones de dominio y arquitectura

No se agregan conceptos de dominio: los cambios afectan la presentación de tablas y reutilizan altas existentes. Se conserva la decisión de `docs/adr/0004-acciones-por-fila-y-edicion-inline.md`: Salidas usa un formulario completo por sus relaciones operativas, mientras los catálogos conservan sus flujos de formulario para altas.

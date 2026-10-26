# Acciones por fila y edición mediante formularios

Los listados de Clientes, Inventario y Rutas ofrecen sus acciones sobre cada fila, y las altas y ediciones se realizan mediante formularios. Las acciones por fila acercan las operaciones pertinentes al registro; la edición mediante formulario conserva en un mismo flujo las validaciones y los datos relacionados de cada entidad. Salidas también conserva la edición mediante formulario completo porque relaciona datos operativos y reemplaza su copia operativa; checkpoints y equipo mínimo mantienen sus acciones en sus paneles, y las filas de Salida muestran solo las acciones válidas para su estado.

La edición de una Salida reemplaza el contenido central de la ventana actual y mantiene visible la navegación de la aplicación, con Salidas activa. Se elige este patrón para que la edición de Salidas siga la navegación del resto de módulos, sin fragmentar los datos operativos en ventanas separadas. El formulario completo puede desplazarse y ofrece Guardar y Cancelar; ambas acciones regresan al listado de Salidas, que se actualiza después de guardar.

Si el usuario intenta cambiar de sección mientras el formulario contiene cambios sin guardar, se solicita confirmación. Permanecer conserva el formulario; confirmar el descarte permite continuar a la sección elegida. Sin cambios pendientes, la navegación continúa sin confirmación.

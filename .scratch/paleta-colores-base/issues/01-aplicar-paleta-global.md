# Aplicar la paleta de colores base a la interfaz

Status: closed

Implementar el tema visual global de TrekRisk según la [especificación](../../../docs/specs/paleta-colores-base/spec.md).

La paleta debe integrarse en todas las pantallas, navegación, formularios, tablas, diálogos y notificaciones. El verde oscuro-medio `#32321f` reemplaza el azul de fondo de la sidebar; el verde medio `#454425` distingue la sección activa; el verde claro se usa para acciones principales y acentos; el tono claro neutro `#eae9e4` identifica selecciones en tablas, listas, filtros y menús desplegables; el verde oscuro para texto y elementos oscuros; y `#f4f4f4` para el hover de una selección activa. Las opciones no seleccionadas conservan su superficie normal y, sin hover, las selecciones usan `#eae9e4`. Se conservan superficies blancas y colores funcionales para errores, advertencias y éxitos. Los estados hover, foco y selección deben mantener contraste y ser distinguibles.

La validación prevista es manual en JavaFX sobre las secciones principales y controles mencionados en la especificación. La distribución y el comportamiento actual deben conservarse.

## Comments

- Implementación completada en la hoja de estilos compartida y revisada contra la especificación.
- Compilación Maven y `git diff --check` completados correctamente. La revisión no encontró pendientes.
- El usuario aprobó cerrar la spec y el ticket el 2026-10-27.

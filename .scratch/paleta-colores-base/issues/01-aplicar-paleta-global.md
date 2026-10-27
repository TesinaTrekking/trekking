# Aplicar la paleta de colores base a la interfaz

Status: closed

Implementar el tema visual global de TrekRisk según la [especificación](../../../docs/specs/paleta-colores-base/spec.md).

La paleta debe integrarse en todas las pantallas, navegación, formularios, tablas, diálogos y notificaciones. El verde oscuro-medio `#32321f` reemplaza el azul de fondo de la sidebar; el verde medio `#454425` distingue la sección activa; el verde claro se usa para acciones principales y acentos; las selecciones en tablas, listas, filtros y menús desplegables usan `#e1ded5`, y el hover de elementos no seleccionados usa `#eae9e4`. Un elemento seleccionado conserva `#e1ded5` bajo el cursor y al retirarlo. El verde oscuro se usa para texto y elementos oscuros. Se conservan superficies blancas y colores funcionales para errores, advertencias y éxitos. Los estados hover, foco y selección deben mantener contraste y ser distinguibles.

La validación prevista es manual en JavaFX sobre las secciones principales y controles mencionados en la especificación. La distribución y el comportamiento actual deben conservarse.

## Comments

- Implementación completada en la hoja de estilos compartida y revisada contra la especificación.
- Compilación Maven y `git diff --check` completados correctamente. La revisión no encontró pendientes.
- El usuario aprobó cerrar la spec y el ticket el 2026-10-27.
- Ajuste posterior aprobado: selección `#e1ded5` y hover `#eae9e4`, con selección estable al pasar o retirar el cursor; se aplicó a tablas, listas y opciones de filtros. Implementado y compilado el 2026-10-27.

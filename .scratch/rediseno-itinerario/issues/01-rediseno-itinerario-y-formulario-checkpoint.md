# Rediseño de gestión del Itinerario y formulario de Checkpoint

Status: closed

Implementar la presentación de la gestión del Itinerario y el formulario de alta/edición de Checkpoint de acuerdo con la especificación funcional. Mantener la edición pendiente y el guardado conjunto existentes; mostrar los Checkpoints por hora, conservar el orden actual en empates y quitar los controles de reordenamiento manual. El formulario debe reemplazar el contenido central dentro de la shell y seguir el patrón visual de los formularios rediseñados.

Especificación: [spec.md](../../../docs/specs/rediseno-itinerario/spec.md)

## Comments

- Seam previsto: recorrido manual desde «Ver checkpoints» hasta alta/edición, guardado o descarte del Itinerario en la aplicación JavaFX, más compilación Maven.
- El ADR del orden explícito continúa vigente; la interfaz conserva el orden persistido en empates y no ofrece controles para alterarlo.
- Implementación aplicada. `mvn compile` y `mvn test` concluyeron correctamente; Maven reportó que no hay pruebas que ejecutar. Los FXML pasaron validación XML y `git diff --check` fue correcto.
- Pendiente revisión manual visual de JavaFX.
- Ajuste visual solicitado: horas sin cápsula y acciones por fila con los iconos SVG existentes del proyecto.
- Ajuste visual adicional: alta con `ADD.svg`, hora alineada con el nombre y acciones por fila con visibilidad hover como en las tablas.
- Revisión visual aprobada por el usuario el 2026-10-26; issue cerrado.

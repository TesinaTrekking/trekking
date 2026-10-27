# Claude Code: instrucciones del proyecto

Importa y sigue las instrucciones compartidas del repositorio en @AGENTS.md.

## Flujo de trabajo

- Respeta las etapas separadas: especificación, desglose en tickets, implementación y revisión. Presenta cada resultado y espera la aprobación explícita del usuario antes de comenzar la siguiente etapa.
- Antes de cambios de dominio, consulta `docs/agents/domain.md` y la guía de seguimiento en `docs/agents/issue-tracker.md` cuando corresponda.
- Conserva los nombres de dominio y el vocabulario de interfaz en español.
- Para cambios de código, ejecuta como mínimo `mvn -f trekking-app/pom.xml compile`. No describas la fase Maven `test` como cobertura de comportamiento: el proyecto no tiene pruebas automatizadas configuradas.

## Rutas protegidas

No leas ni modifiques estas rutas salvo que el usuario lo pida expresamente:

- `/target/`
- `trekking-app/mvnw`, `trekking-app/mvnw.cmd` y `trekking-app/.mvn/`
- `trekking-app/trekking.db`
- `trekking-app/scripts/`
- `trekking-app/src/main/resources/com/example/fonts/`

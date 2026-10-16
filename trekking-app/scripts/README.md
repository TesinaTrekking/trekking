# Seed demo — `scripts/seed_demo.py`

Puebla la base SQLite con un dataset fijo y determinista para mostrar la app
sin cargar datos en vivo.

## Qué carga

| Módulo | Cantidad | Detalle |
|---|---|---|
| Rutas | 8 | 6 activas + 2 inactivas |
| Checkpoints | 20 | 3–4 por ruta; 2 compartidos entre rutas |
| Equipamiento | 25 | Todas las categorías; 2 dados de baja |
| Clientes | 30 | 27 mayores + 3 menores con tutor; 3 inactivos |
| Recorridos | 12 | Fechas relativas a hoy (−60 a +30 días); 1 sin `hora_fin`, 1 sin participantes |

También crea las relaciones `ruta_checkpoints` (con orden),
`rutas_equipamiento` (con cantidad requerida), `contactos_emergencia` y
`recorrido_cliente`. Las fichas médicas se dejan en `NULL`.

## Requisitos

Solo Python 3 con stdlib (`sqlite3`, `argparse`). Sin dependencias pip.

## Uso

Desde la carpeta `trekking-app/`:

```bash
# Pobla (o reemplaza solo el demo anterior)
python3 scripts/seed_demo.py

# Contra otra base
python3 scripts/seed_demo.py --db /ruta/a/trekking.db

# Elimina solo el demo sin poblar
python3 scripts/seed_demo.py --clean
```

## Garantías

- **No toca datos del usuario.** El script registra sus filas en la tabla
  manifiesto `__seed_demo` y en cada corrida borra únicamente esas filas
  antes de reinsertar. Es re-ejecutable sin duplicar ni perder datos reales.
- **Respeta las validaciones de la app**: nombres únicos, enums de
  terreno/dificultad/categoría/estado, formato `HH:mm`, coordenadas,
  DNI 7–8 dígitos, `hora_fin > hora_inicio`, UNIQUEs de las tablas puente.
- La base local `trekking.db` no se commitea; el artefacto versionado es
  este script.

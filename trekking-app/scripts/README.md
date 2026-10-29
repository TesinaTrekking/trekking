# Carga de datos de ejemplo

Desde la raíz del repositorio, ejecutá:

```bash
python3 trekking-app/scripts/seed_data.py
```

Por defecto prepara `trekking.db` en la raíz. La app busca la raíz del
repositorio y usa ese mismo archivo aunque se inicie desde `trekking-app/`.
Para usar otra base desde el script:

```bash
python3 trekking-app/scripts/seed_data.py --db /ruta/a/trekking.db
```

El comando compila la app y ejecuta la inicialización real del esquema por
Java, sin abrir JavaFX. Luego carga nueve Rutas de las sierras de Córdoba (una inactiva) con sus
Checkpoints, quince Clientes con datos personales ficticios y sin ficha médica,
diez Elementos de Inventario y nueve Salidas. Las fechas se calculan desde el
día de ejecución. Las coordenadas y altitudes son aproximadas y no sirven para
navegación.

Las claves estables de `__seed_owned` identifican las filas administradas por
la carga. Una ejecución posterior actualiza esas filas y las copias operativas
de sus Salidas sin borrar datos del usuario. Las colisiones de nombre de Ruta,
nombre de Elemento o DNI se informan y se omiten junto con las relaciones que
dependen del registro. Al final se informa el resumen de insertados,
actualizados y omitidos, además de los totales de las tablas.

El verificador automatizado puede ejecutarse desde la raíz con:

```bash
python3 trekking-app/scripts/test_seed_data.py
```

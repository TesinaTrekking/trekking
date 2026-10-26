# Carga de datos demo

Desde la raíz del repositorio, ejecutá:

```bash
python3 trekking-app/scripts/seed_demo.py
```

Por defecto prepara `trekking.db` en la raíz. La app busca la raíz del
repositorio y usa ese mismo archivo aunque se inicie desde `trekking-app/`.
Para usar otra base desde el script:

```bash
python3 trekking-app/scripts/seed_demo.py --db /ruta/a/demo.db
```

El comando compila la app y ejecuta la inicialización real del esquema por
Java, sin abrir JavaFX. Luego carga siete Rutas (incluye una Ruta borrador y
una inactiva), quince Clientes ficticios sin ficha médica, diez
Elementos de Inventario y nueve Salidas. Las fechas se calculan desde el día de
ejecución. Las coordenadas son aproximadas y solo sirven para demostración.

Las claves estables de `__seed_demo_owned` identifican las filas administradas por
la carga. Una ejecución posterior actualiza esas filas y las copias operativas
de sus Salidas sin borrar datos del usuario. Las colisiones de nombre de Ruta,
nombre de Elemento o DNI se informan y se omiten junto con las relaciones que
dependen del registro. Al final se informa el resumen de insertados,
actualizados y omitidos, además de los totales de las tablas.

El verificador automatizado puede ejecutarse desde la raíz con:

```bash
python3 trekking-app/scripts/test_seed_demo.py
```

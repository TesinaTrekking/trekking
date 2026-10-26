#!/usr/bin/env python3
"""Carga aditiva de datos demostrativos para Trekking App (Python 3 stdlib)."""
from __future__ import annotations

import argparse
import datetime as dt
from pathlib import Path
import sqlite3
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / "trekking-app"
DEFAULT_DB = ROOT / "trekking.db"
MANIFEST = "__seed_demo_owned"

ROUTES = [
    ("demo-ruta-condorito", "Demo Quebrada del Condorito", 2100, "Rocoso", "Media", "Media", 1),
    ("demo-ruta-champqui", "Demo Villa Alpina y Cerro Champaquí", 2790, "Mixto", "Alta", "Alta", 1),
    ("demo-ruta-gigantes", "Demo Los Gigantes", 2370, "Rocoso", "Alta", "Alta", 1),
    ("demo-ruta-uritorco", "Demo Cerro Uritorco", 1979, "Mixto", "Media", "Alta", 1),
    ("demo-ruta-alpina", "Demo paseo serrano de Villa Alpina", 1600, "Boscoso", "Baja", "Baja", 1),
    ("demo-ruta-borrador", "Ruta demo borrador", 1450, "Sendero", "Baja", "Baja", 1),
    ("demo-ruta-inactiva", "Ruta demo inactiva", 1200, "Boscoso", "Baja", "Baja", 0),
]
POINTS = {
    "demo-ruta-condorito": [("Ingreso al Parque", "08:00", -31.650, -64.700, 1500, "Coordenada aproximada de demostración."), ("Balcon Norte", "11:00", -31.630, -64.750, 1900, "Punto representativo; no es navegación."), ("Mirador de la Quebrada", "14:00", -31.620, -64.780, 2000, "Referencia demostrativa.")],
    "demo-ruta-champqui": [("Villa Alpina", "07:00", -31.900, -64.780, 1350, "Inicio representativo."), ("Refugio de montaña", "12:00", -31.930, -64.820, 2300, "Ubicación aproximada."), ("Cerro Champaqui", "16:00", -31.980, -64.930, 2790, "Coordenada aproximada de demostración.")],
    "demo-ruta-gigantes": [("Playa de los Rios", "08:00", -31.380, -64.800, 1900, "Referencia de acceso."), ("El Mogote", "12:00", -31.390, -64.820, 2300, "Punto representativo."), ("Cerro Los Gigantes", "15:00", -31.400, -64.830, 2370, "Coordenada aproximada de demostración.")],
    "demo-ruta-uritorco": [("Capilla del Monte", "08:00", -30.860, -64.520, 980, "Punto de encuentro demostrativo."), ("Mirador del Uritorco", "11:00", -30.870, -64.530, 1500, "Referencia aproximada."), ("Cumbre del Uritorco", "14:00", -30.880, -64.540, 1979, "Coordenada aproximada de demostración.")],
    "demo-ruta-alpina": [("Villa Alpina", "09:00", -31.900, -64.780, 1350, "Referencia demostrativa."), ("Arroyo serrano", "11:00", -31.910, -64.790, 1450, "Coordenada aproximada.")],
}
GEAR = [
    ("demo-equipo-carpa", "Carpa demo", "Campamento", 8, "Disponible", 1),
    ("demo-equipo-dormir", "Bolsa de dormir demo", "Campamento", 16, "Disponible", 1),
    ("demo-equipo-gps", "GPS demo", "Navegación", 4, "Disponible", 1),
    ("demo-equipo-mapa", "Mapa topográfico demo", "Navegación", 2, "En Inspección", 1),
    ("demo-equipo-radio", "Radio VHF demo", "Comunicación", 6, "Disponible", 1),
    ("demo-equipo-casco", "Casco demo", "Seguridad", 12, "Disponible", 1),
    ("demo-equipo-cuerda", "Cuerda demo", "Escalada", 4, "En Reparación", 1),
    ("demo-equipo-campera", "Campera impermeable demo", "Vestimenta", 10, "Disponible", 1),
    ("demo-equipo-filtro", "Filtro de agua demo", "Hidratación", 3, "Disponible", 1),
    ("demo-equipo-botiquin", "Botiquín demo", "Primeros Auxilios", 5, "Disponible", 1),
]
CLIENTS = [
    ("40100001", "Lucía", "Demo", "Femenino"), ("40100002", "Mateo", "Demo", "Masculino"),
    ("40100003", "Sofía", "Demo", "Femenino"), ("40100004", "Tomás", "Demo", "Masculino"),
    ("40100005", "Valentina", "Demo", "Femenino"), ("40100006", "Julián", "Demo", "Masculino"),
    ("40100007", "Camila", "Demo", "Femenino"), ("40100008", "Nicolás", "Demo", "Masculino"),
    ("40100009", "Agustina", "Demo", "Femenino"), ("40100010", "Bruno", "Demo", "Masculino"),
    ("40100011", "Florencia", "Demo", "No binario"), ("40100012", "Santiago", "Demo", "Masculino"),
    ("40100013", "Martina", "Demo", "Femenino"), ("40100014", "Facundo", "Demo", "Masculino"),
    ("40100015", "Julieta", "Demo", "Prefiero no decir"),
]
TRIPS = [
    ("demo-salida-1", 0, -2, "planificada", [0, 1, 2], [("demo-equipo-gps", 1), ("demo-equipo-radio", 2)]),
    ("demo-salida-2", 1, 5, "planificada", [3, 4, 5, 6], [("demo-equipo-carpa", 2), ("demo-equipo-casco", 4)]),
    ("demo-salida-3", 2, 12, "planificada", [7, 8], [("demo-equipo-gps", 1), ("demo-equipo-botiquin", 1)]),
    ("demo-salida-4", 3, -5, "planificada", [0, 4], [("demo-equipo-radio", 1)]),
    ("demo-salida-5", 0, -8, "realizada", [1, 2, 3, 5], [("demo-equipo-filtro", 1)]),
    ("demo-salida-6", 1, -35, "realizada", [6, 7, 8], [("demo-equipo-carpa", 1)]),
    ("demo-salida-7", 2, -60, "realizada", [], []),
    ("demo-salida-8", 3, 20, "cancelada", [9], []),
    ("demo-salida-9", 4, 30, "cancelada", [], [("demo-equipo-dormir", 2)]),
]


def java_classpath() -> str:
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    pom = ET.parse(APP / "pom.xml").getroot()
    sqlite_version = next(d.findtext("m:version", namespaces=ns) for d in pom.findall("m:dependencies/m:dependency", ns)
                         if d.findtext("m:artifactId", namespaces=ns) == "sqlite-jdbc")
    m2 = Path.home() / ".m2/repository"
    sqlite_jar = m2 / f"org/xerial/sqlite-jdbc/{sqlite_version}/sqlite-jdbc-{sqlite_version}.jar"
    slf4j_jar = m2 / "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
    if not sqlite_jar.is_file():
        raise FileNotFoundError(f"No se encontró el driver SQLite de Maven: {sqlite_jar}")
    jars = [str(sqlite_jar)] + ([str(slf4j_jar)] if slf4j_jar.is_file() else [])
    return ":".join([str(APP / "target/classes"), *jars])


def initialize_schema(db: Path | None = None, *, cwd: Path = ROOT, compile_app: bool = True) -> None:
    """Compile and call the app's schema initializer without starting JavaFX."""
    if compile_app:
        subprocess.run(["mvn", "-q", "-f", str(APP / "pom.xml"), "compile"], cwd=ROOT, check=True)
    command = ["java"]
    if db is not None:
        command.append(f"-Dtrekking.db={db}")
    command.extend(["-cp", java_classpath(), "com.example.database.ConexionDB"])
    subprocess.run(command, cwd=cwd, check=True)


def own(conn: sqlite3.Connection, entity: str, key: str) -> int | None:
    row = conn.execute(f"SELECT id1 FROM {MANIFEST} WHERE entidad=? AND clave=?", (entity, key)).fetchone()
    return row[0] if row else None


def remember(conn: sqlite3.Connection, entity: str, key: str, id1: int, id2: int | None = None) -> None:
    conn.execute(f"INSERT INTO {MANIFEST}(entidad,clave,id1,id2) VALUES(?,?,?,?)", (entity, key, id1, id2))


def upsert_owned_row(conn: sqlite3.Connection, entity: str, key: str, table: str,
                     columns: tuple[str, ...], values: tuple, natural_sql: str | None = None,
                     natural_value: str | tuple[str, ...] | None = None) -> tuple[int | None, str]:
    old = own(conn, entity, key)
    if old is not None:
        assignments = ",".join(f"{c}=?" for c in columns)
        conn.execute(f"UPDATE {table} SET {assignments} WHERE id=?", (*values, old))
        return old, "actualizados"
    natural_values = natural_value if isinstance(natural_value, tuple) else (natural_value,)
    if natural_sql and conn.execute(natural_sql, natural_values).fetchone():
        print(f"Conflicto omitido: {entity} {key} colisiona con {natural_value}", file=sys.stderr)
        return None, "omitidos"
    marks = ",".join("?" for _ in columns)
    cur = conn.execute(f"INSERT INTO {table} ({','.join(columns)}) VALUES ({marks})", values)
    row_id = cur.lastrowid
    remember(conn, entity, key, row_id)
    return row_id, "insertados"


def seed(conn: sqlite3.Connection) -> dict[str, int]:
    conn.execute(f"CREATE TABLE IF NOT EXISTS {MANIFEST} (entidad TEXT NOT NULL, clave TEXT NOT NULL, id1 INTEGER NOT NULL, id2 INTEGER, PRIMARY KEY(entidad, clave))")
    counts = {"insertados": 0, "actualizados": 0, "omitidos": 0}
    def note(result: str) -> None: counts[result] += 1
    today = dt.date.today()
    routes: dict[str, tuple[int, tuple]] = {}
    for key, name, altitude, terrain, tech, physical, active in ROUTES:
        vals = (name, name.casefold(), altitude, terrain, tech, physical, active)
        rid, result = upsert_owned_row(conn, "ruta", key, "rutas", ("nombre", "nombre_clave", "altitud_maxima", "tipo_terreno", "dificultad_tecnica", "dificultad_fisica", "activo"), vals, "SELECT id FROM rutas WHERE nombre=? COLLATE NOCASE OR nombre_clave=?", (name, name.casefold()))
        note(result)
        # The collision query accepts two parameters.
        if rid is not None: routes[key] = (rid, vals)
    sexos = dict(conn.execute("SELECT nombre,id FROM sexos"))
    clients: dict[int, tuple[int, tuple]] = {}
    for i, (dni, name, surname, sex) in enumerate(CLIENTS):
        vals = (dni, name, surname, "1990-01-01", f"{dni}@demo.invalid", f"+54 351 555-{i:04d}", sexos[sex], sex, 1)
        cid, result = upsert_owned_row(conn, "cliente", f"demo-cliente-{dni}", "clientes", ("dni", "nombre", "apellido", "fecha_nacimiento", "email", "telefono", "sexo_id", "sexo", "activo"), vals, "SELECT id FROM clientes WHERE dni=?", dni)
        note(result)
        if cid is not None: clients[i] = (cid, vals)
    gear: dict[str, tuple[int, tuple]] = {}
    for key, name, category, amount, state, active in GEAR:
        vals = (name, category, amount, state, active)
        eid, result = upsert_owned_row(conn, "inventario", key, "equipamiento", ("nombre", "categoria", "cantidad", "estado", "activo"), vals, "SELECT id FROM equipamiento WHERE nombre=? COLLATE NOCASE", name)
        note(result)
        if eid is not None: gear[key] = (eid, vals)
    route_checkpoints: dict[str, list[tuple]] = {}
    for route_key, points in POINTS.items():
        if route_key not in routes: continue
        rid, _ = routes[route_key]
        copied = []
        for order, point in enumerate(points):
            key = f"{route_key}-checkpoint-{order}"
            name, hour, lat, lon, altitude, description = point
            associated = False
            row = conn.execute(f"SELECT id1 FROM {MANIFEST} WHERE entidad='checkpoint' AND clave=?", (key,)).fetchone()
            if row:
                pid = row[0]
                conn.execute("UPDATE checkpoints SET nombre=?,hora=?,latitud=?,longitud=?,altitud=?,descripcion=? WHERE id=?", (*point, pid))
                note("actualizados")
            else:
                cur = conn.execute("INSERT INTO checkpoints(nombre,hora,latitud,longitud,altitud,descripcion) VALUES(?,?,?,?,?,?)", point)
                pid = cur.lastrowid
                remember(conn, "checkpoint", key, pid); note("insertados")
            relation_key = f"{route_key}|checkpoint|{order}"
            relation = conn.execute(f"SELECT id1,id2 FROM {MANIFEST} WHERE entidad='ruta_checkpoint' AND clave=?", (relation_key,)).fetchone()
            if relation:
                try:
                    cursor = conn.execute("UPDATE ruta_checkpoints SET orden=? WHERE ruta_id=? AND checkpoint_id=?", (order, relation[0], relation[1]))
                    if cursor.rowcount:
                        note("actualizados")
                        associated = True
                    else:
                        conn.execute(f"DELETE FROM {MANIFEST} WHERE entidad='ruta_checkpoint' AND clave=?", (relation_key,))
                        relation = None
                except sqlite3.IntegrityError as exc:
                    counts["omitidos"] += 1
                    print(f"Conflicto omitido: Itinerario {relation_key}: {exc}", file=sys.stderr)
            if not relation:
                occupied = conn.execute("SELECT 1 FROM ruta_checkpoints WHERE ruta_id=? AND orden=? OR checkpoint_id=?", (rid, order, pid)).fetchone()
                if occupied:
                    counts["omitidos"] += 1
                    print(f"Conflicto omitido: Itinerario {relation_key} ya tiene una asociación ajena", file=sys.stderr)
                else:
                    conn.execute("INSERT INTO ruta_checkpoints(ruta_id,checkpoint_id,orden) VALUES(?,?,?)", (rid, pid, order))
                    remember(conn, "ruta_checkpoint", relation_key, rid, pid)
                    note("insertados")
                    associated = True
            if associated:
                copied.append(point)
        route_checkpoints[route_key] = copied
    # Each demo Salida owns its operational snapshot and child rows.
    for index, (key, route_index, offset, state, participant_indices, assignments) in enumerate(TRIPS):
        route_key = ROUTES[route_index][0]
        if route_key not in routes or any(i not in clients for i in participant_indices) or any(k not in gear for k, _ in assignments):
            counts["omitidos"] += 1
            print(f"Dependencias omitidas para Salida {key}", file=sys.stderr)
            continue
        rid, route = routes[route_key]
        trip_values = (rid, (today + dt.timedelta(days=offset)).isoformat(), state, *route[:1], *route[2:6])
        # Route snapshot columns deliberately match the catalog values.
        sid = own(conn, "salida", key)
        owned_children: dict[str, set[str]] = {}
        if sid:
            conn.execute("UPDATE salidas SET ruta_id=?,fecha=?,estado=?,ruta_nombre=?,ruta_altitud_maxima=?,ruta_tipo_terreno=?,ruta_dificultad_tecnica=?,ruta_dificultad_fisica=? WHERE id=?", (*trip_values, sid)); note("actualizados")
            child_tables = {
                "salida_checkpoint": ("salida_checkpoints", "id"),
                "salida_equipamiento": ("salida_equipamiento", "equipamiento_id"),
                "salida_participante": ("salida_participantes", "cliente_id"),
            }
            for entity, (table, relation_id) in child_tables.items():
                rows = conn.execute(f"SELECT clave,id1,id2 FROM {MANIFEST} WHERE entidad=? AND substr(clave,1,?)=?", (entity, len(key) + 1, key + "|")).fetchall()
                owned_children[entity] = {row[0] for row in rows}
                for child_key, id1, id2 in rows:
                    if entity == "salida_checkpoint":
                        conn.execute("DELETE FROM salida_checkpoints WHERE id=?", (id1,))
                    else:
                        conn.execute(f"DELETE FROM {table} WHERE salida_id=? AND {relation_id}=?", (id1, id2))
                    conn.execute(f"DELETE FROM {MANIFEST} WHERE entidad=? AND clave=?", (entity, child_key))
        else:
            cur = conn.execute("INSERT INTO salidas(ruta_id,fecha,estado,ruta_nombre,ruta_altitud_maxima,ruta_tipo_terreno,ruta_dificultad_tecnica,ruta_dificultad_fisica) VALUES(?,?,?,?,?,?,?,?)", trip_values)
            sid = cur.lastrowid; remember(conn, "salida", key, sid); note("insertados")
        for order, point in enumerate(route_checkpoints.get(route_key, [])):
            child_key = f"{key}|checkpoint|{order}"
            try:
                cur = conn.execute("INSERT INTO salida_checkpoints(salida_id,nombre,hora,latitud,longitud,altitud,descripcion,orden) VALUES(?,?,?,?,?,?,?,?)", (sid, *point, order))
                remember(conn, "salida_checkpoint", child_key, cur.lastrowid)
                note("actualizados" if child_key in owned_children.get("salida_checkpoint", set()) else "insertados")
            except sqlite3.IntegrityError as exc:
                counts["omitidos"] += 1
                print(f"Conflicto omitido: copia de Checkpoint {child_key}: {exc}", file=sys.stderr)
        for gkey, amount in assignments:
            eid, item = gear[gkey]
            child_key = f"{key}|equipo|{gkey}"
            try:
                conn.execute("INSERT INTO salida_equipamiento(salida_id,equipamiento_id,nombre_copia,categoria_copia,cantidad) VALUES(?,?,?,?,?)", (sid, eid, item[0], item[1], amount))
                remember(conn, "salida_equipamiento", child_key, sid, eid)
                note("actualizados" if child_key in owned_children.get("salida_equipamiento", set()) else "insertados")
            except sqlite3.IntegrityError as exc:
                counts["omitidos"] += 1
                print(f"Conflicto omitido: Equipamiento de Salida {child_key}: {exc}", file=sys.stderr)
        for client_index in participant_indices:
            cid, client = clients[client_index]
            child_key = f"{key}|cliente|{client[0]}"
            try:
                conn.execute("INSERT INTO salida_participantes(salida_id,cliente_id,dni_copia,nombre_copia,apellido_copia,telefono_copia) VALUES(?,?,?,?,?,?)", (sid, cid, client[0], client[1], client[2], client[5]))
                remember(conn, "salida_participante", child_key, sid, cid)
                note("actualizados" if child_key in owned_children.get("salida_participante", set()) else "insertados")
            except sqlite3.IntegrityError as exc:
                counts["omitidos"] += 1
                print(f"Conflicto omitido: Participante de Salida {child_key}: {exc}", file=sys.stderr)
    return counts


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Prepara datos demo de Trekking App.")
    parser.add_argument("--db", type=Path, default=DEFAULT_DB, help="Base SQLite (por defecto trekking.db en la raíz).")
    args = parser.parse_args(argv)
    db = args.db.expanduser().resolve()
    db.parent.mkdir(parents=True, exist_ok=True)
    try:
        initialize_schema(db)
        with sqlite3.connect(db) as conn:
            conn.execute("PRAGMA foreign_keys=ON")
            result = seed(conn)
            conn.commit()
            totals = {table: conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] for table in ("rutas", "clientes", "equipamiento", "salidas")}
        print("Demo lista. " + ", ".join(f"{k}: {v}" for k, v in result.items()))
        print("Totales de tabla (incluyen registros ajenos): " + ", ".join(f"{k}: {v}" for k, v in totals.items()))
        return 0
    except (OSError, sqlite3.Error, subprocess.CalledProcessError) as exc:
        print(f"No se pudo cargar la base demo: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

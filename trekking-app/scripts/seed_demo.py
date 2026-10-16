#!/usr/bin/env python3
"""Seed demo para la app de trekking (SQLite, solo stdlib).

Puebla Rutas, Checkpoints, Equipamiento, Clientes y Recorridos con un
dataset fijo y determinista, apto para demo sin carga en vivo.

Propiedades:
- Solo borra/reemplaza filas creadas por este script (manifiesto
  `__seed_demo`). Los datos del usuario quedan intactos.
- Re-ejecutable: cada corrida elimina el demo anterior y lo reinserta.
- Respeta las validaciones de la app (enums, formatos, FKs, UNIQUEs).

Uso:
    python3 scripts/seed_demo.py [--db trekking.db] [--reset-demo] [--clean]
"""
from __future__ import annotations

import argparse
import datetime as _dt
import sqlite3
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_DB = SCRIPT_DIR.parent / "trekking.db"

SEXOS = ["Masculino", "Femenino", "No binario", "Otro", "Prefiero no decir"]

RUTAS = [
    # (nombre, altitud_maxima, tipo_terreno, dif_tecnica, dif_fisica, activo)
    ("Cerro Champaqui Norte", 2790.0, "Mixto", "Alta", "Alta", 1),
    ("Quebrada Del Condorito", 2100.0, "Rocoso", "Media", "Media", 1),
    ("Sendero Del Bosque Serrano", 1450.0, "Boscoso", "Baja", "Baja", 1),
    ("Cumbre Del Cerro Uritorco", 1979.0, "Mixto", "Media", "Alta", 1),
    ("Travesia Pampa De Achala", 2300.0, "Sendero", "Baja", "Media", 1),
    ("Filo Del Cerro Los Gigantes", 2370.0, "Rocoso", "Alta", "Alta", 1),
    ("Sendero Del Bosque Antiguo", 1200.0, "Boscoso", "Baja", "Baja", 0),
    ("Camino De Las Altas Cumbres Sur", 1850.0, "Sendero", "Media", "Media", 0),
]

CHECKPOINTS = [
    # (nombre, hora HH:mm, lat, lon, descripcion)
    ("Base La Cumbrecita", "08:00", -31.8950, -64.7780, "Registro de ingreso y control de equipo."),
    ("Mirador Del Valle", "11:30", -31.9010, -64.7900, "Vista panoramica del valle de Calamuchita."),
    ("Cumbre Norte", "15:00", -31.9100, -64.8020, "Punto mas alto de la ruta, foto grupal."),
    ("Puesto La Pampa", "07:30", -31.6200, -64.7600, "Puesto de guardaparques, brief de seguridad."),
    ("Balcon Del Condor", "10:45", -31.6300, -64.7750, "Avistaje de condores, parada tecnica."),
    ("Quebrada Honda", "14:15", -31.6450, -64.7900, "Cruce de arroyo, recarga de agua."),
    ("Entrada Del Bosque", "09:00", -31.4500, -64.5500, "Inicio del sendero interpretativo."),
    ("Claro Del Arroyo", "12:00", -31.4580, -64.5620, "Zona de almuerzo junto al arroyo."),
    ("Base Capilla", "08:30", -30.8500, -64.4800, "Chequeo de inscriptos y ficha medica."),
    ("Refugio El Paso", "11:00", -30.8600, -64.4900, "Refugio intermedio, descanso de 30 minutos."),
    ("Cumbre Uritorco", "14:00", -30.8710, -64.4970, "Cumbre, control de horario de descenso."),
    ("Estancia Vieja", "07:00", -31.7000, -64.8500, "Estancia, punto de encuentro de grupos."),
    ("Pampa Alta", "13:30", -31.7200, -64.8700, "Planicie de altura, control de viento."),
    ("Playa De Rio", "08:15", -31.3300, -64.6100, "Vado del rio, evaluacion de caudal."),
    ("Filo Central", "12:30", -31.3450, -64.6250, "Filo expuesto, uso de casco obligatorio."),
    ("Cumbre Gigantes", "16:00", -31.3550, -64.6380, "Cumbre, registro de egreso."),
    ("Porton Sur", "09:30", -31.5000, -64.5000, "Acceso sur, ruta actualmente inactiva."),
    ("Mirador Antiguo", "11:45", -31.5080, -64.5120, "Mirador en desuso, solo referencia historica."),
    ("Cruce Del Camino", "08:00", -31.6000, -64.6800, "Cruce de senderos, cartelera informativa."),
    ("Puesto Sur", "12:15", -31.6150, -64.6950, "Puesto sanitario del camino sur."),
]

# Indices (0-based) de CHECKPOINTS por ruta (0-based). Idx 0 y 1 se reusan
# en dos rutas para mostrar checkpoints compartidos.
RUTA_CHECKPOINTS = {
    0: [0, 1, 2],
    1: [3, 4, 5],
    2: [6, 7],
    3: [8, 9, 10, 1],   # comparte "Mirador Del Valle"
    4: [11, 12, 0],     # comparte "Base La Cumbrecita"
    5: [13, 14, 15],
    6: [16, 17],
    7: [18, 19],
}

EQUIPOS = [
    # (nombre, categoria, cantidad, estado, activo)
    ("Carpa 4 Personas", "Campamento", 12, "Disponible", 1),
    ("Bolsa De Dormir Andina", "Campamento", 30, "Disponible", 1),
    ("Aislante Termico", "Campamento", 30, "Disponible", 1),
    ("Cocina Portatil", "Campamento", 8, "Asignado", 1),
    ("GPS Montana", "Navegación", 6, "Disponible", 1),
    ("Mapa Topografico Edicion 2015", "Navegación", 4, "Fuera de Servicio", 0),
    ("Brujula Clasica", "Navegación", 15, "Disponible", 1),
    ("Radio VHF", "Comunicación", 10, "Disponible", 1),
    ("Telefono Satelital", "Comunicación", 2, "En Inspección", 1),
    ("Casco Escalada", "Seguridad", 20, "Disponible", 1),
    ("Arnes Regulable", "Seguridad", 20, "Disponible", 1),
    ("Cuerda Dinamica 60m", "Seguridad", 6, "En Inspección", 1),
    ("Set Mosquetones", "Escalada", 25, "Disponible", 1),
    ("Descensor", "Escalada", 10, "Disponible", 1),
    ("Piolet Tecnico", "Escalada", 12, "En Reparación", 1),
    ("Campera Impermeable", "Vestimenta", 28, "Disponible", 1),
    ("Pantalon Trekking", "Vestimenta", 28, "Disponible", 1),
    ("Mochila Hidratacion", "Hidratación", 18, "Disponible", 1),
    ("Filtro De Agua", "Hidratación", 5, "Disponible", 1),
    ("Botiquin Completo", "Primeros Auxilios", 10, "Disponible", 1),
    ("Ferula Rigida", "Primeros Auxilios", 6, "Disponible", 1),
    ("Navaja Multiuso", "Herramientas", 14, "Disponible", 1),
    ("Linterna Frontal Antigua", "Herramientas", 10, "Extraviado", 0),
    ("Mochila 65L", "Transporte", 22, "Disponible", 1),
    ("Bastones Trekking", "Transporte", 26, "Asignado", 1),
]

# (indices 0-based de EQUIPOS, cantidad_requerida) por ruta (0-based).
RUTA_EQUIPOS = {
    0: [(0, 4), (1, 12), (4, 2), (9, 12), (15, 12), (23, 12)],
    1: [(4, 2), (6, 8), (7, 4), (18, 2), (19, 2)],
    2: [(1, 8), (2, 8), (17, 8), (21, 4)],
    3: [(0, 3), (4, 2), (9, 10), (10, 10), (21, 2)],
    4: [(3, 2), (7, 3), (17, 10), (19, 2), (23, 10), (24, 10)],
    5: [(9, 8), (10, 8), (11, 3), (12, 8), (13, 4)],
    6: [(1, 4), (21, 2), (23, 4)],
    7: [(4, 1), (7, 2), (19, 1), (23, 6)],
}

# (nombre, apellido, anio_nac, mes, dia o None=dinamico-menor,
#  email, telefono, sexo, contacto o None, activo)
# contacto = (nombre, telefono, relacion)
CLIENTES = [
    ("Lucia", "Fernandez", 1990, 3, 12, "lucia.fernandez@mail.com", "+54 351 5550101", "Femenino", ("Mario Fernandez", "+54 351 5550102", "Padre"), 1),
    ("Martin", "Gomez", 1985, 7, 25, "martin.gomez@mail.com", "+54 351 5550103", "Masculino", ("Ana Gomez", "+54 351 5550104", "Esposa"), 1),
    ("Sofia", "Lopez", 1995, 11, 5, "sofia.lopez@mail.com", "+54 351 5550105", "Femenino", ("Pedro Lopez", "+54 351 5550106", "Hermano"), 1),
    ("Javier", "Ruiz", 1978, 1, 30, "javier.ruiz@mail.com", "+54 351 5550107", "Masculino", ("Laura Ruiz", "+54 351 5550108", "Esposa"), 1),
    ("Camila", "Torres", 2000, 5, 19, "camila.torres@mail.com", "+54 351 5550109", "Femenino", None, 1),
    ("Diego", "Morales", 1992, 9, 8, "diego.morales@mail.com", "+54 351 5550110", "Masculino", ("Sofia Morales", "+54 351 5550111", "Hermana"), 1),
    ("Valentina", "Rossi", 1988, 12, 22, "valen.rossi@mail.com", "+54 351 5550112", "Femenino", ("Marco Rossi", "+54 351 5550113", "Pareja"), 1),
    ("Tomas", "Herrera", 1998, 4, 2, "tomas.herrera@mail.com", "+54 351 5550114", "Masculino", None, 1),
    ("Agustina", "Silva", 1993, 6, 14, "agus.silva@mail.com", "+54 351 5550115", "Femenino", ("Rosa Silva", "+54 351 5550116", "Madre"), 1),
    ("Nicolas", "Vargas", 1980, 10, 9, "nico.vargas@mail.com", "+54 351 5550117", "Masculino", ("Elena Vargas", "+54 351 5550118", "Esposa"), 1),
    ("Florencia", "Castro", 1997, 2, 27, "flor.castro@mail.com", "+54 351 5550119", "No binario", ("Alex Castro", "+54 351 5550120", "Hermane"), 1),
    ("Santiago", "Mendoza", 1991, 8, 16, "santi.mendoza@mail.com", "+54 351 5550121", "Masculino", None, 1),
    ("Martina", "Aguilar", 2002, 3, 3, "marti.aguilar@mail.com", "+54 351 5550122", "Femenino", ("Jorge Aguilar", "+54 351 5550123", "Padre"), 1),
    ("Bruno", "Paredes", 1975, 5, 11, "bruno.paredes@mail.com", "+54 351 5550124", "Masculino", ("Nora Paredes", "+54 351 5550125", "Esposa"), 1),
    ("Julieta", "Ramos", 1996, 9, 29, "juli.ramos@mail.com", "+54 351 5550126", "Femenino", None, 1),
    ("Facundo", "Ortega", 1983, 12, 1, "facu.ortega@mail.com", "+54 351 5550127", "Masculino", ("Luz Ortega", "+54 351 5550128", "Pareja"), 1),
    ("Micaela", "Sosa", 1999, 7, 7, "mica.sosa@mail.com", "+54 351 5550129", "Femenino", ("Hugo Sosa", "+54 351 5550130", "Padre"), 1),
    ("Ignacio", "Navarro", 1970, 4, 18, "nacho.navarro@mail.com", "+54 351 5550131", "Masculino", ("Mirta Navarro", "+54 351 5550132", "Esposa"), 1),
    ("Paula", "Rios", 2001, 1, 23, "paula.rios@mail.com", "+54 351 5550133", "Femenino", None, 1),
    ("Emiliano", "Diaz", 1994, 10, 31, "emi.diaz@mail.com", "+54 351 5550134", "Otro", ("Sol Diaz", "+54 351 5550135", "Hermana"), 1),
    ("Carolina", "Pereyra", 1987, 6, 6, "caro.pereyra@mail.com", "+54 351 5550136", "Femenino", ("Luis Pereyra", "+54 351 5550137", "Esposo"), 1),
    ("Gonzalo", "Farias", 1982, 2, 13, "gonza.farias@mail.com", "+54 351 5550138", "Masculino", None, 1),
    ("Daniela", "Quintero", 1990, 11, 17, "dani.quintero@mail.com", "+54 351 5550139", "Prefiero no decir", ("Marta Quintero", "+54 351 5550140", "Madre"), 1),
    ("Ramiro", "Bustos", 2003, 8, 21, "rami.bustos@mail.com", "+54 351 5550141", "Masculino", ("Carlos Bustos", "+54 351 5550142", "Padre"), 1),
    ("Antonella", "Luna", 1989, 5, 28, "anto.luna@mail.com", "+54 351 5550143", "Femenino", None, 1),
    ("Matias", "Godoy", 1977, 9, 4, "mati.godoy@mail.com", "+54 351 5550144", "Masculino", ("Sara Godoy", "+54 351 5550145", "Esposa"), 0),
    ("Victoria", "Campos", 1995, 12, 10, "vicky.campos@mail.com", "+54 351 5550146", "Femenino", ("Raul Campos", "+54 351 5550147", "Padre"), 0),
    ("Benicio", "Alvarez", None, None, None, "beni.alvarez@mail.com", "+54 351 5550148", "Masculino", ("Carolina Alvarez", "+54 351 5550149", "Madre"), 1),
    ("Emma", "Benitez", None, None, None, "emma.benitez@mail.com", "+54 351 5550150", "Femenino", ("Pablo Benitez", "+54 351 5550151", "Padre"), 1),
    ("Dylan", "Correa", None, None, None, "dylan.correa@mail.com", "+54 351 5550152", "Masculino", ("Luciana Correa", "+54 351 5550153", "Madre"), 0),
]

# DNIs fijos de 8 digitos para los 27 mayores; los 3 menores se generan
# dinamicos de 7 digitos para garantizar DNI valido sin colision.
DNIS_FIJOS = [
    30123456, 29548123, 33120987, 27456321, 35987654, 31234567, 28901234,
    34567890, 32765432, 26123456, 35123987, 30876543, 38901234, 25678901,
    33456789, 27987654, 34234567, 24890123, 36789012, 31567890, 28345678,
    27123987, 30678901, 37567890, 29988776, 24556789, 33889900,
]
DNI_BASE_MENORES = [7012345, 7123456, 7234567]

TUTORES_MENORES = [
    ("Carolina", "Alvarez", 27456123, "+54 351 5550201"),
    ("Pablo", "Benitez", 26987654, "+54 351 5550202"),
    ("Luciana", "Correa", 28123456, "+54 351 5550203"),
]

# (ruta_idx, dia_offset, hora_inicio o None, hora_fin o None, [cliente_idx...])
RECORRIDOS = [
    (0, -60, "08:00", "16:00", [0, 1, 2, 3, 5, 7, 9, 11]),
    (1, -45, "07:30", "15:30", [4, 6, 8, 10, 12, 14]),
    (2, -30, "09:00", "13:00", [0, 4, 12, 16, 20]),
    (3, -21, "08:30", "17:00", [1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23]),
    (0, -14, "08:00", "16:30", [2, 6, 10, 14, 18, 22]),
    (4, -7, "07:00", "14:00", [0, 1, 5, 9, 13, 25]),
    (5, -3, "08:15", "16:45", [3, 7, 11, 15, 19, 27, 28]),
    (2, -1, "09:00", None, [4, 8, 12, 16]),
    (3, 2, "08:30", "17:00", [0, 2, 4, 6, 8, 10, 12, 14, 16, 18]),
    (4, 7, "07:00", "14:00", [1, 5, 9, 13, 17, 21, 24]),
    (6, 15, "09:30", "12:30", []),
    (1, 30, "07:30", "15:30", [6, 10, 14, 18, 22, 26]),
]


def ensure_schema(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS rutas (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL COLLATE NOCASE UNIQUE,
            altitud_maxima REAL NOT NULL,
            tipo_terreno TEXT NOT NULL,
            dificultad_tecnica TEXT NOT NULL,
            dificultad_fisica TEXT NOT NULL,
            activo INTEGER NOT NULL DEFAULT 1
        );
        CREATE TABLE IF NOT EXISTS checkpoints (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            hora TEXT NOT NULL,
            latitud REAL NOT NULL,
            longitud REAL NOT NULL,
            descripcion TEXT
        );
        CREATE TABLE IF NOT EXISTS ruta_checkpoints (
            ruta_id INTEGER NOT NULL,
            checkpoint_id INTEGER NOT NULL,
            orden INTEGER NOT NULL,
            PRIMARY KEY (ruta_id, checkpoint_id),
            UNIQUE (ruta_id, orden),
            FOREIGN KEY (ruta_id) REFERENCES rutas(id),
            FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
        );
        CREATE TABLE IF NOT EXISTS equipamiento (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            categoria TEXT NOT NULL,
            cantidad INTEGER NOT NULL,
            estado TEXT NOT NULL,
            activo INTEGER NOT NULL DEFAULT 1
        );
        CREATE TABLE IF NOT EXISTS rutas_equipamiento (
            rutas_equipamiento_id INTEGER PRIMARY KEY AUTOINCREMENT,
            ruta_id INTEGER NOT NULL,
            equipamiento_id INTEGER NOT NULL,
            cantidad_requerida INTEGER NOT NULL,
            FOREIGN KEY (ruta_id) REFERENCES rutas(id),
            FOREIGN KEY (equipamiento_id) REFERENCES equipamiento(id)
        );
        CREATE TABLE IF NOT EXISTS clientes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            dni TEXT UNIQUE,
            nombre TEXT NOT NULL,
            apellido TEXT NOT NULL,
            fecha_nacimiento TEXT,
            email TEXT,
            telefono TEXT,
            sexo_id INTEGER,
            sexo TEXT NOT NULL,
            contacto_emergencia_nombre TEXT,
            contacto_emergencia_telefono TEXT,
            contacto_emergencia_relacion TEXT,
            autorizacion_menores INTEGER NOT NULL DEFAULT 0,
            tutor_nombre TEXT,
            tutor_apellido TEXT,
            tutor_dni TEXT,
            tutor_telefono TEXT,
            activo INTEGER NOT NULL DEFAULT 1,
            ficha_medica BLOB,
            nombre_ficha_medica TEXT,
            fecha_alta TEXT NOT NULL DEFAULT CURRENT_DATE,
            fecha_modificacion TEXT NOT NULL DEFAULT CURRENT_DATE
        );
        CREATE TABLE IF NOT EXISTS sexos (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL UNIQUE
        );
        CREATE TABLE IF NOT EXISTS contactos_emergencia (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            cliente_id INTEGER NOT NULL UNIQUE,
            nombre TEXT NOT NULL,
            telefono TEXT NOT NULL,
            relacion TEXT NOT NULL,
            FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        );
        CREATE TABLE IF NOT EXISTS recorridos (
            recorrido_id INTEGER PRIMARY KEY AUTOINCREMENT,
            ruta_id INTEGER NOT NULL,
            fecha DATE NOT NULL,
            hora_inicio TIME,
            hora_fin TIME,
            FOREIGN KEY (ruta_id) REFERENCES rutas(id)
        );
        CREATE TABLE IF NOT EXISTS recorrido_cliente (
            recorrido_cliente_id INTEGER PRIMARY KEY AUTOINCREMENT,
            recorrido_id INTEGER NOT NULL,
            cliente_id INTEGER NOT NULL,
            FOREIGN KEY (recorrido_id) REFERENCES recorridos(recorrido_id),
            FOREIGN KEY (cliente_id) REFERENCES clientes(id)
        );
        CREATE UNIQUE INDEX IF NOT EXISTS idx_recorrido_cliente_unico
            ON recorrido_cliente (recorrido_id, cliente_id);
        CREATE TABLE IF NOT EXISTS __seed_demo (
            entidad TEXT NOT NULL,
            id1 INTEGER NOT NULL,
            id2 INTEGER,
            PRIMARY KEY (entidad, id1, id2)
        );
        INSERT OR IGNORE INTO sexos (nombre)
            VALUES ('Masculino'), ('Femenino'), ('No binario'),
                   ('Otro'), ('Prefiero no decir');
        """
    )


def manifest_ids(conn: sqlite3.Connection, entidad: str) -> list[tuple]:
    rows = conn.execute(
        "SELECT id1, id2 FROM __seed_demo WHERE entidad = ?", (entidad,)
    ).fetchall()
    return [(r[0], r[1]) for r in rows]


def clear_demo(conn: sqlite3.Connection) -> dict[str, int]:
    """Borra solo filas registradas en __seed_demo. Devuelve conteo."""
    borrados: dict[str, int] = {}
    # Orden hijo -> padre.
    for entidad, sql in [
        ("recorrido_cliente",
         "DELETE FROM recorrido_cliente WHERE recorrido_cliente_id = ?"),
        ("contactos_emergencia",
         "DELETE FROM contactos_emergencia WHERE id = ?"),
        ("ruta_checkpoints",
         "DELETE FROM ruta_checkpoints WHERE ruta_id = ? AND checkpoint_id = ?"),
        ("rutas_equipamiento",
         "DELETE FROM rutas_equipamiento WHERE rutas_equipamiento_id = ?"),
        ("recorridos", "DELETE FROM recorridos WHERE recorrido_id = ?"),
        ("clientes", "DELETE FROM clientes WHERE id = ?"),
        ("rutas", "DELETE FROM rutas WHERE id = ?"),
        ("checkpoints", "DELETE FROM checkpoints WHERE id = ?"),
        ("equipamiento", "DELETE FROM equipamiento WHERE id = ?"),
    ]:
        n = 0
        for id1, id2 in manifest_ids(conn, entidad):
            if entidad == "ruta_checkpoints":
                cur = conn.execute(sql, (id1, id2))
            else:
                cur = conn.execute(sql, (id1,))
            n += cur.rowcount
        borrados[entidad] = n
    conn.execute("DELETE FROM __seed_demo")
    return borrados


def track(conn: sqlite3.Connection, entidad: str, id1: int, id2=None) -> None:
    conn.execute(
        "INSERT OR IGNORE INTO __seed_demo (entidad, id1, id2) VALUES (?, ?, ?)",
        (entidad, id1, id2),
    )


def existing_dnis(conn: sqlite3.Connection) -> set[str]:
    return {
        r[0]
        for r in conn.execute("SELECT dni FROM clientes WHERE dni IS NOT NULL")
    }


def existing_ruta_names(conn: sqlite3.Connection) -> set[str]:
    return {
        (r[0] or "").lower()
        for r in conn.execute("SELECT nombre FROM rutas")
    }


def free_dni(tomados: set[str], base: int) -> str:
    cand = base
    while str(cand) in tomados:
        cand += 1000
        if cand > 99999999:
            cand = 10000000
    tomados.add(str(cand))
    return str(cand)


def seed(conn: sqlite3.Connection) -> dict[str, int]:
    hoy = _dt.date.today()
    tomados_dni = existing_dnis(conn)
    nombres_ruta = existing_ruta_names(conn)
    sexo_id = {
        r[0]: r[1] for r in conn.execute("SELECT nombre, id FROM sexos")
    }
    resumen: dict[str, int] = {}

    # ---- Rutas ----
    ruta_ids: list[int] = []
    for nombre, alt, terreno, dt, df, activo in RUTAS:
        final = nombre
        suf = 2
        while final.lower() in nombres_ruta:
            final = f"{nombre} Demo {suf}"
            suf += 1
        cur = conn.execute(
            "INSERT INTO rutas (nombre, altitud_maxima, tipo_terreno,"
            " dificultad_tecnica, dificultad_fisica, activo)"
            " VALUES (?, ?, ?, ?, ?, ?)",
            (final, alt, terreno, dt, df, activo),
        )
        nombres_ruta.add(final.lower())
        ruta_ids.append(cur.lastrowid)
        track(conn, "rutas", cur.lastrowid)
    resumen["rutas"] = len(ruta_ids)

    # ---- Checkpoints ----
    cp_ids: list[int] = []
    for nombre, hora, lat, lon, desc in CHECKPOINTS:
        cur = conn.execute(
            "INSERT INTO checkpoints (nombre, hora, latitud, longitud, descripcion)"
            " VALUES (?, ?, ?, ?, ?)",
            (nombre, hora, lat, lon, desc),
        )
        cp_ids.append(cur.lastrowid)
        track(conn, "checkpoints", cur.lastrowid)
    resumen["checkpoints"] = len(cp_ids)

    # ---- Ruta-checkpoints ----
    n_rc = 0
    for ruta_idx, cp_list in RUTA_CHECKPOINTS.items():
        orden = 1
        for cp_idx in cp_list:
            conn.execute(
                "INSERT INTO ruta_checkpoints (ruta_id, checkpoint_id, orden)"
                " VALUES (?, ?, ?)",
                (ruta_ids[ruta_idx], cp_ids[cp_idx], orden),
            )
            track(conn, "ruta_checkpoints",
                  ruta_ids[ruta_idx], cp_ids[cp_idx])
            orden += 1
            n_rc += 1
    resumen["ruta_checkpoints"] = n_rc

    # ---- Equipamiento ----
    eq_ids: list[int] = []
    for nombre, cat, cant, estado, activo in EQUIPOS:
        cur = conn.execute(
            "INSERT INTO equipamiento (nombre, categoria, cantidad, estado, activo)"
            " VALUES (?, ?, ?, ?, ?)",
            (nombre, cat, cant, estado, activo),
        )
        eq_ids.append(cur.lastrowid)
        track(conn, "equipamiento", cur.lastrowid)
    resumen["equipamiento"] = len(eq_ids)

    # ---- Rutas-equipamiento ----
    n_re = 0
    for ruta_idx, items in RUTA_EQUIPOS.items():
        for eq_idx, cant_req in items:
            cur = conn.execute(
                "INSERT INTO rutas_equipamiento"
                " (ruta_id, equipamiento_id, cantidad_requerida)"
                " VALUES (?, ?, ?)",
                (ruta_ids[ruta_idx], eq_ids[eq_idx], cant_req),
            )
            track(conn, "rutas_equipamiento", cur.lastrowid)
            n_re += 1
    resumen["rutas_equipamiento"] = n_re

    # ---- Clientes ----
    cli_ids: list[int] = []
    for i, (nom, ape, anio, mes, dia, email, tel, sexo,
            contacto, activo) in enumerate(CLIENTES):
        if anio is None:  # menor dinamico: 10, 12 y 14 anios
            edad = (10, 12, 14)[i - 27]
            fnac = (hoy - _dt.timedelta(days=edad * 365)).isoformat()
            dni = free_dni(tomados_dni, DNI_BASE_MENORES[i - 27])
            t_nom, t_ape, t_dni, t_tel = TUTORES_MENORES[i - 27]
            cur = conn.execute(
                """INSERT INTO clientes (dni, nombre, apellido,
                       fecha_nacimiento, email, telefono, sexo_id, sexo,
                       contacto_emergencia_nombre, contacto_emergencia_telefono,
                       contacto_emergencia_relacion, autorizacion_menores,
                       tutor_nombre, tutor_apellido, tutor_dni, tutor_telefono,
                       activo)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?)""",
                (dni, nom, ape, fnac, email, tel,
                 sexo_id.get(sexo), sexo,
                 contacto[0], contacto[1], contacto[2],
                 t_nom, t_ape, str(t_dni), t_tel, activo),
            )
        else:
            fnac = _dt.date(anio, mes, dia).isoformat()
            dni = free_dni(tomados_dni, DNIS_FIJOS[i])
            if contacto is None:
                cur = conn.execute(
                    """INSERT INTO clientes (dni, nombre, apellido,
                           fecha_nacimiento, email, telefono, sexo_id, sexo,
                           activo)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    (dni, nom, ape, fnac, email, tel,
                     sexo_id.get(sexo), sexo, activo),
                )
            else:
                cur = conn.execute(
                    """INSERT INTO clientes (dni, nombre, apellido,
                           fecha_nacimiento, email, telefono, sexo_id, sexo,
                           contacto_emergencia_nombre,
                           contacto_emergencia_telefono,
                           contacto_emergencia_relacion, activo)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    (dni, nom, ape, fnac, email, tel,
                     sexo_id.get(sexo), sexo,
                     contacto[0], contacto[1], contacto[2], activo),
                )
        cli_ids.append(cur.lastrowid)
        track(conn, "clientes", cur.lastrowid)
        if contacto is not None:
            cur2 = conn.execute(
                "INSERT INTO contactos_emergencia"
                " (cliente_id, nombre, telefono, relacion)"
                " VALUES (?, ?, ?, ?)",
                (cur.lastrowid, contacto[0], contacto[1], contacto[2]),
            )
            track(conn, "contactos_emergencia", cur2.lastrowid)
    resumen["clientes"] = len(cli_ids)

    # ---- Recorridos + participantes ----
    n_rec, n_part = 0, 0
    for ruta_idx, offset, hi, hf, parts in RECORRIDOS:
        fecha = (hoy + _dt.timedelta(days=offset)).isoformat()
        cur = conn.execute(
            "INSERT INTO recorridos (ruta_id, fecha, hora_inicio, hora_fin)"
            " VALUES (?, ?, ?, ?)",
            (ruta_ids[ruta_idx], fecha, hi, hf),
        )
        rec_id = cur.lastrowid
        track(conn, "recorridos", rec_id)
        n_rec += 1
        for c_idx in parts:
            cur2 = conn.execute(
                "INSERT OR IGNORE INTO recorrido_cliente"
                " (recorrido_id, cliente_id) VALUES (?, ?)",
                (rec_id, cli_ids[c_idx]),
            )
            if cur2.rowcount:
                track(conn, "recorrido_cliente", cur2.lastrowid)
                n_part += 1
    resumen["recorridos"] = n_rec
    resumen["recorrido_cliente"] = n_part
    return resumen


def verify(conn: sqlite3.Connection) -> dict[str, int]:
    tablas = ["rutas", "checkpoints", "ruta_checkpoints", "equipamiento",
              "rutas_equipamiento", "clientes", "contactos_emergencia",
              "recorridos", "recorrido_cliente"]
    return {
        t: conn.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0]
        for t in tablas
    }


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Puebla la DB con datos demo.")
    ap.add_argument("--db", type=Path, default=DEFAULT_DB,
                    help="Ruta al sqlite (default: trekking.db del proyecto).")
    ap.add_argument("--reset-demo", action="store_true",
                    help="Reemplaza el demo anterior (comportamiento default).")
    ap.add_argument("--clean", action="store_true",
                    help="Elimina solo el demo y sale sin poblar.")
    args = ap.parse_args(argv)

    if not args.db.exists():
        print(f"DB no existe, se creara: {args.db}", file=sys.stderr)
    conn = sqlite3.connect(args.db)
    try:
        conn.execute("PRAGMA foreign_keys = OFF")
        ensure_schema(conn)
        with conn:
            borrados = clear_demo(conn)
        if args.clean:
            conn.commit()
            print("Demo eliminado:", borrados)
            print("Totales:", verify(conn))
            return 0
        with conn:
            resumen = seed(conn)
        conn.commit()
        print("Demo anterior eliminado:", borrados)
        print("Demo insertado:", resumen)
        print("Totales en DB:", verify(conn))
        return 0
    except sqlite3.Error as exc:
        print(f"ERROR seed: {exc}", file=sys.stderr)
        return 1
    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())

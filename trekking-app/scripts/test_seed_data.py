"""End-to-end checks for the seed command against an isolated SQLite file."""
from pathlib import Path
import sqlite3
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).with_name("seed_data.py")
sys.path.insert(0, str(SCRIPT.parent))
import seed_data  # noqa: E402


class SeedDataCommandTest(unittest.TestCase):
    def test_app_uses_repository_database_from_root_or_module_directory(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            repository = Path(directory)
            module = repository / "trekking-app"
            module.mkdir()
            (module / "pom.xml").touch()

            seed_data.initialize_schema(cwd=repository)
            self.assertTrue((repository / "trekking.db").is_file())
            self.assertFalse((module / "trekking.db").exists())

            seed_data.initialize_schema(cwd=module, compile_app=False)
            self.assertTrue((repository / "trekking.db").is_file())
            self.assertFalse((module / "trekking.db").exists())

    def test_nominal_population(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            db = Path(directory) / "clean.db"
            result = subprocess.run([sys.executable, str(SCRIPT), "--db", str(db)], check=True,
                                    text=True, capture_output=True)
            self.assertNotIn("Conflicto omitido", result.stderr)
            with sqlite3.connect(db) as conn:
                for table, expected in (("rutas", 9), ("clientes", 15), ("equipamiento", 10), ("salidas", 9)):
                    self.assertEqual(conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0], expected)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salidas WHERE estado='planificada'").fetchone()[0], 4)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salidas WHERE estado='realizada'").fetchone()[0], 3)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salidas WHERE estado='cancelada'").fetchone()[0], 2)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salida_participantes").fetchone()[0] > 0, True)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salida_equipamiento").fetchone()[0] > 0, True)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM ruta_checkpoints").fetchone()[0], 23)
                self.assertEqual(conn.execute("PRAGMA foreign_key_check").fetchall(), [])

    def test_conflicts_rerun_dates_and_relations(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            db = Path(directory) / "seed.db"
            seed_data.initialize_schema(db)
            with sqlite3.connect(db) as conn:
                conn.execute("INSERT INTO rutas(nombre,nombre_clave,altitud_maxima,tipo_terreno,dificultad_tecnica,dificultad_fisica) VALUES(?,?,?,?,?,?)",
                             (seed_data.ROUTES[0][1], seed_data.ROUTES[0][1].casefold(), 1, "Rocoso", "Baja", "Baja"))
                conn.execute("INSERT INTO clientes(dni,nombre,apellido,sexo) VALUES('38120451','Registro','Ajeno','Otro')")

            first = subprocess.run([sys.executable, str(SCRIPT), "--db", str(db)], check=True,
                                   text=True, capture_output=True)
            self.assertIn("Conflicto omitido: ruta", first.stderr)
            self.assertIn("Conflicto omitido: cliente", first.stderr)
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM __seed_owned WHERE entidad='ruta'").fetchone()[0], 8)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM __seed_owned WHERE entidad='cliente'").fetchone()[0], 14)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM __seed_owned WHERE entidad='salida'").fetchone()[0], 6)
                self.assertEqual(conn.execute("SELECT ficha_medica FROM clientes WHERE dni='36745918'").fetchone()[0], None)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salida_checkpoints").fetchone()[0], 17)
                self.assertEqual(conn.execute("""SELECT COUNT(*) FROM (
                    SELECT e.id FROM equipamiento e JOIN salida_equipamiento se ON se.equipamiento_id=e.id
                    JOIN salidas s ON s.id=se.salida_id AND s.estado='planificada'
                    GROUP BY e.id HAVING SUM(se.cantidad)>e.cantidad)""").fetchone()[0], 0)
                self.assertEqual(conn.execute("PRAGMA foreign_key_check").fetchall(), [])
                managed_salida = conn.execute("SELECT id1 FROM __seed_owned WHERE entidad='salida' AND clave='salida-2'").fetchone()[0]
                conn.execute("UPDATE salidas SET fecha='2000-01-01' WHERE id=?", (managed_salida,))
                map_id = conn.execute("SELECT id FROM equipamiento WHERE nombre='Cartas topográficas IGN 1:50.000'").fetchone()[0]
                extra_client_id = conn.execute("SELECT id FROM clientes WHERE dni='41562083'").fetchone()[0]
                conn.execute("INSERT INTO salida_equipamiento VALUES(?,?,?,?,?)", (managed_salida, map_id, "Mapa personal", "Navegación", 1))
                conn.execute("INSERT INTO salida_participantes(salida_id,cliente_id,dni_copia,nombre_copia,apellido_copia) VALUES(?,?,?,?,?)", (managed_salida, extra_client_id, "41562083", "Julieta", "Vega"))
                conn.execute("INSERT INTO salida_checkpoints(salida_id,nombre,hora,latitud,longitud,altitud,orden) VALUES(?,?,?,?,?,?,?)", (managed_salida, "Checkpoint agregado por usuario", "18:00", -31.0, -64.0, 100.0, 99))
                managed_route = conn.execute("SELECT id FROM rutas WHERE nombre=?", (seed_data.ROUTES[1][1],)).fetchone()[0]
                custom_checkpoint = conn.execute("INSERT INTO checkpoints(nombre,hora,latitud,longitud,altitud,descripcion) VALUES('Checkpoint propio de ruta','19:00',-31,-64,100,'Usuario')").lastrowid
                conn.execute("INSERT INTO ruta_checkpoints(ruta_id,checkpoint_id,orden) VALUES(?,?,99)", (managed_route, custom_checkpoint))
                before = conn.execute("SELECT COUNT(*) FROM salidas").fetchone()[0]

            second = subprocess.run([sys.executable, str(SCRIPT), "--db", str(db)], check=True,
                                    text=True, capture_output=True)
            self.assertIn("actualizados", second.stdout)
            with sqlite3.connect(db) as conn:
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salidas").fetchone()[0], before)
                self.assertNotEqual(conn.execute("SELECT fecha FROM salidas WHERE id=?", (managed_salida,)).fetchone()[0], "2000-01-01")
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salida_equipamiento WHERE salida_id=? AND equipamiento_id=? AND nombre_copia='Mapa personal'", (managed_salida, map_id)).fetchone()[0], 1)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salida_participantes WHERE salida_id=? AND cliente_id=?", (managed_salida, extra_client_id)).fetchone()[0], 1)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM salida_checkpoints WHERE salida_id=? AND nombre='Checkpoint agregado por usuario'", (managed_salida,)).fetchone()[0], 1)
                self.assertEqual(conn.execute("SELECT COUNT(*) FROM ruta_checkpoints WHERE ruta_id=? AND checkpoint_id=? AND orden=99", (managed_route, custom_checkpoint)).fetchone()[0], 1)
                self.assertEqual(conn.execute("PRAGMA foreign_key_check").fetchall(), [])


if __name__ == "__main__":
    unittest.main()

package com.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import com.example.model.Ruta;

public class ConexionDB {

    private static final String URL = "jdbc:sqlite:trekking.db";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void crearTabla() {
        String sql = """
                            CREATE TABLE IF NOT EXISTS rutas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    altitud_maxima REAL NOT NULL,
                    tipo_terreno TEXT NOT NULL,
                    dificultad_tecnica TEXT NOT NULL,
                    dificultad_fisica TEXT NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1
                )
                            """;
        String sqlCheckpoints = """
                CREATE TABLE IF NOT EXISTS checkpoints (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    hora TEXT NOT NULL,
                    latitud REAL NOT NULL,
                    longitud REAL NOT NULL,
                    descripcion TEXT
                )
                """;
        String sqlRutaCheckpoints = """
                CREATE TABLE IF NOT EXISTS ruta_checkpoints (
                    ruta_id INTEGER NOT NULL,
                    checkpoint_id INTEGER NOT NULL,
                    orden INTEGER NOT NULL,
                    PRIMARY KEY (ruta_id, checkpoint_id),
                    UNIQUE (ruta_id, orden),
                    FOREIGN KEY (ruta_id) REFERENCES rutas(id),
                    FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
                )
                """;
        String sqlEquipamiento = """
                CREATE TABLE IF NOT EXISTS equipamiento (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    categoria TEXT NOT NULL,
                    cantidad INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1
                )
                """;
        String sqlRutasEquipamiento = """
                CREATE TABLE IF NOT EXISTS rutas_equipamiento (
                    rutas_equipamiento_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ruta_id INTEGER NOT NULL,
                    equipamiento_id INTEGER NOT NULL,
                    cantidad_requerida INTEGER NOT NULL,
                    FOREIGN KEY (ruta_id) REFERENCES rutas(id),
                    FOREIGN KEY (equipamiento_id) REFERENCES equipamiento(id)
                )
                """;
        String sqlClientes = """  
            CREATE TABLE IF NOT EXISTS clientes ( id INTEGER PRIMARY KEY AUTOINCREMENT, dni TEXT UNIQUE, nombre TEXT NOT NULL, apellido TEXT NOT NULL, fecha_nacimiento TEXT, email TEXT, telefono TEXT, sexo_id INTEGER, sexo TEXT NOT NULL, contacto_emergencia_nombre TEXT, contacto_emergencia_telefono TEXT, contacto_emergencia_relacion TEXT, autorizacion_menores INTEGER NOT NULL DEFAULT 0, tutor_nombre TEXT, tutor_apellido TEXT, tutor_dni TEXT, tutor_telefono TEXT, activo INTEGER NOT NULL DEFAULT 1, ficha_medica BLOB, nombre_ficha_medica TEXT, fecha_alta TEXT NOT NULL DEFAULT CURRENT_DATE, fecha_modificacion TEXT NOT NULL DEFAULT CURRENT_DATE )
                """;
        String sqlSexos = """ 
            CREATE TABLE IF NOT EXISTS sexos ( id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE ) 
                """;
        String sqlContactosEmergencia = """ 
            CREATE TABLE IF NOT EXISTS contactos_emergencia ( id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id INTEGER NOT NULL UNIQUE, nombre TEXT NOT NULL, telefono TEXT NOT NULL, relacion TEXT NOT NULL, FOREIGN KEY (cliente_id) REFERENCES clientes(id) ) 
                """;
        String sqlRecorridos = """
                CREATE TABLE IF NOT EXISTS recorridos (
                    recorrido_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ruta_id INTEGER NOT NULL,
                    fecha DATE NOT NULL,
                    hora_inicio TIME,
                    hora_fin TIME,
                    FOREIGN KEY (ruta_id) REFERENCES rutas(id)
                )
                """;
        String sqlRecorridoCliente = """
                CREATE TABLE IF NOT EXISTS recorrido_cliente (
                    recorrido_cliente_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recorrido_id INTEGER NOT NULL,
                    cliente_id INTEGER NOT NULL,
                    FOREIGN KEY (recorrido_id) REFERENCES recorridos(recorrido_id),
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
                """;
        try (
                Connection conexion = conectar(); Statement statement = conexion.createStatement()) {
            statement.execute(sql);
            // Tabla de checkpoints
            statement.execute(sqlCheckpoints);
            statement.execute(sqlRutaCheckpoints);
            statement.execute(sqlEquipamiento);
            statement.execute(sqlRutasEquipamiento);
            statement.execute(sqlClientes);
            statement.execute(sqlSexos);
            statement.execute(sqlContactosEmergencia); // Valores iniciales de la tabla sexos 
            statement.execute(sqlRecorridos);
            statement.execute(sqlRecorridoCliente);
            statement.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_recorrido_cliente_unico
                    ON recorrido_cliente (recorrido_id, cliente_id)
                    """);
            statement.executeUpdate(""" 
                INSERT OR IGNORE INTO sexos (nombre) VALUES ('Masculino'), ('Femenino'), ('No binario'), ('Otro'), ('Prefiero no decir') 
                """);
            normalizarDatosExistentes(conexion);

            statement.executeUpdate(
                    "DROP INDEX IF EXISTS idx_rutas_nombre_unique");

            statement.executeUpdate(
                    "CREATE UNIQUE INDEX idx_rutas_nombre_unique "
                    + "ON rutas(nombre COLLATE NOCASE)");

            System.out.println("Tabla 'rutas' lista.");

        } catch (SQLException e) {
            System.out.println(
                    "Error al crear la tabla: "
                    + e.getMessage());
        }
    }

    private static void normalizarDatosExistentes(Connection conexion)
            throws SQLException {

        Set<String> nombresUsados = new HashSet<>();

        String consulta = "SELECT id, nombre FROM rutas ORDER BY id";

        try (
                Statement statement = conexion.createStatement(); ResultSet resultSet = statement.executeQuery(consulta); PreparedStatement actualizar = conexion.prepareStatement(
                "UPDATE rutas SET nombre = ? WHERE id = ?")) {
            while (resultSet.next()) {

                int id = resultSet.getInt("id");

                String nombre = Ruta.normalizarNombre(
                        resultSet.getString("nombre"));

                if (!Ruta.nombreValido(nombre)) {
                    nombre = "Ruta " + id;
                }

                String nombreBase = nombre;
                int sufijo = 2;

                while (!nombresUsados.add(Ruta.claveNombre(nombre))) {
                    nombre = nombreBase + " " + sufijo++;
                }

                actualizar.setString(1, nombre);
                actualizar.setInt(2, id);
                actualizar.executeUpdate();
            }
        }
    }
}

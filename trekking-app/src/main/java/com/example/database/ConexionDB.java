package com.example.database;

import com.example.model.Ruta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

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
                latitud_inicial REAL NOT NULL,
                longitud_inicial REAL NOT NULL,
                altitud_maxima REAL NOT NULL,
                tipo_terreno TEXT NOT NULL,
                dificultad_tecnica TEXT NOT NULL,
                dificultad_fisica TEXT NOT NULL
            )
            """;

        try (
            Connection conexion = conectar();
            Statement statement = conexion.createStatement()
        ) {
            statement.execute(sql);

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
                            + e.getMessage()
            );
        }
    }

    private static void normalizarDatosExistentes(Connection conexion)
            throws SQLException {

        Set<String> nombresUsados = new HashSet<>();

        String consulta = "SELECT id, nombre FROM rutas ORDER BY id";

        try (
            Statement statement = conexion.createStatement();
            ResultSet resultSet = statement.executeQuery(consulta);
            PreparedStatement actualizar = conexion.prepareStatement(
                    "UPDATE rutas SET nombre = ? WHERE id = ?")
        ) {
            while (resultSet.next()) {

                int id = resultSet.getInt("id");

                String nombre = Ruta.normalizarNombre(
                        resultSet.getString("nombre")
                );

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
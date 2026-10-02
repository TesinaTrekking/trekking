package com.example.dao;

import com.example.database.ConexionDB;
import com.example.model.Ruta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RutaDAO {

    public boolean existeNombre(String nombre, int idExcluido) {

        String sql = "SELECT 1 FROM rutas "
                + "WHERE nombre = ? COLLATE NOCASE AND id <> ? LIMIT 1";

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setString(1, nombre);
            statement.setInt(2, idExcluido);

            return statement.executeQuery().next();

        } catch (SQLException e) {

            System.out.println(
                    "Error al comprobar el nombre: "
                            + e.getMessage());

            return true;
        }
    }

    public int insertar(Ruta ruta) {

        String sql = """
                INSERT INTO rutas
                (
                    nombre,
                    altitud_maxima,
                    tipo_terreno,
                    dificultad_tecnica,
                    dificultad_fisica
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, ruta.getNombre());
            statement.setDouble(2, ruta.getAltitudMaxima());
            statement.setString(3, ruta.getTipoTerreno());
            statement.setString(4, ruta.getDificultadTecnica());
            statement.setString(5, ruta.getDificultadFisica());

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {

                if (resultSet.next()) {

                    int idGenerado = resultSet.getInt(1);

                    System.out.println(
                            "Ruta guardada correctamente. ID: "
                                    + idGenerado);

                    return idGenerado;
                }
            }

            System.out.println(
                    "La ruta se guardó, pero no se pudo obtener su ID.");

            return -1;

        } catch (SQLException e) {

            System.out.println(
                    "Error al guardar la ruta: "
                            + e.getMessage());

            return -1;
        }
    }

    public boolean actualizar(Ruta ruta) {

        String sql = """
                UPDATE rutas
                SET
                    nombre = ?,
                    altitud_maxima = ?,
                    tipo_terreno = ?,
                    dificultad_tecnica = ?,
                    dificultad_fisica = ?
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setString(1, ruta.getNombre());
            statement.setDouble(2, ruta.getAltitudMaxima());
            statement.setString(3, ruta.getTipoTerreno());
            statement.setString(4, ruta.getDificultadTecnica());
            statement.setString(5, ruta.getDificultadFisica());
            statement.setInt(6, ruta.getId());

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas > 0) {

                System.out.println(
                        "Ruta actualizada correctamente.");

                return true;
            }

            System.out.println("No se encontró la ruta.");

            return false;

        } catch (SQLException e) {

            System.out.println(
                    "Error al actualizar la ruta: "
                            + e.getMessage());

            return false;
        }
    }

    public boolean eliminar(int id) {

        String sql = "UPDATE rutas SET activo = 0 WHERE id = ?";

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, id);

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas > 0) {

                System.out.println(
                        "Ruta eliminada correctamente.");

                return true;
            }

            System.out.println("No se encontró la ruta.");

            return false;

        } catch (SQLException e) {

            System.out.println(
                    "Error al eliminar la ruta: "
                            + e.getMessage());

            return false;
        }
    }

    public List<Ruta> obtenerTodas() {

        List<Ruta> rutas = new ArrayList<>();

        String sql = """
                SELECT
                    id,
                    nombre,
                    altitud_maxima,
                    tipo_terreno,
                    dificultad_tecnica,
                    dificultad_fisica
                FROM rutas
                WHERE activo = 1
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                rutas.add(mapRow(resultSet));
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al obtener las rutas: "
                            + e.getMessage());
        }

        return rutas;
    }

    public List<Ruta> obtenerTodasIncluyendoInactivas() {

        String sql = """
                SELECT
                    id,
                    nombre,
                    altitud_maxima,
                    tipo_terreno,
                    dificultad_tecnica,
                    dificultad_fisica
                FROM rutas
                """;

        List<Ruta> rutas = new ArrayList<>();

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement =
                        conexion.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rutas.add(mapRow(resultSet));
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al obtener todas las rutas: "
                            + e.getMessage());
        }

        return rutas;
    }

    public boolean estaActiva(int id) {

        String sql = "SELECT activo FROM rutas WHERE id = ?";

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement =
                        conexion.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                return resultSet.next()
                        && resultSet.getInt("activo") == 1;
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al consultar estado de la ruta: "
                            + e.getMessage());

            return false;
        }
    }

    private Ruta mapRow(ResultSet rs) throws SQLException {

        return new Ruta(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getDouble("altitud_maxima"),
                rs.getString("tipo_terreno"),
                rs.getString("dificultad_tecnica"),
                rs.getString("dificultad_fisica"));
    }
}
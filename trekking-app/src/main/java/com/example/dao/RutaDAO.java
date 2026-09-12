package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.example.database.ConexionDB;
import com.example.model.Ruta;

public class RutaDAO {

    public boolean existeNombre(String nombre, int idExcluido) {

        String sql = "SELECT 1 FROM rutas "
                + "WHERE nombre = ? COLLATE NOCASE AND id <> ? LIMIT 1";

        try (
            Connection conexion = ConexionDB.conectar();
            PreparedStatement statement = conexion.prepareStatement(sql)
        ) {
            statement.setString(1, nombre);
            statement.setInt(2, idExcluido);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            System.out.println("Error al comprobar el nombre: " + e.getMessage());
            return true;
        }
    }

    public boolean insertar(Ruta ruta) {

        String sql = """
            INSERT INTO rutas
            (
                nombre,
                latitud_inicial,
                longitud_inicial,
                altitud_maxima,
                tipo_terreno,
                dificultad_tecnica,
                dificultad_fisica
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (
            Connection conexion = ConexionDB.conectar();
            PreparedStatement statement = conexion.prepareStatement(sql)
        ) {
            statement.setString(1, ruta.getNombre());
            statement.setDouble(2, ruta.getLatitudInicial());
            statement.setDouble(3, ruta.getLongitudInicial());
            statement.setDouble(4, ruta.getAltitudMaxima());
            statement.setString(5, ruta.getTipoTerreno());
            statement.setString(6, ruta.getDificultadTecnica());
            statement.setString(7, ruta.getDificultadFisica());
            statement.executeUpdate();

            System.out.println("Ruta guardada correctamente.");
            return true;
        } catch (SQLException e) {
            System.out.println("Error al guardar la ruta: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Ruta ruta) {

        String sql = """
            UPDATE rutas
            SET
                nombre = ?,
                latitud_inicial = ?,
                longitud_inicial = ?,
                altitud_maxima = ?,
                tipo_terreno = ?,
                dificultad_tecnica = ?,
                dificultad_fisica = ?
            WHERE id = ?
            """;

        try (
            Connection conexion = ConexionDB.conectar();
            PreparedStatement statement = conexion.prepareStatement(sql)
        ) {
            statement.setString(1, ruta.getNombre());
            statement.setDouble(2, ruta.getLatitudInicial());
            statement.setDouble(3, ruta.getLongitudInicial());
            statement.setDouble(4, ruta.getAltitudMaxima());
            statement.setString(5, ruta.getTipoTerreno());
            statement.setString(6, ruta.getDificultadTecnica());
            statement.setString(7, ruta.getDificultadFisica());
            statement.setInt(8, ruta.getId());

            int filasAfectadas = statement.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Ruta actualizada correctamente.");
                return true;
            }

            System.out.println("No se encontró la ruta.");
            return false;
        } catch (SQLException e) {
            System.out.println("Error al actualizar la ruta: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {

        String sql = "DELETE FROM rutas WHERE id = ?";

        try (
            Connection conexion = ConexionDB.conectar();
            PreparedStatement statement = conexion.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            int filasAfectadas = statement.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Ruta eliminada correctamente.");
                return true;
            }

            System.out.println("No se encontró la ruta.");
            return false;
        } catch (SQLException e) {
            System.out.println("Error al eliminar la ruta: " + e.getMessage());
            return false;
        }
    }

    public List<Ruta> obtenerTodas() {

        List<Ruta> rutas = new ArrayList<>();
        String sql = """
            SELECT
                id,
                nombre,
                latitud_inicial,
                longitud_inicial,
                altitud_maxima,
                tipo_terreno,
                dificultad_tecnica,
                dificultad_fisica
            FROM rutas
            """;

        try (
            Connection conexion = ConexionDB.conectar();
            Statement statement = conexion.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)
        ) {
            while (resultSet.next()) {
                rutas.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener las rutas: " + e.getMessage());
        }

        return rutas;
    }

    private Ruta mapRow(ResultSet rs) throws SQLException {
        return new Ruta(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getDouble("latitud_inicial"),
                rs.getDouble("longitud_inicial"),
                rs.getDouble("altitud_maxima"),
                rs.getString("tipo_terreno"),
                rs.getString("dificultad_tecnica"),
                rs.getString("dificultad_fisica"));
    }
}

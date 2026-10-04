package com.example.dao;

import com.example.database.ConexionDB;
import com.example.model.Equipamiento;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EquipamientoDAO {

    public static void insertar(Equipamiento equipamiento) {

        String sql = """
                INSERT INTO equipamiento
                (nombre, categoria, cantidad, estado, activo)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setString(1, equipamiento.getNombre());
            statement.setString(2, equipamiento.getCategoria());
            statement.setInt(3, equipamiento.getCantidad());
            statement.setString(4, equipamiento.getEstado());
            statement.setInt(5, 1);

            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "Error al insertar equipamiento: "
                            + e.getMessage());
        }
    }

    public static void actualizar(Equipamiento equipamiento) {

        String sql = """
                UPDATE equipamiento
                SET nombre = ?,
                    categoria = ?,
                    cantidad = ?,
                    estado = ?
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setString(1, equipamiento.getNombre());
            statement.setString(2, equipamiento.getCategoria());
            statement.setInt(3, equipamiento.getCantidad());
            statement.setString(4, equipamiento.getEstado());
            statement.setInt(5, equipamiento.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "Error al actualizar equipamiento: "
                            + e.getMessage());
        }
    }

    public static void eliminar(int id) {

        String sql = """
                UPDATE equipamiento
                SET activo = 0
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "Error al eliminar equipamiento: "
                            + e.getMessage());
        }
    }

    public static void reactivar(int id) {

        String sql = """
                UPDATE equipamiento
                SET activo = 1
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println(
                    "Error al reactivar equipamiento: "
                            + e.getMessage());
        }
    }

    public static ObservableList<Equipamiento> obtenerTodos() {

        ObservableList<Equipamiento> equipamientos =
                FXCollections.observableArrayList();

        String sql = """
                SELECT *
                FROM equipamiento
                WHERE activo = 1
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                equipamientos.add(mapRow(resultSet));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener equipamiento: "
                            + e.getMessage());
        }

        return equipamientos;
    }

    public static ObservableList<Equipamiento> obtenerDadosDeBaja() {

        ObservableList<Equipamiento> equipamientos =
                FXCollections.observableArrayList();

        String sql = """
                SELECT *
                FROM equipamiento
                WHERE activo = 0
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                equipamientos.add(mapRow(resultSet));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener equipamiento dado de baja: "
                            + e.getMessage());
        }

        return equipamientos;
    }

    private static Equipamiento mapRow(ResultSet resultSet)
            throws SQLException {

        return new Equipamiento(
                resultSet.getInt("id"),
                resultSet.getString("nombre"),
                resultSet.getString("categoria"),
                resultSet.getInt("cantidad"),
                resultSet.getString("estado"));
    }
}
package com.example.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.example.database.ConexionDB;
import com.example.model.Recorrido;

public class RecorridoDAO {

    public List<Recorrido> obtenerTodos() {
        List<Recorrido> recorridos = new ArrayList<>();
        String sql = """
                SELECT
                    r.recorrido_id,
                    r.ruta_id,
                    ru.nombre AS nombre_ruta,
                    r.fecha,
                    r.hora_inicio,
                    r.hora_fin
                FROM recorridos r
                JOIN rutas ru ON ru.id = r.ruta_id
                ORDER BY r.fecha DESC, r.hora_inicio
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                recorridos.add(new Recorrido(
                        resultSet.getInt("recorrido_id"),
                        resultSet.getInt("ruta_id"),
                        resultSet.getString("nombre_ruta"),
                        LocalDate.parse(resultSet.getString("fecha")),
                        parsearHora(resultSet.getString("hora_inicio")),
                        parsearHora(resultSet.getString("hora_fin"))));
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println(
                    "Error al obtener los recorridos: " + e.getMessage());
        }

        return recorridos;
    }

    public List<Integer> obtenerClientes(int recorridoId) {
        List<Integer> clientes = new ArrayList<>();
        String sql = """
                SELECT cliente_id
                FROM recorrido_cliente
                WHERE recorrido_id = ?
                ORDER BY cliente_id
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, recorridoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clientes.add(resultSet.getInt("cliente_id"));
                }
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener los participantes del recorrido: "
                            + e.getMessage());
        }

        return clientes;
    }

    public boolean insertar(Recorrido recorrido, List<Integer> clienteIds) {
        String sql = """
                INSERT INTO recorridos
                    (ruta_id, fecha, hora_inicio, hora_fin)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conexion = ConexionDB.conectar()) {
            conexion.setAutoCommit(false);
            try {
                validarReferencias(conexion, recorrido.getRutaId(), clienteIds);

                int recorridoId;
                try (PreparedStatement statement = conexion.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    asignarDatos(statement, recorrido);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException(
                                    "No se obtuvo el ID del recorrido creado.");
                        }
                        recorridoId = generatedKeys.getInt(1);
                    }
                }

                guardarParticipantes(conexion, recorridoId, clienteIds);
                conexion.commit();
                return true;
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al crear el recorrido: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Recorrido recorrido, List<Integer> clienteIds) {
        String sql = """
                UPDATE recorridos
                SET ruta_id = ?, fecha = ?, hora_inicio = ?, hora_fin = ?
                WHERE recorrido_id = ?
                """;

        try (Connection conexion = ConexionDB.conectar()) {
            conexion.setAutoCommit(false);
            try {
                validarReferencias(conexion, recorrido.getRutaId(), clienteIds);

                int filas;
                try (PreparedStatement statement = conexion.prepareStatement(sql)) {
                    asignarDatos(statement, recorrido);
                    statement.setInt(5, recorrido.getId());
                    filas = statement.executeUpdate();
                }
                if (filas == 0) {
                    conexion.rollback();
                    return false;
                }

                try (PreparedStatement statement = conexion.prepareStatement(
                        "DELETE FROM recorrido_cliente WHERE recorrido_id = ?")) {
                    statement.setInt(1, recorrido.getId());
                    statement.executeUpdate();
                }
                guardarParticipantes(
                        conexion, recorrido.getId(), clienteIds);
                conexion.commit();
                return true;
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al actualizar el recorrido: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int recorridoId) {
        try (Connection conexion = ConexionDB.conectar()) {
            conexion.setAutoCommit(false);
            try {
                try (PreparedStatement statement = conexion.prepareStatement(
                        "DELETE FROM recorrido_cliente WHERE recorrido_id = ?")) {
                    statement.setInt(1, recorridoId);
                    statement.executeUpdate();
                }

                int filas;
                try (PreparedStatement statement = conexion.prepareStatement(
                        "DELETE FROM recorridos WHERE recorrido_id = ?")) {
                    statement.setInt(1, recorridoId);
                    filas = statement.executeUpdate();
                }
                if (filas == 0) {
                    conexion.rollback();
                    return false;
                }
                conexion.commit();
                return true;
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al eliminar el recorrido: " + e.getMessage());
            return false;
        }
    }

    private void validarReferencias(
            Connection conexion,
            int rutaId,
            List<Integer> clienteIds) throws SQLException {
        try (PreparedStatement statement = conexion.prepareStatement(
                "SELECT 1 FROM rutas WHERE id = ?")) {
            statement.setInt(1, rutaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("La ruta seleccionada no existe.");
                }
            }
        }

        try (PreparedStatement statement = conexion.prepareStatement(
                "SELECT 1 FROM clientes WHERE id = ?")) {
            for (Integer clienteId : clienteIds) {
                if (clienteId == null) {
                    throw new SQLException(
                            "La lista de participantes contiene un ID vacio.");
                }
                statement.setInt(1, clienteId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException(
                                "Uno de los clientes seleccionados no existe.");
                    }
                }
            }
        }
    }

    private void guardarParticipantes(
            Connection conexion,
            int recorridoId,
            List<Integer> clienteIds) throws SQLException {
        String sql = """
                INSERT INTO recorrido_cliente (recorrido_id, cliente_id)
                VALUES (?, ?)
                """;
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            for (Integer clienteId : clienteIds) {
                statement.setInt(1, recorridoId);
                statement.setInt(2, clienteId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void asignarDatos(
            PreparedStatement statement,
            Recorrido recorrido) throws SQLException {
        statement.setInt(1, recorrido.getRutaId());
        statement.setString(2, recorrido.getFecha().toString());
        statement.setString(
                3,
                recorrido.getHoraInicio() == null
                        ? null
                        : recorrido.getHoraInicio().toString());
        statement.setString(
                4,
                recorrido.getHoraFin() == null
                        ? null
                        : recorrido.getHoraFin().toString());
    }

    private LocalTime parsearHora(String hora) {
        return hora == null || hora.isBlank()
                ? null
                : LocalTime.parse(hora);
    }
}

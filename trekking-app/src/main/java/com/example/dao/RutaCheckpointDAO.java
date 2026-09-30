package com.example.dao;

import com.example.database.ConexionDB;
import com.example.model.Checkpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RutaCheckpointDAO {

    public List<Checkpoint> obtenerPorRuta(int rutaId) {
        String sql = """
                SELECT
                    c.id,
                    c.nombre,
                    c.hora,
                    c.latitud,
                    c.longitud,
                    c.descripcion
                FROM checkpoints c
                INNER JOIN ruta_checkpoints rc
                    ON rc.checkpoint_id = c.id
                WHERE rc.ruta_id = ?
                ORDER BY rc.orden
                """;

        List<Checkpoint> checkpoints = new ArrayList<>();

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)
        ) {
            statement.setInt(1, rutaId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    checkpoints.add(new Checkpoint(
                            resultSet.getLong("id"),
                            resultSet.getString("nombre"),
                            resultSet.getString("hora"),
                            resultSet.getDouble("latitud"),
                            resultSet.getDouble("longitud"),
                            resultSet.getString("descripcion")
                    ));
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener los checkpoints de la ruta: "
                            + e.getMessage());
        }

        return checkpoints;
    }

    public boolean reemplazarPorRuta(
            int rutaId,
            List<Checkpoint> checkpoints) {

        String sqlEliminar = """
                DELETE FROM ruta_checkpoints
                WHERE ruta_id = ?
                """;

        String sqlInsertar = """
                INSERT INTO ruta_checkpoints
                (ruta_id, checkpoint_id, orden)
                VALUES (?, ?, ?)
                """;

        try (Connection conexion = ConexionDB.conectar()) {

            conexion.setAutoCommit(false);

            try (
                    PreparedStatement eliminar =
                            conexion.prepareStatement(sqlEliminar);
                    PreparedStatement insertar =
                            conexion.prepareStatement(sqlInsertar)
            ) {
                eliminar.setInt(1, rutaId);
                eliminar.executeUpdate();

                int orden = 1;

                for (Checkpoint checkpoint : checkpoints) {
                    insertar.setInt(1, rutaId);
                    insertar.setLong(2, checkpoint.getId());
                    insertar.setInt(3, orden);
                    insertar.addBatch();

                    orden++;
                }

                insertar.executeBatch();

                conexion.commit();
                return true;

            } catch (SQLException e) {
                conexion.rollback();
                System.out.println(
                        "Error al guardar los checkpoints de la ruta: "
                                + e.getMessage());
                return false;

            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al conectar con la base de datos: "
                            + e.getMessage());
            return false;
        }
    }
}
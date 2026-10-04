package com.example.dao;

import com.example.database.ConexionDB;
import com.example.model.RutaEquipamiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RutaEquipamientoDAO {

    public List<RutaEquipamiento> obtenerPorRuta(int rutaId) {

        String sql = """
                SELECT ruta_id,
                       equipamiento_id,
                       cantidad_requerida
                FROM rutas_equipamiento
                WHERE ruta_id = ?
                """;

        List<RutaEquipamiento> requerimientos = new ArrayList<>();

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement =
                        conexion.prepareStatement(sql)
        ) {

            statement.setInt(1, rutaId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    requerimientos.add(
                            new RutaEquipamiento(
                                    resultSet.getInt("ruta_id"),
                                    resultSet.getInt("equipamiento_id"),
                                    resultSet.getInt("cantidad_requerida")
                            )
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al obtener el equipamiento de la ruta: "
                            + e.getMessage());
        }

        return requerimientos;
    }

    public boolean reemplazarPorRuta(
            int rutaId,
            List<RutaEquipamiento> requerimientos) {

        String sqlEliminar = """
                DELETE FROM rutas_equipamiento
                WHERE ruta_id = ?
                """;

        String sqlInsertar = """
                INSERT INTO rutas_equipamiento
                (ruta_id, equipamiento_id, cantidad_requerida)
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

                for (RutaEquipamiento requerimiento : requerimientos) {

                    insertar.setInt(1, rutaId);
                    insertar.setInt(
                            2,
                            requerimiento.getEquipamientoId());
                    insertar.setInt(
                            3,
                            requerimiento.getCantidadRequerida());

                    insertar.addBatch();
                }

                insertar.executeBatch();

                conexion.commit();

                return true;

            } catch (SQLException e) {

                conexion.rollback();

                System.out.println(
                        "Error al guardar el equipamiento de la ruta: "
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
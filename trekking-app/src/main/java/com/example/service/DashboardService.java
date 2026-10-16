package com.example.service;

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

/**
 * Datos y consultas del panel de Inicio.
 *
 * <p>Vocabulario fijado en diseño:
 * <ul>
 *   <li>Inicio = sección lateral {@code dashboard}.</li>
 *   <li>Dashboard = panel informativo de solo lectura.</li>
 *   <li>Recorrido = instancia fechada de una Ruta con participantes (Cliente).</li>
 * </ul>
 */
public class DashboardService {

    /** Stock bajo: cantidad menor o igual a este valor (equipos activos). */
    public static final int UMBRAL_STOCK_BAJO = 2;

    /** Ventana de próximos recorridos en días (hoy + 7). */
    public static final int VENTANA_PROXIMOS_DIAS = 7;

    /** Límite de filas en el dashboard. */
    public static final int LIMITE_PROXIMOS = 5;

    public record Kpis(
            int rutasActivas,
            int clientesActivos,
            int recorridosProximos,
            int equipamientoCritico) {
    }

    public record ProximoRecorrido(
            int id,
            String nombreRuta,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            int participantes) {
    }

    public Kpis obtenerKpis() {
        return new Kpis(
                contarRutasActivas(),
                contarClientesActivos(),
                contarRecorridosProximos(),
                contarEquipamientoCritico());
    }

    public int contarRutasActivas() {
        return contar("SELECT COUNT(*) FROM rutas WHERE activo = 1");
    }

    public int contarClientesActivos() {
        return contar("SELECT COUNT(*) FROM clientes WHERE activo = 1");
    }

    public int contarRecorridosProximos() {
        String sql = """
                SELECT COUNT(*)
                FROM recorridos
                WHERE fecha >= ? AND fecha <= ?
                """;
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, LocalDate.now().toString());
            statement.setString(
                    2, LocalDate.now().plusDays(VENTANA_PROXIMOS_DIAS).toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al contar recorridos próximos: " + e.getMessage());
            return 0;
        }
    }

    public int contarEquipamientoCritico() {
        String sql = """
                SELECT COUNT(*)
                FROM equipamiento
                WHERE activo = 1
                  AND (cantidad <= ?
                       OR estado IN ('Fuera de Servicio', 'Extraviado', 'En Reparación'))
                """;
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, UMBRAL_STOCK_BAJO);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al contar equipamiento crítico: " + e.getMessage());
            return 0;
        }
    }

    public List<ProximoRecorrido> obtenerProximosRecorridos() {
        return obtenerProximosRecorridos(LIMITE_PROXIMOS);
    }

    public List<ProximoRecorrido> obtenerProximosRecorridos(int limite) {
        String sql = """
                SELECT
                    r.recorrido_id,
                    ru.nombre AS nombre_ruta,
                    r.fecha,
                    r.hora_inicio,
                    r.hora_fin,
                    (SELECT COUNT(*)
                     FROM recorrido_cliente rc
                     WHERE rc.recorrido_id = r.recorrido_id) AS participantes
                FROM recorridos r
                JOIN rutas ru ON ru.id = r.ruta_id
                WHERE r.fecha >= ? AND r.fecha <= ?
                ORDER BY r.fecha ASC, r.hora_inicio ASC
                LIMIT ?
                """;
        List<ProximoRecorrido> proximos = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, LocalDate.now().toString());
            statement.setString(
                    2, LocalDate.now().plusDays(VENTANA_PROXIMOS_DIAS).toString());
            statement.setInt(3, limite);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    proximos.add(new ProximoRecorrido(
                            resultSet.getInt("recorrido_id"),
                            resultSet.getString("nombre_ruta"),
                            LocalDate.parse(resultSet.getString("fecha")),
                            parsearHora(resultSet.getString("hora_inicio")),
                            parsearHora(resultSet.getString("hora_fin")),
                            resultSet.getInt("participantes")));
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println(
                    "Error al obtener próximos recorridos: " + e.getMessage());
        }
        return proximos;
    }

    private int contar(String sql) {
        try (Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println("Error al contar: " + e.getMessage());
            return 0;
        }
    }

    private static LocalTime parsearHora(String hora) {
        return hora == null || hora.isBlank() ? null : LocalTime.parse(hora);
    }
}

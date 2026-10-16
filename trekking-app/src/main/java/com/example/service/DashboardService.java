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
import java.util.Set;

import com.example.database.ConexionDB;
import com.example.model.Equipamiento;

/**
 * Datos y consultas del panel de Inicio.
 *
 * <p>Vocabulario fijado en diseño:
 * <ul>
 *   <li>Inicio = sección lateral {@code dashboard}.</li>
 *   <li>Dashboard = panel informativo de solo lectura + accesos.</li>
 *   <li>Recorrido = instancia fechada de una Ruta con participantes (Cliente).</li>
 *   <li>Alerta = condición que requiere acción.</li>
 * </ul>
 */
public class DashboardService {

    /** Stock bajo: cantidad menor o igual a este valor (equipos activos). */
    public static final int UMBRAL_STOCK_BAJO = 2;

    /** Ventana de próximos recorridos en días (hoy + 7). */
    public static final int VENTANA_PROXIMOS_DIAS = 7;

    /** Límite de filas en el dashboard. */
    public static final int LIMITE_PROXIMOS = 5;
    public static final int LIMITE_ALERTAS = 8;

    private static final Set<String> ESTADOS_CRITICOS = Set.of(
            "Fuera de Servicio",
            "Extraviado",
            "En Reparación");

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

    public record Alerta(
            String mensaje,
            String seccionDestino) {
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

    public List<Alerta> obtenerAlertas() {
        return obtenerAlertas(LIMITE_ALERTAS);
    }

    public List<Alerta> obtenerAlertas(int limite) {
        List<Alerta> alertas = new ArrayList<>();
        alertas.addAll(alertasEquipamiento());
        alertas.addAll(alertasRecorridosSinParticipantes());
        alertas.addAll(alertasClientesSinFicha());
        alertas.addAll(alertasRutasSinCheckpoints());
        if (alertas.size() > limite) {
            return alertas.subList(0, limite);
        }
        return alertas;
    }

    public List<Equipamiento> obtenerEquipamientoCritico() {
        String sql = """
                SELECT id, nombre, categoria, cantidad, estado
                FROM equipamiento
                WHERE activo = 1
                  AND (cantidad <= ?
                       OR estado IN ('Fuera de Servicio', 'Extraviado', 'En Reparación'))
                ORDER BY cantidad ASC, nombre COLLATE NOCASE
                LIMIT ?
                """;
        List<Equipamiento> equipos = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, UMBRAL_STOCK_BAJO);
            statement.setInt(2, LIMITE_ALERTAS);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    equipos.add(new Equipamiento(
                            resultSet.getInt("id"),
                            resultSet.getString("nombre"),
                            resultSet.getString("categoria"),
                            resultSet.getInt("cantidad"),
                            resultSet.getString("estado")));
                }
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener equipamiento crítico: " + e.getMessage());
        }
        return equipos;
    }

    private List<Alerta> alertasEquipamiento() {
        List<Alerta> alertas = new ArrayList<>();
        for (Equipamiento equipo : obtenerEquipamientoCritico()) {
            String detalle;
            if (equipo.getCantidad() <= 0) {
                detalle = "sin stock";
            } else if (equipo.getCantidad() <= UMBRAL_STOCK_BAJO) {
                detalle = "stock bajo (" + equipo.getCantidad() + ")";
            } else {
                detalle = estadoCritico(equipo.getEstado())
                        ? equipo.getEstado().toLowerCase()
                        : "requiere atención";
            }
            alertas.add(new Alerta(
                    "Equipo: " + equipo.getNombre() + " — " + detalle,
                    "equipment"));
        }
        return alertas;
    }

    private List<Alerta> alertasRecorridosSinParticipantes() {
        String sql = """
                SELECT r.recorrido_id, ru.nombre AS nombre_ruta, r.fecha
                FROM recorridos r
                JOIN rutas ru ON ru.id = r.ruta_id
                WHERE r.fecha >= ? AND r.fecha <= ?
                  AND NOT EXISTS (
                      SELECT 1 FROM recorrido_cliente rc
                      WHERE rc.recorrido_id = r.recorrido_id
                  )
                ORDER BY r.fecha ASC
                LIMIT ?
                """;
        List<Alerta> alertas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, LocalDate.now().toString());
            statement.setString(
                    2, LocalDate.now().plusDays(VENTANA_PROXIMOS_DIAS).toString());
            statement.setInt(3, LIMITE_ALERTAS);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    alertas.add(new Alerta(
                            "Recorrido sin participantes: "
                                    + resultSet.getString("nombre_ruta")
                                    + " (" + resultSet.getString("fecha") + ")",
                            "trips"));
                }
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener recorridos sin participantes: "
                            + e.getMessage());
        }
        return alertas;
    }

    private List<Alerta> alertasClientesSinFicha() {
        String sql = """
                SELECT nombre, apellido
                FROM clientes
                WHERE activo = 1 AND ficha_medica IS NULL
                ORDER BY apellido COLLATE NOCASE, nombre COLLATE NOCASE
                LIMIT ?
                """;
        List<Alerta> alertas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, LIMITE_ALERTAS);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    alertas.add(new Alerta(
                            "Cliente sin ficha médica: "
                                    + resultSet.getString("apellido")
                                    + ", " + resultSet.getString("nombre"),
                            "clients"));
                }
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener clientes sin ficha: " + e.getMessage());
        }
        return alertas;
    }

    private List<Alerta> alertasRutasSinCheckpoints() {
        String sql = """
                SELECT r.nombre
                FROM rutas r
                LEFT JOIN ruta_checkpoints rc ON rc.ruta_id = r.id
                WHERE r.activo = 1 AND rc.ruta_id IS NULL
                ORDER BY r.nombre COLLATE NOCASE
                LIMIT ?
                """;
        List<Alerta> alertas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, LIMITE_ALERTAS);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    alertas.add(new Alerta(
                            "Ruta sin checkpoints: "
                                    + resultSet.getString("nombre"),
                            "routes"));
                }
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener rutas sin checkpoints: "
                            + e.getMessage());
        }
        return alertas;
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

    private static boolean estadoCritico(String estado) {
        return estado != null && ESTADOS_CRITICOS.contains(estado);
    }

    private static LocalTime parsearHora(String hora) {
        return hora == null || hora.isBlank() ? null : LocalTime.parse(hora);
    }
}

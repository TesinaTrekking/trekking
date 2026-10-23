package com.example.dao;

import com.example.database.ConexionDB;
import com.example.model.Ruta;
import com.example.model.Salida;
import com.example.model.Checkpoint;
import com.example.model.Equipamiento;
import com.example.model.AsignacionEquipamiento;
import com.example.model.AsignacionParticipante;
import com.example.model.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class SalidaDAO {
    private final RutaDAO rutaDAO = new RutaDAO();

    public List<Salida> obtenerTodas() {
        String sql = "SELECT * FROM salidas ORDER BY fecha DESC, id DESC";
        List<Salida> salidas = new ArrayList<>();
        try (Connection c = ConexionDB.conectar();
             Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) salidas.add(mapear(rs));
        } catch (SQLException | RuntimeException e) {
            throw new IllegalStateException("No se pudieron cargar las salidas.", e);
        }
        return salidas;
    }

    public Map<String, Integer> obtenerIndicadores() {
        LocalDate hoy = LocalDate.now();
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement()) {
            int proximas = contar(s, "SELECT COUNT(*) FROM salidas WHERE estado = 'planificada' AND fecha BETWEEN '" + hoy + "' AND '" + hoy.plusDays(7) + "'");
            int rutas = contar(s, "SELECT COUNT(*) FROM rutas WHERE activo = 1");
            int clientes = contar(s, "SELECT COUNT(*) FROM clientes WHERE activo = 1");
            int criticos = contar(s, """
                    SELECT COUNT(*) FROM equipamiento e
                    WHERE e.activo = 1 AND (
                        e.cantidad - COALESCE((SELECT SUM(se.cantidad) FROM salida_equipamiento se
                            JOIN salidas sa ON sa.id = se.salida_id
                            WHERE se.equipamiento_id = e.id AND sa.estado = 'planificada'), 0) <= 2
                        OR e.estado IN ('En Reparación', 'Fuera de Servicio', 'Extraviado'))
                    """);
            return Map.of("proximas", proximas, "rutas", rutas, "clientes", clientes, "criticos", criticos);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron calcular los indicadores.", e);
        }
    }

    private int contar(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) { return rs.next() ? rs.getInt(1) : 0; }
    }

    public int crear(LocalDate fecha, int rutaId) throws SQLException {
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                Ruta ruta = rutaDAO.obtenerPorId(c, rutaId);
                if (ruta == null) throw new SQLException("La ruta seleccionada no existe.");
                try (PreparedStatement activa = c.prepareStatement(
                        "SELECT activo FROM rutas WHERE id = ?")) {
                    activa.setInt(1, rutaId);
                    try (ResultSet rs = activa.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != 1)
                            throw new SQLException("Selecciona una ruta activa para crear la salida.");
                    }
                }
                String sql = """
                        INSERT INTO salidas (ruta_id, fecha, estado, ruta_nombre,
                            ruta_altitud_maxima, ruta_tipo_terreno,
                            ruta_dificultad_tecnica, ruta_dificultad_fisica)
                        VALUES (?, ?, 'planificada', ?, ?, ?, ?, ?)
                        """;
                int id;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, rutaId); ps.setString(2, fecha.toString());
                    ps.setString(3, ruta.getNombre()); ps.setDouble(4, ruta.getAltitudMaxima());
                    ps.setString(5, ruta.getTipoTerreno()); ps.setString(6, ruta.getDificultadTecnica());
                    ps.setString(7, ruta.getDificultadFisica()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se obtuvo el ID de la salida.");
                        id = keys.getInt(1);
                    }
                }
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Confirma una Salida y, opcionalmente, su Ruta nueva en una única transacción. */
    public int confirmar(LocalDate fecha, Integer rutaId, Ruta rutaNueva) throws SQLException {
        return confirmar(fecha, rutaId, rutaNueva, List.of());
    }

    /** Confirma la Salida y sus reservas de Inventario de forma atómica. */
    public int confirmar(LocalDate fecha, Integer rutaId, Ruta rutaNueva,
            List<AsignacionEquipamiento> asignaciones) throws SQLException {
        return confirmar(fecha, rutaId, rutaNueva, asignaciones, List.of());
    }

    /** Confirma la Salida, sus reservas y sus Participantes en una sola transacción. */
    public int confirmar(LocalDate fecha, Integer rutaId, Ruta rutaNueva,
            List<AsignacionEquipamiento> asignaciones, List<AsignacionParticipante> participantes) throws SQLException {
        if (fecha == null) throw new SQLException("La fecha de la Salida es obligatoria.");
        if ((rutaId == null) == (rutaNueva == null))
            throw new SQLException("Selecciona una Ruta o completa una Ruta nueva.");
        try (Connection c = ConexionDB.conectar()) {
            try (Statement timeout = c.createStatement()) { timeout.execute("PRAGMA busy_timeout = 5000"); }
            c.setAutoCommit(false);
            try {
                bloquearInventario(c, asignaciones);
                int idRuta = rutaId == null ? insertarRuta(c, rutaNueva) : rutaId;
                Ruta ruta = rutaDAO.obtenerPorId(c, idRuta);
                if (ruta == null) throw new SQLException("La Ruta seleccionada no existe.");
                if (rutaId != null) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM rutas WHERE id = ?")) {
                        ps.setInt(1, idRuta);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || rs.getInt(1) != 1)
                                throw new SQLException("Selecciona una Ruta activa.");
                        }
                    }
                }
                String sql = """
                        INSERT INTO salidas (ruta_id, fecha, estado, ruta_nombre, ruta_altitud_maxima,
                            ruta_tipo_terreno, ruta_dificultad_tecnica, ruta_dificultad_fisica)
                        VALUES (?, ?, 'planificada', ?, ?, ?, ?, ?)
                        """;
                int salidaId;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idRuta); ps.setString(2, fecha.toString());
                    ps.setString(3, ruta.getNombre()); ps.setDouble(4, ruta.getAltitudMaxima());
                    ps.setString(5, ruta.getTipoTerreno()); ps.setString(6, ruta.getDificultadTecnica());
                    ps.setString(7, ruta.getDificultadFisica()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se obtuvo el ID de la Salida.");
                        salidaId = keys.getInt(1);
                    }
                }
                List<Checkpoint> puntos = new RutaCheckpointDAO().obtenerPorRuta(c, idRuta);
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO salida_checkpoints
                            (salida_id, nombre, hora, latitud, longitud, altitud, descripcion, orden)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    int orden = 0;
                    for (Checkpoint cp : puntos) {
                        if (cp.getAltitud() > ruta.getAltitudMaxima())
                            throw new SQLException("Un Checkpoint supera la altitud máxima de la Ruta.");
                        ps.setInt(1, salidaId); ps.setString(2, cp.getNombre()); ps.setString(3, cp.getHora());
                        ps.setDouble(4, cp.getLatitud()); ps.setDouble(5, cp.getLongitud());
                        ps.setDouble(6, cp.getAltitud()); ps.setString(7, cp.getDescripcion());
                        ps.setInt(8, orden++); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                guardarAsignaciones(c, salidaId, asignaciones);
                guardarParticipantes(c, salidaId, participantes);
                c.commit();
                return salidaId;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                if (e instanceof SQLException sqlException) throw sqlException;
                throw new SQLException("No se pudo confirmar la Salida.", e);
            }
        }
    }

    private void guardarParticipantes(Connection c, int salidaId,
            List<AsignacionParticipante> participantes) throws SQLException {
        if (participantes == null) throw new SQLException("La lista de Participantes es inválida.");
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        java.util.Set<String> dnis = new java.util.HashSet<>();
        ClienteDAO clienteDAO = new ClienteDAO();
        for (AsignacionParticipante asignacion : participantes) {
            if (asignacion == null || asignacion.cliente() == null)
                throw new SQLException("Hay un Participante inválido.");
            Cliente cliente = asignacion.cliente();
            String dni = cliente.getDni() == null ? "" : cliente.getDni().trim();
            if (dni.isEmpty() || !dnis.add(dni))
                throw new SQLException("No se puede asociar dos veces el mismo Cliente.");
            int clienteId = cliente.getId();
            if (asignacion.nuevo()) {
                try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM clientes WHERE dni = ?")) {
                    ps.setString(1, dni);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            if (rs.getInt("activo") != 1)
                                throw new SQLException("El DNI ya pertenece a un Cliente inactivo. Reactívalo desde Clientes antes de asociarlo.");
                            throw new SQLException("El DNI ya pertenece a un Cliente activo. Busca y selecciona el registro existente.");
                        }
                    }
                }
                clienteId = clienteDAO.insertarEnTransaccion(c, cliente,
                        asignacion.fichaMedica(), asignacion.nombreFichaMedica());
            } else {
                try (PreparedStatement ps = c.prepareStatement("SELECT dni, activo FROM clientes WHERE id = ?")) {
                    ps.setInt(1, clienteId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("El Cliente seleccionado ya no existe.");
                        if (rs.getInt("activo") != 1)
                            throw new SQLException("El Cliente está inactivo. Reactívalo desde Clientes antes de asociarlo.");
                        if (!dni.equals(rs.getString("dni")))
                            throw new SQLException("El DNI del Cliente seleccionado cambió. Actualiza la lista y vuelve a seleccionarlo.");
                    }
                }
            }
            if (!ids.add(clienteId)) throw new SQLException("No se puede asociar dos veces el mismo Cliente.");
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO salida_participantes
                        (salida_id, cliente_id, dni_copia, nombre_copia, apellido_copia, telefono_copia,
                         emergencia_nombre_copia, emergencia_telefono_copia, emergencia_relacion_copia)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setInt(1, salidaId); ps.setInt(2, clienteId); ps.setString(3, dni);
                ps.setString(4, cliente.getNombre()); ps.setString(5, cliente.getApellido());
                ps.setString(6, cliente.getTelefono());
                ps.setString(7, cliente.getContactoEmergenciaNombre());
                ps.setString(8, cliente.getContactoEmergenciaTelefono());
                ps.setString(9, cliente.getContactoEmergenciaRelacion());
                ps.executeUpdate();
            }
        }
    }

    private void bloquearInventario(Connection c, List<AsignacionEquipamiento> asignaciones)
            throws SQLException {
        // SQLite obtains its database write lock on this statement, before reading availability.
        try (PreparedStatement ps = c.prepareStatement("UPDATE equipamiento SET cantidad = cantidad WHERE id = ?")) {
            for (AsignacionEquipamiento item : asignaciones.stream()
                    .filter(a -> !a.nuevo()).sorted(Comparator.comparingInt(a -> a.equipamiento().getId())).toList()) {
                ps.setInt(1, item.equipamiento().getId());
                if (ps.executeUpdate() != 1) throw new SQLException("El Elemento de inventario ya no existe.");
            }
        }
    }

    private void guardarAsignaciones(Connection c, int salidaId,
            List<AsignacionEquipamiento> asignaciones) throws SQLException {
        if (asignaciones == null) throw new SQLException("La lista de Equipamiento es inválida.");
        String insertItem = "INSERT INTO equipamiento (nombre, categoria, cantidad, estado, activo) VALUES (?, ?, ?, ?, 1)";
        String insertAsignacion = """
                INSERT INTO salida_equipamiento (salida_id, equipamiento_id, nombre_copia, categoria_copia, cantidad)
                VALUES (?, ?, ?, ?, ?)
                """;
        for (AsignacionEquipamiento asignacion : asignaciones) {
            if (asignacion == null || asignacion.equipamiento() == null || asignacion.cantidad() <= 0)
                throw new SQLException("La cantidad asignada debe ser un entero positivo.");
            Equipamiento item = asignacion.equipamiento();
            int id = item.getId();
            int total = item.getCantidad();
            if (asignacion.nuevo()) {
                if (item.getNombre() == null || item.getNombre().isBlank())
                    throw new SQLException("El nombre del Elemento de inventario es obligatorio.");
                if (total < 0) throw new SQLException("La cantidad total debe ser mayor o igual a cero.");
                if (!Equipamiento.categoriaValida(item.getCategoria()))
                    throw new SQLException("Selecciona una categoría válida para el Elemento de inventario.");
                if (!Equipamiento.estadoValido(item.getEstado()))
                    throw new SQLException("Selecciona un estado válido para el Elemento de inventario.");
                try (PreparedStatement ps = c.prepareStatement(insertItem, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, item.getNombre()); ps.setString(2, item.getCategoria());
                    ps.setInt(3, total); ps.setString(4, item.getEstado()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se pudo guardar el Elemento de inventario nuevo.");
                        id = keys.getInt(1);
                    }
                }
            } else {
                Equipamiento actual = EquipamientoDAO.obtenerPorId(c, id);
                if (actual == null || actual.getId() != id) throw new SQLException("El Elemento de inventario seleccionado ya no existe.");
                if (!esActivo(c, id)) throw new SQLException("El Elemento de inventario está dado de baja y no puede asignarse.");
                total = actual.getCantidad();
                int disponible = total - actual.getCantidadReservada();
                if (asignacion.cantidad() > disponible)
                    throw new SQLException("No hay cantidad suficiente de " + actual.getNombre() + ". Disponible: " + disponible + ".");
                item = actual;
            }
            if (asignacion.cantidad() > total)
                throw new SQLException("No hay cantidad suficiente de " + item.getNombre() + ". Disponible: " + total + ".");
            try (PreparedStatement ps = c.prepareStatement(insertAsignacion)) {
                ps.setInt(1, salidaId); ps.setInt(2, id); ps.setString(3, item.getNombre());
                ps.setString(4, item.getCategoria()); ps.setInt(5, asignacion.cantidad()); ps.executeUpdate();
            }
        }
    }

    private boolean esActivo(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM equipamiento WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    private int insertarRuta(Connection c, Ruta ruta) throws SQLException {
        String nombre = Ruta.normalizarNombre(ruta.getNombre());
        if (nombre.isBlank() || nombre.length() > 100 || !Ruta.nombreValido(nombre))
            throw new SQLException("El nombre de la Ruta es obligatorio y debe tener hasta 100 caracteres válidos.");
        if (ruta.getAltitudMaxima() < 0 || !Double.isFinite(ruta.getAltitudMaxima()))
            throw new SQLException("La altitud máxima debe ser un número mayor o igual a cero.");
        try (PreparedStatement check = c.prepareStatement("SELECT 1 FROM rutas WHERE nombre = ? COLLATE NOCASE")) {
            check.setString(1, nombre);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) throw new SQLException("Ya existe una Ruta con ese nombre.");
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO rutas (nombre, altitud_maxima, tipo_terreno, dificultad_tecnica, dificultad_fisica)
                VALUES (?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre); ps.setDouble(2, ruta.getAltitudMaxima());
            ps.setString(3, ruta.getTipoTerreno()); ps.setString(4, ruta.getDificultadTecnica());
            ps.setString(5, ruta.getDificultadFisica()); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se pudo guardar la Ruta nueva.");
                return keys.getInt(1);
            }
        }
    }

    public void cambiarEstado(int id, String nuevoEstado) throws SQLException {
        if (!List.of("realizada", "cancelada").contains(nuevoEstado))
            throw new SQLException("Estado de cierre inválido.");
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(
                "UPDATE salidas SET estado = ? WHERE id = ? AND estado = 'planificada'")) {
            ps.setString(1, nuevoEstado); ps.setInt(2, id);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Solo se puede cerrar una salida planificada.");
        }
    }

    private Salida mapear(ResultSet rs) throws SQLException {
        return new Salida(rs.getInt("id"), rs.getInt("ruta_id"),
                LocalDate.parse(rs.getString("fecha")), rs.getString("estado"),
                rs.getString("ruta_nombre"), rs.getDouble("ruta_altitud_maxima"),
                rs.getString("ruta_tipo_terreno"), rs.getString("ruta_dificultad_tecnica"),
                rs.getString("ruta_dificultad_fisica"));
    }
}

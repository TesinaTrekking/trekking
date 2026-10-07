package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import com.example.model.FichaMedica;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ConexionDB {

    private static final String URL = "jdbc:sqlite:crud.db";

    static Connection conectar() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontro el driver JDBC de SQLite.", e);
        }
        return DriverManager.getConnection(URL);
    }

    public static void crearTabla() {
        String sql = "CREATE TABLE IF NOT EXISTS clientes (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "dni TEXT UNIQUE," +
                     "nombre TEXT NOT NULL," +
                     "apellido TEXT NOT NULL," +
                     "fecha_nacimiento TEXT," +
                     "email TEXT," +
                     "telefono TEXT," +
                     "sexo_id INTEGER," +
                     "sexo TEXT NOT NULL," +
                     "contacto_emergencia_nombre TEXT," +
                     "contacto_emergencia_telefono TEXT," +
                     "contacto_emergencia_relacion TEXT," +
                     "autorizacion_menores INTEGER NOT NULL DEFAULT 0," +
                     "tutor_nombre TEXT," +
                     "tutor_apellido TEXT," +
                     "tutor_dni TEXT," +
                     "tutor_telefono TEXT," +
                     "activo INTEGER NOT NULL DEFAULT 1," +
                     "ficha_medica BLOB," +
                     "nombre_ficha_medica TEXT," +
                     "fecha_alta TEXT NOT NULL DEFAULT CURRENT_DATE," +
                     "fecha_modificacion TEXT NOT NULL DEFAULT CURRENT_DATE)";

        try (Connection conexion = conectar();
             Statement statement = conexion.createStatement()) {

            statement.execute(sql);
            agregarColumnasSiFaltan(conexion);
                statement.execute("CREATE TABLE IF NOT EXISTS sexos ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE)");
                statement.execute("INSERT OR IGNORE INTO sexos (nombre) VALUES "
                    + "('Masculino'), ('Femenino'), ('No binario'), ('Otro'), ('Prefiero no decir')");
                agregarColumnaSexoIdSiFalta(conexion);
                statement.execute("UPDATE clientes SET sexo_id = (SELECT id FROM sexos WHERE sexos.nombre = clientes.sexo) "
                    + "WHERE sexo_id IS NULL AND sexo IS NOT NULL");
                statement.execute("CREATE TABLE IF NOT EXISTS contactos_emergencia ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id INTEGER NOT NULL UNIQUE,"
                    + "nombre TEXT NOT NULL, telefono TEXT NOT NULL, relacion TEXT NOT NULL,"
                    + "FOREIGN KEY (cliente_id) REFERENCES clientes(id))");
                statement.execute("INSERT OR IGNORE INTO contactos_emergencia (cliente_id, nombre, telefono, relacion) "
                    + "SELECT id, contacto_emergencia_nombre, contacto_emergencia_telefono, "
                    + "contacto_emergencia_relacion FROM clientes WHERE contacto_emergencia_nombre IS NOT NULL "
                    + "AND contacto_emergencia_nombre <> '' AND contacto_emergencia_telefono IS NOT NULL "
                    + "AND contacto_emergencia_telefono <> '' AND contacto_emergencia_relacion IS NOT NULL "
                    + "AND contacto_emergencia_relacion <> ''");
            System.out.println("Tabla 'clientes' lista.");

        } catch (SQLException e) {
            System.out.println("Error al crear la tabla: " + e.getMessage());
        }
    }

    private static void agregarColumnasSiFaltan(Connection conexion) throws SQLException {
        String[] columnas = {"dni TEXT", "fecha_nacimiento TEXT", "telefono TEXT",
                "contacto_emergencia_nombre TEXT", "contacto_emergencia_telefono TEXT",
                "contacto_emergencia_relacion TEXT", "fecha_alta TEXT", "fecha_modificacion TEXT"};
        for (String columna : columnas) {
            String nombreColumna = columna.substring(0, columna.indexOf(' '));
            if (!existeColumna(conexion, "clientes", nombreColumna)) {
                try (Statement statement = conexion.createStatement()) {
                    statement.execute("ALTER TABLE clientes ADD COLUMN " + columna);
                }
            }
        }
        String[] columnasFicha = {"ficha_medica BLOB", "nombre_ficha_medica TEXT"};
        for (String columna : columnasFicha) {
            String nombreColumna = columna.substring(0, columna.indexOf(' '));
            if (!existeColumna(conexion, "clientes", nombreColumna)) {
                try (Statement statement = conexion.createStatement()) {
                    statement.execute("ALTER TABLE clientes ADD COLUMN " + columna);
                }
            }
        }
        String[] columnasTutor = {"autorizacion_menores INTEGER NOT NULL DEFAULT 0", "tutor_nombre TEXT",
                "tutor_apellido TEXT", "tutor_dni TEXT", "tutor_telefono TEXT"};
        for (String columna : columnasTutor) {
            String nombreColumna = columna.substring(0, columna.indexOf(' '));
            if (!existeColumna(conexion, "clientes", nombreColumna)) {
                try (Statement statement = conexion.createStatement()) {
                    statement.execute("ALTER TABLE clientes ADD COLUMN " + columna);
                }
            }
        }
    }

    private static boolean existeColumna(Connection conexion, String tabla, String columna) throws SQLException {
        String sql = "SELECT 1 FROM pragma_table_info(?) WHERE name = ?";
        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, tabla);
            statement.setString(2, columna);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void agregarColumnaSexoIdSiFalta(Connection conexion) throws SQLException {
        if (!existeColumna(conexion, "clientes", "sexo_id")) {
            try (Statement statement = conexion.createStatement()) {
                statement.execute("ALTER TABLE clientes ADD COLUMN sexo_id INTEGER");
            }
        }
    }

    public static boolean insertarCliente(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion, boolean activo, byte[] fichaMedica, String nombreFichaMedica) {
        return insertarCliente(dni, nombre, apellido, fechaNacimiento, email, telefono, sexo, contactoNombre,
            contactoTelefono, contactoRelacion, activo, false, null, null, null, null, fichaMedica,
            nombreFichaMedica);
    }

    public static boolean insertarCliente(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion, boolean activo, boolean autorizacionMenores, String tutorNombre,
            String tutorApellido, String tutorDni, String tutorTelefono, byte[] fichaMedica,
            String nombreFichaMedica) {
        String sql = "INSERT INTO clientes (dni, nombre, apellido, fecha_nacimiento, email, telefono, sexo, "
                + "contacto_emergencia_nombre, contacto_emergencia_telefono, contacto_emergencia_relacion, "
                + "autorizacion_menores, tutor_nombre, tutor_apellido, tutor_dni, tutor_telefono, activo, "
                + "ficha_medica, nombre_ficha_medica, fecha_alta, fecha_modificacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, CURRENT_DATE)";

        try (Connection conexion = conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            conexion.setAutoCommit(false);

            try {
                statement.setString(1, dni);
                statement.setString(2, nombre);
                statement.setString(3, apellido);
                statement.setString(4, fechaNacimiento == null ? null : fechaNacimiento.toString());
                statement.setString(5, textoONull(email));
                statement.setString(6, textoONull(telefono));
                statement.setString(7, sexo);
                statement.setString(8, textoONull(contactoNombre));
                statement.setString(9, textoONull(contactoTelefono));
                statement.setString(10, textoONull(contactoRelacion));
                statement.setBoolean(11, autorizacionMenores);
                statement.setString(12, textoONull(tutorNombre));
                statement.setString(13, textoONull(tutorApellido));
                statement.setString(14, textoONull(tutorDni));
                statement.setString(15, textoONull(tutorTelefono));
                statement.setBoolean(16, activo);
                statement.setBytes(17, fichaMedica);
                statement.setString(18, textoONull(nombreFichaMedica));

                statement.executeUpdate();
                int clienteId = obtenerUltimoId(conexion);
                sincronizarSexoYContacto(conexion, clienteId, sexo, contactoNombre, contactoTelefono,
                        contactoRelacion);
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
            System.out.println("Cliente guardado con exito.");
            return true;

        } catch (SQLException e) {
            System.out.println("Error al guardar: " + e.getMessage());
            return false;
        }
    }

    public static boolean actualizarCliente(int id, String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion, boolean activo) {
        return actualizarCliente(id, dni, nombre, apellido, fechaNacimiento, email, telefono, sexo, contactoNombre,
            contactoTelefono, contactoRelacion, activo, false, null, null, null, null, null, null, false);
        }

        public static boolean actualizarCliente(int id, String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion, boolean activo, byte[] fichaMedica, String nombreFichaMedica,
            boolean actualizarFichaMedica) {
        return actualizarCliente(id, dni, nombre, apellido, fechaNacimiento, email, telefono, sexo, contactoNombre,
            contactoTelefono, contactoRelacion, activo, false, null, null, null, null, fichaMedica,
            nombreFichaMedica, actualizarFichaMedica);
        }

        public static boolean actualizarCliente(int id, String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion, boolean activo, boolean autorizacionMenores, String tutorNombre,
            String tutorApellido, String tutorDni, String tutorTelefono, byte[] fichaMedica,
            String nombreFichaMedica, boolean actualizarFichaMedica) {
        String sql = "UPDATE clientes SET dni = ?, nombre = ?, apellido = ?, fecha_nacimiento = ?, email = ?, "
                + "telefono = ?, sexo = ?, contacto_emergencia_nombre = ?, contacto_emergencia_telefono = ?, "
            + "contacto_emergencia_relacion = ?, autorizacion_menores = ?, tutor_nombre = ?, tutor_apellido = ?, "
            + "tutor_dni = ?, tutor_telefono = ?, activo = ?, ";
        sql += actualizarFichaMedica ? "ficha_medica = ?, nombre_ficha_medica = ?, "
            : "";
        sql += "fecha_modificacion = CURRENT_DATE WHERE id = ?";

        try (Connection conexion = conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            conexion.setAutoCommit(false);

            try {
                statement.setString(1, dni);
                statement.setString(2, nombre);
                statement.setString(3, apellido);
                statement.setString(4, fechaNacimiento == null ? null : fechaNacimiento.toString());
                statement.setString(5, textoONull(email));
                statement.setString(6, textoONull(telefono));
                statement.setString(7, sexo);
                statement.setString(8, textoONull(contactoNombre));
                statement.setString(9, textoONull(contactoTelefono));
                statement.setString(10, textoONull(contactoRelacion));
                statement.setBoolean(11, autorizacionMenores);
                statement.setString(12, textoONull(tutorNombre));
                statement.setString(13, textoONull(tutorApellido));
                statement.setString(14, textoONull(tutorDni));
                statement.setString(15, textoONull(tutorTelefono));
                statement.setBoolean(16, activo);
                int indiceId = 17;
                if (actualizarFichaMedica) {
                    statement.setBytes(17, fichaMedica);
                    statement.setString(18, textoONull(nombreFichaMedica));
                    indiceId = 19;
                }
                statement.setInt(indiceId, id);

                statement.executeUpdate();
                if (!existeCliente(conexion, id)) {
                    conexion.rollback();
                    System.out.println("No se encontro ningun cliente con ese ID.");
                    return false;
                }
                sincronizarSexoYContacto(conexion, id, sexo, contactoNombre, contactoTelefono,
                        contactoRelacion);
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
            System.out.println("Cliente actualizado con exito.");
            return true;

        } catch (SQLException e) {
            System.out.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    public static void eliminarCliente(int id) {
        String sql = "UPDATE clientes SET activo = 0, fecha_modificacion = CURRENT_DATE WHERE id = ?";

        try (Connection conexion = conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, id);

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas > 0) {
                System.out.println("Cliente eliminado con exito.");
            } else {
                System.out.println("No se encontro ningun cliente con ese ID.");
            }

        } catch (SQLException e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }

    public static FichaMedica obtenerFichaMedica(int clienteId) {
        String sql = "SELECT ficha_medica, nombre_ficha_medica FROM clientes WHERE id = ?";
        try (Connection conexion = conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, clienteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getBytes("ficha_medica") != null) {
                    return new FichaMedica(resultSet.getBytes("ficha_medica"),
                            resultSet.getString("nombre_ficha_medica"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener la ficha medica: " + e.getMessage());
        }
        return null;
    }

    public static ObservableList<Cliente> obtenerTodosLosClientes() {
        ObservableList<Cliente> clientes = FXCollections.observableArrayList();
        String sql = "SELECT c.id, c.dni, c.nombre, c.apellido, c.fecha_nacimiento, c.email, c.telefono, "
            + "COALESCE(s.nombre, c.sexo) AS sexo, "
            + "COALESCE(ce.nombre, c.contacto_emergencia_nombre) AS contacto_nombre, "
            + "COALESCE(ce.telefono, c.contacto_emergencia_telefono) AS contacto_telefono, "
            + "COALESCE(ce.relacion, c.contacto_emergencia_relacion) AS contacto_relacion, c.activo, "
                + "c.autorizacion_menores, c.tutor_nombre, c.tutor_apellido, c.tutor_dni, c.tutor_telefono, "
                + "c.fecha_alta, c.fecha_modificacion FROM clientes c "
                + "LEFT JOIN sexos s ON s.id = c.sexo_id "
                + "LEFT JOIN contactos_emergencia ce ON ce.cliente_id = c.id "
                + "ORDER BY c.apellido, c.nombre";

        try (Connection conexion = conectar();
             Statement statement = conexion.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String dni = resultSet.getString("dni");
                String nombre = resultSet.getString("nombre");
                String apellido = resultSet.getString("apellido");
                LocalDate fechaNacimiento = parsearFecha(resultSet.getString("fecha_nacimiento"));
                String email = resultSet.getString("email");
                String telefono = resultSet.getString("telefono");
                String sexo = resultSet.getString("sexo");
                String contactoNombre = resultSet.getString("contacto_nombre");
                String contactoTelefono = resultSet.getString("contacto_telefono");
                String contactoRelacion = resultSet.getString("contacto_relacion");
                boolean autorizacionMenores = resultSet.getBoolean("autorizacion_menores");
                String tutorNombre = resultSet.getString("tutor_nombre");
                String tutorApellido = resultSet.getString("tutor_apellido");
                String tutorDni = resultSet.getString("tutor_dni");
                String tutorTelefono = resultSet.getString("tutor_telefono");
                boolean activo = resultSet.getBoolean("activo");
                LocalDate fechaAlta = parsearFecha(resultSet.getString("fecha_alta"));
                LocalDate fechaModificacion = parsearFecha(resultSet.getString("fecha_modificacion"));
                clientes.add(new Cliente(id, dni, nombre, apellido, fechaNacimiento, email, telefono, sexo,
                    contactoNombre, contactoTelefono, contactoRelacion, autorizacionMenores, tutorNombre,
                    tutorApellido, tutorDni, tutorTelefono, activo, fechaAlta, fechaModificacion));
            }

        } catch (SQLException e) {
            System.out.println("Error al obtener clientes: " + e.getMessage());
        }

        return clientes;
    }

    private static LocalDate parsearFecha(String fecha) {
        return fecha == null || fecha.isEmpty() ? null : LocalDate.parse(fecha);
    }

    private static int obtenerUltimoId(Connection conexion) throws SQLException {
        try (Statement statement = conexion.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT last_insert_rowid()")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private static boolean existeCliente(Connection conexion, int id) throws SQLException {
        try (PreparedStatement statement = conexion.prepareStatement("SELECT 1 FROM clientes WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void sincronizarSexoYContacto(Connection conexion, int clienteId, String sexo,
            String contactoNombre, String contactoTelefono, String contactoRelacion) throws SQLException {
        try (PreparedStatement statement = conexion.prepareStatement(
                "UPDATE clientes SET sexo_id = (SELECT id FROM sexos WHERE nombre = ?) WHERE id = ?")) {
            statement.setString(1, sexo);
            statement.setInt(2, clienteId);
            statement.executeUpdate();
        }

        boolean tieneContacto = tieneTexto(contactoNombre) && tieneTexto(contactoTelefono)
            && tieneTexto(contactoRelacion);
        if (tieneContacto) {
            try (PreparedStatement statement = conexion.prepareStatement(
                    "INSERT INTO contactos_emergencia (cliente_id, nombre, telefono, relacion) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(cliente_id) DO UPDATE SET nombre = excluded.nombre, telefono = excluded.telefono, "
                    + "relacion = excluded.relacion")) {
                statement.setInt(1, clienteId);
                statement.setString(2, contactoNombre);
                statement.setString(3, contactoTelefono);
                statement.setString(4, contactoRelacion);
                statement.executeUpdate();
            }
        } else {
            try (PreparedStatement statement = conexion.prepareStatement(
                    "DELETE FROM contactos_emergencia WHERE cliente_id = ?")) {
                statement.setInt(1, clienteId);
                statement.executeUpdate();
            }
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private static String textoONull(String valor) {
        return tieneTexto(valor) ? valor.trim() : null;
    }

    }

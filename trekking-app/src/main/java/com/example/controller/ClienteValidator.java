package com.example.controller;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public final class ClienteValidator {

    private static final Pattern DNI_VALIDO = Pattern.compile("\\d{7,8}");
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[\\p{L}]+(?:[ '-][\\p{L}]+)*");
    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern TELEFONO_VALIDO = Pattern.compile("[0-9+() -]{7,20}");

    private ClienteValidator() {
    }

    public static String validar(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion) {
        return validar(dni, nombre, apellido, fechaNacimiento, email, telefono, sexo, contactoNombre,
            contactoTelefono, contactoRelacion, false, "", "", "", "");
        }

        public static String validar(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String email, String telefono, String sexo, String contactoNombre, String contactoTelefono,
            String contactoRelacion, boolean autorizacionMenores, String tutorNombre, String tutorApellido,
            String tutorDni, String tutorTelefono) {
        if (dni.isEmpty() || !DNI_VALIDO.matcher(dni).matches()) {
            return "El DNI es obligatorio y debe contener entre 7 y 8 numeros.";
        }
        if (nombre.isEmpty() || apellido.isEmpty()) {
            return "El nombre y apellido son obligatorios.";
        }
        if (!NOMBRE_VALIDO.matcher(nombre).matches() || !NOMBRE_VALIDO.matcher(apellido).matches()) {
            return "El nombre y apellido solo pueden contener letras, espacios, guiones o apostrofes.";
        }
        if (nombre.length() > 50 || apellido.length() > 50) {
            return "El nombre y apellido no pueden superar los 50 caracteres.";
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
            return "La fecha de nacimiento es obligatoria y no puede ser futura.";
        }
        if (email.length() > 100 || (!email.isEmpty() && !EMAIL_VALIDO.matcher(email).matches())) {
            return "Ingresa un email valido de hasta 100 caracteres.";
        }
        if (telefono.isEmpty() || !TELEFONO_VALIDO.matcher(telefono).matches()) {
            return "El telefono es obligatorio y debe tener un formato valido.";
        }
        if (sexo == null || sexo.isEmpty()) {
            return "Selecciona el sexo del cliente.";
        }
        boolean contactoIncompleto = contactoNombre.isEmpty() || contactoTelefono.isEmpty()
                || contactoRelacion.isEmpty();
        boolean contactoIniciado = !contactoNombre.isEmpty() || !contactoTelefono.isEmpty()
                || !contactoRelacion.isEmpty();
        if (contactoIniciado && contactoIncompleto) {
            return "Completa nombre, telefono y relacion del contacto de emergencia.";
        }
        if (!contactoTelefono.isEmpty() && !TELEFONO_VALIDO.matcher(contactoTelefono).matches()) {
            return "El telefono del contacto de emergencia no es valido.";
        }
        boolean menor = Period.between(fechaNacimiento, LocalDate.now()).getYears() < 18;
        if (menor && !autorizacionMenores) {
            return "Para menores de 18 anos debes marcar la autorizacion de menores.";
        }
        if (menor && (tutorNombre.isEmpty() || tutorApellido.isEmpty() || tutorDni.isEmpty()
                || tutorTelefono.isEmpty())) {
            return "Para menores de 18 anos debes completar los datos del tutor.";
        }
        if (menor && (!NOMBRE_VALIDO.matcher(tutorNombre).matches()
                || !NOMBRE_VALIDO.matcher(tutorApellido).matches())) {
            return "El nombre y apellido del tutor no son validos.";
        }
        if (menor && !DNI_VALIDO.matcher(tutorDni).matches()) {
            return "El DNI del tutor debe contener entre 7 y 8 numeros.";
        }
        if (menor && !TELEFONO_VALIDO.matcher(tutorTelefono).matches()) {
            return "El telefono del tutor no tiene un formato valido.";
        }
        return null;
    }
}
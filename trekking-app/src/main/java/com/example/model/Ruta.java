package com.example.model;

import java.util.Locale;

public class Ruta {

    public static String normalizarNombre(String nombre) {
        return nombre == null
                ? ""
                : nombre.trim().replaceAll("\\s+", " ");
    }

    public static boolean nombreValido(String nombre) {
        return normalizarNombre(nombre)
                .matches("[\\p{L}\\p{N}]+(?: [\\p{L}\\p{N}]+)*");
    }

    public static String claveNombre(String nombre) {
        return normalizarNombre(nombre).toLowerCase(Locale.ROOT);
    }

    private int id;
    private String nombre;
    private double latitudInicial;
    private double longitudInicial;
    private double altitudMaxima;
    private String tipoTerreno;
    private String dificultadTecnica;
    private String dificultadFisica;

    // Constructor para crear una ruta nueva
    public Ruta(
            String nombre,
            double latitudInicial,
            double longitudInicial,
            double altitudMaxima,
            String tipoTerreno,
            String dificultadTecnica,
            String dificultadFisica) {

        this.nombre = nombre;
        this.latitudInicial = latitudInicial;
        this.longitudInicial = longitudInicial;
        this.altitudMaxima = altitudMaxima;
        this.tipoTerreno = tipoTerreno;
        this.dificultadTecnica = dificultadTecnica;
        this.dificultadFisica = dificultadFisica;
    }

    // Constructor para recuperar una ruta existente desde SQLite
    public Ruta(
            int id,
            String nombre,
            double latitudInicial,
            double longitudInicial,
            double altitudMaxima,
            String tipoTerreno,
            String dificultadTecnica,
            String dificultadFisica) {

        this.id = id;
        this.nombre = nombre;
        this.latitudInicial = latitudInicial;
        this.longitudInicial = longitudInicial;
        this.altitudMaxima = altitudMaxima;
        this.tipoTerreno = tipoTerreno;
        this.dificultadTecnica = dificultadTecnica;
        this.dificultadFisica = dificultadFisica;
    }

    // GETTERS

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public double getLatitudInicial() {
        return latitudInicial;
    }

    public double getLongitudInicial() {
        return longitudInicial;
    }

    public double getAltitudMaxima() {
        return altitudMaxima;
    }

    public String getTipoTerreno() {
        return tipoTerreno;
    }

    public String getDificultadTecnica() {
        return dificultadTecnica;
    }

    public String getDificultadFisica() {
        return dificultadFisica;
    }

    // SETTERS

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setLatitudInicial(double latitudInicial) {
        this.latitudInicial = latitudInicial;
    }

    public void setLongitudInicial(double longitudInicial) {
        this.longitudInicial = longitudInicial;
    }

    public void setAltitudMaxima(double altitudMaxima) {
        this.altitudMaxima = altitudMaxima;
    }

    public void setTipoTerreno(String tipoTerreno) {
        this.tipoTerreno = tipoTerreno;
    }

    public void setDificultadTecnica(String dificultadTecnica) {
        this.dificultadTecnica = dificultadTecnica;
    }

    public void setDificultadFisica(String dificultadFisica) {
        this.dificultadFisica = dificultadFisica;
    }
}
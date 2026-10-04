package com.example.model;

public class Equipamiento {

    private int id;
    private String nombre;
    private String categoria;
    private int cantidad;
    private String estado;

    public Equipamiento(int id, String nombre, String categoria,
                         int cantidad, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.cantidad = cantidad;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getEstado() {
        return estado;
    }
}
package com.example.model;

public class Equipamiento {

    private int id;
    private String nombre;
    private String categoria;
    private int cantidad;
    private int cantidadReservada;
    private String estado;

    public Equipamiento(int id, String nombre, String categoria,
                         int cantidad, String estado) {
        this(id, nombre, categoria, cantidad, estado, 0);
    }

    public Equipamiento(int id, String nombre, String categoria,
                         int cantidad, String estado, int cantidadReservada) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.cantidad = cantidad;
        this.estado = estado;
        this.cantidadReservada = cantidadReservada;
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

    public int getCantidadReservada() {
        return cantidadReservada;
    }

    public int getCantidadDisponible() {
        return cantidad - cantidadReservada;
    }
}

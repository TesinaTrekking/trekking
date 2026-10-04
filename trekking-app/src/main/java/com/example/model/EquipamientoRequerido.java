package com.example.model;

public class EquipamientoRequerido {

    private final Equipamiento equipamiento;
    private int cantidadRequerida;

    public EquipamientoRequerido(
            Equipamiento equipamiento,
            int cantidadRequerida) {

        this.equipamiento = equipamiento;
        this.cantidadRequerida = cantidadRequerida;
    }

    public Equipamiento getEquipamiento() {
        return equipamiento;
    }

    public int getCantidadRequerida() {
        return cantidadRequerida;
    }

    public void setCantidadRequerida(int cantidadRequerida) {
        this.cantidadRequerida = cantidadRequerida;
    }
}
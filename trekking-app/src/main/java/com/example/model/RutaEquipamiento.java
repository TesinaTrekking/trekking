package com.example.model;

public class RutaEquipamiento {

    private int rutaId;
    private int equipamientoId;
    private int cantidadRequerida;

    public RutaEquipamiento(int rutaId, int equipamientoId,
                            int cantidadRequerida) {
        this.rutaId = rutaId;
        this.equipamientoId = equipamientoId;
        this.cantidadRequerida = cantidadRequerida;
    }

    public int getRutaId() {
        return rutaId;
    }

    public int getEquipamientoId() {
        return equipamientoId;
    }

    public int getCantidadRequerida() {
        return cantidadRequerida;
    }
}
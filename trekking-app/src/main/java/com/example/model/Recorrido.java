package com.example.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Recorrido {

    private final int id;
    private final int rutaId;
    private final String nombreRuta;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;

    public Recorrido(
            int id,
            int rutaId,
            String nombreRuta,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin) {
        this.id = id;
        this.rutaId = rutaId;
        this.nombreRuta = nombreRuta;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public int getId() {
        return id;
    }

    public int getRutaId() {
        return rutaId;
    }

    public String getNombreRuta() {
        return nombreRuta;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }
}

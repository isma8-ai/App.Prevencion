package com.example.hackeatonbackend;

import com.google.android.gms.maps.model.LatLng;

public class ReporteMarcador {
    public final String id;
    public final String titulo;
    public final LatLng posicion;
    public final String usuario;
    public final String fecha;
    public final String comentario;

    public ReporteMarcador(String id, String titulo, LatLng posicion, String usuario, String fecha, String comentario) {
        this.id = id;
        this.titulo = titulo;
        this.posicion = posicion;
        this.usuario = usuario;
        this.fecha = fecha;
        this.comentario = comentario;
    }
}
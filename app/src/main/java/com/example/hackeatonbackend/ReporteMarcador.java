package com.example.hackeatonbackend;

import com.google.android.gms.maps.model.LatLng;

public class ReporteMarcador {
    public final String id;
    public final String titulo;
    public final LatLng posicion;
    public final String usuario;
    public final String fecha;
    public final String comentario;
    /** Momento (ms desde epoch) en que la marca desaparece. 0 = no expira. */
    public final long expiraEn;
    /** true si es una marca temporal creada desde una alerta SOS (se dibuja en morado). */
    public final boolean esSos;

    public ReporteMarcador(String id, String titulo, LatLng posicion, String usuario, String fecha, String comentario) {
        this(id, titulo, posicion, usuario, fecha, comentario, 0L, false);
    }

    public ReporteMarcador(String id, String titulo, LatLng posicion, String usuario, String fecha,
                           String comentario, long expiraEn, boolean esSos) {
        this.id = id;
        this.titulo = titulo;
        this.posicion = posicion;
        this.usuario = usuario;
        this.fecha = fecha;
        this.comentario = comentario;
        this.expiraEn = expiraEn;
        this.esSos = esSos;
    }

    public boolean haExpirado() {
        return expiraEn > 0 && System.currentTimeMillis() >= expiraEn;
    }
}
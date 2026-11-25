package com.example.hackeatonbackend;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Registro local del historial de actividad. Guarda los eventos en SharedPreferences (JSON).
 * Uso desde cualquier Activity:
 *   HistorialManager.registrar(this, HistorialManager.TIPO_SOS, "Alerta SOS activada");
 */
public class HistorialManager {

    public static final String TIPO_LOGIN = "LOGIN";
    public static final String TIPO_MAPA = "MAPA";
    public static final String TIPO_SOS = "SOS";
    public static final String TIPO_CHAT = "CHAT";
    public static final String TIPO_PERFIL = "PERFIL";

    private static final String PREF_NAME = "HistorialPrefs";
    private static final String KEY_EVENTOS = "eventos";
    private static final int MAX_EVENTOS = 200;

    public static class Evento {
        public final String tipo;
        public final String descripcion;
        public final String fecha;

        public Evento(String tipo, String descripcion, String fecha) {
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.fecha = fecha;
        }

        public String getIcono() {
            switch (tipo) {
                case TIPO_LOGIN:  return "🔐";
                case TIPO_MAPA:   return "📍";
                case TIPO_SOS:    return "🚨";
                case TIPO_CHAT:   return "💬";
                case TIPO_PERFIL: return "👤";
                default:          return "📝";
            }
        }

        public String getTitulo() {
            switch (tipo) {
                case TIPO_LOGIN:  return "Inicio de sesión";
                case TIPO_MAPA:   return "Punto en el mapa";
                case TIPO_SOS:    return "Llamada SOS";
                case TIPO_CHAT:   return "Chat anónimo";
                case TIPO_PERFIL: return "Cambios en el perfil";
                default:          return "Evento";
            }
        }
    }

    public static synchronized void registrar(Context context, String tipo, String descripcion) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        try {
            JSONArray actual = new JSONArray(prefs.getString(KEY_EVENTOS, "[]"));

            // El más reciente va primero
            JSONArray nuevo = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("tipo", tipo);
            obj.put("descripcion", descripcion);
            obj.put("fecha", fecha);
            nuevo.put(obj);

            for (int i = 0; i < actual.length() && nuevo.length() < MAX_EVENTOS; i++) {
                nuevo.put(actual.getJSONObject(i));
            }
            prefs.edit().putString(KEY_EVENTOS, nuevo.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<Evento> obtener(Context context) {
        List<Evento> lista = new ArrayList<>();
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_EVENTOS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                lista.add(new Evento(o.getString("tipo"), o.getString("descripcion"), o.getString("fecha")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static synchronized void limpiar(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_EVENTOS).apply();
    }
}
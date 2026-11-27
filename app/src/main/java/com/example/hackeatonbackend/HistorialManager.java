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

        SharedPreferences sessionPrefs = context.getApplicationContext()
                .getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        String usuarioActual = sessionPrefs.getString("usuarioSesion", sessionPrefs.getString("username", ""));

        guardarEnClave(prefs, KEY_EVENTOS, tipo, descripcion, fecha);
        if (usuarioActual != null && !usuarioActual.isEmpty()) {
            guardarEnClave(prefs, "eventos_user_" + usuarioActual.toLowerCase(Locale.ROOT), tipo, descripcion, fecha);
        }
    }

    public static synchronized void registrarParaUsuario(Context context, String username, String tipo, String descripcion) {
        if (username == null || username.isEmpty()) return;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        guardarEnClave(prefs, "eventos_user_" + username.toLowerCase(Locale.ROOT), tipo, descripcion, fecha);
    }

    private static void guardarEnClave(SharedPreferences prefs, String clave, String tipo, String descripcion, String fecha) {
        try {
            JSONArray actual = new JSONArray(prefs.getString(clave, "[]"));
            JSONArray nuevo = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("tipo", tipo);
            obj.put("descripcion", descripcion);
            obj.put("fecha", fecha);
            nuevo.put(obj);

            for (int i = 0; i < actual.length() && nuevo.length() < MAX_EVENTOS; i++) {
                nuevo.put(actual.getJSONObject(i));
            }
            prefs.edit().putString(clave, nuevo.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<Evento> obtener(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return leerDesdeClave(prefs, KEY_EVENTOS);
    }

    public static synchronized List<Evento> obtenerPorUsuario(Context context, String username) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<Evento> propios = leerDesdeClave(prefs, "eventos_user_" + username.toLowerCase(Locale.ROOT));
        if (!propios.isEmpty()) return propios;

        List<Evento> filtrados = new ArrayList<>();
        for (Evento e : leerDesdeClave(prefs, KEY_EVENTOS)) {
            if (e.descripcion != null && e.descripcion.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
                filtrados.add(e);
            }
        }
        return filtrados;
    }

    private static List<Evento> leerDesdeClave(SharedPreferences prefs, String clave) {
        List<Evento> lista = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(clave, "[]"));
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
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

    private static String obtenerUsuarioActivo(Context context) {
        SharedPreferences sessionPrefs = context.getApplicationContext()
                .getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        String usuario = sessionPrefs.getString("usuarioSesion", "");
        if (usuario == null || usuario.trim().isEmpty()) {
            usuario = sessionPrefs.getString("username", "");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            usuario = sessionPrefs.getString("protectorName", "");
        }
        return usuario != null ? usuario.trim() : "";
    }

    private static String claveUsuario(String username) {
        return "eventos_user_" + username.trim().toLowerCase(Locale.ROOT);
    }

    public static synchronized void registrar(Context context, String tipo, String descripcion) {
        String usuarioActual = obtenerUsuarioActivo(context);
        if (usuarioActual.isEmpty()) return;
        registrarParaUsuario(context, usuarioActual, tipo, descripcion);
    }

    public static synchronized void registrarParaUsuario(Context context, String username, String tipo, String descripcion) {
        if (username == null || username.trim().isEmpty()) return;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        guardarEnClave(prefs, claveUsuario(username), tipo, descripcion, fecha);
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
        String usuarioActual = obtenerUsuarioActivo(context);
        if (usuarioActual.isEmpty()) return new ArrayList<>();
        return obtenerPorUsuario(context, usuarioActual);
    }

    public static synchronized List<Evento> obtenerPorUsuario(Context context, String username) {
        if (username == null || username.trim().isEmpty()) return new ArrayList<>();
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return leerDesdeClave(prefs, claveUsuario(username));
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
        String usuarioActual = obtenerUsuarioActivo(context);
        if (usuarioActual.isEmpty()) return;
        limpiarPorUsuario(context, usuarioActual);
    }

    public static synchronized void limpiarPorUsuario(Context context, String username) {
        if (username == null || username.trim().isEmpty()) return;
        context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().remove(claveUsuario(username)).apply();
    }
}
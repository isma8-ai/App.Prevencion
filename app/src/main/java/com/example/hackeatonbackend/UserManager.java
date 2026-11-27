package com.example.hackeatonbackend;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private static final String PREF_NAME = "CentinelaUsersPrefs";
    private static final String KEY_USERS = "users_list";

    public static class Usuario {
        public String username;
        public String password;
        public String role; // "USUARIA" o "PROTECTOR"
        public String fullName;
        public String uniqueId; // ID aleatorio y único generado automáticamente

        public Usuario(String username, String password, String role, String fullName, String uniqueId) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.fullName = fullName;
            this.uniqueId = uniqueId;
        }
    }

    public static synchronized void inicializarUsuariosPorDefecto(Context context) {
        List<Usuario> lista = obtenerUsuarios(context);
        if (lista.isEmpty()) {
            // Usuario de prueba predeterminado "User" con contraseña "123123"
            lista.add(new Usuario("User", "123123", "USUARIA", "Usuaria Demo", "CENTINELA-888888"));
            guardarUsuarios(context, lista);
        }
    }

    public static synchronized List<Usuario> obtenerUsuarios(Context context) {
        List<Usuario> lista = new ArrayList<>();
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_USERS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                lista.add(new Usuario(
                        o.getString("username"),
                        o.getString("password"),
                        o.optString("role", "USUARIA"),
                        o.optString("fullName", "Usuario"),
                        o.optString("uniqueId", "CENTINELA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static synchronized Usuario registrarNuevaUsuaria(Context context, String username, String password, String fullName) {
        inicializarUsuariosPorDefecto(context);
        List<Usuario> lista = obtenerUsuarios(context);

        // Generar un ID único aleatorio automáticamente sin que la usuaria tenga que escribirlo
        String uniqueId = "CENTINELA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Usuario nuevo = new Usuario(username, password, "USUARIA", fullName, uniqueId);
        lista.add(nuevo);
        guardarUsuarios(context, lista);
        return nuevo;
    }

    public static synchronized Usuario validarLogin(Context context, String username, String password) {
        inicializarUsuariosPorDefecto(context);
        List<Usuario> lista = obtenerUsuarios(context);
        for (Usuario u : lista) {
            if (u.username.equals(username) && u.password.equals(password)) {
                return u;
            }
        }
        return null;
    }

    public static synchronized Usuario buscarPorUniqueId(Context context, String uniqueId) {
        inicializarUsuariosPorDefecto(context);
        List<Usuario> lista = obtenerUsuarios(context);
        for (Usuario u : lista) {
            if (u.uniqueId.equalsIgnoreCase(uniqueId) || u.username.equalsIgnoreCase(uniqueId)) {
                return u;
            }
        }
        return null;
    }

    private static synchronized void guardarUsuarios(Context context, List<Usuario> lista) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray array = new JSONArray();
            for (Usuario u : lista) {
                JSONObject o = new JSONObject();
                o.put("username", u.username);
                o.put("password", u.password);
                o.put("role", u.role);
                o.put("fullName", u.fullName);
                o.put("uniqueId", u.uniqueId);
                array.put(o);
            }
            prefs.edit().putString(KEY_USERS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

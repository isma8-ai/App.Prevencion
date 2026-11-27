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
        public String role;
        public String fullName;
        public String uniqueId;
        public String phone;
        public String dob;
        public String email;
        public String bloodType;
        public String allergies;
        public String medicalInfo;
        public double lat;
        public double lng;

        public Usuario(String username, String password, String role, String fullName, String uniqueId) {
            this(username, password, role, fullName, uniqueId, "", "", "", "", "", "",
                    MapaDatos.CENTRO_DEFAULT.latitude, MapaDatos.CENTRO_DEFAULT.longitude);
        }

        public Usuario(String username, String password, String role, String fullName, String uniqueId,
                       String phone, String dob, String email, String bloodType, String allergies,
                       String medicalInfo, double lat, double lng) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.fullName = fullName;
            this.uniqueId = uniqueId;
            this.phone = phone;
            this.dob = dob;
            this.email = email;
            this.bloodType = bloodType;
            this.allergies = allergies;
            this.medicalInfo = medicalInfo;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public static synchronized void inicializarUsuariosPorDefecto(Context context) {
        List<Usuario> lista = obtenerUsuarios(context);
        boolean changed = false;

        boolean hasUser = false;
        boolean hasAdmin = false;
        boolean hasProtector = false;
        for (Usuario u : lista) {
            if ("User".equalsIgnoreCase(u.username)) hasUser = true;
            if ("ADMIN".equalsIgnoreCase(u.role) || "Admin".equalsIgnoreCase(u.username)) hasAdmin = true;
            if ("PROTECTOR".equalsIgnoreCase(u.role) || "Protector".equalsIgnoreCase(u.username)) hasProtector = true;
        }

        if (!hasUser) {
            lista.add(new Usuario("User", "123123", "USUARIA", "Usuaria Demo", "CENTINELA-888888",
                    "6681000001", "15/05/1999", "user@centinela.mx", "O+", "Ninguna", "Ninguna",
                    25.7912, -108.9980));
            changed = true;
        }

        if (!hasAdmin) {
            lista.add(new Usuario("Admin", "admin123", "ADMIN", "Administrador Centinela", "CENTINELA-ADM001",
                    "6681000000", "01/01/1990", "admin@centinela.mx", "A+", "Ninguna", "Administrador del sistema",
                    MapaDatos.CENTRO_DEFAULT.latitude, MapaDatos.CENTRO_DEFAULT.longitude));
            changed = true;
        }

        if (!hasProtector) {
            lista.add(new Usuario("Protector", "", "PROTECTOR", "Protector Demo", "CENTINELA-PRT001",
                    "6681000002", "10/08/1995", "protector@centinela.mx", "O+", "Ninguna", "Protector registrado",
                    MapaDatos.CENTRO_DEFAULT.latitude, MapaDatos.CENTRO_DEFAULT.longitude));
            changed = true;
        }

        if (changed) {
            guardarUsuarios(context, lista);
        }
    }

    public static synchronized Usuario registrarProtector(Context context, String nombreProtector, String idProtegido) {
        if (nombreProtector == null || nombreProtector.trim().isEmpty()) return null;
        inicializarUsuariosPorDefecto(context);
        List<Usuario> lista = obtenerUsuarios(context);

        String limpio = nombreProtector.trim();
        for (Usuario u : lista) {
            if (u.username.equalsIgnoreCase(limpio) || u.fullName.equalsIgnoreCase(limpio)) {
                u.role = "PROTECTOR";
                if (u.medicalInfo == null || u.medicalInfo.isEmpty()) {
                    u.medicalInfo = "Vinculado a: " + idProtegido;
                }
                guardarUsuarios(context, lista);
                return u;
            }
        }

        String uniqueId = "CENTINELA-PRT-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Usuario nuevo = new Usuario(
                limpio,
                "",
                "PROTECTOR",
                limpio,
                uniqueId,
                "",
                "",
                "",
                "",
                "",
                "Vinculado a: " + idProtegido,
                MapaDatos.CENTRO_DEFAULT.latitude,
                MapaDatos.CENTRO_DEFAULT.longitude
        );
        lista.add(nuevo);
        guardarUsuarios(context, lista);
        return nuevo;
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
                        o.optString("uniqueId", "CENTINELA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()),
                        o.optString("phone", ""),
                        o.optString("dob", ""),
                        o.optString("email", ""),
                        o.optString("bloodType", ""),
                        o.optString("allergies", ""),
                        o.optString("medicalInfo", ""),
                        o.optDouble("lat", MapaDatos.CENTRO_DEFAULT.latitude),
                        o.optDouble("lng", MapaDatos.CENTRO_DEFAULT.longitude)
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

        String uniqueId = "CENTINELA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Usuario nuevo = new Usuario(username, password, "USUARIA", fullName, uniqueId);
        lista.add(nuevo);
        guardarUsuarios(context, lista);
        return nuevo;
    }

    public static synchronized void actualizarPerfilUsuario(Context context, String username, String fullName,
                                                            String phone, String dob, String email,
                                                            String bloodType, String allergies, String medicalInfo) {
        if (username == null || username.isEmpty()) return;
        inicializarUsuariosPorDefecto(context);
        List<Usuario> lista = obtenerUsuarios(context);
        for (Usuario u : lista) {
            if (u.username.equalsIgnoreCase(username)) {
                if (fullName != null && !fullName.isEmpty()) u.fullName = fullName;
                u.phone = phone != null ? phone : "";
                u.dob = dob != null ? dob : "";
                u.email = email != null ? email : "";
                u.bloodType = bloodType != null ? bloodType : "";
                u.allergies = allergies != null ? allergies : "";
                u.medicalInfo = medicalInfo != null ? medicalInfo : "";
                break;
            }
        }
        guardarUsuarios(context, lista);
    }

    public static synchronized void actualizarUbicacionUsuario(Context context, String username, double lat, double lng) {
        if (username == null || username.isEmpty()) return;
        List<Usuario> lista = obtenerUsuarios(context);
        for (Usuario u : lista) {
            if (u.username.equalsIgnoreCase(username)) {
                u.lat = lat;
                u.lng = lng;
                break;
            }
        }
        guardarUsuarios(context, lista);
    }

    public static synchronized Usuario buscarPorUsername(Context context, String username) {
        if (username == null) return null;
        inicializarUsuariosPorDefecto(context);
        for (Usuario u : obtenerUsuarios(context)) {
            if (u.username.equalsIgnoreCase(username)) {
                return u;
            }
        }
        return null;
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
                o.put("phone", u.phone);
                o.put("dob", u.dob);
                o.put("email", u.email);
                o.put("bloodType", u.bloodType);
                o.put("allergies", u.allergies);
                o.put("medicalInfo", u.medicalInfo);
                o.put("lat", u.lat);
                o.put("lng", u.lng);
                array.put(o);
            }
            prefs.edit().putString(KEY_USERS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
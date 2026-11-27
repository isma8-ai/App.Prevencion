package com.example.hackeatonbackend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SesionRolHelper {

    public static final String PREF_NAME = "CentinelaPrefs";
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_PROTECTOR = "PROTECTOR";
    public static final String ROL_USUARIA = "USUARIA";

    private SesionRolHelper() {}

    public static boolean esAdmin(Context ctx) {
        SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean("isAdminMode", false)) return true;
        return ROL_ADMIN.equalsIgnoreCase(prefs.getString("userRole", ""));
    }

    public static boolean esProtector(Context ctx) {
        SharedPreferences prefs = ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (esAdmin(ctx)) return false;
        if (prefs.getBoolean("isProtectorMode", false)) return true;
        return ROL_PROTECTOR.equalsIgnoreCase(prefs.getString("userRole", ""));
    }

    public static void guardarRol(Context ctx, String rol, String username) {
        String rolNormalizado = rol != null ? rol.toUpperCase() : ROL_USUARIA;
        boolean isAdmin = ROL_ADMIN.equals(rolNormalizado);
        boolean isProtector = ROL_PROTECTOR.equals(rolNormalizado);

        SharedPreferences.Editor editor = ctx.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("userRole", rolNormalizado)
                .putBoolean("isAdminMode", isAdmin)
                .putBoolean("isProtectorMode", isProtector);

        if (username != null && !username.isEmpty()) {
            editor.putString("usuarioSesion", username);
            editor.putString("username", username);
        }
        editor.apply();
    }

    public static boolean redirigirSiEsAdmin(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;
        if (activity instanceof AdminMenuActivity) return false;
        if (esAdmin(activity)) {
            Intent intent = new Intent(activity, AdminMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            activity.finish();
            return true;
        }
        return false;
    }

    public static Intent intentMenuPrincipal(Context ctx) {
        if (esAdmin(ctx)) {
            return new Intent(ctx, AdminMenuActivity.class);
        }
        return new Intent(ctx, MainActivity.class);
    }
}
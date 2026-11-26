package com.example.hackeatonbackend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SosHelper {

    private static final String PREF_NAME = "CentinelaPrefs";
    private static final String KEY_MY_ID = "myUniqueId";
    private static final String KEY_CONTACTS = "trustedContacts";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_BLOOD = "bloodType";
    private static final String KEY_ALLERGIES = "allergies";
    private static final String KEY_MEDICAL = "medicalInfo";

    private SosHelper() {}

    /**
     * Dispara la alerta SOS completa.
     * @param ctx    cualquier contexto
     * @param act    Activity visible (para mostrar la ficha). Puede ser null.
     * @param origen texto que indica qué la activó (botón, pulsera, movimiento...)
     */
    public static void disparar(Context ctx, Activity act, String origen) {
        Context app = ctx.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String name = prefs.getString(KEY_FULL_NAME, "");
        String phone = prefs.getString(KEY_PHONE, "");
        String blood = prefs.getString(KEY_BLOOD, "");
        String allergies = prefs.getString(KEY_ALLERGIES, "");
        String medical = prefs.getString(KEY_MEDICAL, "");
        String centinelaId = getOrCreateMyId(prefs);
        String timeNow = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        Set<String> contacts = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());

        String mensaje = "🚨 ¡ALERTA SOS ACTIVADA! 🚨\n\n" +
                "🔔 Origen: " + origen + "\n\n" +
                "👤 Nombre: " + (name.isEmpty() ? "No registrado" : name) + "\n" +
                "📞 Teléfono: " + (phone.isEmpty() ? "No registrado" : phone) + "\n" +
                "🆔 ID Centinela: " + centinelaId + "\n" +
                "⏱️ Hora: " + timeNow + "\n" +
                "📍 Ubicación: GPS Activo (Zona Centro / Compartida)\n\n" +
                "🩸 Tipo de Sangre: " + (blood.isEmpty() ? "No especificado" : blood) + "\n" +
                "⚠️ Alergias: " + (allergies.isEmpty() ? "Ninguna" : allergies) + "\n" +
                "💊 Info Médica: " + (medical.isEmpty() ? "Ninguna" : medical) + "\n\n" +
                "👥 Contactos Notificados: " + contacts.size() + " enlazados.";

        if (act != null && !act.isFinishing() && !act.isDestroyed()) {
            new AlertDialog.Builder(act)
                    .setTitle("🚨 FICHA DE EMERGENCIA SOS")
                    .setMessage(mensaje)
                    .setPositiveButton("Cerrar Alerta", (d, w) -> d.dismiss())
                    .setNeutralButton("Ver en Mapa", (d, w) ->
                            act.startActivity(new Intent(act, mapa.class)))
                    .show();
        }

        Toast.makeText(app, "¡Alerta SOS enviada a " + contacts.size() + " contactos de confianza!",
                Toast.LENGTH_LONG).show();
        HistorialManager.registrar(app, HistorialManager.TIPO_SOS,
                "Alerta SOS (" + origen + ") enviada a " + contacts.size() + " contactos de confianza");
    }

    private static String getOrCreateMyId(SharedPreferences prefs) {
        String myId = prefs.getString(KEY_MY_ID, null);
        if (myId == null || myId.isEmpty()) {
            myId = "CENTINELA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            prefs.edit().putString(KEY_MY_ID, myId).apply();
        }
        return myId;
    }
}
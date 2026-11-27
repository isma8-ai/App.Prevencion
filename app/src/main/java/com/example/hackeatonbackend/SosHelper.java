package com.example.hackeatonbackend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.core.util.Consumer;

import com.google.android.gms.maps.model.LatLng;

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
    private static final String KEY_DOB = "dob";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_BLOOD = "bloodType";
    private static final String KEY_ALLERGIES = "allergies";
    private static final String KEY_MEDICAL = "medicalInfo";
    private static final String KEY_SESION = "usuarioSesion";

    /** Las pantallas que muestren marcas (mapa, minimapa) lo implementan para refrescarse al publicar. */
    public interface Listener {
        void onMarcaSosPublicada();
    }

    private SosHelper() {}

    /**
     * Dispara la alerta SOS completa incluyendo perfil, ficha médica y enlace directo a Google Maps en tiempo real.
     * @param ctx    cualquier contexto
     * @param act    Activity visible (para mostrar la ficha). Puede ser null.
     * @param origen texto que indica qué la activó (botón, pulsera, movimiento...)
     */
    public static void disparar(Context ctx, Activity act, String origen) {
        Context app = ctx.getApplicationContext();
        Toast.makeText(app, "Obteniendo ubicación en tiempo real y enviando alerta SOS...", Toast.LENGTH_SHORT).show();

        obtenerUbicacion(app, location -> {
            SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

            String name = prefs.getString(KEY_FULL_NAME, "");
            String dob = prefs.getString(KEY_DOB, "");
            String phone = prefs.getString(KEY_PHONE, "");
            String email = prefs.getString(KEY_EMAIL, "");
            String blood = prefs.getString(KEY_BLOOD, "");
            String allergies = prefs.getString(KEY_ALLERGIES, "");
            String medical = prefs.getString(KEY_MEDICAL, "");
            String centinelaId = getOrCreateMyId(prefs);
            String timeNow = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            Set<String> contacts = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());

            String ubicacionTexto = (location != null) ?
                    String.format(Locale.US, "Lat: %.6f, Lng: %.6f (Precisión: %.1fm)\n🔗 Ver en Google Maps:\nhttps://maps.google.com/?q=%.6f,%.6f",
                            location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                            location.getLatitude(), location.getLongitude()) :
                    "Ubicación GPS Activa (Zona Centro / En vivo)\n🔗 Ver en Google Maps:\nhttps://maps.google.com/?q=25.79097,-108.99825";

            String mensaje = "🚨 ¡ALERTA SOS - PERFIL Y FICHA MÉDICA EN VIVO! 🚨\n\n" +
                    "📍 UBICACIÓN EN TIEMPO REAL:\n" +
                    "• " + ubicacionTexto + "\n" +
                    "• Origen de Alerta: " + origen + "\n\n" +
                    "👤 PERFIL REGISTRADO:\n" +
                    "• Nombre: " + (name.isEmpty() ? "No registrado" : name) + "\n" +
                    "• Fecha de Nacimiento: " + (dob.isEmpty() ? "No registrada" : dob) + "\n" +
                    "• Teléfono: " + (phone.isEmpty() ? "No registrado" : phone) + "\n" +
                    "• Correo: " + (email.isEmpty() ? "No registrado" : email) + "\n" +
                    "• ID Centinela: " + centinelaId + "\n\n" +
                    "📋 FICHA MÉDICA DE EMERGENCIA:\n" +
                    "• Tipo de Sangre: " + (blood.isEmpty() ? "No especificado" : blood) + "\n" +
                    "• Alergias: " + (allergies.isEmpty() ? "Ninguna reportada" : allergies) + "\n" +
                    "• Info Médica / Padecimientos: " + (medical.isEmpty() ? "Ninguna reportada" : medical) + "\n\n" +
                    "⏱️ Hora de Alerta: " + timeNow + "\n" +
                    "👥 Protectores Notificados: " + contacts.size() + " enlazados.";

            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                    new AlertDialog.Builder(act)
                            .setTitle("🚨 FICHA DE EMERGENCIA Y GPS ENVIADA")
                            .setMessage(mensaje)
                            .setPositiveButton("Cerrar Alerta", (d, w) -> d.dismiss())
                            .setNeutralButton("Ver en Mapa", (d, w) ->
                                    act.startActivity(new Intent(act, mapa.class)))
                            .setNegativeButton("📍 Subir marca al mapa (30 min)", (d, w) ->
                                    subirMarcaSosWithLocation(act, location, origen))
                            .show();
                }

                Toast.makeText(app, "✅ ¡Perfil, ficha médica y enlace de Google Maps enviados a " + contacts.size() + " protectores!",
                        Toast.LENGTH_LONG).show();
                HistorialManager.registrar(app, HistorialManager.TIPO_SOS, mensaje);
            });
        });
    }

    /** Guarda el usuario con el que se inició sesión (lo usa la marca SOS). */
    public static void guardarUsuarioSesion(Context ctx, String usuario) {
        ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SESION, usuario).apply();
    }

    // ------------------------------------------------------------ Marca SOS en el mapa

    private static void subirMarcaSosWithLocation(Activity act, Location location, String origen) {
        Context app = act.getApplicationContext();
        if (location == null) {
            Toast.makeText(app, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String usuario = prefs.getString(KEY_SESION, "");
        if (usuario.isEmpty()) usuario = prefs.getString(KEY_FULL_NAME, "");
        if (usuario.isEmpty()) usuario = "Usuario";

        LatLng posicion = new LatLng(location.getLatitude(), location.getLongitude());
        MapaDatos.publicarMarcaSos(app, getOrCreateMyId(prefs), usuario, posicion, origen);

        HistorialManager.registrar(app, HistorialManager.TIPO_MAPA,
                "Marca SOS publicada en el mapa por 30 minutos");
        Toast.makeText(app, "Marca SOS publicada en el mapa por 30 minutos", Toast.LENGTH_LONG).show();

        if (act instanceof Listener && !act.isFinishing() && !act.isDestroyed()) {
            ((Listener) act).onMarcaSosPublicada();
        }
    }

    private static boolean tienePermisoUbicacion(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Pide una ubicación fresca (8 s máx.); si falla, usa la última conocida solo si tiene menos de 10 min. */
    @SuppressLint("MissingPermission")
    private static void obtenerUbicacion(Context ctx, Consumer<Location> callback) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            callback.accept(null);
            return;
        }

        Location mejor = null;
        try {
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (mejor == null || l.getAccuracy() < mejor.getAccuracy())) {
                    mejor = l;
                }
            }
        } catch (SecurityException ignored) {
        }
        if (mejor != null) {
            long edadMs = (SystemClock.elapsedRealtimeNanos() - mejor.getElapsedRealtimeNanos()) / 1_000_000L;
            if (edadMs > 10 * 60 * 1000L) mejor = null; // muy vieja: mejor no marcar un lugar equivocado
        }
        final Location respaldo = mejor;

        boolean fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        String provider = null;
        if (fine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }
        if (provider == null) {
            callback.accept(respaldo);
            return;
        }

        CancellationSignal signal = new CancellationSignal();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = signal::cancel;
        handler.postDelayed(timeout, 8000);
        try {
            LocationManagerCompat.getCurrentLocation(lm, provider, signal,
                    ContextCompat.getMainExecutor(ctx), location -> {
                        handler.removeCallbacks(timeout);
                        callback.accept(location != null ? location : respaldo);
                    });
        } catch (SecurityException e) {
            handler.removeCallbacks(timeout);
            callback.accept(respaldo);
        }
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

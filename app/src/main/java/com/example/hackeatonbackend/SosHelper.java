package com.example.hackeatonbackend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.core.util.Consumer;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

    public static final float RADIO_ALERTA_SOS_METROS = 300f;
    private static final String CHANNEL_SOS_ID = "centinela_sos_channel";

    public interface Listener {
        void onMarcaSosPublicada();
    }

    private SosHelper() {}

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

            String usuarioSesion = prefs.getString(KEY_SESION, "");
            if (location != null && !usuarioSesion.isEmpty()) {
                UserManager.actualizarUbicacionUsuario(app, usuarioSesion, location.getLatitude(), location.getLongitude());
            }

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

            int smsEnviados = enviarSmsAContactos(app, act, contacts, mensaje);

            Handler mainHandler = new Handler(Looper.getMainLooper());
            final int totalSms = smsEnviados;
            mainHandler.post(() -> {
                if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                    new AlertDialog.Builder(act)
                            .setTitle("🚨 FICHA DE EMERGENCIA Y GPS ENVIADA")
                            .setMessage(mensaje + "\n\n📲 SMS enviados a contactos: " + totalSms)
                            .setPositiveButton("Cerrar Alerta", (d, w) -> d.dismiss())
                            .setNeutralButton("Ver en Mapa", (d, w) ->
                                    act.startActivity(new Intent(act, mapa.class)))
                            .setNegativeButton("📍 Subir marca al mapa (30 min)", (d, w) ->
                                    subirMarcaSosWithLocation(act, location, origen))
                            .show();
                }

                Toast.makeText(app, "✅ Alerta SOS enviada. SMS a contactos: " + totalSms,
                        Toast.LENGTH_LONG).show();
                HistorialManager.registrar(app, HistorialManager.TIPO_SOS, mensaje);
            });
        });
    }

    private static int enviarSmsAContactos(Context app, Activity act, Set<String> contacts, String mensaje) {
        if (contacts == null || contacts.isEmpty()) return 0;

        if (ContextCompat.checkSelfPermission(app, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.SEND_SMS}, 1091);
            }
            return 0;
        }

        SmsManager smsManager = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? app.getSystemService(SmsManager.class)
                : SmsManager.getDefault();
        if (smsManager == null) return 0;

        Set<String> numerosDestino = new HashSet<>();
        for (String contacto : contacts) {
            String telefono = resolverTelefonoContacto(app, contacto);
            if (telefono != null && !telefono.isEmpty()) {
                numerosDestino.add(telefono);
            }
        }

        int enviados = 0;
        ArrayList<String> partes = smsManager.divideMessage(mensaje);
        for (String numero : numerosDestino) {
            try {
                smsManager.sendMultipartTextMessage(numero, null, partes, null, null);
                enviados++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return enviados;
    }

    private static String resolverTelefonoContacto(Context app, String contacto) {
        if (contacto == null) return null;
        String limpio = contacto.replaceAll("\\s*\\(.*\\)", "").trim();
        String soloDigitos = limpio.replaceAll("[^0-9+]", "");
        if (soloDigitos.length() >= 7) {
            return soloDigitos;
        }
        for (UserManager.Usuario u : UserManager.obtenerUsuarios(app)) {
            if (u.username.equalsIgnoreCase(limpio) || u.fullName.equalsIgnoreCase(limpio) || u.uniqueId.equalsIgnoreCase(limpio)) {
                if (u.phone != null && !u.phone.trim().isEmpty()) {
                    return u.phone.replaceAll("[^0-9+]", "");
                }
            }
        }
        return null;
    }

    public static void guardarUsuarioSesion(Context ctx, String usuario) {
        ctx.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SESION, usuario).apply();
    }

    private static void subirMarcaSosWithLocation(Activity act, Location location, String origen) {
        Context app = act.getApplicationContext();
        LatLng posicion = (location != null)
                ? new LatLng(location.getLatitude(), location.getLongitude())
                : MapaDatos.CENTRO_DEFAULT;

        SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String usuario = prefs.getString(KEY_SESION, "");
        if (usuario.isEmpty()) usuario = prefs.getString(KEY_FULL_NAME, "");
        if (usuario.isEmpty()) usuario = "Usuario";

        String myId = getOrCreateMyId(prefs);
        MapaDatos.publicarMarcaSos(app, myId, usuario, posicion, origen);

        if (!usuario.isEmpty()) {
            UserManager.actualizarUbicacionUsuario(app, usuario, posicion.latitude, posicion.longitude);
        }

        int usuariasNotificadas = notificarUsuariasComunesEnRadio(app, posicion, usuario, myId);

        String msgConteo = "Marca SOS publicada por 30 min. " + usuariasNotificadas
                + (usuariasNotificadas == 1
                ? " usuaria común en un radio de 300 m ha recibido tu alerta."
                : " usuarias comunes en un radio de 300 m han recibido tu alerta.");

        HistorialManager.registrar(app, HistorialManager.TIPO_MAPA, msgConteo);
        Toast.makeText(app, msgConteo, Toast.LENGTH_LONG).show();

        if (act != null && !act.isFinishing() && !act.isDestroyed()) {
            new AlertDialog.Builder(act)
                    .setTitle("📡 Alcance de Alerta SOS (300 m)")
                    .setMessage(msgConteo)
                    .setPositiveButton("Aceptar", null)
                    .show();

            if (act instanceof Listener) {
                ((Listener) act).onMarcaSosPublicada();
            }
        }
    }

    private static int notificarUsuariasComunesEnRadio(Context app, LatLng marcaSos, String emisorUsername, String emisorId) {
        crearCanalNotificacionSos(app);
        int recibidas = 0;

        for (UserManager.Usuario u : UserManager.obtenerUsuarios(app)) {
            if (!"USUARIA".equalsIgnoreCase(u.role)) continue;
            if (u.username.equalsIgnoreCase(emisorUsername) || u.uniqueId.equalsIgnoreCase(emisorId)) continue;

            LatLng posUsuaria = new LatLng(u.lat, u.lng);
            if (MapaDatos.estaEnRadio(posUsuaria, marcaSos, RADIO_ALERTA_SOS_METROS)) {
                recibidas++;
                mostrarNotificacionLocalSos(app, u.username, emisorUsername, recibidas);
            }
        }
        return recibidas;
    }

    private static void crearCanalNotificacionSos(Context app) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_SOS_ID,
                    "Alertas SOS Cercanas (300 m)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones de alerta SOS en un radio de 300 metros");
            NotificationManager nm = app.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @SuppressLint("MissingPermission")
    private static void mostrarNotificacionLocalSos(Context app, String destinataria, String emisor, int notifId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(app, mapa.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
                app, notifId, intent, android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CHANNEL_SOS_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("🚨 Alerta SOS a menos de 300 m")
                .setContentText("Para " + destinataria + ": " + emisor + " activó una marca SOS cerca.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManagerCompat.from(app).notify(2000 + notifId, builder.build());
    }

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
            if (edadMs > 10 * 60 * 1000L) mejor = null;
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
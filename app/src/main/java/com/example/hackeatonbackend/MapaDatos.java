package com.example.hackeatonbackend;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.location.Location;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Datos compartidos del mapa (puntos de calor, reportes, icono de alerta).
 * Lo usan tanto la actividad "mapa" como el minimapa de MainActivity.
 */
public class MapaDatos {

    public static final LatLng CENTRO_DEFAULT = new LatLng(25.79097, -108.99825); // Los Mochis centro
    public static final float RADIO_MINIMAPA_METROS = 100f;
    /** Duración de una marca SOS en el mapa: media hora. */
    public static final long DURACION_SOS_MS = 30L * 60L * 1000L;

    private static final String PREF_NAME = "ReportesMapaPrefs";
    private static final String KEY_REPORTES = "reportes_anonimos";

    private MapaDatos() {}

    // ---------------------------------------------------------------- Puntos de calor

    public static List<LatLng> puntosCalorBase() {
        List<LatLng> puntos = new ArrayList<>();
        puntos.add(new LatLng(25.784167, -109.031111)); // Los Virreyes
        puntos.add(new LatLng(25.753333, -108.988333)); // Las Flores
        puntos.add(new LatLng(25.774500, -108.969000)); // Las Canteras
        puntos.add(new LatLng(25.780278, -109.025278)); // Álamos Country
        puntos.add(new LatLng(25.790970, -108.998250)); // Zona Centro
        puntos.add(new LatLng(25.600833, -109.052222)); // Topolobampo
        return puntos;
    }

    /** Puntos base + posiciones de los reportes guardados en el dispositivo. */
    public static List<LatLng> obtenerPuntosCalor(Context context) {
        List<LatLng> puntos = puntosCalorBase();
        for (ReporteMarcador r : obtenerReportesLocales(context)) {
            if (!r.esSos) puntos.add(r.posicion);
        }
        return puntos;
    }

    // ---------------------------------------------------------------- Reportes

    public static List<ReporteMarcador> reportesPredeterminados() {
        List<ReporteMarcador> reportes = new ArrayList<>();

        reportes.add(new ReporteMarcador(
                "m1",
                "Bar El Delfín (zona Obregón)",
                new LatLng(25.7918, -108.9978),
                "Anónimo",
                "24/11/2026 21:30",
                "Intentaron poner algo en mi bebida y me sentí observada."
        ));

        reportes.add(new ReporteMarcador(
                "m2",
                "José Guillermo Blake y San Mateo (Las Canteras)",
                new LatLng(25.7745, -108.9690),
                "Anónimo",
                "24/11/2026 21:55",
                "Un señor de aspecto sospechoso me estuvo siguiendo."
        ));

        reportes.add(new ReporteMarcador(
                "m3",
                "Entre Las Veredas y Álamos Country",
                new LatLng(25.7815, -109.0265),
                "Rosalina123",
                "24/11/2026 22:10",
                "Está muy oscuro y casi siempre hay borrachos por la zona, recomiendo rodear por el blvd."
        ));

        return reportes;
    }

    /** Reportes predeterminados + reportes anónimos guardados en el dispositivo. */
    public static List<ReporteMarcador> obtenerTodosLosReportes(Context context) {
        List<ReporteMarcador> todos = reportesPredeterminados();
        todos.addAll(obtenerReportesLocales(context));
        return todos;
    }

    public static void guardarReporteLocal(Context context, ReporteMarcador reporte) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_REPORTES, "[]");
        try {
            JSONArray actual = new JSONArray(jsonString);
            JSONArray array = new JSONArray();
            long ahora = System.currentTimeMillis();
            for (int i = 0; i < actual.length(); i++) {
                JSONObject o = actual.getJSONObject(i);
                long exp = o.optLong("expira", 0L);
                if (exp > 0 && exp <= ahora) continue; // purga marcas ya vencidas
                if (reporte.esSos && reporte.id.equals(o.optString("id"))) continue; // un nuevo SOS reemplaza al anterior
                array.put(o);
            }
            JSONObject obj = new JSONObject();
            obj.put("id", reporte.id);
            obj.put("titulo", reporte.titulo);
            obj.put("lat", reporte.posicion.latitude);
            obj.put("lng", reporte.posicion.longitude);
            obj.put("usuario", reporte.usuario);
            obj.put("fecha", reporte.fecha);
            obj.put("comentario", reporte.comentario);
            obj.put("expira", reporte.expiraEn);
            obj.put("sos", reporte.esSos);
            array.put(obj);
            prefs.edit().putString(KEY_REPORTES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<ReporteMarcador> obtenerReportesLocales(Context context) {
        List<ReporteMarcador> lista = new ArrayList<>();
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_REPORTES, "[]");
        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                long expira = obj.optLong("expira", 0L);
                if (expira > 0 && expira <= System.currentTimeMillis()) continue; // marca SOS vencida
                lista.add(new ReporteMarcador(
                        obj.getString("id"),
                        obj.getString("titulo"),
                        new LatLng(obj.getDouble("lat"), obj.getDouble("lng")),
                        obj.getString("usuario"),
                        obj.getString("fecha"),
                        obj.getString("comentario"),
                        expira,
                        obj.optBoolean("sos", false)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ---------------------------------------------------------------- Marca SOS

    /**
     * Publica una marca SOS temporal (30 min) con usuario, fecha y hora.
     * Cada usuario tiene una sola marca SOS: una nueva reemplaza a la anterior.
     */
    public static ReporteMarcador publicarMarcaSos(Context context, String idUsuario, String usuario,
                                                   LatLng posicion, String origen) {
        long ahora = System.currentTimeMillis();
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(ahora));
        ReporteMarcador marca = new ReporteMarcador(
                "sos_" + idUsuario,
                "🚨 SOS de " + usuario,
                posicion,
                usuario,
                fecha,
                "Alerta SOS activada (" + origen + ").",
                ahora + DURACION_SOS_MS,
                true);
        guardarReporteLocal(context, marca);
        return marca;
    }

    // ---------------------------------------------------------------- Radio

    public static boolean estaEnRadio(LatLng punto, LatLng centro, float metros) {
        float[] resultado = new float[1];
        Location.distanceBetween(centro.latitude, centro.longitude,
                punto.latitude, punto.longitude, resultado);
        return resultado[0] <= metros;
    }

    public static List<LatLng> filtrarPorRadio(List<LatLng> puntos, LatLng centro, float metros) {
        List<LatLng> dentro = new ArrayList<>();
        for (LatLng p : puntos) {
            if (estaEnRadio(p, centro, metros)) dentro.add(p);
        }
        return dentro;
    }

    /** Cuadro que contiene el círculo de radio "metros" alrededor del centro. */
    public static LatLngBounds limitesDeRadio(LatLng centro, float metros) {
        double dLat = metros / 111320.0;
        double dLng = metros / (111320.0 * Math.cos(Math.toRadians(centro.latitude)));
        return new LatLngBounds(
                new LatLng(centro.latitude - dLat, centro.longitude - dLng),
                new LatLng(centro.latitude + dLat, centro.longitude + dLng));
    }

    // ---------------------------------------------------------------- Icono

    /** Icono de alerta. escala 1f = tamaño normal (mapa grande), 0.5f = minimapa. */
    public static BitmapDescriptor crearIconoAlerta(float escala) {
        return crearIcono(escala, 0xFFE53935, 0xFFD32F2F);
    }

    /** Icono morado para las marcas SOS temporales. */
    public static BitmapDescriptor crearIconoSos(float escala) {
        return crearIcono(escala, 0xFF7B1FA2, 0xFF4A148C);
    }

    private static BitmapDescriptor crearIcono(float escala, int colorPin, int colorSigno) {
        int width = Math.max(1, Math.round(96 * escala));
        int height = Math.max(1, Math.round(112 * escala));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(escala, escala);

        Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(colorPin);
        pinPaint.setStyle(Paint.Style.FILL);

        Paint circuloClaroPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circuloClaroPaint.setColor(Color.parseColor("#E3F2FD"));
        circuloClaroPaint.setStyle(Paint.Style.FILL);

        Paint exclamacionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        exclamacionPaint.setColor(colorSigno);
        exclamacionPaint.setStyle(Paint.Style.FILL);
        exclamacionPaint.setStrokeCap(Paint.Cap.ROUND);

        // Cabeza circular del pin
        canvas.drawCircle(48f, 40f, 32f, pinPaint);

        // Punta inferior del pin apuntando a la coordenada exacta
        Path punta = new Path();
        punta.moveTo(21f, 54f);
        punta.lineTo(75f, 54f);
        punta.lineTo(48f, 104f);
        punta.close();
        canvas.drawPath(punta, pinPaint);

        // Círculo interior claro
        canvas.drawCircle(48f, 40f, 23f, circuloClaroPaint);

        // Signo de exclamación (!)
        canvas.drawRoundRect(new RectF(44f, 22f, 52f, 43f), 4f, 4f, exclamacionPaint);
        canvas.drawCircle(48f, 51.5f, 4.2f, exclamacionPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
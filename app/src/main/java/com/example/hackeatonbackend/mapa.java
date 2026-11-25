package com.example.hackeatonbackend;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class mapa extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private HeatmapTileProvider heatmapProvider;
    private TileOverlay heatmapOverlay;
    private final Map<String, ReporteMarcador> reportesPorMarkerId = new HashMap<>();
    private final List<LatLng> heatmapPoints = new ArrayList<>();

    private static final String PREF_NAME = "ReportesMapaPrefs";
    private static final String KEY_REPORTES = "reportes_anonimos";

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                    habilitarUbicacionEnMapa();
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_mapa);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button button9 = findViewById(R.id.button9);
        button9.setOnClickListener(v -> {
            Intent intent = new Intent(mapa.this, MainActivity.class);
            startActivity(intent);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);

        LatLng losMochis = new LatLng(25.79097, -108.99825);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(losMochis, 13f));

        // Puntos base iniciales del mapa de calor
        heatmapPoints.clear();
        heatmapPoints.add(new LatLng(25.784167, -109.031111)); // Los Virreyes
        heatmapPoints.add(new LatLng(25.753333, -108.988333)); // Las Flores
        heatmapPoints.add(new LatLng(25.774500, -108.969000)); // Las Canteras
        heatmapPoints.add(new LatLng(25.780278, -109.025278)); // Álamos Country
        heatmapPoints.add(new LatLng(25.790970, -108.998250)); // Zona Centro
        heatmapPoints.add(new LatLng(25.600833, -109.052222)); // Topolobampo

        // Cargar reportes guardados localmente en el dispositivo
        cargarReportesGuardados();

        heatmapProvider = new HeatmapTileProvider.Builder()
                .data(heatmapPoints)
                .radius(50)
                .opacity(0.7)
                .build();

        heatmapOverlay = googleMap.addTileOverlay(
                new TileOverlayOptions().tileProvider(heatmapProvider)
        );

        agregarMarcadoresDeAlerta();

        // Permitir que el usuario toque cualquier zona del mapa para agregar un reporte anónimo
        googleMap.setOnMapClickListener(latLng -> mostrarDialogoNuevoReporte(latLng));

        verificarPermisoUbicacion();
    }

    private void agregarMarcadoresDeAlerta() {
        List<ReporteMarcador> reportes = new ArrayList<>();

        // Reportes predeterminados
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

        // Cargar reportes anónimos guardados por usuarias
        reportes.addAll(obtenerReportesLocales());

        BitmapDescriptor iconoAlerta = crearIconoAlerta();

        for (ReporteMarcador reporte : reportes) {
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(reporte.posicion)
                    .title(reporte.titulo)
                    .anchor(0.5f, 1.0f)
                    .icon(iconoAlerta));

            if (marker != null) {
                marker.setTag(reporte);
                reportesPorMarkerId.put(marker.getId(), reporte);
            }
        }

        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof ReporteMarcador) {
                mostrarMenuComentario((ReporteMarcador) tag);
                return true;
            }
            return false;
        });
    }

    private void mostrarDialogoNuevoReporte(LatLng latLng) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_nuevo_reporte, null);

        EditText edtTitulo = dialogView.findViewById(R.id.edtTituloReporte);
        EditText edtComentario = dialogView.findViewById(R.id.edtComentarioReporte);

        new AlertDialog.Builder(this)
                .setTitle("Reportar Zona de Peligro (Anónimo)")
                .setView(dialogView)
                .setPositiveButton("Publicar Anónimo", (dialog, which) -> {
                    String titulo = edtTitulo.getText().toString().trim();
                    String comentario = edtComentario.getText().toString().trim();

                    if (titulo.isEmpty()) {
                        titulo = "Zona de Riesgo Reportada";
                    }
                    if (comentario.isEmpty()) {
                        comentario = "Reporte anónimo de zona considerada peligrosa.";
                    }

                    String fechaStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    String id = "usr_" + System.currentTimeMillis();

                    ReporteMarcador nuevoReporte = new ReporteMarcador(
                            id,
                            titulo,
                            latLng,
                            "Anónimo",
                            fechaStr,
                            comentario
                    );

                    // Guardar localmente
                    guardarReporteLocal(nuevoReporte);

                    // Agregar marcador al mapa en tiempo real
                    BitmapDescriptor iconoAlerta = crearIconoAlerta();
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                            .position(nuevoReporte.posicion)
                            .title(nuevoReporte.titulo)
                            .anchor(0.5f, 1.0f)
                            .icon(iconoAlerta));

                    if (marker != null) {
                        marker.setTag(nuevoReporte);
                        reportesPorMarkerId.put(marker.getId(), nuevoReporte);
                    }

                    // Actualizar mapa de calor
                    heatmapPoints.add(latLng);
                    if (heatmapProvider != null) {
                        heatmapProvider.setData(heatmapPoints);
                        if (heatmapOverlay != null) {
                            heatmapOverlay.remove();
                        }
                        heatmapOverlay = googleMap.addTileOverlay(
                                new TileOverlayOptions().tileProvider(heatmapProvider)
                        );
                    }

                    Toast.makeText(this, "Reporte anónimo publicado correctamente", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarReporteLocal(ReporteMarcador reporte) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_REPORTES, "[]");
        try {
            JSONArray array = new JSONArray(jsonString);
            JSONObject obj = new JSONObject();
            obj.put("id", reporte.id);
            obj.put("titulo", reporte.titulo);
            obj.put("lat", reporte.posicion.latitude);
            obj.put("lng", reporte.posicion.longitude);
            obj.put("usuario", reporte.usuario);
            obj.put("fecha", reporte.fecha);
            obj.put("comentario", reporte.comentario);
            array.put(obj);
            prefs.edit().putString(KEY_REPORTES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ReporteMarcador> obtenerReportesLocales() {
        List<ReporteMarcador> lista = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_REPORTES, "[]");
        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString("id");
                String titulo = obj.getString("titulo");
                double lat = obj.getDouble("lat");
                double lng = obj.getDouble("lng");
                String usuario = obj.getString("usuario");
                String fecha = obj.getString("fecha");
                String comentario = obj.getString("comentario");

                lista.add(new ReporteMarcador(id, titulo, new LatLng(lat, lng), usuario, fecha, comentario));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    private void cargarReportesGuardados() {
        for (ReporteMarcador r : obtenerReportesLocales()) {
            heatmapPoints.add(r.posicion);
        }
    }

    private BitmapDescriptor crearIconoAlerta() {
        int width = 96;
        int height = 112;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(Color.parseColor("#E53935"));
        pinPaint.setStyle(Paint.Style.FILL);

        Paint circuloClaroPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circuloClaroPaint.setColor(Color.parseColor("#E3F2FD"));
        circuloClaroPaint.setStyle(Paint.Style.FILL);

        Paint exclamacionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        exclamacionPaint.setColor(Color.parseColor("#D32F2F"));
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

        // Círculo interior claro similar a la referencia
        canvas.drawCircle(48f, 40f, 23f, circuloClaroPaint);

        // Barra superior del signo de exclamación (!)
        RectF barraExclamacion = new RectF(44f, 22f, 52f, 43f);
        canvas.drawRoundRect(barraExclamacion, 4f, 4f, exclamacionPaint);

        // Punto inferior del signo de exclamación
        canvas.drawCircle(48f, 51.5f, 4.2f, exclamacionPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void mostrarMenuComentario(ReporteMarcador reporte) {
        String detalle = "Nombre de usuario: " + reporte.usuario
                + "\nFecha: " + reporte.fecha
                + "\n\nComentario:\n" + reporte.comentario;

        new AlertDialog.Builder(this)
                .setTitle(reporte.titulo)
                .setMessage(detalle)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void verificarPermisoUbicacion() {
        boolean fineOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fineOk || coarseOk) {
            habilitarUbicacionEnMapa();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void habilitarUbicacionEnMapa() {
        if (googleMap == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }
}

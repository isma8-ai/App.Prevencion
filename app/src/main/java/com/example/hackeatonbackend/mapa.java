package com.example.hackeatonbackend;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class mapa extends AppCompatActivity implements OnMapReadyCallback, SosHelper.Listener {

    private GoogleMap googleMap;
    private HeatmapTileProvider heatmapProvider;
    private TileOverlay heatmapOverlay;
    private BitmapDescriptor iconoAlerta;
    private BitmapDescriptor iconoSos;
    private final Map<String, Marker> marcadoresSos = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ReporteMarcador> reportesPorMarkerId = new HashMap<>();
    private final List<LatLng> heatmapPoints = new ArrayList<>();

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

        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        boolean isProtectorMode = prefs.getBoolean("isProtectorMode", false);
        boolean isAdminMode = prefs.getBoolean("isAdminMode", false);

        Button button9 = findViewById(R.id.button9);
        button9.setOnClickListener(v -> finish());

        Button btnLeyenda = findViewById(R.id.btnLeyendaMapa);
        btnLeyenda.setOnClickListener(v -> mostrarDialogoLeyendaRojos());

        if (isAdminMode) {
            findViewById(R.id.main).setBackgroundColor(Color.parseColor("#C62828"));
            button9.setBackgroundColor(Color.parseColor("#8E0000"));
            btnLeyenda.setBackgroundColor(Color.parseColor("#8E0000"));
        }

        View cardInstruccion = findViewById(R.id.cardInstruccion);
        if (cardInstruccion != null) {
            cardInstruccion.setVisibility(isProtectorMode ? View.GONE : View.VISIBLE);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        iconoAlerta = MapaDatos.crearIconoAlerta(1f);
        iconoSos = MapaDatos.crearIconoSos(1f);

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MapaDatos.CENTRO_DEFAULT, 13f));

        heatmapPoints.clear();
        heatmapPoints.addAll(MapaDatos.obtenerPuntosCalor(this));

        heatmapProvider = new HeatmapTileProvider.Builder()
                .data(heatmapPoints)
                .radius(50)
                .opacity(0.7)
                .build();

        heatmapOverlay = googleMap.addTileOverlay(
                new TileOverlayOptions().tileProvider(heatmapProvider)
        );

        agregarMarcadoresDeAlerta();

        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        boolean isProtectorMode = prefs.getBoolean("isProtectorMode", false);
        if (!isProtectorMode) {
            googleMap.setOnMapClickListener(this::mostrarDialogoNuevoReporte);
        } else {
            googleMap.setOnMapClickListener(null);
            String protectedUserId = prefs.getString("protectedUserId", "Usuaria Protegida");
            LatLng ubicacionTiempoReal = new LatLng(25.7925, -108.9950);
            googleMap.addMarker(new MarkerOptions()
                    .position(ubicacionTiempoReal)
                    .title("🟢 En Tiempo Real: " + protectedUserId)
                    .snippet("GPS Activo • Monitoreando ubicación en vivo"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacionTiempoReal, 14f));
            Toast.makeText(this, "🔴 Monitoreando ubicación en tiempo real de: " + protectedUserId, Toast.LENGTH_LONG).show();
        }

        verificarPermisoUbicacion();
    }

    private void mostrarDialogoLeyendaRojos() {
        new AlertDialog.Builder(this)
                .setTitle("🔴 ¿Qué son los Puntos Rojos?")
                .setMessage("Los puntos y marcadores rojos en el mapa representan:\n\n" +
                        "1. **Zonas de Riesgo:** Áreas reportadas por la comunidad como peligrosas (falta de iluminación, zonas solitarias, etc.).\n\n" +
                        "2. **Alertas SOS:** Ubicaciones exactas donde se han activado llamadas de emergencia o marcas SOS recientes.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void agregarMarcadorAlMapa(ReporteMarcador reporte) {
        if (reporte.esSos) {
            Marker previo = marcadoresSos.remove(reporte.id);
            if (previo != null) previo.remove();
        }

        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(reporte.posicion)
                .title(reporte.titulo)
                .anchor(0.5f, 1.0f)
                .icon(reporte.esSos ? iconoSos : iconoAlerta)
                .zIndex(reporte.esSos ? 1f : 0f));

        if (marker != null) {
            marker.setTag(reporte);
            reportesPorMarkerId.put(marker.getId(), reporte);
            if (reporte.esSos) {
                marcadoresSos.put(reporte.id, marker);
                programarExpiracion(reporte, marker);
            }
        }
    }

    private void programarExpiracion(ReporteMarcador reporte, Marker marker) {
        long espera = Math.max(0L, reporte.expiraEn - System.currentTimeMillis());
        handler.postDelayed(() -> {
            if (marcadoresSos.get(reporte.id) == marker) marcadoresSos.remove(reporte.id);
            reportesPorMarkerId.remove(marker.getId());
            marker.remove();
        }, espera);
    }

    @Override
    public void onMarcaSosPublicada() {
        if (googleMap == null) return;
        for (ReporteMarcador r : MapaDatos.obtenerTodosLosReportes(this)) {
            if (r.esSos) agregarMarcadorAlMapa(r);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void agregarMarcadoresDeAlerta() {
        for (ReporteMarcador reporte : MapaDatos.obtenerTodosLosReportes(this)) {
            agregarMarcadorAlMapa(reporte);
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

        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        String autorReal = prefs.getString("usuarioSesion", prefs.getString("username", "Anónimo"));

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
                            autorReal,
                            fechaStr,
                            comentario,
                            0L,
                            false
                    );

                    MapaDatos.guardarReporteLocal(this, nuevoReporte);

                    HistorialManager.registrar(this, HistorialManager.TIPO_MAPA,
                            "Reporte \"" + titulo + "\" en (" +
                                    String.format(Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude) + ")");

                    agregarMarcadorAlMapa(nuevoReporte);

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

    private void mostrarMenuComentario(ReporteMarcador reporte) {
        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("isAdminMode", false);
        String usuarioMostrado = (isAdmin && reporte.autorReal != null && !reporte.autorReal.isEmpty())
                ? reporte.autorReal
                : reporte.usuario;

        String detalle = "Nombre de usuario: " + usuarioMostrado
                + "\nFecha: " + reporte.fecha
                + "\n\nComentario:\n" + reporte.comentario;
        if (reporte.esSos && reporte.expiraEn > 0) {
            detalle += "\n\n⏳ Esta marca desaparece a las "
                    + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(reporte.expiraEn));
        }

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
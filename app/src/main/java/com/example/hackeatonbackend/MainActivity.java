package com.example.hackeatonbackend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "CentinelaPrefs";
    private static final String KEY_MY_ID = "myUniqueId";
    private static final String KEY_CONTACTS = "trustedContacts";

    // Minimapa (solo lectura)
    private GoogleMap miniMap;
    private View miniMapView;
    private TextView tvMiniMapaInfo;
    private LatLng centroMini = MapaDatos.CENTRO_DEFAULT;
    private boolean ubicacionReal = false;
    private boolean miniMapaListo = false;

    private final ActivityResultLauncher<String[]> miniLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (tienePermisoUbicacion()) {
                    obtenerUbicacionParaMinimapa();
                } else {
                    usarUbicacionMinimapa(null);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Botón SOS | Pedir Ayuda (Activa Ficha de Emergencia y Alerta)
        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(v -> triggerSosEmergencyCard());

        // Botón Comentario / Muro Anónimo
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, chat.class);
            startActivity(intent);
        });

        // Botón Ver Mapa
        Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, mapa.class);
            startActivity(intent);
        });

        // Botón Mi Pulsera
        Button button5 = findViewById(R.id.button5);
        button5.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, pulsera.class)));

        // Si la pulsera quedó encendida, reconectarla automáticamente
        PulseraManager.get(this).iniciarSiEstaHabilitada();

        // Botón Historial
        Button button6 = findViewById(R.id.button6);
        button6.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, historial.class);
            startActivity(intent);
        });

        // Botón Menú Superior (☰ Menú)
        Button btnTopMenu = findViewById(R.id.btnTopMenu);
        if (btnTopMenu != null) {
            btnTopMenu.setOnClickListener(v -> showQuickMenuDialog());
        }

        // Minimapa de solo lectura
        tvMiniMapaInfo = findViewById(R.id.tvMiniMapaInfo);
        miniMapView = findViewById(R.id.miniMapFragment);
        SupportMapFragment miniFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.miniMapFragment);
        if (miniFragment != null) {
            miniFragment.getMapAsync(map -> onMiniMapReady(map));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresca por si se publicaron reportes nuevos desde la pantalla del mapa
        if (miniMap != null && miniMapaListo) {
            dibujarMinimapa();
        }
    }

    // ===================== MINIMAPA (solo lectura, radio 100 m) =====================

    private void onMiniMapReady(GoogleMap map) {
        miniMap = map;

        // Sin interacción: no se puede mover, hacer zoom ni agregar marcas
        UiSettings ui = miniMap.getUiSettings();
        ui.setAllGesturesEnabled(false);
        ui.setZoomControlsEnabled(false);
        ui.setMyLocationButtonEnabled(false);
        ui.setMapToolbarEnabled(false);
        ui.setCompassEnabled(false);
        miniMap.setOnMarkerClickListener(marker -> true); // ignora toques en marcas

        if (tienePermisoUbicacion()) {
            obtenerUbicacionParaMinimapa();
        } else {
            miniLocationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean tienePermisoUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionParaMinimapa() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            usarUbicacionMinimapa(null);
            return;
        }

        // 1) Última ubicación conocida, si es reciente (menos de 10 min)
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
            if (edadMs < 10 * 60 * 1000L) {
                usarUbicacionMinimapa(mejor);
                return;
            }
        }

        // 2) Pedir una ubicación actual (con tiempo límite de 8 s)
        solicitarUbicacionActual(lm, mejor);
    }

    @SuppressLint("MissingPermission")
    private void solicitarUbicacionActual(LocationManager lm, Location respaldo) {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        String provider = null;
        if (fine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        if (provider == null) {
            usarUbicacionMinimapa(respaldo);
            return;
        }

        CancellationSignal signal = new CancellationSignal();
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeout = signal::cancel;
        handler.postDelayed(timeout, 8000);

        try {
            LocationManagerCompat.getCurrentLocation(lm, provider, signal,
                    ContextCompat.getMainExecutor(this), location -> {
                        handler.removeCallbacks(timeout);
                        usarUbicacionMinimapa(location != null ? location : respaldo);
                    });
        } catch (SecurityException e) {
            handler.removeCallbacks(timeout);
            usarUbicacionMinimapa(respaldo);
        }
    }

    private void usarUbicacionMinimapa(Location location) {
        if (isFinishing() || isDestroyed()) return;
        if (location != null) {
            centroMini = new LatLng(location.getLatitude(), location.getLongitude());
            ubicacionReal = true;
        } else {
            centroMini = MapaDatos.CENTRO_DEFAULT;
            ubicacionReal = false;
        }
        miniMapaListo = true;
        dibujarMinimapa();
    }

    @SuppressLint("MissingPermission")
    private void dibujarMinimapa() {
        if (miniMap == null) return;
        miniMap.clear();

        final float radio = MapaDatos.RADIO_MINIMAPA_METROS;

        // Círculo que marca el radio de 100 m
        miniMap.addCircle(new CircleOptions()
                .center(centroMini)
                .radius(radio)
                .strokeColor(0xFF1976D2)
                .strokeWidth(3f)
                .fillColor(0x141976D2));

        // Puntos calientes dentro del radio (el proveedor falla si la lista está vacía)
        List<LatLng> puntos = MapaDatos.filtrarPorRadio(MapaDatos.obtenerPuntosCalor(this), centroMini, radio);
        if (!puntos.isEmpty()) {
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .data(puntos)
                    .radius(40)
                    .opacity(0.7)
                    .build();
            miniMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        }

        // Marcas de alerta dentro del radio (icono reducido, sin título para no abrir ventanas)
        BitmapDescriptor icono = MapaDatos.crearIconoAlerta(0.5f);
        int cercanos = 0;
        for (ReporteMarcador r : MapaDatos.obtenerTodosLosReportes(this)) {
            if (MapaDatos.estaEnRadio(r.posicion, centroMini, radio)) {
                miniMap.addMarker(new MarkerOptions()
                        .position(r.posicion)
                        .anchor(0.5f, 1.0f)
                        .icon(icono));
                cercanos++;
            }
        }

        // Punto azul de "mi ubicación" (solo visual)
        if (tienePermisoUbicacion()) {
            try {
                miniMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
            }
        }

        // Texto informativo
        String reportesTxt = cercanos + (cercanos == 1 ? " reporte" : " reportes");
        if (ubicacionReal) {
            tvMiniMapaInfo.setText("📍 Tu zona (radio de 100 m) · " + reportesTxt + " cerca");
        } else {
            tvMiniMapaInfo.setText("📍 Ubicación no disponible: mostrando Centro de Los Mochis (100 m) · "
                    + reportesTxt);
        }

        ajustarCamaraMinimapa();
    }

    private void ajustarCamaraMinimapa() {
        miniMapView.post(() -> {
            if (miniMap == null) return;
            int w = miniMapView.getWidth();
            int h = miniMapView.getHeight();
            try {
                if (w > 0 && h > 0) {
                    int padding = (int) (8 * getResources().getDisplayMetrics().density);
                    miniMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                            MapaDatos.limitesDeRadio(centroMini, MapaDatos.RADIO_MINIMAPA_METROS),
                            w, h, padding));
                } else {
                    miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centroMini, 14f));
                }
            } catch (Exception e) {
                miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centroMini, 14f));
            }
        });
    }

    private String getOrCreateMyId() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String myId = prefs.getString(KEY_MY_ID, null);
        if (myId == null || myId.isEmpty()) {
            myId = "CENTINELA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            prefs.edit().putString(KEY_MY_ID, myId).apply();
        }
        return myId;
    }

    private void triggerSosEmergencyCard() {
        SosHelper.disparar(this, this, "Botón SOS de la pantalla");
    }

    private void showQuickMenuDialog() {
        String[] options = {"👤 Mi Perfil y Ficha Médica", "➕ Agregar Contacto por ID", "🆔 Ver Mi ID Único", "👥 Ver Mis Contactos Vinculados"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Menú Rápido - Centinela");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(MainActivity.this, perfil.class);
                startActivity(intent);
            } else if (which == 1) {
                showAddContactDialog();
            } else if (which == 2) {
                showMyIdDialog();
            } else if (which == 3) {
                showViewContactsDialog();
            }
        });
        builder.show();
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Vincular Contacto de Confianza");

        final EditText input = new EditText(this);
        input.setHint("Ingresa el ID del usuario...");
        builder.setView(input);

        builder.setPositiveButton("Vincular", (dialog, which) -> {
            String contactId = input.getText().toString().trim();
            if (!contactId.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                Set<String> contacts = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());
                Set<String> newContacts = new HashSet<>(contacts);
                if (newContacts.add(contactId)) {
                    prefs.edit().putStringSet(KEY_CONTACTS, newContacts).apply();
                    Toast.makeText(this, "Contacto vinculado: " + contactId, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Este contacto ya está vinculado", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showMyIdDialog() {
        String myId = getOrCreateMyId();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mi ID Único");
        builder.setMessage("Comparte este ID para vincularte:\n\n" + myId);
        builder.setPositiveButton("Copiar", (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("ID Centinela", myId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cerrar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showViewContactsDialog() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> contactsSet = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());
        List<String> contactsList = new ArrayList<>(contactsSet);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Contactos Vinculados");

        if (contactsList.isEmpty()) {
            builder.setMessage("Aún no tienes contactos vinculados.");
            builder.setPositiveButton("Aceptar", null);
        } else {
            String[] items = contactsList.toArray(new String[0]);
            builder.setItems(items, (dialog, which) -> {
                String selectedId = items[which];
                Toast.makeText(this, "Contacto: " + selectedId, Toast.LENGTH_SHORT).show();
            });
            builder.setPositiveButton("Cerrar", null);
        }
        builder.show();
    }
}
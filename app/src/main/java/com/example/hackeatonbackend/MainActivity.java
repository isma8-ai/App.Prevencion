package com.example.hackeatonbackend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.location.LocationManagerCompat;
import androidx.core.os.CancellationSignal;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SosHelper.Listener {

    private static final String PREF_NAME = "CentinelaPrefs";
    private static final String KEY_MY_ID = "myUniqueId";
    private static final String KEY_CONTACTS = "trustedContacts";

    private GoogleMap miniMap;
    private View miniMapView;
    private TextView tvMiniMapaInfo;
    private LatLng centroMini = MapaDatos.CENTRO_DEFAULT;
    private boolean ubicacionReal = false;
    private boolean miniMapaListo = false;
    private final Handler miniHandler = new Handler(Looper.getMainLooper());
    private final Runnable redibujarMinimapa = () -> {
        if (miniMap != null && miniMapaListo) dibujarMinimapa();
    };

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
        if (SesionRolHelper.redirigirSiEsAdmin(this)) {
            return;
        }

        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isProtectorMode = SesionRolHelper.esProtector(this);
        String protectedUserId = prefs.getString("protectedUserId", "");

        Button button2 = findViewById(R.id.button2);
        Button button3 = findViewById(R.id.button3);
        Button button4 = findViewById(R.id.button4);
        Button button5 = findViewById(R.id.button5);
        Button button6 = findViewById(R.id.button6);
        Button button8 = findViewById(R.id.button8);

        View cardGuiaProtector = findViewById(R.id.cardGuiaProtector);
        if (cardGuiaProtector != null) {
            cardGuiaProtector.setVisibility(isProtectorMode ? View.VISIBLE : View.GONE);
        }

        ConstraintLayout.LayoutParams p3 = (ConstraintLayout.LayoutParams) button3.getLayoutParams();
        ConstraintLayout.LayoutParams p4 = (ConstraintLayout.LayoutParams) button4.getLayoutParams();
        Button buttonCerrarSesion = findViewById(R.id.buttonCerrarSesion);
        ConstraintLayout.LayoutParams pLogout = buttonCerrarSesion != null ? (ConstraintLayout.LayoutParams) buttonCerrarSesion.getLayoutParams() : null;

        if (isProtectorMode) {
            if (button2 != null) button2.setVisibility(View.GONE);
            if (button5 != null) button5.setVisibility(View.GONE);
            if (button6 != null) button6.setVisibility(View.GONE);
            if (button8 != null) button8.setVisibility(View.GONE);

            p3.topToBottom = R.id.cardGuiaProtector;
            p4.topToBottom = R.id.cardGuiaProtector;
            if (pLogout != null) pLogout.topToBottom = R.id.button3;

            if (button3 != null) {
                button3.setText("📥 Bandeja de Alertas (" + (protectedUserId.isEmpty() ? "Protegida" : protectedUserId) + ")");
                button3.setVisibility(View.VISIBLE);
                button3.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BandejaChatActivity.class)));
            }
        } else {
            p3.topToBottom = R.id.button2;
            p4.topToBottom = R.id.button2;
            if (pLogout != null) pLogout.topToBottom = R.id.button8;

            if (button2 != null) {
                button2.setVisibility(View.VISIBLE);
                button2.setOnClickListener(v -> mostrarConfirmacionSos());
            }
            if (button3 != null) {
                button3.setVisibility(View.VISIBLE);
                button3.setText("💬 Chat con Protectores");
                button3.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BandejaChatActivity.class)));
            }
            if (button5 != null) {
                button5.setVisibility(View.VISIBLE);
                button5.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, pulsera.class)));
            }
            PulseraManager.get(this).iniciarSiEstaHabilitada();

            if (button6 != null) {
                button6.setVisibility(View.VISIBLE);
                button6.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, historial.class)));
            }
            if (button8 != null) {
                button8.setVisibility(View.VISIBLE);
                button8.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AcercaDeActivity.class)));
            }
        }

        button3.setLayoutParams(p3);
        button4.setLayoutParams(p4);
        if (buttonCerrarSesion != null && pLogout != null) {
            buttonCerrarSesion.setLayoutParams(pLogout);
            buttonCerrarSesion.setVisibility(View.VISIBLE);
            buttonCerrarSesion.setOnClickListener(v -> cerrarSesion());
        }

        if (button4 != null) {
            button4.setVisibility(View.VISIBLE);
            button4.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, mapa.class)));
        }

        Button btnTopMenu = findViewById(R.id.btnTopMenu);
        if (btnTopMenu != null) {
            if (isProtectorMode) {
                btnTopMenu.setVisibility(View.GONE);
            } else {
                btnTopMenu.setVisibility(View.VISIBLE);
                btnTopMenu.setText("☰ Menú");
                btnTopMenu.setOnClickListener(v -> showQuickMenuDialog());
            }
        }

        tvMiniMapaInfo = findViewById(R.id.tvMiniMapaInfo);
        miniMapView = findViewById(R.id.miniMapFragment);
        SupportMapFragment miniFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.miniMapFragment);
        if (miniFragment != null) {
            miniFragment.getMapAsync(this::onMiniMapReady);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SesionRolHelper.redirigirSiEsAdmin(this)) {
            return;
        }
        if (miniMap != null && miniMapaListo) {
            dibujarMinimapa();
        }
    }

    @Override
    protected void onDestroy() {
        miniHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onMarcaSosPublicada() {
        if (miniMap != null && miniMapaListo) dibujarMinimapa();
    }

    private void onMiniMapReady(GoogleMap map) {
        miniMap = map;

        UiSettings ui = miniMap.getUiSettings();
        ui.setAllGesturesEnabled(false);
        ui.setZoomControlsEnabled(false);
        ui.setMyLocationButtonEnabled(false);
        ui.setMapToolbarEnabled(false);
        ui.setCompassEnabled(false);
        miniMap.setOnMarkerClickListener(marker -> true);

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

        miniMap.addCircle(new CircleOptions()
                .center(centroMini)
                .radius(radio)
                .strokeColor(0xFF1976D2)
                .strokeWidth(3f)
                .fillColor(0x141976D2));

        List<LatLng> puntos = MapaDatos.filtrarPorRadio(MapaDatos.obtenerPuntosCalor(this), centroMini, radio);
        if (!puntos.isEmpty()) {
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .data(puntos)
                    .radius(40)
                    .opacity(0.7)
                    .build();
            miniMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        }

        BitmapDescriptor icono = MapaDatos.crearIconoAlerta(0.5f);
        BitmapDescriptor iconoSos = MapaDatos.crearIconoSos(0.5f);
        int cercanos = 0;
        long proximaExpiracion = 0;
        for (ReporteMarcador r : MapaDatos.obtenerTodosLosReportes(this)) {
            if (MapaDatos.estaEnRadio(r.posicion, centroMini, radio)) {
                miniMap.addMarker(new MarkerOptions()
                        .position(r.posicion)
                        .anchor(0.5f, 1.0f)
                        .icon(r.esSos ? iconoSos : icono)
                        .zIndex(r.esSos ? 1f : 0f));
                cercanos++;
                if (r.esSos && r.expiraEn > 0
                        && (proximaExpiracion == 0 || r.expiraEn < proximaExpiracion)) {
                    proximaExpiracion = r.expiraEn;
                }
            }
        }

        miniHandler.removeCallbacks(redibujarMinimapa);
        if (proximaExpiracion > 0) {
            miniHandler.postDelayed(redibujarMinimapa,
                    Math.max(1000L, proximaExpiracion - System.currentTimeMillis() + 500L));
        }

        if (tienePermisoUbicacion()) {
            try {
                miniMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
            }
        }

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

    private void mostrarConfirmacionSos() {
        new AlertDialog.Builder(this)
                .setTitle("🚨 Confirmación de Alerta SOS")
                .setMessage("¿Deseas enviar la alerta SOS con tu ubicación en tiempo real y ficha médica a tus protectores y familiares enlazados?")
                .setPositiveButton("Sí, Enviar Alerta", (dialog, which) -> triggerSosEmergencyCard())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void triggerSosEmergencyCard() {
        SosHelper.disparar(this, this, "Botón SOS de la pantalla");
    }

    private void showQuickMenuDialog() {
        String[] options = {"👤 Mi Perfil y Ficha Médica", "📋 Ver y Copiar Mi ID Único", "👥 Ver Mis Contactos Vinculados"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MenúRápido - Centinela");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                startActivity(new Intent(MainActivity.this, perfil.class));
            } else if (which == 1) {
                showMyIdDialog();
            } else if (which == 2) {
                showViewContactsDialog();
            }
        });
        builder.show();
    }

    private void cerrarSesion() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("isProtectorMode", false)
                .putBoolean("isAdminMode", false)
                .apply();

        Intent i = new Intent(MainActivity.this, sesion.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void showMyIdDialog() {
        String myId = getOrCreateMyId();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("ID Centinela", myId);
            clipboard.setPrimaryClip(clip);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🆔 Mi ID Único");
        builder.setMessage("¡ID copiado al portapapeles!\n\nTu ID:\n" + myId + "\n\nComparte este ID con tus protectores para que puedan enlazarse contigo.");
        builder.setPositiveButton("Copiar de nuevo", (dialog, which) -> {
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("ID Centinela", myId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cerrar", null);
        builder.show();
    }

    private void showViewContactsDialog() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> contactsSet = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());

        List<UserManager.Usuario> usuarios = UserManager.obtenerUsuarios(this);
        List<String> contactsList = new ArrayList<>();

        for (String contact : contactsSet) {
            String displayName = contact;
            for (UserManager.Usuario u : usuarios) {
                if (u.username.equalsIgnoreCase(contact)) {
                    displayName = u.fullName + " (" + u.username + ")";
                    break;
                }
            }
            contactsList.add(displayName);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("👥 Contactos Vinculados (Toca para Borrar)");

        if (contactsList.isEmpty()) {
            builder.setMessage("Aún no tienes contactos vinculados.");
            builder.setPositiveButton("Aceptar", null);
        } else {
            String[] items = contactsList.toArray(new String[0]);
            builder.setItems(items, (dialog, which) -> confirmarEliminarContacto(items[which]));
            builder.setPositiveButton("Cerrar", null);
        }
        builder.show();
    }

    private void confirmarEliminarContacto(String selectedContact) {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Borrar Contacto Vinculado")
                .setMessage("¿Estás segura/o de que deseas eliminar a \"" + selectedContact + "\" de tu lista de contactos vinculados?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    Set<String> contactsSet = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());
                    Set<String> newContacts = new HashSet<>(contactsSet);

                    String rawId = selectedContact.replaceAll("\\s*\\(.*\\)", "").trim();
                    newContacts.remove(selectedContact);
                    newContacts.remove(rawId);

                    prefs.edit().putStringSet(KEY_CONTACTS, newContacts).apply();

                    HistorialManager.registrar(this, HistorialManager.TIPO_PERFIL, "Contacto desvinculado: " + selectedContact);
                    Toast.makeText(this, "Contacto \"" + selectedContact + "\" eliminado exitosamente", Toast.LENGTH_SHORT).show();

                    showViewContactsDialog();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
package com.example.hackeatonbackend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

/**
 * Maneja toda la comunicación BLE con la pulsera (ESP32-C3 + MPU6050 + botón).
 *
 * Protocolo (el firmware de la pulsera debe exponer esto):
 *  - Servicio de batería estándar 0x180F, característica 0x2A19 (READ + NOTIFY, 1 byte: 0-100)
 *  - Servicio propio 0xFF00, característica 0xFF01 (NOTIFY, 1 byte): 1 = botón, 2 = movimiento brusco
 */
@SuppressLint("MissingPermission")
@SuppressWarnings("deprecation")
public class PulseraManager {

    public static final UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID ALERTA_SERVICE = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
    public static final UUID ALERTA_CHAR = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int ALERTA_BOTON = 1;
    public static final int ALERTA_MOVIMIENTO = 2;

    private static final String PREF_NAME = "PulseraPrefs";
    private static final String KEY_ON = "habilitada";
    private static final String KEY_ADDR = "direccion";
    private static final String KEY_NAME = "nombre";

    private static final long ESCANEO_MS = 10_000;
    private static final long REINTENTO_MS = 5_000;
    private static final long ANTIREBOTE_MS = 10_000;

    /** La pantalla de la pulsera implementa esto para refrescar su interfaz. */
    public interface Listener {
        default void onEstado() {}
        default void onDispositivo(BluetoothDevice device, String nombre, int rssi) {}
        default void onEscaneoTerminado() {}
    }

    private static PulseraManager instancia;

    public static synchronized PulseraManager get(Context ctx) {
        if (instancia == null) instancia = new PulseraManager(ctx.getApplicationContext());
        return instancia;
    }

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private Listener listener;
    private WeakReference<Activity> actividadActual;

    private boolean habilitada;
    private boolean conectada = false;
    private boolean conectando = false;
    private boolean escaneando = false;
    private int bateria = -1;
    private String nombreDispositivo;
    private long ultimaAlerta = 0;

    private final Queue<Runnable> cola = new ArrayDeque<>();
    private boolean ocupado = false;

    private final Runnable detenerEscaneoRunnable = this::detenerEscaneo;
    private final Runnable reintentoRunnable = () -> {
        if (habilitada && gatt == null && tienePermisos() && bluetoothActivo()) conectarGuardado();
    };

    private PulseraManager(Context c) {
        appContext = c;
        prefs = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        habilitada = prefs.getBoolean(KEY_ON, false);
        nombreDispositivo = prefs.getString(KEY_NAME, null);

        BluetoothManager bm = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bm != null ? bm.getAdapter() : null;

        // Para saber en qué pantalla mostrar la ficha SOS cuando llegue una alerta
        ((Application) c).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) { actividadActual = new WeakReference<>(a); }
            @Override public void onActivityPaused(Activity a) {
                if (actividadActual != null && actividadActual.get() == a) actividadActual = null;
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    // ------------------------------------------------------------ Estado público

    public void setListener(Listener l) { this.listener = l; }
    public boolean isHabilitada() { return habilitada; }
    public boolean isConectada() { return conectada; }
    public boolean isConectando() { return conectando; }
    public boolean isEscaneando() { return escaneando; }
    public int getBateria() { return bateria; }
    public String getNombreDispositivo() { return nombreDispositivo; }
    public boolean bluetoothDisponible() { return adapter != null; }
    public boolean bluetoothActivo() { return adapter != null && adapter.isEnabled(); }

    public String[] permisosNecesarios() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    public boolean tienePermisos() {
        for (String p : permisosNecesarios()) {
            if (ContextCompat.checkSelfPermission(appContext, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void notificarEstado() {
        if (listener != null) listener.onEstado();
    }

    // ------------------------------------------------------------ Encender / apagar

    /** Llamar al abrir la app: si la pulsera quedó encendida, se reconecta sola. */
    public void iniciarSiEstaHabilitada() {
        if (habilitada && gatt == null && tienePermisos() && bluetoothActivo()) conectarGuardado();
    }

    public void setHabilitada(boolean on) {
        habilitada = on;
        prefs.edit().putBoolean(KEY_ON, on).apply();
        if (on) {
            if (gatt == null) conectarGuardado();
        } else {
            detenerEscaneo();
            cerrarGatt();
            conectando = false;
        }
        notificarEstado();
    }

    // ------------------------------------------------------------ Escaneo

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord record = result.getScanRecord();
            String nombre = record != null ? record.getDeviceName() : null;
            if (nombre == null) nombre = result.getDevice().getName();
            if (nombre == null) return; // se ignoran dispositivos sin nombre
            if (listener != null) listener.onDispositivo(result.getDevice(), nombre, result.getRssi());
        }

        @Override
        public void onScanFailed(int errorCode) {
            escaneando = false;
            if (listener != null) listener.onEscaneoTerminado();
            Toast.makeText(appContext, "Error al buscar dispositivos (" + errorCode + ")", Toast.LENGTH_SHORT).show();
        }
    };

    public void iniciarEscaneo() {
        if (adapter == null || !tienePermisos() || !adapter.isEnabled() || escaneando) return;
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) return;
        escaneando = true;
        scanner.startScan(scanCallback);
        main.postDelayed(detenerEscaneoRunnable, ESCANEO_MS);
        notificarEstado();
    }

    public void detenerEscaneo() {
        main.removeCallbacks(detenerEscaneoRunnable);
        if (escaneando && scanner != null && adapter != null && adapter.isEnabled()) {
            try { scanner.stopScan(scanCallback); } catch (Exception ignored) {}
        }
        boolean eraEscaneando = escaneando;
        escaneando = false;
        if (eraEscaneando && listener != null) listener.onEscaneoTerminado();
    }

    // ------------------------------------------------------------ Conexión

    private void conectarGuardado() {
        String addr = prefs.getString(KEY_ADDR, null);
        if (addr == null || adapter == null) return;
        try {
            conectar(adapter.getRemoteDevice(addr), prefs.getString(KEY_NAME, null));
        } catch (IllegalArgumentException ignored) {}
    }

    public void conectar(BluetoothDevice device, String nombre) {
        if (adapter == null || !tienePermisos()) return;
        detenerEscaneo();
        cerrarGatt();
        habilitada = true;
        conectando = true;
        nombreDispositivo = nombre != null ? nombre : device.getAddress();
        prefs.edit()
                .putBoolean(KEY_ON, true)
                .putString(KEY_ADDR, device.getAddress())
                .putString(KEY_NAME, nombreDispositivo)
                .apply();
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        notificarEstado();
    }

    private void cerrarGatt() {
        main.removeCallbacks(reintentoRunnable);
        if (gatt != null) {
            try { gatt.close(); } catch (Exception ignored) {}
            gatt = null;
        }
        cola.clear();
        ocupado = false;
        conectada = false;
        bateria = -1;
    }

    // ------------------------------------------------------------ Cola de operaciones GATT

    private void encolar(Runnable r) {
        cola.add(r);
        if (!ocupado) siguiente();
    }

    private void siguiente() {
        Runnable r = cola.poll();
        if (r == null) { ocupado = false; return; }
        ocupado = true;
        r.run();
    }

    private void leer(BluetoothGattCharacteristic c) {
        encolar(() -> {
            if (gatt == null || !gatt.readCharacteristic(c)) siguiente();
        });
    }

    private void activarNotificaciones(BluetoothGattCharacteristic c) {
        encolar(() -> {
            if (gatt == null) { siguiente(); return; }
            gatt.setCharacteristicNotification(c, true);
            BluetoothGattDescriptor d = c.getDescriptor(CCCD);
            if (d == null) { siguiente(); return; }
            boolean ok;
            if (Build.VERSION.SDK_INT >= 33) {
                ok = gatt.writeDescriptor(d, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        == BluetoothStatusCodes.SUCCESS;
            } else {
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                ok = gatt.writeDescriptor(d);
            }
            if (!ok) siguiente();
        });
    }

    // ------------------------------------------------------------ Callbacks GATT

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            main.post(() -> {
                if (g != gatt) {
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) g.close();
                    return;
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    conectada = true;
                    conectando = false;
                    notificarEstado();
                    g.discoverServices();
                } else {
                    cerrarGatt();
                    conectando = false;
                    notificarEstado();
                    if (habilitada) main.postDelayed(reintentoRunnable, REINTENTO_MS);
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            main.post(() -> {
                if (g != gatt) return;
                BluetoothGattService bat = g.getService(BATTERY_SERVICE);
                BluetoothGattCharacteristic batCh = bat != null ? bat.getCharacteristic(BATTERY_LEVEL) : null;
                BluetoothGattService alerta = g.getService(ALERTA_SERVICE);
                BluetoothGattCharacteristic alertaCh = alerta != null ? alerta.getCharacteristic(ALERTA_CHAR) : null;

                if (batCh != null) {
                    leer(batCh);
                    activarNotificaciones(batCh);
                }
                if (alertaCh != null) {
                    activarNotificaciones(alertaCh);
                } else {
                    Toast.makeText(appContext,
                            "Este dispositivo no tiene el servicio de alertas de Centinela",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor d, int status) {
            main.post(PulseraManager.this::siguiente);
        }

        // Android 12 y anteriores
        @Override
        public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, int status) {
            byte[] v = c.getValue();
            UUID u = c.getUuid();
            main.post(() -> { procesarValor(u, v); siguiente(); });
        }

        // Android 13+
        @Override
        public void onCharacteristicRead(BluetoothGatt g, BluetoothGattCharacteristic c, byte[] value, int status) {
            UUID u = c.getUuid();
            main.post(() -> { procesarValor(u, value); siguiente(); });
        }

        // Android 12 y anteriores
        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c) {
            byte[] v = c.getValue();
            UUID u = c.getUuid();
            main.post(() -> procesarValor(u, v));
        }

        // Android 13+
        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic c, byte[] value) {
            UUID u = c.getUuid();
            main.post(() -> procesarValor(u, value));
        }
    };

    private void procesarValor(UUID uuid, byte[] v) {
        if (v == null || v.length == 0) return;
        int valor = v[0] & 0xFF;
        if (BATTERY_LEVEL.equals(uuid)) {
            bateria = Math.min(valor, 100);
            notificarEstado();
        } else if (ALERTA_CHAR.equals(uuid)) {
            if (habilitada) dispararAlerta(valor);
        }
    }

    // ------------------------------------------------------------ Alertas

    /** Para probar la alerta sin tener la pulsera conectada. */
    public void simularAlerta(int tipo) {
        dispararAlerta(tipo);
    }

    private void dispararAlerta(int tipo) {
        long ahora = SystemClock.elapsedRealtime();
        if (ultimaAlerta != 0 && ahora - ultimaAlerta < ANTIREBOTE_MS) return; // evita dobles disparos
        ultimaAlerta = ahora;

        String origen = (tipo == ALERTA_MOVIMIENTO)
                ? "Movimiento brusco detectado por la pulsera"
                : "Botón de la pulsera";
        Activity a = actividadActual != null ? actividadActual.get() : null;
        SosHelper.disparar(appContext, a, origen);
    }
}
package com.example.hackeatonbackend; // Usa la misma línea package que tiene tu MainActivity.java
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class CentinelaBleManager {
    public interface Listener {
        void onEstado(String estado);
        void onTelemetria(String datos);
        void onAlertaSos(int tipoAlerta, String motivo);
    }

    private static final String NOMBRE_OBJETIVO = "Centinela-Pulsera";
    private static final UUID UUID_SERVICIO_ALERTA = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CH_ALERTA     = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CH_TELEMETRIA = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CCCD          = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Queue<BluetoothGattDescriptor> colaDescriptores = new LinkedList<>();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private boolean escaneando = false;

    public CentinelaBleManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        BluetoothManager bm = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm != null) bluetoothAdapter = bm.getAdapter();
    }

    public static void pedirPermisos(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, requestCode);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, requestCode);
        }
    }

    private boolean tienePermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public void iniciarBusqueda() {
        if (!tienePermisos()) {
            notificarEstado("Faltan permisos Bluetooth");
            return;
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            notificarEstado("Enciende el Bluetooth del celular");
            return;
        }
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null || escaneando) return;

        escaneando = true;
        notificarEstado("Buscando Centinela-Pulsera...");
        scanner.startScan(scanCallback);
        mainHandler.postDelayed(this::detenerBusqueda, 12000);
    }

    @SuppressLint("MissingPermission")
    public void detenerBusqueda() {
        if (scanner != null && escaneando) {
            scanner.stopScan(scanCallback);
            escaneando = false;
        }
    }

    @SuppressLint("MissingPermission")
    public void cerrar() {
        detenerBusqueda();
        if (gatt != null) {
            gatt.close();
            gatt = null;
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String nombre = device.getName();
            if (NOMBRE_OBJETIVO.equals(nombre)) {
                detenerBusqueda();
                notificarEstado("Conectando a " + nombre + "...");
                gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                notificarEstado("Conectado a Centinela-Pulsera");
                g.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                notificarEstado("Desconectado. Reintentando...");
                mainHandler.postDelayed(() -> iniciarBusqueda(), 1500);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            BluetoothGattService servicio = g.getService(UUID_SERVICIO_ALERTA);
            if (servicio == null) {
                notificarEstado("Servicio 0xFF00 no encontrado");
                return;
            }
            colaDescriptores.clear();
            prepararNotificacion(g, servicio.getCharacteristic(UUID_CH_ALERTA));
            prepararNotificacion(g, servicio.getCharacteristic(UUID_CH_TELEMETRIA));
            escribirSiguienteDescriptor(g);
        }

        @SuppressLint("MissingPermission")
        private void prepararNotificacion(BluetoothGatt g, BluetoothGattCharacteristic ch) {
            if (ch == null) return;
            g.setCharacteristicNotification(ch, true);
            BluetoothGattDescriptor desc = ch.getDescriptor(UUID_CCCD);
            if (desc != null) {
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                colaDescriptores.add(desc);
            }
        }

        @SuppressLint("MissingPermission")
        private void escribirSiguienteDescriptor(BluetoothGatt g) {
            BluetoothGattDescriptor siguiente = colaDescriptores.poll();
            if (siguiente != null) {
                g.writeDescriptor(siguiente);
            } else {
                notificarEstado("Listo: escuchando sensores y SOS");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor, int status) {
            escribirSiguienteDescriptor(g);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic ch) {
            UUID uuid = ch.getUuid();
            byte[] valor = ch.getValue();
            if (valor == null || valor.length == 0) return;

            if (UUID_CH_ALERTA.equals(uuid)) {
                int codigo = valor[0] & 0xFF;
                if (codigo == 1) {
                    dispararSos(1, "Botón SOS presionado");
                } else if (codigo == 2) {
                    dispararSos(2, "Movimiento brusco detectado");
                }
            } else if (UUID_CH_TELEMETRIA.equals(uuid)) {
                String texto = new String(valor, StandardCharsets.UTF_8);
                mainHandler.post(() -> listener.onTelemetria(texto));
            }
        }
    };

    private void notificarEstado(String estado) {
        mainHandler.post(() -> listener.onEstado(estado));
    }

    private void dispararSos(int tipo, String motivo) {
        mainHandler.post(() -> listener.onAlertaSos(tipo, motivo));
    }
}
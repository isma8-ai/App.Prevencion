package com.example.hackeatonbackend;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class pulsera extends AppCompatActivity implements PulseraManager.Listener {

    private PulseraManager manager;
    private TextView tvEstado, tvBateria, tvDispositivo;
    private ImageView imgPulsera;
    private MaterialSwitch swPulsera;
    private Button btnBuscar;
    private ListView listDispositivos;

    private final List<BluetoothDevice> dispositivos = new ArrayList<>();
    private final List<String> nombres = new ArrayList<>();
    private final List<String> etiquetas = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private boolean actualizandoSwitch = false;
    private Runnable pendiente;

    private final ActivityResultLauncher<String[]> permisosLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (manager.tienePermisos()) {
                    requerirBluetooth(pendiente);
                } else {
                    Toast.makeText(this, "Se necesita el permiso de Bluetooth para usar la pulsera",
                            Toast.LENGTH_LONG).show();
                    refrescarUI();
                }
            });

    private final ActivityResultLauncher<Intent> activarBtLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (manager.bluetoothActivo()) {
                    ejecutarPendiente();
                } else {
                    Toast.makeText(this, "Bluetooth sigue apagado", Toast.LENGTH_SHORT).show();
                    refrescarUI();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_pulsera);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        manager = PulseraManager.get(this);

        tvEstado = findViewById(R.id.tvEstado);
        tvBateria = findViewById(R.id.tvBateria);
        tvDispositivo = findViewById(R.id.tvDispositivo);
        imgPulsera = findViewById(R.id.imgPulsera);
        swPulsera = findViewById(R.id.swPulsera);
        btnBuscar = findViewById(R.id.btnBuscar);
        listDispositivos = findViewById(R.id.listDispositivos);
        Button btnProbar = findViewById(R.id.btnProbar);
        Button btnVolver = findViewById(R.id.btnVolverPulsera);

        adapter = new ArrayAdapter<>(this, R.layout.item_dispositivo, R.id.tvItem, etiquetas);
        listDispositivos.setAdapter(adapter);

        if (!manager.bluetoothDisponible()) {
            Toast.makeText(this, "Este dispositivo no tiene Bluetooth", Toast.LENGTH_LONG).show();
        }

        swPulsera.setOnCheckedChangeListener((b, activado) -> {
            if (actualizandoSwitch) return;
            if (activado) {
                requerirBluetooth(() -> manager.setHabilitada(true));
            } else {
                manager.setHabilitada(false);
            }
            refrescarUI();
        });

        btnBuscar.setOnClickListener(v -> {
            if (manager.isEscaneando()) {
                manager.detenerEscaneo();
            } else {
                requerirBluetooth(() -> {
                    dispositivos.clear();
                    nombres.clear();
                    etiquetas.clear();
                    adapter.notifyDataSetChanged();
                    manager.iniciarEscaneo();
                    refrescarUI();
                });
            }
        });

        listDispositivos.setOnItemClickListener((parent, view, position, id) ->
                requerirBluetooth(() -> {
                    manager.conectar(dispositivos.get(position), nombres.get(position));
                    Toast.makeText(this, "Conectando a " + nombres.get(position) + "…", Toast.LENGTH_SHORT).show();
                }));

        btnProbar.setOnClickListener(v -> manager.simularAlerta(PulseraManager.ALERTA_BOTON));
        btnVolver.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.setListener(this);
        refrescarUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.detenerEscaneo();
        manager.setListener(null);
    }

    // ---------------------------------------------------- Permisos y Bluetooth

    private void requerirBluetooth(Runnable accion) {
        pendiente = accion;
        if (!manager.bluetoothDisponible()) {
            Toast.makeText(this, "Este dispositivo no tiene Bluetooth", Toast.LENGTH_SHORT).show();
            refrescarUI();
            return;
        }
        if (!manager.tienePermisos()) {
            permisosLauncher.launch(manager.permisosNecesarios());
            return;
        }
        if (!manager.bluetoothActivo()) {
            activarBtLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }
        ejecutarPendiente();
    }

    private void ejecutarPendiente() {
        Runnable r = pendiente;
        pendiente = null;
        if (r != null) r.run();
        refrescarUI();
    }

    // ---------------------------------------------------- UI

    private void refrescarUI() {
        actualizandoSwitch = true;
        swPulsera.setChecked(manager.isHabilitada());
        actualizandoSwitch = false;

        if (!manager.isHabilitada()) {
            tvEstado.setText("⚫ Apagada");
            tvEstado.setTextColor(0xFF9E9E9E);
            imgPulsera.setAlpha(0.4f);
        } else if (manager.isConectada()) {
            tvEstado.setText("🟢 Conectada");
            tvEstado.setTextColor(0xFF00E676);
            imgPulsera.setAlpha(1f);
        } else if (manager.isConectando()) {
            tvEstado.setText("🟡 Conectando…");
            tvEstado.setTextColor(0xFFFFD54F);
            imgPulsera.setAlpha(0.7f);
        } else {
            tvEstado.setText("🔴 Desconectada");
            tvEstado.setTextColor(0xFFFF5252);
            imgPulsera.setAlpha(0.7f);
        }

        int bat = manager.getBateria();
        tvBateria.setText(bat >= 0 && manager.isConectada() ? "🔋 Batería: " + bat + "%" : "🔋 Batería: --");

        String nombre = manager.getNombreDispositivo();
        tvDispositivo.setText(nombre != null ? "Dispositivo: " + nombre : "Sin dispositivo vinculado");

        btnBuscar.setText(manager.isEscaneando() ? "⏹ Detener búsqueda" : "📡 Buscar dispositivos Bluetooth");
    }

    // ---------------------------------------------------- Listener del manager

    @Override
    public void onEstado() {
        refrescarUI();
    }

    @Override
    public void onDispositivo(BluetoothDevice device, String nombre, int rssi) {
        String etiqueta = nombre + "\n" + device.getAddress() + "  ·  " + rssi + " dBm";
        for (int i = 0; i < dispositivos.size(); i++) {
            if (dispositivos.get(i).getAddress().equals(device.getAddress())) {
                etiquetas.set(i, etiqueta);
                adapter.notifyDataSetChanged();
                return;
            }
        }
        dispositivos.add(device);
        nombres.add(nombre);
        etiquetas.add(etiqueta);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onEscaneoTerminado() {
        refrescarUI();
    }
}
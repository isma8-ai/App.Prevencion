package com.example.hackeatonbackend;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class historial extends AppCompatActivity {

    private ListView listViewHistorial;
    private TextView tvVacio;
    private EventoAdapter adapter;
    private final List<HistorialManager.Evento> eventos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_historial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listViewHistorial = findViewById(R.id.listViewHistorial);
        tvVacio = findViewById(R.id.tvHistorialVacio);
        Button button10 = findViewById(R.id.button10);
        Button btnLimpiar = findViewById(R.id.btnLimpiarHistorial);

        adapter = new EventoAdapter();
        listViewHistorial.setAdapter(adapter);
        listViewHistorial.setEmptyView(tvVacio);

        button10.setOnClickListener(v -> {
            Intent intent = SesionRolHelper.intentMenuPrincipal(historial.this);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnLimpiar.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Borrar historial")
                .setMessage("¿Seguro que quieres borrar todo el historial?")
                .setPositiveButton("Borrar", (d, w) -> {
                    HistorialManager.limpiar(this);
                    cargarEventos();
                    Toast.makeText(this, "Historial borrado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEventos();
    }

    private void cargarEventos() {
        eventos.clear();
        eventos.addAll(HistorialManager.obtener(this));
        adapter.notifyDataSetChanged();
    }

    private class EventoAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return eventos.size();
        }

        @Override
        public HistorialManager.Evento getItem(int position) {
            return eventos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(historial.this)
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            HistorialManager.Evento e = getItem(position);
            TextView t1 = view.findViewById(android.R.id.text1);
            TextView t2 = view.findViewById(android.R.id.text2);
            t1.setText(e.getIcono() + "  " + e.getTitulo());
            t2.setText(e.descripcion + "\n" + e.fecha);
            return view;
        }
    }
}
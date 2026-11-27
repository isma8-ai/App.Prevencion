package com.example.hackeatonbackend;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestionUsuariosAdminActivity extends AppCompatActivity {

    private final List<UserManager.Usuario> todosUsuarios = new ArrayList<>();
    private final List<UserManager.Usuario> filtrados = new ArrayList<>();
    private final List<String> etiquetas = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_gestion_usuarios_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etBuscar = findViewById(R.id.etBuscarUsuario);
        ListView listUsuarios = findViewById(R.id.listUsuariosAdmin);
        Button btnVolver = findViewById(R.id.btnVolverAdminUsuarios);

        UserManager.inicializarUsuariosPorDefecto(this);
        todosUsuarios.addAll(UserManager.obtenerUsuarios(this));

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etiquetas);
        listUsuarios.setAdapter(adapter);

        aplicarFiltro("");

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltro(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listUsuarios.setOnItemClickListener((parent, view, position, id) -> {
            UserManager.Usuario u = filtrados.get(position);
            mostrarDetalleUsuario(u);
        });

        btnVolver.setOnClickListener(v -> finish());
    }

    private void aplicarFiltro(String query) {
        filtrados.clear();
        etiquetas.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (UserManager.Usuario u : todosUsuarios) {
            if (q.isEmpty() || u.username.toLowerCase(Locale.ROOT).contains(q)) {
                filtrados.add(u);
                etiquetas.add("Nombre de usuario: " + u.username + " | Tipo de usuario: " + u.role);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void mostrarDetalleUsuario(UserManager.Usuario u) {
        String datos = "Nombre de usuario: " + u.username + "\n"
                + "Tipo de usuario: " + u.role + "\n"
                + "Nombre completo: " + u.fullName + "\n"
                + "ID Centinela: " + u.uniqueId + "\n"
                + "Teléfono: " + (u.phone.isEmpty() ? "No registrado" : u.phone) + "\n"
                + "Fecha de nacimiento: " + (u.dob.isEmpty() ? "No registrada" : u.dob) + "\n"
                + "Correo: " + (u.email.isEmpty() ? "No registrado" : u.email) + "\n"
                + "Tipo de sangre: " + (u.bloodType.isEmpty() ? "No especificado" : u.bloodType) + "\n"
                + "Alergias: " + (u.allergies.isEmpty() ? "Ninguna" : u.allergies) + "\n"
                + "Info médica: " + (u.medicalInfo.isEmpty() ? "Ninguna" : u.medicalInfo);

        new AlertDialog.Builder(this)
                .setTitle("Datos de " + u.username)
                .setMessage(datos)
                .setPositiveButton("Ver Historial", (d, w) -> mostrarHistorialUsuario(u))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void mostrarHistorialUsuario(UserManager.Usuario u) {
        List<HistorialManager.Evento> eventos = HistorialManager.obtenerPorUsuario(this, u.username);
        StringBuilder sb = new StringBuilder();
        if (eventos.isEmpty()) {
            sb.append("Sin eventos individuales registrados para ").append(u.username).append(".");
        } else {
            for (HistorialManager.Evento e : eventos) {
                sb.append(e.getIcono()).append(" ").append(e.getTitulo())
                        .append(" (").append(e.fecha).append(")\n")
                        .append(e.descripcion).append("\n\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Historial de " + u.username)
                .setMessage(sb.toString().trim())
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
package com.example.hackeatonbackend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class chat extends AppCompatActivity {

    private static class MensajeComunitario {
        final String texto;
        final String autorReal;
        final boolean anonimo;

        MensajeComunitario(String texto, String autorReal, boolean anonimo) {
            this.texto = texto;
            this.autorReal = autorReal;
            this.anonimo = anonimo;
        }
    }

    private final ArrayList<MensajeComunitario> mensajesData = new ArrayList<>();
    private final ArrayList<String> displayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private boolean isAdmin = false;
    private String usuarioActual = "Anónimo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        isAdmin = SesionRolHelper.esAdmin(this);
        usuarioActual = prefs.getString("usuarioSesion", prefs.getString("username", "Anónimo"));

        ListView listViewMessages = findViewById(R.id.listViewMessages);
        EditText etMessage = findViewById(R.id.etMessage);
        Button btnSend = findViewById(R.id.btnSend);
        Button button12 = findViewById(R.id.button12);
        TextView tvPrivacyNotice = findViewById(R.id.tvPrivacyNotice);

        if (isAdmin) {
            tvPrivacyNotice.setText("🔓 Vista Admin: Se muestra el nombre de usuario de autores en reportes anónimos.");
        }

        mensajesData.add(new MensajeComunitario(
                "🛡️ [Aviso de Privacidad]: Tus comentarios y reportes son anónimos para usuarias comunes.",
                "Sistema", false));
        mensajesData.add(new MensajeComunitario(
                "⚠️ [Alerta Comunitaria] Zona Parque Central: Reportes de acoso por la noche.",
                "marisol_lm", true));
        mensajesData.add(new MensajeComunitario(
                "⚠️ [Alerta Comunitaria] Calle Juárez: Falta iluminación por la noche.",
                "valeria_99", true));

        refrescarLista();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listViewMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String reportText = etMessage.getText().toString().trim();
            if (!reportText.isEmpty()) {
                mensajesData.add(new MensajeComunitario(
                        "🚨 [Reporte Anónimo de Prevención]: " + reportText,
                        usuarioActual,
                        true
                ));
                refrescarLista();
                adapter.notifyDataSetChanged();
                etMessage.setText("");
                listViewMessages.smoothScrollToPosition(displayList.size() - 1);
                Toast.makeText(this, "Alerta publicada", Toast.LENGTH_SHORT).show();
                HistorialManager.registrar(this, HistorialManager.TIPO_CHAT,
                        "Publicaste un mensaje anónimo");
            } else {
                Toast.makeText(this, "Escribe una descripción de la situación", Toast.LENGTH_SHORT).show();
            }
        });

        button12.setOnClickListener(v -> {
            Intent intent = SesionRolHelper.intentMenuPrincipal(chat.this);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void refrescarLista() {
        displayList.clear();
        for (MensajeComunitario m : mensajesData) {
            if (m.anonimo && isAdmin) {
                displayList.add(m.texto + "  [Usuario: " + m.autorReal + "]");
            } else {
                displayList.add(m.texto);
            }
        }
    }
}
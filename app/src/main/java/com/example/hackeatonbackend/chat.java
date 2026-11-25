package com.example.hackeatonbackend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class chat extends AppCompatActivity {

    private ArrayList<String> reportList;
    private ArrayAdapter<String> adapter;
    private EditText etMessage;
    private ListView listViewMessages;

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

        listViewMessages = findViewById(R.id.listViewMessages);
        etMessage = findViewById(R.id.etMessage);
        Button btnSend = findViewById(R.id.btnSend);
        Button button12 = findViewById(R.id.button12);

        reportList = new ArrayList<>();
        // Mensaje informativo de seguridad y privacidad
        reportList.add("🛡️ [Aviso de Privacidad]: Tus comentarios y reportes son completamente anónimos para la comunidad. Solo la Asociación de Mujeres tendrá acceso a tus datos protegidos por seguridad.");
        // Reportes comunitarios de prevención previos
        reportList.add("⚠️ [Alerta Comunitaria] Zona Parque Central: Reportes de acoso por la noche. ¡Tengan cuidado!");
        reportList.add("⚠️ [Alerta Comunitaria] Calle Juárez: Falta iluminación y se reportó intento de asalto anoche.");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reportList);
        listViewMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String reportText = etMessage.getText().toString().trim();
            if (!reportText.isEmpty()) {
                reportList.add("🚨 [Reporte Anónimo de Prevención]: " + reportText);
                adapter.notifyDataSetChanged();
                etMessage.setText("");
                listViewMessages.smoothScrollToPosition(reportList.size() - 1);
                Toast.makeText(this, "Alerta publicada de forma anónima para advertir a la comunidad", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Escribe una descripción de la situación", Toast.LENGTH_SHORT).show();
            }
        });

        button12.setOnClickListener(v -> {
            Intent intent = new Intent(chat.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
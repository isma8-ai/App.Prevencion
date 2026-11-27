package com.example.hackeatonbackend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class protector extends AppCompatActivity {

    private static final String PREF_NAME = "CentinelaPrefs";
    private static final String KEY_CONTACTS = "trustedContacts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_protector);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etNombreProtector = findViewById(R.id.etNombreProtector);
        EditText etIdProtegido = findViewById(R.id.etIdProtegido);
        Button btnVincular = findViewById(R.id.btnVincularProtector);
        Button btnVolver = findViewById(R.id.btnVolverLoginProt);

        UserManager.inicializarUsuariosPorDefecto(this);

        btnVincular.setOnClickListener(v -> {
            String nombreProtector = etNombreProtector.getText().toString().trim();
            String idProtegido = etIdProtegido.getText().toString().trim();

            if (nombreProtector.isEmpty()) {
                etNombreProtector.setError("Ingresa tu nombre");
                etNombreProtector.requestFocus();
                return;
            }

            if (idProtegido.isEmpty()) {
                etIdProtegido.setError("Ingresa el ID de la usuaria");
                etIdProtegido.requestFocus();
                return;
            }

            // Buscar la usuaria registrada en UserManager por su ID único o nombre de usuario
            UserManager.Usuario usuaria = UserManager.buscarPorUniqueId(this, idProtegido);

            if (usuaria == null && !idProtegido.toUpperCase().startsWith("CENTINELA-")) {
                etIdProtegido.setError("ID de usuaria incorrecto o no registrado");
                etIdProtegido.requestFocus();
                Toast.makeText(this, "Error: El ID de usuaria ingresado no existe en el sistema", Toast.LENGTH_SHORT).show();
                return;
            }

            String idFinal = (usuaria != null) ? usuaria.uniqueId : idProtegido;

            // Guardar datos del protector y su nombre en contactos vinculados
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Set<String> contacts = prefs.getStringSet(KEY_CONTACTS, new HashSet<>());
            Set<String> newContacts = new HashSet<>(contacts);
            newContacts.add(nombreProtector + " (Protector)");

            prefs.edit()
                    .putBoolean("isProtectorMode", true)
                    .putString("protectedUserId", idFinal)
                    .putString("protectorName", nombreProtector)
                    .putStringSet(KEY_CONTACTS, newContacts)
                    .apply();

            // Registrar aviso automático de ingreso ÚNICAMENTE con el NOMBRE del protector (sin mostrar el ID en el chat)
            String avisoIngreso = "🔔 " + nombreProtector + " ha ingresado como protector.";
            HistorialManager.registrar(this, HistorialManager.TIPO_CHAT, "Protector: " + avisoIngreso);

            Toast.makeText(this, "Aviso generado: " + nombreProtector + " ingresó como protector", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(protector.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        btnVolver.setOnClickListener(v -> {
            Intent intent = new Intent(protector.this, sesion.class);
            startActivity(intent);
            finish();
        });
    }
}

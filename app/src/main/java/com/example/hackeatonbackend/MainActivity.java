package com.example.hackeatonbackend;

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

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, chat.class);
            startActivity(intent);
        });

        Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, mapa.class);
            startActivity(intent);
        });

        Button button6 = findViewById(R.id.button6);
        button6.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, historial.class);
            startActivity(intent);
        });

        Button button7 = findViewById(R.id.button7);
        button7.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, configuracion.class);
            startActivity(intent);
        });

        Button btnTopMenu = findViewById(R.id.btnTopMenu);
        if (btnTopMenu != null) {
            btnTopMenu.setOnClickListener(v -> showQuickMenuDialog());
        }
    }

    private String getOrCreateMyId() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String myId = prefs.getString(KEY_MY_ID, null);
        if (myId == null || myId.isEmpty()) {
            myId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            prefs.edit().putString(KEY_MY_ID, myId).apply();
        }
        return myId;
    }

    private void showQuickMenuDialog() {
        String[] options = {"➕ Agregar Contacto por ID", "🆔 Ver Mi ID Único", "👥 Ver Mis Contactos Vinculados"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Menú Rápido - Centinela");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showAddContactDialog();
            } else if (which == 1) {
                showMyIdDialog();
            } else if (which == 2) {
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
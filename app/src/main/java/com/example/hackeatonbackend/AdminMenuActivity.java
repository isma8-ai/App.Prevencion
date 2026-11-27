package com.example.hackeatonbackend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_admin_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("isAdminMode", true).putBoolean("isProtectorMode", false).apply();

        findViewById(R.id.btnAdminGestionUsuarios).setOnClickListener(v ->
                startActivity(new Intent(this, GestionUsuariosAdminActivity.class)));

        findViewById(R.id.btnAdminSos).setOnClickListener(v ->
                SosHelper.disparar(this, this, "Botón SOS (Admin)"));

        findViewById(R.id.btnAdminMapa).setOnClickListener(v ->
                startActivity(new Intent(this, mapa.class)));

        findViewById(R.id.btnAdminChatAnonimo).setOnClickListener(v ->
                startActivity(new Intent(this, chat.class)));

        findViewById(R.id.btnAdminBandeja).setOnClickListener(v ->
                startActivity(new Intent(this, BandejaChatActivity.class)));

        findViewById(R.id.btnAdminPulsera).setOnClickListener(v ->
                startActivity(new Intent(this, pulsera.class)));

        findViewById(R.id.btnAdminHistorial).setOnClickListener(v ->
                startActivity(new Intent(this, historial.class)));

        findViewById(R.id.btnAdminPerfil).setOnClickListener(v ->
                startActivity(new Intent(this, perfil.class)));

        findViewById(R.id.btnAdminAcerca).setOnClickListener(v ->
                startActivity(new Intent(this, AcercaDeActivity.class)));

        Button btnCerrar = findViewById(R.id.btnAdminCerrarSesion);
        btnCerrar.setOnClickListener(v -> {
            prefs.edit().putBoolean("isAdminMode", false).apply();
            Intent i = new Intent(this, sesion.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}
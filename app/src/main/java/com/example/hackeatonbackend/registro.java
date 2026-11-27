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

public class registro extends AppCompatActivity {

    private static final String PREF_NAME = "CentinelaPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_registro);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etNombre = findViewById(R.id.etNombreReg);
        EditText etUsuario = findViewById(R.id.etUsuarioReg);
        EditText etPassword = findViewById(R.id.etPasswordReg);
        Button btnRegistrar = findViewById(R.id.btnRegistrar);
        Button btnVolver = findViewById(R.id.btnVolverLogin);

        btnRegistrar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String usuario = etUsuario.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (nombre.isEmpty() || usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            UserManager.Usuario nuevoUser = UserManager.registrarNuevaUsuaria(this, usuario, password, nombre);

            // 1. Guardar primero la sesión y el rol
            SesionRolHelper.guardarRol(this, SesionRolHelper.ROL_USUARIA, usuario);
            SosHelper.guardarUsuarioSesion(this, usuario);

            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("fullName", nombre)
                    .putString("password", password)
                    .putString("myUniqueId", nuevoUser.uniqueId)
                    .putString("protectedUserId", "")
                    .apply();

            // 2. Registrar el evento ahora que la sesión ya está activa
            HistorialManager.registrar(this, HistorialManager.TIPO_LOGIN,
                    "Registro de nueva cuenta: " + usuario + " (ID: " + nuevoUser.uniqueId + ")");

            Toast.makeText(this, "¡Cuenta creada con éxito! Tu ID es: " + nuevoUser.uniqueId, Toast.LENGTH_LONG).show();

            Intent intent = SesionRolHelper.intentMenuPrincipal(registro.this);
            startActivity(intent);
            finish();
        });

        btnVolver.setOnClickListener(v -> {
            Intent intent = new Intent(registro.this, sesion.class);
            startActivity(intent);
            finish();
        });
    }
}
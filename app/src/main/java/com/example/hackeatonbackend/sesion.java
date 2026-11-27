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

public class sesion extends AppCompatActivity {

    private static final String PREF_NAME = "CentinelaPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_sesion);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etUsuario = findViewById(R.id.editTextText);
        EditText etPassword = findViewById(R.id.editTextTextPassword);
        Button btnEntrar = findViewById(R.id.button);
        Button btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        Button btnIngresarProtector = findViewById(R.id.btnIngresarProtector);

        UserManager.inicializarUsuariosPorDefecto(this);

        btnEntrar.setOnClickListener(v -> {
            String usuarioIngresado = etUsuario.getText().toString().trim();
            String passwordIngresada = etPassword.getText().toString().trim();

            if (usuarioIngresado.isEmpty()) {
                etUsuario.setError("Ingresa el usuario");
                etUsuario.requestFocus();
                return;
            }

            if (passwordIngresada.isEmpty()) {
                etPassword.setError("Ingresa la contraseña");
                etPassword.requestFocus();
                return;
            }

            UserManager.Usuario user = UserManager.validarLogin(this, usuarioIngresado, passwordIngresada);

            if (user != null) {
                Toast.makeText(this, "Bienvenido, " + user.fullName + " [" + user.role + "]", Toast.LENGTH_SHORT).show();

                SesionRolHelper.guardarRol(this, user.role, usuarioIngresado);

                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putString("fullName", user.fullName)
                        .putString("myUniqueId", user.uniqueId)
                        .putString("protectedUserId", SesionRolHelper.esProtector(this) ? "UsuariaProtegida" : "")
                        .apply();

                HistorialManager.registrar(this, HistorialManager.TIPO_LOGIN,
                        "Inicio de sesión como " + usuarioIngresado + " (" + user.role + ")");
                SosHelper.guardarUsuarioSesion(this, usuarioIngresado);

                Intent intent = SesionRolHelper.intentMenuPrincipal(sesion.this);
                intent.putExtra("USUARIO", usuarioIngresado);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });

        btnCrearCuenta.setOnClickListener(v -> {
            Intent intent = new Intent(sesion.this, registro.class);
            startActivity(intent);
        });

        btnIngresarProtector.setOnClickListener(v -> {
            Intent intent = new Intent(sesion.this, protector.class);
            startActivity(intent);
        });
    }
}
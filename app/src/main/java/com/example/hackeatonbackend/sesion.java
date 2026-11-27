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

        // Inicializar usuarios por defecto en UserManager
        UserManager.inicializarUsuariosPorDefecto(this);

        // Validar al hacer clic en el botón de iniciar sesión con UserManager
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

                boolean isProtector = user.role.equals("PROTECTOR");
                SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putBoolean("isProtectorMode", isProtector)
                        .putString("protectedUserId", isProtector ? "UsuariaProtegida" : "")
                        .apply();

                Intent intent = new Intent(sesion.this, MainActivity.class);
                intent.putExtra("USUARIO", usuarioIngresado);
                HistorialManager.registrar(this, HistorialManager.TIPO_LOGIN,
                        "Inicio de sesión como " + usuarioIngresado + " (" + user.role + ")");

                SosHelper.guardarUsuarioSesion(this, usuarioIngresado);

                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });

        // Navegación a Crear Cuenta
        btnCrearCuenta.setOnClickListener(v -> {
            Intent intent = new Intent(sesion.this, registro.class);
            startActivity(intent);
        });

        // Navegación a Ingresar como Protector
        btnIngresarProtector.setOnClickListener(v -> {
            Intent intent = new Intent(sesion.this, protector.class);
            startActivity(intent);
        });
    }
}

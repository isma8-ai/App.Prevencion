package com.example.hackeatonbackend;

import android.content.Intent;
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

    // PERFILES
    String nme = "User";
    String psw = "123123";

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
        EditText etPassword = findViewById(R.id.editTextTextPassword); // <-- Paréntesis cerrado
        Button btnEntrar = findViewById(R.id.button);

        // Validar al hacer clic en el botón
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

            if (usuarioIngresado.equals(nme) && passwordIngresada.equals(psw)) {
                Toast.makeText(this, "Bienvenido, " + nme, Toast.LENGTH_SHORT).show();

                // Cambia MainActivity.class por la Activity a la que quieras avanzar
                Intent intent = new Intent(sesion.this, MainActivity.class);
                intent.putExtra("USUARIO", usuarioIngresado);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
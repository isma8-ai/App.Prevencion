package com.example.hackeatonbackend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.UUID;

public class perfil extends AppCompatActivity {

    private static final String PREF_NAME = "CentinelaPrefs";
    private static final String KEY_MY_ID = "myUniqueId";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_DOB = "dob";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_BLOOD = "bloodType";
    private static final String KEY_ALLERGIES = "allergies";
    private static final String KEY_MEDICAL = "medicalInfo";

    private EditText etFullName, etDob, etPhone, etEmail, etBloodType, etAllergies, etMedicalInfo;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        TextView tvCentinelaId = findViewById(R.id.tvCentinelaId);
        etFullName = findViewById(R.id.etFullName);
        etDob = findViewById(R.id.etDob);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etBloodType = findViewById(R.id.etBloodType);
        etAllergies = findViewById(R.id.etAllergies);
        etMedicalInfo = findViewById(R.id.etMedicalInfo);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);
        Button btnSaveTop = findViewById(R.id.btnSaveTop);
        Button btnBack = findViewById(R.id.btnBack);

        // Generar o cargar ID Centinela
        String myId = prefs.getString(KEY_MY_ID, null);
        if (myId == null) {
            myId = "CENTINELA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            prefs.edit().putString(KEY_MY_ID, myId).apply();
        }
        tvCentinelaId.setText(myId);

        // Cargar datos guardados previamente
        etFullName.setText(prefs.getString(KEY_FULL_NAME, ""));
        etDob.setText(prefs.getString(KEY_DOB, ""));
        etPhone.setText(prefs.getString(KEY_PHONE, ""));
        etEmail.setText(prefs.getString(KEY_EMAIL, ""));
        etBloodType.setText(prefs.getString(KEY_BLOOD, ""));
        etAllergies.setText(prefs.getString(KEY_ALLERGIES, ""));
        etMedicalInfo.setText(prefs.getString(KEY_MEDICAL, ""));

        // Guardar perfil (método común)
        Runnable saveAction = () -> {
            String name = etFullName.getText().toString().trim();
            String dob = etDob.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String blood = etBloodType.getText().toString().trim();
            String allergies = etAllergies.getText().toString().trim();
            String medical = etMedicalInfo.getText().toString().trim();

            List<String> cambios = new ArrayList<>();
            if (!name.equals(prefs.getString(KEY_FULL_NAME, ""))) cambios.add("Nombre");
            if (!dob.equals(prefs.getString(KEY_DOB, ""))) cambios.add("Fecha de nacimiento");
            if (!phone.equals(prefs.getString(KEY_PHONE, ""))) cambios.add("Teléfono");
            if (!email.equals(prefs.getString(KEY_EMAIL, ""))) cambios.add("Correo");
            if (!blood.equals(prefs.getString(KEY_BLOOD, ""))) cambios.add("Tipo de sangre");
            if (!allergies.equals(prefs.getString(KEY_ALLERGIES, ""))) cambios.add("Alergias");
            if (!medical.equals(prefs.getString(KEY_MEDICAL, ""))) cambios.add("Info médica");

            prefs.edit()
                    .putString(KEY_FULL_NAME, name)
                    .putString(KEY_DOB, dob)
                    .putString(KEY_PHONE, phone)
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_BLOOD, blood)
                    .putString(KEY_ALLERGIES, allergies)
                    .putString(KEY_MEDICAL, medical)
                    .apply();

            if (!cambios.isEmpty()) {
                HistorialManager.registrar(this, HistorialManager.TIPO_PERFIL,
                        "Se modificó: " + String.join(", ", cambios));
            }
            Toast.makeText(this, "Perfil y Ficha de Emergencia guardados correctamente", Toast.LENGTH_SHORT).show();
        };

        btnSaveProfile.setOnClickListener(v -> saveAction.run());
        if (btnSaveTop != null) {
            btnSaveTop.setOnClickListener(v -> saveAction.run());
        }

        // Volver
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(perfil.this, MainActivity.class);
            startActivity(intent);
        });
    }
}

package com.example.hackeatonbackend;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class AcercaDeActivity extends AppCompatActivity {

    private static final String README_CONTENIDO =
            "# App.Prevencion\n" +
                    "Esta es una aplicacion pensada en la seguridad de los usuarios, donde afrontamos la problematica combinando hardware y software para la prevencion de posibles ataques o abusos en contra de la mujer.\n\n" +
                    "# 🛡️ Sistema Inteligente de Prevención y Atención de Emergencias\n\n" +
                    "**Proyecto de Hackatón | FIM-UAS 2026**\n\n" +
                    "## 📌 Descripción del proyecto\n\n" +
                    "Este proyecto busca desarrollar una solución tecnológica enfocada en la prevención y atención de situaciones de violencia y peligro mediante una aplicación móvil conectada a un dispositivo portátil inteligente (pulsera de emergencia).\n\n" +
                    "El objetivo principal es proporcionar a las personas una herramienta accesible, discreta y rápida para solicitar ayuda cuando se encuentren en una situación de riesgo.\n\n" +
                    "La solución combina el uso de software y hardware para crear un sistema de alerta que permita notificar a contactos de confianza y compartir la ubicación de la persona afectada.\n\n" +
                    "## 🎯 Problemática\n\n" +
                    "Muchas personas se enfrentan diariamente a situaciones de inseguridad, acoso o violencia en las que solicitar ayuda rápidamente puede resultar complicado.\n\n" +
                    "En determinados escenarios, utilizar directamente un teléfono celular para realizar una llamada o enviar un mensaje puede no ser una opción segura o viable.\n\n" +
                    "Por esta razón, se propone implementar un mecanismo alternativo de alerta que permita solicitar ayuda de manera discreta y sencilla.\n\n" +
                    "## 💡 Nuestra solución\n\n" +
                    "Desarrollar un sistema compuesto por dos elementos principales:\n\n" +
                    "### 📱 1. Aplicación móvil\n\n" +
                    "La aplicación será el centro de control del sistema. Sus funciones contempladas incluyen:\n\n" +
                    "- Registro e inicio de sesión de usuarios.\n" +
                    "- Configuración de contactos de emergencia.\n" +
                    "- Recepción de alertas enviadas desde la pulsera.\n" +
                    "- Obtención y envío de la ubicación del usuario.\n" +
                    "- Notificaciones de emergencia.\n" +
                    "- Consulta del historial de alertas.\n\n" +
                    "### ⌚ 2. Pulsera inteligente de emergencia\n\n" +
                    "Se plantea desarrollar un dispositivo portátil que permita activar una alerta mediante un botón físico.\n\n" +
                    "Características contempladas:\n\n" +
                    "- Diseño compacto y fácil de transportar.\n" +
                    "- Botón de activación de emergencia.\n" +
                    "- Comunicación inalámbrica con el teléfono mediante Bluetooth.\n" +
                    "- Envío de señales de emergencia a la aplicación.\n" +
                    "- Posibilidad de incorporar nuevas funciones en futuras versiones.\n\n" +
                    "## ⚙️ ¿Cómo funcionaría?\n\n" +
                    "1. El usuario instala la aplicación y configura sus contactos de emergencia.\n" +
                    "2. Vincula su pulsera inteligente con el teléfono.\n" +
                    "3. En caso de encontrarse en una situación de peligro, presiona el botón de emergencia.\n" +
                    "4. La pulsera envía una señal mediante Bluetooth a la aplicación.\n" +
                    "5. La aplicación procesa la alerta y obtiene la ubicación disponible del usuario.\n" +
                    "6. Se envía una notificación a los contactos de emergencia configurados.\n\n" +
                    "## 🧰 Tecnologías contempladas\n\n" +
                    "| Tecnología | Uso |\n" +
                    "|---|---|\n" +
                    "| java | Programación del microcontrolador |\n" +
                    "| ESP32 | Control y comunicación del dispositivo |\n" +
                    "| Bluetooth Low Energy (BLE) | Comunicación entre pulsera y teléfono |\n" +
                    "| Aplicación móvil | Interfaz y gestión de emergencias |\n" +
                    "| GPS | Obtención de ubicación mediante el teléfono |\n" +
                    "| Git y GitHub | Control de versiones y colaboración |\n\n" +
                    "*Las tecnologías y componentes podrán ajustarse durante el desarrollo del prototipo.*\n\n" +
                    "## 🚀 Objetivos\n\n" +
                    "- Desarrollar una herramienta tecnológica para la prevención y atención de emergencias.\n" +
                    "- Facilitar la solicitud de ayuda mediante un mecanismo discreto.\n" +
                    "- Integrar hardware y software en una sola solución.\n" +
                    "- Permitir la comunicación rápida con contactos de confianza.\n" +
                    "- Crear un prototipo funcional que pueda evolucionar hacia una solución de uso real.\n\n" +
                    "## 🔮 Mejoras futuras\n\n" +
                    "- Detección automática de posibles situaciones de emergencia.\n" +
                    "- Incorporación de sensores adicionales.\n" +
                    "- Alertas mediante detección de movimientos o patrones específicos.\n" +
                    "- Optimización del consumo energético de la pulsera.\n" +
                    "- Ampliación de la compatibilidad con diferentes dispositivos móviles.\n" +
                    "- Implementación de mecanismos de seguridad y privacidad para proteger los datos del usuario.\n\n" +
                    "## 👨‍💻 Estado del proyecto\n\n" +
                    "**Fase actual:** Planeación y desarrollo del prototipo.\n\n" +
                    "Este repositorio documentará el proceso de diseño, programación, integración de hardware y desarrollo de la aplicación móvil.\n\n" +
                    "## 🤝 Contribuciones\n\n" +
                    "El proyecto está siendo desarrollado en equipo como parte del Hackatón FIM-UAS 2026.\n\n" +
                    "Las mejoras, ideas y aportaciones que contribuyan al desarrollo de la solución son bienvenidas.\n\n" +
                    "---\n\n" +
                    "**Tecnología al servicio de la seguridad.** 🛡️";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_acerca_de);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("isAdminMode", false);

        TextView tvReadme = findViewById(R.id.tvReadmeContenido);
        Button btnVolver = findViewById(R.id.btnVolverAcerca);
        tvReadme.setText(README_CONTENIDO);

        if (isAdmin) {
            findViewById(R.id.main).setBackgroundColor(Color.parseColor("#C62828"));
            tvReadme.setBackgroundColor(Color.parseColor("#FFCDD2"));
            tvReadme.setTextColor(Color.parseColor("#4A0000"));
            btnVolver.setBackgroundColor(Color.parseColor("#8E0000"));
        }

        btnVolver.setOnClickListener(v -> finish());
    }
}
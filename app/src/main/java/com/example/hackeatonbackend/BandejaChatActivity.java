package com.example.hackeatonbackend;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class BandejaChatActivity extends AppCompatActivity {

    private ChatAdapter adapter;
    private final List<HistorialManager.Evento> chatItems = new ArrayList<>();
    private boolean isProtector = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_bandeja_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("CentinelaPrefs", Context.MODE_PRIVATE);
        isProtector = prefs.getBoolean("isProtectorMode", false);

        Button btnRegresar = findViewById(R.id.btnRegresarChat);
        btnRegresar.setOnClickListener(v -> finish());

        ListView listView = findViewById(R.id.listChatAlertas);
        View layoutEmpty = findViewById(R.id.layoutEmptyChat);
        EditText etInput = findViewById(R.id.etChatInput);
        Button btnEnviar = findViewById(R.id.btnEnviarChat);

        cargarMensajes();

        if (chatItems.isEmpty()) {
            listView.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            adapter = new ChatAdapter(this, chatItems, isProtector);
            listView.setAdapter(adapter);
            listView.setSelection(chatItems.size() - 1);
        }

        btnEnviar.setOnClickListener(v -> {
            String texto = etInput.getText().toString().trim();
            if (!texto.isEmpty()) {
                String prefix;
                if (isProtector) {
                    String protectorName = prefs.getString("protectorName", "Protector");
                    prefix = "Protector (" + protectorName + "): ";
                } else {
                    prefix = "Usuaria: ";
                }

                HistorialManager.registrar(this, HistorialManager.TIPO_CHAT, prefix + texto);
                etInput.setText("");
                cargarMensajes();

                layoutEmpty.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                if (adapter == null) {
                    adapter = new ChatAdapter(this, chatItems, isProtector);
                    listView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
                listView.setSelection(chatItems.size() - 1);
                Toast.makeText(this, "Mensaje enviado al grupo seguro", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarMensajes() {
        chatItems.clear();
        List<HistorialManager.Evento> eventos = HistorialManager.obtener(this);
        for (int i = eventos.size() - 1; i >= 0; i--) {
            HistorialManager.Evento e = eventos.get(i);
            if (e.tipo.equals(HistorialManager.TIPO_SOS) || e.tipo.equals(HistorialManager.TIPO_CHAT)) {
                chatItems.add(e);
            }
        }
    }

    private static class ChatAdapter extends ArrayAdapter<HistorialManager.Evento> {
        private final boolean currentIsProtector;

        public ChatAdapter(Context context, List<HistorialManager.Evento> items, boolean isProtector) {
            super(context, 0, items);
            this.currentIsProtector = isProtector;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            HistorialManager.Evento e = getItem(position);
            if (e != null) {
                if (currentIsProtector && e.descripcion.startsWith("Protector")) {
                    return 1; // Outgoing for protector
                }
                if (!currentIsProtector && e.descripcion.startsWith("Usuaria:")) {
                    return 1; // Outgoing for regular user
                }
            }
            return 0; // Incoming
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            HistorialManager.Evento evento = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                if (type == 1) {
                    convertView = inflater.inflate(R.layout.item_chat_outgoing, parent, false);
                } else {
                    convertView = inflater.inflate(R.layout.item_chat_incoming, parent, false);
                }
            }

            if (type == 1) {
                TextView tvText = convertView.findViewById(R.id.tvOutgoingText);
                TextView tvTime = convertView.findViewById(R.id.tvOutgoingTime);
                if (evento != null) {
                    String desc = evento.descripcion.replaceFirst("(Protector.*:|Usuaria:)\\s*", "");
                    tvText.setText(desc);
                    tvTime.setText(evento.fecha);
                }
            } else {
                TextView tvText = convertView.findViewById(R.id.tvIncomingText);
                TextView tvTime = convertView.findViewById(R.id.tvIncomingTime);
                if (evento != null) {
                    String desc = evento.descripcion.replaceFirst("(Protector.*:|Usuaria:)\\s*", "");
                    tvText.setText(desc);
                    tvTime.setText(evento.fecha);
                }
            }

            return convertView;
        }
    }
}

package com.example.hackeatonbackend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;

public class mapa extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private HeatmapTileProvider heatmapProvider;
    private TileOverlay heatmapOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_mapa);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button button9 = findViewById(R.id.button9);
        button9.setOnClickListener(v -> {
            Intent intent = new Intent(mapa.this, MainActivity.class);
            startActivity(intent);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        LatLng losMochis = new LatLng(25.79097, -108.99825);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(losMochis, 13));

        List<LatLng> heatmapPoints = new ArrayList<>();
        heatmapPoints.add(new LatLng(25.79097, -108.99825));
        heatmapPoints.add(new LatLng(25.79150, -108.99900));
        heatmapPoints.add(new LatLng(25.78950, -108.99700));

        heatmapProvider = new HeatmapTileProvider.Builder()
                .data(heatmapPoints)
                .radius(50)
                .opacity(0.7)
                .build();

        heatmapOverlay = googleMap.addTileOverlay(
                new TileOverlayOptions().tileProvider(heatmapProvider)
        );
    }
}
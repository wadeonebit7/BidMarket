package com.example.bidmarket.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bidmarket.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button btnLogin = findViewById(R.id.btnIrLogin);
        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        Button btnRegister = findViewById(R.id.btnIrRegister);
        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        SharedPreferences preferencias =
                getSharedPreferences("MisPreferencias", MODE_PRIVATE);

        boolean hayUsuarioGuardado =
                preferencias.getInt("usuario_id", -1) > 0;

        boolean haySesionGoogleGuardada =
                "GOOGLE".equals(preferencias.getString("auth_provider", ""));

        if (savedInstanceState == null
                && (hayUsuarioGuardado || haySesionGoogleGuardada)) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
}
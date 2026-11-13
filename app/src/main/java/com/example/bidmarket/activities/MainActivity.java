package com.example.bidmarket.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bidmarket.R;

/**
 * Muestra la pantalla de bienvenida con opciones para iniciar sesión o registrarse.
 * Si hay una sesión guardada, deriva al inicio de sesión para continuar el acceso.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Prepara la pantalla de bienvenida y conecta sus botones.
     * Abre el inicio de sesión automáticamente solo si Android no está restaurando
     * esta pantalla y hay un ID de usuario guardado o una marca de acceso con Google.
     *
     * @param savedInstanceState estado guardado de la pantalla, o {@code null} si no hay uno.
     */
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
package com.example.bidmarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bidmarket.R;
import com.example.bidmarket.repositories.AuthRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.firebase.auth.FirebaseUser;
import com.example.bidmarket.auth.GoogleAuthManager;
import com.example.bidmarket.auth.GoogleLoginFlow;

public final class RegisterActivity extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private EditText correo, nombre, usuario, pass, confirmacion;
    private CheckBox terminos;
    private Button crear;
    private View panelCorreo, panelIdentidad, panelSeguridad;
    private TextView titulo, descripcion, indicador, volver;
    private int paso = 0;
    private boolean registrando = false;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_register);

        View botonGoogle = findViewById(R.id.btnGoogleRegistro);

        GoogleLoginFlow flujoGoogle = new GoogleLoginFlow(
                this,
                io,
                id -> {
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                },
                botonGoogle,
                findViewById(R.id.btnContinuarCorreo),
                findViewById(R.id.tvIrLogin)
        );

        botonGoogle.setOnClickListener(v -> flujoGoogle.iniciar());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        correo = findViewById(R.id.etEmail);
        nombre = findViewById(R.id.etNombre);
        usuario = findViewById(R.id.etUsername);
        pass = findViewById(R.id.etPassword);
        confirmacion = findViewById(R.id.etRepetirPassword);
        terminos = findViewById(R.id.cbTerminos);
        crear = findViewById(R.id.btnRegistrarse);
        panelCorreo = findViewById(R.id.panelCorreo);
        panelIdentidad = findViewById(R.id.panelIdentidad);
        panelSeguridad = findViewById(R.id.panelSeguridad);
        titulo = findViewById(R.id.tvTituloRegistro);
        descripcion = findViewById(R.id.tvDescripcionRegistro);
        indicador = findViewById(R.id.tvPasoRegistro);
        volver = findViewById(R.id.tvVolverPaso);

        findViewById(R.id.btnContinuarCorreo).setOnClickListener(v -> {
            String email = correo.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                correo.setError("Ingresa un correo válido");
                correo.requestFocus();
                return;
            }
            mostrarPaso(1);
        });
        findViewById(R.id.btnContinuarDatos).setOnClickListener(v -> {
            if (nombre.getText().toString().trim().isEmpty()) {
                nombre.setError("Ingresa tu nombre"); nombre.requestFocus(); return;
            }
            if (usuario.getText().toString().trim().isEmpty()) {
                usuario.setError("Ingresa un usuario"); usuario.requestFocus(); return;
            }
            mostrarPaso(2);
        });
        crear.setOnClickListener(v -> registrar());
        volver.setOnClickListener(v -> { if (!registrando && paso > 0) mostrarPaso(paso - 1); });
        findViewById(R.id.tvIrLogin).setOnClickListener(v -> navegarLogin());

        if (state != null) paso = Math.max(0, Math.min(2, state.getInt("registro_paso", 0)));
        mostrarPaso(paso);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (registrando) return;
                if (paso > 0) mostrarPaso(paso - 1);
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });
    }

    private void mostrarPaso(int nuevoPaso) {
        paso = nuevoPaso;
        panelCorreo.setVisibility(paso == 0 ? View.VISIBLE : View.GONE);
        panelIdentidad.setVisibility(paso == 1 ? View.VISIBLE : View.GONE);
        panelSeguridad.setVisibility(paso == 2 ? View.VISIBLE : View.GONE);
        volver.setVisibility(paso == 0 ? View.GONE : View.VISIBLE);
        indicador.setText("PASO " + (paso + 1) + " DE 3");
        String[] titulos = {"Crea tu cuenta", "¿Cómo te llamas?", "Protege tu cuenta"};
        String[] ayudas = {"Comienza con tu correo electrónico.",
                "Tu nombre y usuario serán visibles en BidMarket+.",
                "Elige una contraseña segura para finalizar."};
        titulo.setText(titulos[paso]);
        descripcion.setText(ayudas[paso]);
    }

    private void registrar() {
        if (registrando) return;
        String n = nombre.getText().toString().trim();
        String u = usuario.getText().toString().trim();
        String e = correo.getText().toString().trim();
        String p = pass.getText().toString();
        String q = confirmacion.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { mostrarPaso(0); correo.setError("Correo inválido"); return; }
        if (n.isEmpty() || u.isEmpty()) { mostrarPaso(1); mensaje("Completa tus datos"); return; }
        if (p.length() < 8 || !p.equals(q)) { mensaje("Contraseña: mínimo 8 caracteres y confirmación igual"); return; }
        if (!terminos.isChecked()) { mensaje("Acepta los términos para continuar"); return; }
        registrando = true;
        crear.setEnabled(false);
        io.execute(() -> {
            boolean exito;
            try {
                new AuthRepository(getApplicationContext()).registrarUsuario(n, u, e, p);
                exito = true;
            } catch (Exception ex) {
                Log.w("BidMarket", "Error en registro", ex);
                exito = false;
            }
            final boolean resultado = exito;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                registrando = false;
                crear.setEnabled(true);
                if (resultado) { mensaje("Cuenta creada"); navegarLogin(); }
                else mensaje("No se pudo registrar. Comprueba correo y usuario.");
            });
        });
    }

    private void navegarLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }
    private void mensaje(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("registro_paso", paso);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }
}

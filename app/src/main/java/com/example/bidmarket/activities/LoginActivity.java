package com.example.bidmarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;

import com.example.bidmarket.repositories.AuthRepository;
import com.example.bidmarket.R;

import android.widget.Toast;
import com.example.bidmarket.auth.GoogleLoginFlow;

/**
 * Permite iniciar sesión con correo y contraseña o con una cuenta de Google.
 * También comprueba si hay una sesión guardada al abrir la pantalla.
 */
public class LoginActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private EditText correo,contrasena;private Button ingresar;
    /**
     * Prepara el formulario y conecta el acceso con Google y el enlace de registro.
     * Si hay indicios de una sesión guardada, oculta el formulario mientras se
     * intenta restaurar el acceso con Google o se comprueba el identificador local.
     *
     * @param state estado guardado de la pantalla, o {@code null} si no hay uno.
     */
    @Override protected void onCreate(Bundle state){super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_login);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        View root = findViewById(R.id.main);

        android.content.SharedPreferences prefs =
                getSharedPreferences("MisPreferencias", MODE_PRIVATE);

        boolean haySesionGuardada = prefs.getInt("usuario_id", -1) > 0
                || "GOOGLE".equals(prefs.getString("auth_provider", ""));

        if (haySesionGuardada) {
            root.setVisibility(View.INVISIBLE);
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            v.setPadding(
                    barras.left,
                    barras.top,
                    barras.right,
                    barras.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);

        correo=findViewById(R.id.etCorreoLogin);contrasena=findViewById(R.id.etPasswordLogin);ingresar=findViewById(R.id.btnIngresar);
        ingresar.setOnClickListener(v->login());
        findViewById(R.id.tvIrRegistro).setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        View botonGoogle = findViewById(R.id.btnGoogleLogin);

        GoogleLoginFlow flujoGoogle = new GoogleLoginFlow(
                this,
                io,
                id -> abrir(),
                botonGoogle,
                ingresar,
                findViewById(R.id.tvIrRegistro)
        );

        botonGoogle.setOnClickListener(v -> flujoGoogle.iniciar());

        if (!flujoGoogle.restaurarSiEsGoogle()
                && getSharedPreferences("MisPreferencias", MODE_PRIVATE)
                .getInt("usuario_id", -1) > 0) {
            abrir();
        }
    }
    /**
     * Comprueba que se hayan ingresado el correo y la contraseña, y consulta la
     * cuenta en segundo plano. Si las credenciales son válidas, guarda el ID del
     * usuario, quita la marca de acceso con Google y abre el catálogo.
     * Si el acceso falla, muestra un aviso.
     */
    private void login(){String email=correo.getText().toString().trim(),pass=contrasena.getText().toString();
        if(email.isEmpty()||pass.isEmpty()){mensaje("Ingresa correo y contraseña");return;}
        ingresar.setEnabled(false);
        io.execute(() -> {
            int id = -1;
            String error = "Credenciales incorrectas";
            try {
                id = new AuthRepository(getApplicationContext())
                        .iniciarSesion(email, pass);
            } catch (Exception ex) {
                android.util.Log.e("BidMarket", "Error al iniciar sesión", ex);
                error = "No se pudo consultar la cuenta";
            }
            final int usuarioId = id;
            final String mensajeError = error;
            runOnUiThread(() -> {
                ingresar.setEnabled(true);
                if (usuarioId > 0) {
                    getSharedPreferences("MisPreferencias", MODE_PRIVATE)
                            .edit()
                            .putInt("usuario_id", usuarioId)
                            .remove("auth_provider")
                            .apply();
                    abrir();
                } else {
                    mensaje(mensajeError);
                }
            });
        });
    }
    /**
     * Abre el catálogo y limpia la pila de pantallas para que el botón Atrás
     * no vuelva al formulario de acceso.
     */
    private void abrir(){Intent i=new Intent(this,DashboardActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();}
    /**
     * Muestra un aviso en la pantalla.
     *
     * @param s texto del aviso que se muestra al usuario.
     */
    private void mensaje(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    /**
     * Deja de aceptar nuevas tareas en segundo plano al destruir la pantalla.
     */
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

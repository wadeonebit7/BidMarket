package com.example.bidmarket;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etCorreoLogin, etPasswordLogin;
    private Button btnIngresar;
    private BidMarketDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. VERIFICAR SESIÓN ACTIVA ANTES DE CARGAR LA INTERFAZ
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        int usuarioId = prefs.getInt("usuario_id", -1);

        if (usuarioId != -1) {
            // El usuario ya tiene sesión. Redirigir al Dashboard.
            irAlDashboard();
            return; // Detenemos la ejecución del onCreate aquí
        }

        // Si no hay sesión, cargamos la interfaz de Login de forma normal
        // Evita captura de pantalla en miniatura multitarea para proteger las contraseñas
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_login);

        dbHelper = new BidMarketDbHelper(this);

        etCorreoLogin = findViewById(R.id.etCorreoLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        btnIngresar = findViewById(R.id.btnIngresar);

        btnIngresar.setOnClickListener(v -> iniciarSesion());
    }

    private void iniciarSesion() {
        String email = etCorreoLogin.getText().toString().trim();
        String password = etPasswordLogin.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String passwordHash = SecurityUtils.hashPassword(password);

        // Consultar la tabla cuentas para verificar credenciales
        String[] columnas = {"id", "status"};
        String seleccion = "email = ? AND password_hash = ?";
        String[] argumentos = {email, passwordHash};

        Cursor cursor = db.query("cuentas", columnas, seleccion, argumentos, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Se encontró la cuenta
            int indexStatus = cursor.getColumnIndex("status");
            int indexId = cursor.getColumnIndex("id");

            String status = cursor.getString(indexStatus);
            int cuentaId = cursor.getInt(indexId); // ID de la tabla cuentas

            if ("ACTIVE".equals(status)) {
                // Buscar el ID del usuario asociado a esta cuenta en la tabla 'usuarios'
                Cursor cursorUsuario = db.query("usuarios", new String[]{"id"}, "cuentas_id = ?",
                        new String[]{String.valueOf(cuentaId)}, null, null, null);

                if (cursorUsuario != null && cursorUsuario.moveToFirst()) {
                    int usuarioId = cursorUsuario.getInt(0); // ID de la tabla usuarios

                    // Guardar el usuarioId en SharedPreferences
                    getSharedPreferences("MisPreferencias", MODE_PRIVATE)
                            .edit()
                            .putInt("usuario_id", usuarioId)
                            .apply();

                    cursorUsuario.close();

                    Toast.makeText(this, "¡Bienvenido a BidMarket!", Toast.LENGTH_SHORT).show();

                    // Redirigir al Dashboard
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Error: Perfil de usuario no encontrado.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Tu cuenta no está activa. Estado: " + status, Toast.LENGTH_LONG).show();
            }
            cursor.close();
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
        }
    }

    // Método auxiliar para manejar la navegación y limpiar la pila de actividades
    private void irAlDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        // Estas dos banderas eliminan el LoginActivity y cualquier otra pantalla previa de la memoria
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
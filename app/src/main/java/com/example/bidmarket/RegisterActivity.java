package com.example.bidmarket;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNombre, etUsername, etEmail, etPassword, etRepetirPassword;
    private CheckBox cbTerminos;
    private Button btnRegistrarse;
    private BidMarketDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new BidMarketDbHelper(this);

        // Enlazar vistas (Asegúrate de que los IDs coincidan con tu activity_register.xml)
        etNombre = findViewById(R.id.etNombre);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepetirPassword = findViewById(R.id.etRepetirPassword);
        cbTerminos = findViewById(R.id.cbTerminos);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);

        btnRegistrarse.setOnClickListener(v -> registrarUsuario());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String repetirPassword = etRepetirPassword.getText().toString();

        // Validaciones basadas en la interfaz de la imagen aportada
        if (nombre.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repetirPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 1. Insertar en la tabla 'cuentas'
        ContentValues cuentaValues = new ContentValues();
        cuentaValues.put("name", nombre);
        cuentaValues.put("user_name", username);
        cuentaValues.put("email", email);
        cuentaValues.put("password_hash", SecurityUtils.hashPassword(password));

        long cuentaId = db.insert("cuentas", null, cuentaValues);

        if (cuentaId != -1) {
            // 2. Insertar en la tabla 'usuarios' vinculando el ID de la cuenta
            ContentValues usuarioValues = new ContentValues();
            usuarioValues.put("cuentas_id", cuentaId);
            usuarioValues.put("name", nombre);
            usuarioValues.put("user_name", username);

            long usuarioId = db.insert("usuarios", null, usuarioValues);

            if (usuarioId != -1) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                // Navegar de regreso al Login mediante Intent
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Error al crear el perfil de usuario", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error en el registro. El correo o usuario podría ya existir.", Toast.LENGTH_SHORT).show();
        }
    }
}
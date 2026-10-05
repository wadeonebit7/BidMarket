package com.example.bidmarket;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private EditText etCorreoLogin, etPasswordLogin;
    private Button btnIngresar;
    private BidMarketDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            String status = cursor.getString(indexStatus);

            if ("ACTIVE".equals(status)) {
                Toast.makeText(this, "¡Bienvenido a BidMarket!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Tu cuenta no está activa. Estado: " + status, Toast.LENGTH_LONG).show();
            }
            cursor.close();
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
        }
    }
}
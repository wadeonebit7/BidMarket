package com.example.bidmarket;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CrearPublicacionActivity extends AppCompatActivity {

    private EditText etTitulo, etDescripcion, etPrecio, etUbicacion;
    private Spinner spinnerCondicion;
    private Button btnPublicar;
    private BidMarketDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_publicacion);

        dbHelper = new BidMarketDbHelper(this);

        etTitulo = findViewById(R.id.etTituloPub);
        etDescripcion = findViewById(R.id.etDescripcionPub);
        etPrecio = findViewById(R.id.etPrecioPub);
        etUbicacion = findViewById(R.id.etUbicacionPub);
        spinnerCondicion = findViewById(R.id.spinnerCondicion);
        btnPublicar = findViewById(R.id.btnPublicar);

        configurarSpinner();

        btnPublicar.setOnClickListener(v -> guardarPublicacion());
    }

    private void configurarSpinner() {
        // Valores exactos requeridos por el esquema CHECK (condition IN ('NEW','USED','NOT_APPLICABLE'))
        String[] condicionesDb = {"NEW", "USED", "NOT_APPLICABLE"};
        String[] condicionesMostrar = {"Nuevo", "Usado", "No Aplica"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, condicionesMostrar);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondicion.setAdapter(adapter);
    }

    private void guardarPublicacion() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String precioStr = etPrecio.getText().toString().trim();
        String ubicacion = etUbicacion.getText().toString().trim();

        if (titulo.isEmpty() || precioStr.isEmpty() || ubicacion.isEmpty()) {
            Toast.makeText(this, "Título, precio y ubicación son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio = Double.parseDouble(precioStr);

        // Mapear la selección del Spinner al valor exacto de la base de datos
        int posicionCondicion = spinnerCondicion.getSelectedItemPosition();
        String[] condicionesDb = {"NEW", "USED", "NOT_APPLICABLE"};
        String condicionSeleccionada = condicionesDb[posicionCondicion];

        // Obtener el ID del usuario actual (Simulado usando SharedPreferences)
        // En tu LoginActivity debiste guardar este ID con:
        // getSharedPreferences("MisPreferencias", MODE_PRIVATE).edit().putInt("usuario_id", id).apply();
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        int vendedorId = prefs.getInt("usuario_id", -1);

        if (vendedorId == -1) {
            Toast.makeText(this, "Error de sesión. Vuelve a iniciar sesión.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Asignar una categoría temporal (En una app real, usarías otro Spinner para consultar la tabla categorias)
        int categoriaIdTemporal = 1;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("vendedor_id", vendedorId);
        values.put("categoria_id", categoriaIdTemporal);
        values.put("title", titulo);
        values.put("description", descripcion);
        values.put("price", precio);
        values.put("currency", "CLP"); // Modificamos el valor por defecto a una moneda local
        values.put("condition", condicionSeleccionada);
        values.put("status", "ACTIVE"); // Valor por defecto admitido por el CHECK
        values.put("location_name", ubicacion);

        long resultadoId = db.insert("publicaciones", null, values);

        if (resultadoId != -1) {
            Toast.makeText(this, "Publicación creada con éxito", Toast.LENGTH_SHORT).show();
            finish(); // Cierra esta Activity y vuelve al Dashboard
        } else {
            Toast.makeText(this, "Error al crear la publicación", Toast.LENGTH_SHORT).show();
        }
    }
}
package com.example.bidmarket;

import com.example.bidmarket.Models.Publicacion;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class DashboardActivity extends AppCompatActivity {

    private RecyclerView rvPublicaciones;
    private FloatingActionButton fabAgregarPublicacion;
    private PublicacionAdapter adapter;
    private List<Publicacion> listaPublicaciones;
    private BidMarketDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        EdgeToEdge.enable(this);

        dbHelper = new BidMarketDbHelper(this);

        // Enlaces a la vista
        rvPublicaciones = findViewById(R.id.rvPublicaciones);
        fabAgregarPublicacion = findViewById(R.id.fabAgregarPublicacion); // <-- LÍNEA FALTANTE AGREGADA

        // Configurar el RecyclerView para que se muestre como una lista vertical
        rvPublicaciones.setLayoutManager(new LinearLayoutManager(this));

        listaPublicaciones = new ArrayList<>();
        adapter = new PublicacionAdapter(listaPublicaciones);
        rvPublicaciones.setAdapter(adapter);

        // Configurar el click del botón flotante
        fabAgregarPublicacion.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, CrearPublicacionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al regresar a esta pantalla, limpiamos la lista y volvemos a consultar la base de datos
        listaPublicaciones.clear();
        cargarPublicaciones();
        adapter.notifyDataSetChanged(); // Notificamos al adaptador que hay nueva información
    }

    private void cargarPublicaciones() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Consulta SQL combinando publicaciones e imágenes (portada)
        String query = "SELECT p.id, p.title, p.price, p.currency, p.condition, i.image_url " +
                "FROM publicaciones p " +
                "LEFT JOIN imagenes i ON p.id = i.publicacion_id AND i.display_order = 1 " +
                "WHERE p.status = 'ACTIVE' " +
                "ORDER BY p.create_at DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String titulo = cursor.getString(1);
                double precio = cursor.getDouble(2);
                String moneda = cursor.getString(3);
                String condicion = cursor.getString(4);
                String imageUrl = cursor.getString(5); // Puede ser null si no tiene imagen

                Publicacion pub = new Publicacion(id, titulo, precio, moneda, condicion, imageUrl);
                listaPublicaciones.add(pub);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}
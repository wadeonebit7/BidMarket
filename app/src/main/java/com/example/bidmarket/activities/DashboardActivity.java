package com.example.bidmarket.activities;

import com.example.bidmarket.adapters.PublicacionAdapter;
import com.example.bidmarket.repositories.PublicacionRepository;
import com.example.bidmarket.R;
import com.example.bidmarket.models.Publicacion;
import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Color;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.core.view.WindowCompat;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Muestra el catálogo de BidMarket+ en dos columnas.
 * Desde esta pantalla se puede abrir una publicación, crear una nueva,
 * revisar los chats y las notificaciones o cerrar sesión.
 */
public class DashboardActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private PublicacionRepository repo;
    private PublicacionAdapter adapter;
    /**
     * Configura la pantalla, la barra superior y la lista de publicaciones.
     * También conecta los botones para crear una publicación y revisar los chats.
     *
     * @param state estado guardado de la pantalla, o {@code null} si se abre por primera vez.
     */
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);EdgeToEdge.enable(this);setContentView(R.layout.activity_dashboard);

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        setSupportActionBar((Toolbar)findViewById(R.id.topAppBar));
        repo=new PublicacionRepository(this);
        RecyclerView lista=findViewById(R.id.rvPublicaciones);
        lista.setLayoutManager(new GridLayoutManager(this, 2));
        adapter=new PublicacionAdapter(new java.util.ArrayList<>(),pub->{Intent i=new Intent(this,DetallePublicacionActivity.class);i.putExtra("publicacion_id",(long)pub.getId());startActivity(i);});
        lista.setAdapter(adapter);
        findViewById(R.id.fabAgregarPublicacion).setOnClickListener(v->startActivity(new Intent(this,CrearPublicacionActivity.class)));
        findViewById(R.id.btnMisChats).setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    HistorialChatsActivity.class
            );
            startActivity(intent);
        });
    }

    /**
     * Vuelve a cargar el catálogo en segundo plano al regresar a esta pantalla.
     * Si falla la consulta, muestra un aviso sin reemplazar la lista.
     */
    @Override protected void onResume(){super.onResume();io.execute(()->{
        try{List<Publicacion> pubs=repo.listar();runOnUiThread(()->adapter.reemplazar(pubs));}
        catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}
    });}

    /**
     * Agrega las opciones de notificaciones y cierre de sesión al menú superior.
     *
     * @param menu menú donde se agregan las opciones del dashboard.
     * @return {@code true} para indicar que se debe mostrar el menú.
     */
    @Override public boolean onCreateOptionsMenu(Menu menu){getMenuInflater().inflate(R.menu.menu_dashboard,menu);return true;}

    /**
     * Abre las notificaciones o inicia el cierre de sesión según la opción elegida.
     * Las demás opciones se dejan a cargo de la clase base.
     *
     * @param item opción que seleccionó el usuario.
     * @return {@code true} si la opción se atendió aquí. Para las demás opciones,
     *         devuelve el resultado de la clase base.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_notificaciones) {
            startActivity(new Intent(this, NotificacionesActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.action_logout) {
            cerrarSesion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Cierra la sesión de Firebase, borra los datos de sesión guardados en el
     * teléfono y solicita limpiar el estado del selector de credenciales.
     * Después vuelve al inicio de sesión, incluso si falla esa última limpieza.
     */
    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();

        getSharedPreferences("MisPreferencias", MODE_PRIVATE)
                .edit()
                .remove("usuario_id")
                .remove("auth_provider")
                .apply();

        CredentialManager.create(this).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                null,
                getMainExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    /**
                     * Vuelve al inicio de sesión cuando se limpia el estado del selector.
                     *
                     * @param resultado valor sin contenido, ya que esta operación no devuelve datos.
                     */
                    @Override
                    public void onResult(Void resultado) {
                        volverAlLogin();
                    }

                    /**
                     * Registra el problema al limpiar el selector y vuelve al inicio de sesión.
                     *
                     * @param error error que informa Credential Manager al limpiar su estado.
                     */
                    @Override
                    public void onError(ClearCredentialException error) {
                        android.util.Log.w(
                                "BidMarketAuth",
                                "No se pudo limpiar el estado del selector",
                                error
                        );
                        volverAlLogin();
                    }
                }
        );
    }

    /**
     * Abre el inicio de sesión y limpia el historial de pantallas para que
     * el botón Atrás no vuelva al catálogo. No navega si esta pantalla ya se está cerrando o fue destruida.
     */
    private void volverAlLogin() {
        if (isFinishing() || isDestroyed()) return;

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Deja de aceptar nuevas tareas en el ejecutor cuando se destruye la pantalla.
     * Las tareas que ya se enviaron pueden terminar.
     */
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

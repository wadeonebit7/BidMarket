package com.example.bidmarket.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bidmarket.repositories.NotificacionRepository;
import java.util.List;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra los avisos locales del usuario y los marca como leídos al consultarlos.
 * Esta pantalla no recibe notificaciones push.
 */
public class NotificacionesActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    /**
     * Comprueba que haya un usuario guardado y carga sus avisos en segundo plano.
     * La lista muestra la fecha y el mensaje de cada aviso, o un texto cuando
     * no hay avisos o falla la consulta.
     *
     * @param state estado guardado de la pantalla, o {@code null} si no hay uno.
     */
    @Override protected void onCreate(Bundle state){super.onCreate(state);
        ListView list=new ListView(this);setContentView(list);
        int usuario=getSharedPreferences("MisPreferencias",MODE_PRIVATE).getInt("usuario_id",-1);
        if(usuario<=0){finish();return;}
        io.execute(() -> {
            ArrayList<String> datos = new ArrayList<>();
            try {
                List<NotificacionRepository.Aviso> avisos =
                        new NotificacionRepository(getApplicationContext())
                                .listarYMarcarLeidas(usuario);
                for (NotificacionRepository.Aviso aviso : avisos) {
                    datos.add(new java.util.Date(aviso.fechaMs) + "\n" + aviso.mensaje);
                }
                if (datos.isEmpty()) {
                    datos.add("No tienes notificaciones todavía");
                }
            } catch (Exception ex) {
                android.util.Log.e("BidMarket", "Error al cargar notificaciones", ex);
                datos.add("No se pudieron cargar las notificaciones");
            }
            runOnUiThread(() -> list.setAdapter(
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, datos)));
        });
    }
    /**
     * Deja de aceptar nuevas tareas de carga cuando se destruye la pantalla.
     */
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

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

/** Bandeja local de eventos (no es una notificación push). */
public class NotificacionesActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
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
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

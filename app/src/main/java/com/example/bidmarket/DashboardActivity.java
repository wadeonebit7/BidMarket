package com.example.bidmarket;

import com.example.bidmarket.Models.Publicacion;
import android.os.Bundle;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private PublicacionRepository repo;
    private PublicacionAdapter adapter;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);EdgeToEdge.enable(this);setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),(v,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        setSupportActionBar((Toolbar)findViewById(R.id.topAppBar));
        repo=new PublicacionRepository(this);
        RecyclerView lista=findViewById(R.id.rvPublicaciones);lista.setLayoutManager(new LinearLayoutManager(this));
        adapter=new PublicacionAdapter(new java.util.ArrayList<>(),pub->{Intent i=new Intent(this,DetallePublicacionActivity.class);i.putExtra("publicacion_id",(long)pub.getId());startActivity(i);});
        lista.setAdapter(adapter);
        findViewById(R.id.fabAgregarPublicacion).setOnClickListener(v->startActivity(new Intent(this,CrearPublicacionActivity.class)));
    }
    @Override protected void onResume(){super.onResume();io.execute(()->{
        try{List<Publicacion> pubs=repo.listar();runOnUiThread(()->adapter.reemplazar(pubs));}
        catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}
    });}
    @Override public boolean onCreateOptionsMenu(Menu menu){getMenuInflater().inflate(R.menu.menu_dashboard,menu);return true;}
    @Override public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId()==R.id.action_notificaciones){startActivity(new Intent(this,NotificacionesActivity.class));return true;}
        if(item.getItemId()==R.id.action_logout){
        getSharedPreferences("MisPreferencias",MODE_PRIVATE).edit().clear().apply();
        Intent intent=new Intent(this,LoginActivity.class);intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(intent);finish();return true;
    }return super.onOptionsItemSelected(item);}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

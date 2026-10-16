package com.example.bidmarket;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
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
        io.execute(()->{
            SQLiteDatabase db=new BidMarketDbHelper(this).getWritableDatabase();ArrayList<String> data=new ArrayList<>();
            try(Cursor c=db.query("notificaciones",new String[]{"mensaje","fecha_ms"},"usuario_id=?",new String[]{""+usuario},null,null,"fecha_ms DESC")){
                while(c.moveToNext())data.add(new java.util.Date(c.getLong(1))+"\n"+c.getString(0));
            }
            if(data.isEmpty())data.add("No tienes notificaciones todavía");
            ContentValues v=new ContentValues();v.put("leida",1);db.update("notificaciones",v,"usuario_id=?",new String[]{""+usuario});
            runOnUiThread(()->list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,data)));
        });
    }
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

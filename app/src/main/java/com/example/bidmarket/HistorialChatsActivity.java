package com.example.bidmarket;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.List;
import java.util.concurrent.*;

public class HistorialChatsActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private ChatRepository repo;private int usuario;
    @Override public void onCreate(Bundle s){super.onCreate(s);setContentView(R.layout.activity_historial_chats);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootHistorial),(v,w)->{Insets i=w.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(i.left,i.top,i.right,i.bottom);return w;});
        usuario=getSharedPreferences("MisPreferencias",MODE_PRIVATE).getInt("usuario_id",-1);if(usuario<=0){finish();return;}
        repo=new ChatRepository(this);
    }
    @Override protected void onResume(){super.onResume();cargar();}
    private void cargar(){io.execute(()->{try{List<ChatRepository.Entrada> entradas=repo.historial(usuario);runOnUiThread(()->{
        LinearLayout cont=findViewById(R.id.listaChats);cont.removeAllViews();
        ((TextView)findViewById(R.id.txtChatsVacios)).setText(entradas.isEmpty()?"Todavía no tienes conversaciones.":"");
        for(ChatRepository.Entrada e:entradas){TextView item=new TextView(this);item.setText(e.titulo+"\n"+e.interlocutor+" · "+e.ultimo);item.setTextSize(16);item.setTextColor(0xFF202936);item.setPadding(20,24,20,24);item.setBackgroundColor(0xFFFFFFFF);
          LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=12;cont.addView(item,lp);
          item.setOnClickListener(v->{Intent in=new Intent(this,ChatActivity.class);in.putExtra("chat_id",e.id);startActivity(in);});}
        });}catch(Exception ex){runOnUiThread(()->Toast.makeText(this,ex.getMessage(),Toast.LENGTH_LONG).show());}});}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

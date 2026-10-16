package com.example.bidmarket;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.List;
import java.util.concurrent.*;

public class ChatActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private ChatRepository repo;private int usuario;private long chatId;
    @Override public void onCreate(Bundle s){super.onCreate(s);setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootChat),(v,w)->{Insets i=w.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(i.left,i.top,i.right,i.bottom);return w;});
        usuario=getSharedPreferences("MisPreferencias",MODE_PRIVATE).getInt("usuario_id",-1);chatId=getIntent().getLongExtra("chat_id",-1);
        if(usuario<=0||chatId<=0){finish();return;} repo=new ChatRepository(this);
        findViewById(R.id.btnEnviarMensaje).setOnClickListener(v->{EditText entrada=findViewById(R.id.etMensaje);String txt=entrada.getText().toString();io.execute(()->{try{repo.enviar(chatId,usuario,txt);runOnUiThread(()->{entrada.setText("");cargar();});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}});});
        cargar();
    }
    private void cargar(){io.execute(()->{try{String titulo=repo.titulo(chatId,usuario);List<ChatRepository.Mensaje> mensajes=repo.mensajes(chatId,usuario);runOnUiThread(()->{
        ((TextView)findViewById(R.id.tituloChat)).setText(titulo);LinearLayout cont=findViewById(R.id.listaMensajes);cont.removeAllViews();
        for(ChatRepository.Mensaje m:mensajes){TextView t=new TextView(this);t.setText(m.texto);t.setTextSize(15);t.setTextColor(m.autor==usuario?0xFFFFFFFF:0xFF202936);t.setPadding(18,12,18,12);t.setBackgroundColor(m.autor==usuario?0xFF202936:0xFFE8EEF4);
          LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.gravity=m.autor==usuario?Gravity.END:Gravity.START;p.setMargins(12,8,12,8);cont.addView(t,p);}
        ((ScrollView)findViewById(R.id.scrollMensajes)).post(()->((ScrollView)findViewById(R.id.scrollMensajes)).fullScroll(ScrollView.FOCUS_DOWN));
        });}catch(Exception e){runOnUiThread(()->{Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();finish();});}});}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

package com.example.bidmarket;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private EditText correo,contrasena;private Button ingresar;
    @Override protected void onCreate(Bundle state){super.onCreate(state);
        if(getSharedPreferences("MisPreferencias",MODE_PRIVATE).getInt("usuario_id",-1)>0){abrir();return;}
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE,android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_login);
        correo=findViewById(R.id.etCorreoLogin);contrasena=findViewById(R.id.etPasswordLogin);ingresar=findViewById(R.id.btnIngresar);
        ingresar.setOnClickListener(v->login());
    }
    private void login(){String email=correo.getText().toString().trim(),pass=contrasena.getText().toString();
        if(email.isEmpty()||pass.isEmpty()){mensaje("Ingresa correo y contraseña");return;}
        ingresar.setEnabled(false);
        io.execute(()->{
            int id=-1;String error="Credenciales incorrectas";
            try{
                SQLiteDatabase db=new BidMarketDbHelper(this).getWritableDatabase();
                try(Cursor c=db.rawQuery("SELECT u.id,c.id,c.password_hash,c.status FROM cuentas c JOIN usuarios u ON u.cuentas_id=c.id WHERE c.email=? COLLATE NOCASE",new String[]{email})){
                    if(c.moveToFirst() && "ACTIVE".equals(c.getString(3)) && SecurityUtils.verificar(pass,c.getString(2))){
                        id=c.getInt(0);
                        if(SecurityUtils.requiereMigracion(c.getString(2))){ContentValues v=new ContentValues();v.put("password_hash",SecurityUtils.hashPassword(pass));db.update("cuentas",v,"id=?",new String[]{""+c.getLong(1)});}
                    }
                }
            }catch(Exception e){error="No se pudo consultar la cuenta";}
            final int user=id;final String mensajeError=error;
            runOnUiThread(()->{ingresar.setEnabled(true);if(user>0){getSharedPreferences("MisPreferencias",MODE_PRIVATE).edit().putInt("usuario_id",user).apply();abrir();}else mensaje(mensajeError);});
        });
    }
    private void abrir(){Intent i=new Intent(this,DashboardActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);finish();}
    private void mensaje(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

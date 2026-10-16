package com.example.bidmarket;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private EditText nombre,usuario,correo,pass,confirmacion;private CheckBox terminos;private Button boton;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_register);
        nombre=findViewById(R.id.etNombre);usuario=findViewById(R.id.etUsername);correo=findViewById(R.id.etEmail);
        pass=findViewById(R.id.etPassword);confirmacion=findViewById(R.id.etRepetirPassword);
        terminos=findViewById(R.id.cbTerminos);boton=findViewById(R.id.btnRegistrarse);
        boton.setOnClickListener(v->registrar());
    }
    private void registrar(){
        String n=nombre.getText().toString().trim(),u=usuario.getText().toString().trim(),e=correo.getText().toString().trim(),p=pass.getText().toString(),q=confirmacion.getText().toString();
        if(n.isEmpty()||u.isEmpty()||!Patterns.EMAIL_ADDRESS.matcher(e).matches()){mensaje("Completa nombre, usuario y correo válido");return;}
        if(p.length()<8||!p.equals(q)){mensaje("Contraseña: mínimo 8 caracteres y confirmación igual");return;}
        if(!terminos.isChecked()){mensaje("Acepta los términos para continuar");return;}
        boton.setEnabled(false);
        io.execute(()->{
            SQLiteDatabase db=new BidMarketDbHelper(this).getWritableDatabase();boolean ok=false;
            db.beginTransaction();
            try{
                ContentValues c=new ContentValues();c.put("name",n);c.put("user_name",u);c.put("email",e);c.put("password_hash",SecurityUtils.hashPassword(p));
                long cuenta=db.insertOrThrow("cuentas",null,c);
                ContentValues profile=new ContentValues();profile.put("cuentas_id",cuenta);profile.put("name",n);profile.put("user_name",u);
                db.insertOrThrow("usuarios",null,profile);
                db.setTransactionSuccessful();ok=true;
            }catch(Exception ex){android.util.Log.w("BidMarket","Fallo de registro",ex);}finally{db.endTransaction();}
            boolean resultado=ok;
            runOnUiThread(()->{boton.setEnabled(true);if(resultado){mensaje("Cuenta creada");startActivity(new Intent(this,LoginActivity.class));finish();}else mensaje("No se pudo registrar. Verifica que el correo y usuario no existan.");});
        });
    }
    private void mensaje(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

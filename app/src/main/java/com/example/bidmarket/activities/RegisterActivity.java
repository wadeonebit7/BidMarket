package com.example.bidmarket.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bidmarket.repositories.AuthRepository;
import com.example.bidmarket.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private EditText nombre,usuario,correo,pass,confirmacion;private CheckBox terminos;private Button boton;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
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
        io.execute(() -> {
            boolean registrado;
            try {
                new AuthRepository(getApplicationContext())
                        .registrarUsuario(n, u, e, p);
                registrado = true;
            } catch (Exception ex) {
                android.util.Log.w("BidMarket", "Fallo de registro", ex);
                registrado = false;
            }
            final boolean resultado = registrado;
            runOnUiThread(() -> {
                boton.setEnabled(true);
                if (resultado) {
                    mensaje("Cuenta creada");
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                } else {
                    mensaje("No se pudo registrar. Verifica que el correo y usuario no existan.");
                }
            });
        });
    }
    private void mensaje(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

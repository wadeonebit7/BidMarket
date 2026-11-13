package com.example.bidmarket.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import android.graphics.Color;

import com.example.bidmarket.repositories.ChatRepository;
import com.example.bidmarket.repositories.ImagenesRepository;
import com.example.bidmarket.repositories.PublicacionRepository;
import com.example.bidmarket.R;

/**
 * Muestra la información y las fotos de una publicación de BidMarket+.
 * Las acciones que aparecen dependen de si el usuario es el dueño, de la
 * modalidad de venta y del estado de la publicación o de la subasta.
 */
public class DetallePublicacionActivity extends AppCompatActivity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private PublicacionRepository repo;
    private long pubId;
    private int usuarioId;
    private boolean propietario;
    private String titulo;
    private final java.util.ArrayList<String> fotosDetalle=new java.util.ArrayList<>();
    private int fotoActual=0;
    private android.widget.TextView contadorImagenes;
    /**
     * Muestra la foto seleccionada completa, sin recortarla, y actualiza su contador.
     * Si la publicación no tiene fotos, deja la vista vacía y muestra «Sin imágenes».
     */
    private void dibujarFoto() {
        ImageView imagen = findViewById(R.id.detImagen);
        imagen.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagen.setImageDrawable(null);
        if (!fotosDetalle.isEmpty())
            imagen.setImageURI(Uri.fromFile(new File(fotosDetalle.get(fotoActual))));
        if (contadorImagenes != null)
            contadorImagenes.setText(
                    fotosDetalle.isEmpty()
                            ? "Sin imágenes"
                            : (fotoActual + 1) + " / " + fotosDetalle.size()
            );
    }

    /**
     * Agrega el contador y permite cambiar de foto tocando la mitad izquierda
     * o derecha de la imagen. Al llegar a un extremo, sigue por el otro.
     */
    private void instalarCarrusel() {
        ImageView imagen = findViewById(R.id.detImagen);

        android.view.ViewGroup contenedor =
                (android.view.ViewGroup) imagen.getParent();

        int posicion = contenedor.indexOfChild(imagen);

        contadorImagenes = new TextView(this);
        contadorImagenes.setGravity(android.view.Gravity.CENTER);
        contadorImagenes.setPadding(12, 16, 12, 16);
        contadorImagenes.setTextColor(0xFFE2E8F0);

        contenedor.addView(contadorImagenes, posicion + 1);

        imagen.setOnTouchListener((v, event) -> {

            if (event.getActionMasked()
                    == android.view.MotionEvent.ACTION_UP) {

                if (fotosDetalle.size() > 1) {

                    float mitad = v.getWidth() / 2f;

                    if (event.getX() < mitad) {
                        // Fotografía anterior
                        fotoActual = (fotoActual - 1
                                + fotosDetalle.size())
                                % fotosDetalle.size();
                    } else {
                        // Fotografía siguiente
                        fotoActual = (fotoActual + 1)
                                % fotosDetalle.size();
                    }

                    dibujarFoto();
                }

                v.performClick();
                return true;
            }

            return true;
        });
    }

    /**
     * Prepara la pantalla, la galería y los botones de la publicación.
     * Cierra la pantalla si falta un identificador válido de publicación o de usuario.
     *
     * @param state estado guardado de la pantalla, o {@code null} si se abre por primera vez.
     */
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_detalle_publicacion);
        // Permite que el fondo ocupe toda la pantalla.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Barras del sistema transparentes.
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Iconos claros para contrastar con el fondo oscuro.
        WindowCompat.getInsetsController(getWindow(),
                        getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        WindowCompat.getInsetsController(getWindow(),
                        getWindow().getDecorView())
                .setAppearanceLightNavigationBars(false);

        // Evita que el contenido se superponga
        // con la barra de estado y de navegación.
        View root = findViewById(R.id.detalleRoot);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {

            Insets barras = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            v.setPadding(
                    barras.left,
                    barras.top,
                    barras.right,
                    barras.bottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);

        instalarCarrusel();
        repo=new PublicacionRepository(this);pubId=getIntent().getLongExtra("publicacion_id",-1);
        usuarioId=getSharedPreferences("MisPreferencias",MODE_PRIVATE).getInt("usuario_id",-1);
        if(pubId<=0||usuarioId<=0){finish();return;}
        findViewById(R.id.btnEditar).setOnClickListener(v->{Intent i=new Intent(this,CrearPublicacionActivity.class);i.putExtra("publicacion_id",pubId);startActivity(i);});
        findViewById(R.id.btnPausar).setOnClickListener(v->actualizar("Pausado"));
        findViewById(R.id.btnEliminar).setOnClickListener(v->new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Eliminar publicación")
            .setMessage("¿Quieres ocultar esta publicación del catálogo?").setNegativeButton("Cancelar",null)
            .setPositiveButton("Eliminar",(d,w)->actualizar("Borrado")).show());
        findViewById(R.id.btnVendido).setOnClickListener(v->actualizar("Vendido"));
        findViewById(R.id.btnPujar).setOnClickListener(v->pujar());
        findViewById(R.id.btnComprar).setOnClickListener(v->new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Confirmar compra local")
            .setMessage("Esto registra un acuerdo de compra en este teléfono. No procesa pagos ni avisa al vendedor en otro dispositivo.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Confirmar",(d,w)->comprar()).show());
        findViewById(R.id.btnCompartir).setOnClickListener(v->{Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,"Mira esta publicación de BidMarket+: "+titulo+" (ID "+pubId+")");startActivity(Intent.createChooser(send,"Compartir"));});
        findViewById(R.id.btnChatVendedor).setOnClickListener(v->io.execute(()->{
            try { long chat=new ChatRepository(this).iniciar(pubId,usuarioId);
                runOnUiThread(()->{Intent i=new Intent(this,ChatActivity.class);i.putExtra("chat_id",chat);startActivity(i);});
            }catch(Exception e){runOnUiThread(()->mensaje(e.getMessage()));}
        }));
        findViewById(R.id.btnMapa).setOnClickListener(v->{Intent i=new Intent(this,MapaActivity.class);i.putExtra("publicacion_id",pubId);startActivity(i);});
    }
    /**
     * Recarga el detalle al volver a la pantalla para reflejar los cambios guardados.
     */
    @Override protected void onResume(){super.onResume();cargar();}
    /**
     * Consulta el detalle, las fotos y el estado de la subasta en segundo plano.
     * Actualiza la vista y decide qué acciones mostrar según el usuario y la
     * publicación. Si la publicación no existe, cierra la pantalla.
     */
    private void cargar(){io.execute(()->{
        try(Cursor c=repo.detalle(pubId)){
            if(!c.moveToFirst()){runOnUiThread(this::finish);return;}
            int vendedor=c.getInt(1);propietario=vendedor==usuarioId;
            String title=c.getString(3),description=c.getString(4),location=c.getString(8),status=c.getString(7),mode=c.getString(11),image=c.getString(12);
            double precio=c.getDouble(5);long[] auction=repo.estadoSubasta(pubId);
            java.util.List<String> imagenes = new ImagenesRepository(this).listar(pubId);
            runOnUiThread(()->{
                titulo=title;((TextView)findViewById(R.id.detTitulo)).setText(title);
                ((TextView)findViewById(R.id.detDescripcion)).setText(description);
                ((TextView)findViewById(R.id.detPrecio)).setText("CLP $"+String.format(java.util.Locale.getDefault(),"%,.0f",precio));
                ((TextView)findViewById(R.id.detUbicacion)).setText(location+" · "+status);
                fotosDetalle.clear();fotosDetalle.addAll(imagenes);fotoActual=0;dibujarFoto();
                for(int id:new int[]{R.id.btnEditar,R.id.btnPausar,R.id.btnEliminar,R.id.btnVendido})findViewById(id).setVisibility(propietario?View.VISIBLE:View.GONE);
                boolean sub="SUBASTA".equals(mode)&&auction!=null;
                boolean permitido=!propietario&&!"Borrado".equals(status)&&
                        ("SUBASTA".equals(mode) ? (auction!=null &&
                        System.currentTimeMillis()>=auction[1]) :
                        ("Activo".equals(status)||"Vendido".equals(status)));
                findViewById(R.id.btnChatVendedor).setVisibility(permitido?View.VISIBLE:View.GONE);
                findViewById(R.id.btnComprar).setVisibility(!sub&&!propietario&&"Activo".equals(status)?View.VISIBLE:View.GONE);
                findViewById(R.id.btnPujar).setVisibility(sub&&!propietario?View.VISIBLE:View.GONE);
                findViewById(R.id.etMontoPuja).setVisibility(sub&&!propietario?View.VISIBLE:View.GONE);
                ((TextView)findViewById(R.id.detSubasta)).setText(sub?"Subasta: finaliza "+new Date(auction[1])+"\nPrecio inicial: $"+auction[2]+" · Mayor puja: $"+auction[3]:"Venta directa");
            });
        }catch(Exception e){runOnUiThread(()->mensaje(e.getMessage()));}
    });}
    /**
     * Pide al repositorio cambiar el estado de la publicación y muestra el resultado.
     * Si el cambio se guarda, cierra la pantalla de detalle.
     *
     * @param estado nuevo estado de la publicación, como {@code Pausado},
     *         {@code Borrado} o {@code Vendido}.
     */
    private void actualizar(String estado){io.execute(()->{boolean ok=repo.cambiarEstado(pubId,usuarioId,estado);runOnUiThread(()->{mensaje(ok?"Estado actualizado":"No se pudo actualizar");if(ok)finish();});});}
    /**
     * Registra la compra en este teléfono, sin procesar pagos ni avisar al
     * vendedor en otro dispositivo. Si se guarda correctamente, cierra el detalle.
     */
    private void comprar(){io.execute(()->{try{repo.comprar(pubId,usuarioId);runOnUiThread(()->{mensaje("Compra registrada localmente");finish();});}
        catch(Exception e){runOnUiThread(()->mensaje(e.getMessage()));}});}
    /**
     * Lee el monto ingresado y pide al repositorio registrar la puja.
     * Si el monto no es un entero en CLP, muestra un aviso. Si la puja se guarda,
     * recarga el detalle para mostrar los datos actualizados.
     */
    private void pujar(){String valor=((EditText)findViewById(R.id.etMontoPuja)).getText().toString().trim();long monto;
        try{monto=Long.parseLong(valor);}catch(Exception e){mensaje("Ingresa un monto entero en CLP");return;}
        io.execute(()->{try{repo.ofertar(pubId,usuarioId,monto);runOnUiThread(()->{mensaje("Oferta registrada");cargar();});}
            catch(Exception e){runOnUiThread(()->mensaje(e.getMessage()));}});
    }
    /**
     * Muestra un aviso breve en la pantalla.
     *
     * @param msg texto que se muestra al usuario.
     */
    private void mensaje(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
    /**
     * Deja de aceptar nuevas tareas en el ejecutor cuando se destruye la pantalla.
     * Las tareas que ya se enviaron pueden terminar.
     */
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

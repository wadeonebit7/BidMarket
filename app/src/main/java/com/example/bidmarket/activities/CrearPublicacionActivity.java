package com.example.bidmarket.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import com.example.bidmarket.repositories.ImagenesRepository;
import com.example.bidmarket.repositories.PublicacionRepository;
import com.example.bidmarket.R;
import com.google.android.gms.location.LocationServices;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.Color;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;

/**
 * Muestra el formulario para crear o editar una publicación.
 * Permite elegir la modalidad de venta, ordenar las fotos y agregar
 * la ubicación antes de guardar los cambios.
 */
public class CrearPublicacionActivity extends AppCompatActivity {
    private EditText titulo,descripcion,precio,ubicacion;
    private Spinner condiciones,categorias,modalidad;
    private ImageView imagenVista;
    private Button finButton;
    private PublicacionRepository repo;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private List<PublicacionRepository.Categoria> categoryList=new ArrayList<>();
    private String imagenRuta;
    private final ArrayList<String> fotos = new ArrayList<>();
    private final ActivityResultLauncher<Intent> editorFotos = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
            ArrayList<String> nuevas = result.getData().getStringArrayListExtra("rutas");
            if (nuevas == null) return;
            fotos.clear(); fotos.addAll(nuevas);
            actualizarPortada();
        });
    /**
     * Muestra la primera foto como portada y actualiza el contador del formulario.
     * Si no hay fotos, limpia la vista previa.
     */
    private void actualizarPortada() {
        imagenRuta = fotos.isEmpty() ? null : fotos.get(0);
        imagenVista.setImageDrawable(null);
        if (imagenRuta != null) imagenVista.setImageURI(Uri.fromFile(new File(imagenRuta)));
        ((Button)findViewById(R.id.btnSeleccionarImagen)).setText(
            "Gestionar fotografías (" + fotos.size() + "/10)");
    }
    /**
     * Abre el editor de fotos con la lista y el orden actuales.
     * El formulario solo actualiza la lista cuando el editor devuelve un resultado válido.
     *
     * @throws android.content.ActivityNotFoundException si Android no puede
     *          abrir la pantalla de la galería
     */
    private void abrirEditorFotos() {
        Intent intent = new Intent(this, GaleriaProductoActivity.class);
        intent.putStringArrayListExtra("rutas", new ArrayList<>(fotos));
        editorFotos.launch(intent);
    }
    private long finMs=0,editarId=0;
    private Double latitud=null,longitud=null;
    private final ActivityResultLauncher<String> gpsPermiso=registerForActivityResult(new RequestPermission(), ok->{if(ok)capturarGps();else mensaje("Permiso de ubicación denegado");});
    private final String[] opcionesCondicion={"Nuevo","Usado como nuevo","Buen Estado","Aceptable"};


    /**
     * Prepara el formulario, conecta los botones y carga las categorías.
     * Si recibe un ID de publicación válido para edición, solicita sus datos.
     *
     * @param state estado guardado por Android, o {@code null} si no hay uno
     */
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_crear_publicacion);

        WindowCompat.setDecorFitsSystemWindows(
                getWindow(), false
        );

        // Barras transparentes
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // Iconos oscuros, adecuados para el fondo claro
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        // Respetar barras del sistema
        View root = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            v.setPadding(
                    barras.left,
                    barras.top,
                    barras.right,
                    barras.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(root);

        repo=new PublicacionRepository(this);
        titulo=findViewById(R.id.etTituloPub);descripcion=findViewById(R.id.etDescripcionPub);precio=findViewById(R.id.etPrecioPub);
        ubicacion=findViewById(R.id.etUbicacionPub);condiciones=findViewById(R.id.spinnerCondicion);categorias=findViewById(R.id.spinnerCategoria);
        modalidad=findViewById(R.id.spinnerModalidad);imagenVista=findViewById(R.id.ivVistaPrevia);finButton=findViewById(R.id.btnFechaFin);
        condiciones.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opcionesCondicion));
        modalidad.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Venta directa","Subasta"}));
        modalidad.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            /**
             * Muestra el botón de fecha de término solo cuando se selecciona Subasta.
             *
             * @param p selector de modalidad que generó el evento
             * @param v vista de la opción seleccionada
             * @param pos posición de la opción seleccionada
             * @param id identificador de la opción en el adaptador
             */
            @Override public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){finButton.setVisibility(pos==1?android.view.View.VISIBLE:android.view.View.GONE);}
            /**
             * Deja el formulario como está cuando no hay una modalidad seleccionada.
             *
             * @param p selector de modalidad que generó el evento
             */
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        io.execute(()->{List<PublicacionRepository.Categoria> r=repo.categorias();runOnUiThread(()->{categoryList=r;categorias.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,r));});});
        findViewById(R.id.btnSeleccionarImagen).setOnClickListener(v->abrirEditorFotos());
        finButton.setOnClickListener(v->elegirFecha());
        findViewById(R.id.btnUsarGps).setOnClickListener(v->{if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)capturarGps();else gpsPermiso.launch(Manifest.permission.ACCESS_FINE_LOCATION);});
        findViewById(R.id.btnPublicar).setOnClickListener(v->guardar());
        editarId=getIntent().getLongExtra("publicacion_id",0);
        actualizarPortada();
        if(editarId>0) cargarParaEditar();
    }

    /**
     * Pide la fecha y la hora de término de la subasta.
     * Al confirmar ambas, guarda el valor en milisegundos y lo muestra en el botón.
     * La validación de que la fecha esté en el futuro se hace al guardar.
     */
    private void elegirFecha(){
        Calendar cal=Calendar.getInstance();
        new DatePickerDialog(this,(view,y,m,d)->new TimePickerDialog(this,(tp,h,min)->{
            Calendar selected=Calendar.getInstance();selected.set(y,m,d,h,min,0);selected.set(Calendar.MILLISECOND,0);
            finMs=selected.getTimeInMillis();finButton.setText("Finaliza: "+java.text.DateFormat.getDateTimeInstance().format(new Date(finMs)));
        },cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show(),cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Consulta la última ubicación disponible si ya se concedió el permiso.
     * Si obtiene una ubicación, guarda sus coordenadas en el formulario.
     * Si no hay una ubicación reciente o falla la consulta, muestra un aviso.
     */
    @SuppressWarnings("MissingPermission") private void capturarGps(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
        LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(this,location->{
            if(location==null){mensaje("No hay ubicación reciente. Activa GPS e inténtalo otra vez.");return;}
            latitud=location.getLatitude();longitud=location.getLongitude();mensaje("Ubicación incorporada a la publicación");
        }).addOnFailureListener(this,e->mensaje("No se pudo obtener ubicación"));
    }
    /**
     * Copia una imagen al almacenamiento interno y la muestra en la vista previa.
     * La copia se realiza en segundo plano. Si falla, se muestra un aviso.
     *
     * @param uri ubicación de la imagen que se quiere copiar
     */
    private void copiarImagen(Uri uri){
        io.execute(()->{
            try(InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new IOException("No se puede abrir la fotografía");
                File f=new File(getFilesDir(),"producto_"+UUID.randomUUID()+".img");
                try(OutputStream out=new FileOutputStream(f)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
                imagenRuta=f.getAbsolutePath();runOnUiThread(()->imagenVista.setImageURI(Uri.fromFile(f)));
            }catch(Exception e){runOnUiThread(()->mensaje("No se pudo guardar la imagen"));}
        });
    }

    /**
     * Consulta los datos, las fotos y la información de subasta de la publicación.
     * Si encuentra la publicación, carga sus datos en el formulario de edición.
     */
    private void cargarParaEditar(){
        io.execute(()->{
            try(android.database.Cursor c=repo.detalle(editarId)){
                if(!c.moveToFirst())return;
                String t=c.getString(3),d=c.getString(4),p=""+c.getDouble(5),co=c.getString(6),u=c.getString(8),mo=c.getString(11),im=c.getString(12);
                long cat=c.getLong(2);
                if(!c.isNull(9)&&!c.isNull(10)){latitud=c.getDouble(9);longitud=c.getDouble(10);}
                List<String> imagenesGuardadas = new ImagenesRepository(this).listar(editarId);
                long[] state=repo.estadoSubasta(editarId);
                runOnUiThread(()->{
                    fotos.clear();fotos.addAll(imagenesGuardadas);
                    titulo.setText(t);descripcion.setText(d);precio.setText(p);ubicacion.setText(u);
                    for(int i=0;i<opcionesCondicion.length;i++)if(opcionesCondicion[i].equals(co))condiciones.setSelection(i);
                    modalidad.setSelection("SUBASTA".equals(mo)?1:0);if(state!=null){finMs=state[1];finButton.setText("Finaliza: "+new Date(finMs));}
                    actualizarPortada();
                    elegirCategoriaCuandoLista(cat);
                    ((Button)findViewById(R.id.btnPublicar)).setText("Guardar cambios");
                });
            }
        });
    }
    /**
     * Selecciona la categoría guardada al editar una publicación.
     * Si la lista todavía está vacía, vuelve a intentar la selección más tarde.
     *
     * @param cat ID de la categoría guardada en la publicación
     */
    private void elegirCategoriaCuandoLista(long cat){
        if(categoryList.isEmpty()){categorias.postDelayed(()->elegirCategoriaCuandoLista(cat),150);return;}
        for(int i=0;i<categoryList.size();i++)if(categoryList.get(i).id==cat){categorias.setSelection(i);break;}
    }

    /**
     * Valida el formulario y solicita al repositorio que guarde la publicación.
     * Comprueba los campos obligatorios, el precio, la sesión y que haya al menos una foto.
     * Si es una subasta, exige una fecha de término futura.
     * Si el repositorio informa un error, muestra un aviso y habilita de nuevo el botón.
     */
    private void guardar(){
        String t=titulo.getText().toString().trim(),d=descripcion.getText().toString().trim(),p=precio.getText().toString().trim(),u=ubicacion.getText().toString().trim();
        if(t.isEmpty()||p.isEmpty()||u.isEmpty()||categoryList.isEmpty()){mensaje("Completa título, precio, ciudad y categoría");return;}
        double valor;try{valor=Double.parseDouble(p);}catch(Exception e){mensaje("Precio inválido");return;}
        if(!Double.isFinite(valor)||valor<=0){mensaje("El precio debe ser mayor que cero");return;}
        boolean sub=modalidad.getSelectedItemPosition()==1;
        if(sub&&(finMs<=System.currentTimeMillis())){mensaje("Selecciona una fecha de término futura");return;}
        int id=getSharedPreferences("MisPreferencias",MODE_PRIVATE).getInt("usuario_id",-1);
        if(id<=0){mensaje("Inicia sesión nuevamente");return;}
        long cat=categoryList.get(categorias.getSelectedItemPosition()).id;
        String condicionSeleccionada=opcionesCondicion[condiciones.getSelectedItemPosition()];
        if (fotos.isEmpty()) { mensaje("Debes agregar al menos una imagen"); return; }
        final ArrayList<String> imagenesGuardar = new ArrayList<>(fotos);
        final Double latGuardada=latitud,lonGuardada=longitud;
        findViewById(R.id.btnPublicar).setEnabled(false);
        io.execute(()->{
            try{repo.guardar(editarId,id,cat,t,d,valor,condicionSeleccionada,u,imagenRuta,sub,System.currentTimeMillis(),finMs,latGuardada,lonGuardada,imagenesGuardar);
                runOnUiThread(()->{mensaje("Publicación guardada");finish();});
            }catch(Exception e){runOnUiThread(()->{findViewById(R.id.btnPublicar).setEnabled(true);mensaje("Error: "+e.getMessage());});}
        });
    }
    /**
     * Muestra un aviso breve en el formulario.
     *
     * @param m texto que se muestra al usuario
     */
    private void mensaje(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
    /**
     * Cierra el ejecutor de tareas en segundo plano al destruir la pantalla.
     */
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

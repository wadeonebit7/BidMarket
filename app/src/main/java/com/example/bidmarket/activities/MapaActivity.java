package com.example.bidmarket.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.bidmarket.repositories.PublicacionRepository;
import com.example.bidmarket.R;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra en el mapa la ubicación de la publicación y, con permiso, la del usuario.
 * También muestra las lecturas del acelerómetro si el teléfono tiene ese sensor.
 * El mapa requiere una clave habilitada para Maps SDK for Android.
 */
public class MapaActivity extends AppCompatActivity implements SensorEventListener {
    private GoogleMap mapa;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private FusedLocationProviderClient fused;
    private SensorManager sensores;
    private Sensor acelerometro;
    private TextView lecturaSensor;
    private final ActivityResultLauncher<String> permiso=registerForActivityResult(new ActivityResultContracts.RequestPermission(),ok->{if(ok)ubicar();else aviso("Sin permiso, se muestra solamente el mapa");});
    /**
     * Prepara la pantalla, el servicio de ubicación y el acelerómetro.
     * Cuando el mapa está listo, solicita la ubicación del usuario y busca
     * las coordenadas de la publicación.
     *
     * @param b estado guardado de la pantalla, o {@code null} si no hay uno.
     */
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);setContentView(R.layout.activity_mapa);

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(
                getWindow(), false
        );

        getWindow().setStatusBarColor(
                android.graphics.Color.TRANSPARENT
        );

        getWindow().setNavigationBarColor(
                android.graphics.Color.TRANSPARENT
        );

        androidx.core.view.WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);

        androidx.core.view.WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightNavigationBars(true);

        android.view.View root = findViewById(R.id.main);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                root, (v, insets) -> {

                    androidx.core.graphics.Insets barras = insets.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.systemBars()
                                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                    );

                    v.setPadding(
                            barras.left,
                            barras.top,
                            barras.right,
                            barras.bottom
                    );

                    return insets;
                });

        androidx.core.view.ViewCompat.requestApplyInsets(root);

        fused=LocationServices.getFusedLocationProviderClient(this);
        lecturaSensor=findViewById(R.id.lecturaSensor);
        sensores=(SensorManager)getSystemService(SENSOR_SERVICE);
        acelerometro=sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(acelerometro==null)lecturaSensor.setText("Este equipo no tiene acelerómetro");
        SupportMapFragment frag=(SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if(frag!=null)frag.getMapAsync(googleMap->{mapa=googleMap;pedirUbicacion();mostrarMarcadorPublicacion();});
    }
    /**
     * Busca la ubicación del usuario si el permiso ya está concedido.
     * Si falta el permiso de ubicación precisa, lo solicita.
     */
    private void pedirUbicacion(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)ubicar();else permiso.launch(Manifest.permission.ACCESS_FINE_LOCATION);}
    /**
     * Activa la capa de ubicación si el mapa está listo y hay permiso.
     * Si hay una ubicación reciente, agrega un marcador y centra el mapa en ella.
     * Si no hay una ubicación reciente o falla la consulta, muestra un aviso.
     */
    @SuppressWarnings("MissingPermission") private void ubicar(){
        if(mapa==null||ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
        mapa.setMyLocationEnabled(true);
        fused.getLastLocation().addOnSuccessListener(this,loc->{if(loc!=null){LatLng ll=new LatLng(loc.getLatitude(),loc.getLongitude());mapa.addMarker(new MarkerOptions().position(ll).title("Mi ubicación"));mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(ll,13));}
            else aviso("No hay ubicación reciente; activa GPS y vuelve a intentar");}).addOnFailureListener(this,e->aviso("No se pudo leer el GPS"));
    }
    /**
     * Busca las coordenadas de la publicación en segundo plano.
     * Si existen, agrega un marcador con su título y centra el mapa en ese punto.
     */
    private void mostrarMarcadorPublicacion(){
        long id=getIntent().getLongExtra("publicacion_id",-1);if(id<=0)return;
        io.execute(()->{try(android.database.Cursor c=new PublicacionRepository(this).detalle(id)){
            if(c.moveToFirst()&&!c.isNull(9)&&!c.isNull(10)){
                double lat=c.getDouble(9),lon=c.getDouble(10);String title=c.getString(3);
                runOnUiThread(()->{LatLng ll=new LatLng(lat,lon);mapa.addMarker(new MarkerOptions().position(ll).title(title));mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(ll,13));});
            }
        }});
    }
    /**
     * Muestra un aviso en la pantalla.
     *
     * @param s texto del aviso que se muestra al usuario.
     */
    private void aviso(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    /**
     * Empieza a recibir lecturas si el teléfono tiene acelerómetro.
     */
    @Override protected void onResume(){super.onResume();if(acelerometro!=null)sensores.registerListener(this,acelerometro,SensorManager.SENSOR_DELAY_NORMAL);}
    /**
     * Deja de escuchar el acelerómetro al pausar la pantalla.
     */
    @Override protected void onPause(){super.onPause();sensores.unregisterListener(this);}
    /**
     * Actualiza la lectura de los ejes X, Y y Z en metros por segundo al cuadrado.
     * Ignora los eventos que no vienen del acelerómetro.
     *
     * @param e evento con el sensor de origen y los valores de la lectura.
     */
    @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER)lecturaSensor.setText(String.format(java.util.Locale.getDefault(),"Acelerómetro: X %.1f · Y %.1f · Z %.1f m/s²",e.values[0],e.values[1],e.values[2]));}
    /**
     * Recibe los cambios de precisión del sensor sin modificar la pantalla.
     *
     * @param s sensor cuya precisión cambió.
     * @param accuracy nuevo nivel de precisión informado por Android.
     */
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}
    /**
     * Deja de aceptar nuevas tareas de consulta cuando se destruye la pantalla.
     */
    @Override protected void onDestroy(){super.onDestroy();io.shutdown();}
}

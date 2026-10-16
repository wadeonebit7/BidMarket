package com.example.bidmarket;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Editor de hasta diez fotografías. La primera es la portada. */
public class GaleriaProductoActivity extends AppCompatActivity {
    private final ArrayList<String> rutas = new ArrayList<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private LinearLayout lista;
    private TextView contador;
    private File pendienteCamara;
    private boolean busy;

    private final ActivityResultLauncher<String> galeria = registerForActivityResult(
        new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris == null || uris.isEmpty()) return;
            if (busy) return;
            int disponibles = 10 - rutas.size();
            if (disponibles <= 0) { aviso("Máximo diez imágenes"); return; }
            int numero = Math.min(disponibles, uris.size());
            if (uris.size() > numero) aviso("Solo se añadirán " + numero + " fotografías");
            busy = true;
            io.execute(() -> {
                ArrayList<String> nuevas = new ArrayList<>();
                for (int i = 0; i < numero; i++) {
                    try { nuevas.add(copiarRedimensionada(uris.get(i))); }
                    catch (Exception ignored) { }
                }
                runOnUiThread(() -> { rutas.addAll(nuevas); busy = false; mostrar(); });
            });
        });

    private final ActivityResultLauncher<Uri> camara = registerForActivityResult(
        new ActivityResultContracts.TakePicture(), ok -> {
            if (pendienteCamara == null) return;
            File original = pendienteCamara;
            pendienteCamara = null;
            if (!ok) { original.delete(); return; }
            busy = true;
            io.execute(() -> {
                String ruta = null;
                try { ruta = copiarRedimensionada(Uri.fromFile(original)); }
                catch (Exception ignored) { }
                original.delete();
                final String resultado = ruta;
                runOnUiThread(() -> {
                    if (resultado != null) rutas.add(resultado);
                    else aviso("No se pudo guardar la foto");
                    busy = false; mostrar();
                });
            });
        });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_galeria_producto);
        lista = findViewById(R.id.listaFotos);
        contador = findViewById(R.id.contadorFotos);
        ArrayList<String> iniciales = getIntent().getStringArrayListExtra("rutas");
        if (iniciales != null) rutas.addAll(iniciales.subList(0, Math.min(iniciales.size(), 10)));
        findViewById(R.id.btnFotosGaleria).setOnClickListener(v -> { if (!busy) galeria.launch("image/*"); });
        findViewById(R.id.btnFotosCamara).setOnClickListener(v -> abrirCamara());
        findViewById(R.id.btnFotosListas).setOnClickListener(v -> {
            if (busy) { aviso("Espera a que terminen de cargarse las fotos"); return; }
            if (rutas.isEmpty()) { aviso("Agrega al menos una fotografía"); return; }
            Intent resultado = new Intent();
            resultado.putStringArrayListExtra("rutas", new ArrayList<>(rutas));
            setResult(RESULT_OK, resultado);
            finish();
        });
        mostrar();
    }

    private void abrirCamara() {
        if (busy || rutas.size() >= 10) { aviso("Máximo diez imágenes"); return; }
        try {
            File dir = new File(getCacheDir(), "camara");
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("No se creó carpeta");
            pendienteCamara = new File(dir, UUID.randomUUID() + ".jpg");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendienteCamara);
            camara.launch(uri);
        } catch (Exception ex) { aviso("No se pudo abrir la cámara: " + ex.getMessage()); }
    }

    private String copiarRedimensionada(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth < 1 || bounds.outHeight < 1) throw new IllegalArgumentException("Imagen inválida");
        BitmapFactory.Options opts = new BitmapFactory.Options();
        int largest = Math.max(bounds.outWidth, bounds.outHeight);
        opts.inSampleSize = 1;
        while (largest / opts.inSampleSize > 1920) opts.inSampleSize *= 2;
        Bitmap bitmap;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new IllegalArgumentException("No se pudo decodificar");
        File destino = new File(getFilesDir(), "producto_" + UUID.randomUUID() + ".jpg");
        try (OutputStream out = new FileOutputStream(destino)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)) throw new IllegalStateException("Error de imagen");
        } finally { bitmap.recycle(); }
        return destino.getAbsolutePath();
    }

    private void mostrar() {
        contador.setText(rutas.size() + " / 10 imágenes · La primera es la portada");
        lista.removeAllViews();
        for (int i = 0; i < rutas.size(); i++) {
            final int index = i;
            LinearLayout fila = new LinearLayout(this);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            fila.setPadding(0, 8, 0, 8);
            ImageView foto = new ImageView(this);
            foto.setScaleType(ImageView.ScaleType.FIT_CENTER);
            foto.setImageURI(Uri.fromFile(new File(rutas.get(i))));
            fila.addView(foto, new LinearLayout.LayoutParams(90 * getResources().getDisplayMetrics().densityDpi / 160,
                    90 * getResources().getDisplayMetrics().densityDpi / 160));
            LinearLayout controles = new LinearLayout(this);
            controles.setOrientation(LinearLayout.VERTICAL);
            TextView nombre = new TextView(this);
            nombre.setText((i + 1) + (i == 0 ? " · Portada" : " · Fotografía"));
            nombre.setTextColor(0xFFFFFFFF);
            controles.addView(nombre);
            LinearLayout botones = new LinearLayout(this);
            Button arriba = boton("↑", () -> mover(index, -1));
            Button abajo = boton("↓", () -> mover(index, 1));
            Button quitar = boton("Quitar", () -> { rutas.remove(index); mostrar(); });
            arriba.setEnabled(i > 0);
            abajo.setEnabled(i + 1 < rutas.size());
            botones.addView(arriba); botones.addView(abajo); botones.addView(quitar);
            controles.addView(botones);
            fila.addView(controles);
            lista.addView(fila);
        }
    }
    private Button boton(String titulo, Runnable accion) {
        MaterialButton b = new MaterialButton(this);
        b.setText(titulo);
        b.setAllCaps(false);
        b.setOnClickListener(v -> accion.run());
        return b;
    }
    private void mover(int index, int paso) {
        int otro = index + paso;
        if (otro < 0 || otro >= rutas.size()) return;
        String temp = rutas.get(index); rutas.set(index, rutas.get(otro)); rutas.set(otro, temp); mostrar();
    }
    private void aviso(String texto) { Toast.makeText(this, texto, Toast.LENGTH_SHORT).show(); }
    @Override protected void onDestroy() { super.onDestroy(); io.shutdown(); }
}

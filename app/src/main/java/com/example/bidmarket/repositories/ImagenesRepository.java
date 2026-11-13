package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bidmarket.database.BidMarketContract.Imagenes;
import com.example.bidmarket.database.BidMarketDbHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta y actualiza las rutas locales de las fotografías de cada publicación.
 * El orden empieza en 1 y la primera ruta corresponde a la portada.
 */
public final class ImagenesRepository {

    private final BidMarketDbHelper helper;

    /**
     * Prepara el acceso a las rutas guardadas en SQLite.
     *
     * @param context contexto del que se obtiene el contexto de la aplicación.
     */
    public ImagenesRepository(Context context) {
        helper = new BidMarketDbHelper(
                context.getApplicationContext()
        );
    }

    /**
     * Consulta las rutas de una publicación en el orden en que se muestran.
     *
     * @param publicacionId ID de la publicación que se quiere consultar.
     * @return rutas desde la portada hasta la última imagen, o una lista vacía
     *         si la publicación no tiene fotografías asociadas.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
     */
    public List<String> listar(long publicacionId) {

        List<String> imagenes = new ArrayList<>();

        SQLiteDatabase db = helper.getReadableDatabase();

        String[] columnas = {
                Imagenes.IMAGE_URL
        };

        String seleccion =
                Imagenes.PUBLICACION_ID + " = ?";

        String[] argumentos = {
                String.valueOf(publicacionId)
        };

        String orden =
                Imagenes.DISPLAY_ORDER + " ASC";

        try (Cursor cursor = db.query(
                Imagenes.TABLE,
                columnas,
                seleccion,
                argumentos,
                null,
                null,
                orden
        )) {

            int indiceImagen = cursor.getColumnIndexOrThrow(
                    Imagenes.IMAGE_URL
            );

            while (cursor.moveToNext()) {
                imagenes.add(
                        cursor.getString(indiceImagen)
                );
            }
        }

        return imagenes;
    }

    /**
     * Reemplaza las asociaciones anteriores por las rutas nuevas y usa la primera
     * como portada. No borra los archivos de imagen del dispositivo.
     * El repositorio que llama a este método debe administrar la transacción para
     * poder revertir el reemplazo completo si una inserción falla.
     *
     * @param db base de datos utilizada por la transacción.
     * @param publicacionId ID de la publicación cuyas fotografías se reemplazan.
     * @param rutas lista ordenada de entre 1 y 10 rutas locales, con la portada primero.
     * @throws IllegalArgumentException si la lista es nula, no tiene entre 1 y 10
     *         rutas o incluye alguna ruta nula o vacía.
     * @throws android.database.sqlite.SQLiteException si falla una operación de SQLite.
     */
    public void reemplazar(
            SQLiteDatabase db,
            long publicacionId,
            List<String> rutas
    ) {

        validarFotografias(rutas);

        String seleccion =
                Imagenes.PUBLICACION_ID + " = ?";

        String[] argumentos = {
                String.valueOf(publicacionId)
        };

        // Eliminar las asociaciones anteriores.
        db.delete(
                Imagenes.TABLE,
                seleccion,
                argumentos
        );

        // Registrar las nuevas fotografías en orden.
        for (int i = 0; i < rutas.size(); i++) {

            ContentValues valores = new ContentValues();

            valores.put(
                    Imagenes.PUBLICACION_ID,
                    publicacionId
            );

            valores.put(
                    Imagenes.IMAGE_URL,
                    rutas.get(i)
            );

            valores.put(
                    Imagenes.DISPLAY_ORDER,
                    Imagenes.ORDEN_PORTADA + i
            );

            db.insertOrThrow(
                    Imagenes.TABLE,
                    null,
                    valores
            );
        }
    }

    /**
     * Comprueba la cantidad de fotos y que las rutas tengan contenido.
     * No comprueba si los archivos existen.
     *
     * @param rutas lista de rutas que se quiere validar.
     * @throws IllegalArgumentException si la lista es nula, no tiene entre 1 y 10
     *         elementos o alguna ruta es nula o queda vacía al quitar espacios.
     */
    private void validarFotografias(List<String> rutas) {

        if (rutas == null
                || rutas.size() < Imagenes.MIN_IMAGENES
                || rutas.size() > Imagenes.MAX_IMAGENES) {

            throw new IllegalArgumentException(
                    "Se requieren entre 1 y 10 fotografías"
            );
        }

        for (String ruta : rutas) {
            if (ruta == null || ruta.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "La ruta de una fotografía no puede estar vacía"
                );
            }
        }
    }
}

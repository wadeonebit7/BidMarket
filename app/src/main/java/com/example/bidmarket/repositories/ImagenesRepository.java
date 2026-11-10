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
 * Repositorio encargado de gestionar las fotografías
 * asociadas a las publicaciones.
 *
 * Las imágenes se almacenan mediante rutas locales.
 * El orden de visualización comienza en 1.
 */
public final class ImagenesRepository {

    private final BidMarketDbHelper helper;

    public ImagenesRepository(Context context) {
        helper = new BidMarketDbHelper(
                context.getApplicationContext()
        );
    }

    /**
     * Obtiene las fotografías de una publicación
     * ordenadas desde la portada hasta la última imagen.
     *
     * @param publicacionId ID de la publicación.
     * @return Lista de rutas de las fotografías.
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
     * Reemplaza las fotografías de una publicación.
     *
     * Debe ejecutarse dentro de una transacción
     * administrada por el repositorio que realiza
     * la operación principal.
     *
     * @param db Base de datos utilizada por la transacción.
     * @param publicacionId ID de la publicación.
     * @param rutas Lista ordenada de rutas locales.
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
     * Verifica que la publicación tenga entre
     * 1 y 10 fotografías con rutas válidas.
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

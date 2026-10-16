package com.example.bidmarket;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/** Las rutas son locales al dispositivo. display_order comienza en 1. */
public final class ImagenesRepository {
    private final BidMarketDbHelper helper;
    public ImagenesRepository(Context context) { helper = new BidMarketDbHelper(context); }

    public List<String> listar(long publicacionId) {
        List<String> imagenes = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT image_url FROM imagenes WHERE publicacion_id=? ORDER BY display_order ASC",
                new String[]{Long.toString(publicacionId)})) {
            while (c.moveToNext()) imagenes.add(c.getString(0));
        }
        return imagenes;
    }

    /** Ejecutar desde hilo secundario y después de validar la propiedad del anuncio. */
    public void reemplazar(SQLiteDatabase db, long publicacionId, List<String> rutas) {
        if (rutas == null || rutas.isEmpty() || rutas.size() > 10)
            throw new IllegalArgumentException("Se requieren de 1 a 10 fotografías");
        db.delete("imagenes", "publicacion_id=?", new String[]{Long.toString(publicacionId)});
        for (int i = 0; i < rutas.size(); i++) {
            ContentValues cv = new ContentValues();
            cv.put("publicacion_id", publicacionId);
            cv.put("image_url", rutas.get(i));
            cv.put("display_order", i + 1);
            db.insertOrThrow("imagenes", null, cv);
        }
    }
}

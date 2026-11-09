package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bidmarket.database.BidMarketContract.Notificaciones;
import com.example.bidmarket.database.BidMarketDbHelper;

import java.util.ArrayList;
import java.util.List;

/** Persistencia de la bandeja local de notificaciones (no push). */
public final class NotificacionRepository {
    private final BidMarketDbHelper helper;

    public NotificacionRepository(Context context) {
        helper = new BidMarketDbHelper(context.getApplicationContext());
    }

    public static final class Aviso {
        public final String mensaje;
        public final long fechaMs;

        public Aviso(String mensaje, long fechaMs) {
            this.mensaje = mensaje;
            this.fechaMs = fechaMs;
        }
    }

    /** Consulta los avisos y marca como leídos los del mismo usuario. */
    public List<Aviso> listarYMarcarLeidas(int usuarioId) {
        if (usuarioId <= 0) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        List<Aviso> avisos = new ArrayList<>();
        db.beginTransaction();
        try {
            String[] columnas = {Notificaciones.MENSAJE, Notificaciones.FECHA};
            try (Cursor cursor = db.query(
                    Notificaciones.TABLE,
                    columnas,
                    Notificaciones.USUARIO_ID + " = ?",
                    new String[]{String.valueOf(usuarioId)},
                    null, null,
                    Notificaciones.FECHA + " DESC")) {

                int indiceMensaje = cursor.getColumnIndexOrThrow(Notificaciones.MENSAJE);
                int indiceFecha = cursor.getColumnIndexOrThrow(Notificaciones.FECHA);
                while (cursor.moveToNext()) {
                    avisos.add(new Aviso(
                            cursor.getString(indiceMensaje),
                            cursor.getLong(indiceFecha)));
                }
            }

            ContentValues valores = new ContentValues();
            valores.put(Notificaciones.LEIDA, Notificaciones.LEIDA_SI);
            db.update(Notificaciones.TABLE, valores,
                    Notificaciones.USUARIO_ID + " = ? AND " + Notificaciones.LEIDA + " = ?",
                    new String[]{String.valueOf(usuarioId),
                            String.valueOf(Notificaciones.NO_LEIDA)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return avisos;
    }
}

package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bidmarket.database.BidMarketContract.Notificaciones;
import com.example.bidmarket.database.BidMarketDbHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta y actualiza los avisos locales guardados en SQLite.
 * No envía notificaciones push.
 */
public final class NotificacionRepository {
    private final BidMarketDbHelper helper;

    /**
     * Prepara el acceso a la base de datos local.
     *
     * @param context contexto del que se obtiene el contexto de la aplicación.
     */
    public NotificacionRepository(Context context) {
        helper = new BidMarketDbHelper(context.getApplicationContext());
    }

    /**
     * Representa un aviso con su mensaje y fecha.
     */
    public static final class Aviso {
        /**
         * Texto del aviso.
         */
        public final String mensaje;
        /**
         * Fecha del aviso en milisegundos desde el inicio de la época Unix.
         */
        public final long fechaMs;

        /**
         * Crea un aviso con los datos consultados.
         *
         * @param mensaje texto del aviso.
         * @param fechaMs fecha del aviso en milisegundos desde el inicio de la época Unix.
         */
        public Aviso(String mensaje, long fechaMs) {
            this.mensaje = mensaje;
            this.fechaMs = fechaMs;
        }
    }

    /**
     * Consulta todos los avisos del usuario y marca como leídos los pendientes.
     * Ambas operaciones se realizan dentro de la misma transacción.
     *
     * @param usuarioId ID del usuario que consulta sus avisos.
     * @return avisos del más reciente al más antiguo, o una lista vacía si no hay ninguno.
     * @throws IllegalArgumentException si el ID del usuario es menor o igual a cero.
     * @throws android.database.sqlite.SQLiteException si falla una operación de SQLite.
     */
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

package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bidmarket.database.BidMarketDbHelper;
import com.example.bidmarket.database.BidMarketContract.Chats;
import com.example.bidmarket.database.BidMarketContract.Mensajes;
import com.example.bidmarket.database.BidMarketContract.Publicaciones;
import com.example.bidmarket.database.BidMarketContract.Subastas;
import com.example.bidmarket.database.BidMarketContract.Usuarios;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de conversaciones y mensajes locales.
 *
 * Centraliza el acceso a SQLite y las validaciones
 * necesarias para iniciar y utilizar conversaciones.
 *
 * No implementa comunicación por Internet ni tiempo real.
 */
public final class ChatRepository {

    private static final int MAX_LONGITUD_MENSAJE = 2000;

    private final BidMarketDbHelper helper;

    public ChatRepository(Context context) {
        helper = new BidMarketDbHelper(
                context.getApplicationContext()
        );
    }

    // =====================================================
    // MODELOS DE RESULTADO
    // =====================================================

    public static final class Entrada {

        public final long id;
        public final String titulo;
        public final String interlocutor;
        public final String ultimo;

        public Entrada(
                long id,
                String titulo,
                String interlocutor,
                String ultimo
        ) {
            this.id = id;
            this.titulo = titulo;
            this.interlocutor = interlocutor;
            this.ultimo = ultimo;
        }
    }

    public static final class Mensaje {

        public final long id;
        public final int autor;
        public final String texto;

        public Mensaje(long id, int autor, String texto) {
            this.id = id;
            this.autor = autor;
            this.texto = texto;
        }
    }

    // =====================================================
    // VALIDAR INICIO DE CONVERSACIÓN
    // =====================================================

    /**
     * Comprueba las restricciones de la publicación
     * antes de permitir una nueva conversación.
     *
     * Se valida desde SQLite y no únicamente desde la UI.
     */
    private void validarInicio(
            SQLiteDatabase db,
            long publicacionId,
            int solicitante
    ) {

        String sql =
                "SELECT " +
                        "p." + Publicaciones.VENDEDOR_ID + ", " +
                        "p." + Publicaciones.MODALIDAD + ", " +
                        "p." + Publicaciones.STATUS + ", " +
                        "s." + Subastas.FIN + ", " +
                        "s." + Subastas.STATUS + " " +

                        "FROM " + Publicaciones.TABLE + " p " +

                        "LEFT JOIN " + Subastas.TABLE + " s " +
                        "ON s." + Subastas.PUBLICACION_ID +
                        " = p." + Publicaciones.ID + " " +

                        "WHERE p." + Publicaciones.ID + " = ?";

        String[] argumentos = {
                String.valueOf(publicacionId)
        };

        try (Cursor cursor = db.rawQuery(sql, argumentos)) {

            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException(
                        "Publicación inexistente"
                );
            }

            int vendedor = cursor.getInt(0);
            String modalidad = cursor.getString(1);
            String estadoPublicacion = cursor.getString(2);

            if (vendedor == solicitante) {
                throw new IllegalArgumentException(
                        "No puedes escribirte a ti mismo"
                );
            }

            if (Publicaciones.BORRADO.equals(estadoPublicacion)) {
                throw new IllegalArgumentException(
                        "Publicación eliminada"
                );
            }

            if (Publicaciones.SUBASTA.equals(modalidad)) {

                boolean sinFechaFin = cursor.isNull(3);

                boolean noFinalizada = !sinFechaFin
                        && System.currentTimeMillis()
                        < cursor.getLong(3);

                boolean cancelada = Subastas.CANCELADA.equals(
                        cursor.getString(4)
                );

                if (sinFechaFin || noFinalizada || cancelada) {
                    throw new IllegalArgumentException(
                            "El chat estará disponible cuando " +
                                    "finalice la subasta"
                    );
                }

            } else if (!Publicaciones.ACTIVO.equals(estadoPublicacion)
                    && !Publicaciones.VENDIDO.equals(estadoPublicacion)) {

                throw new IllegalArgumentException(
                        "La publicación no permite nuevas conversaciones"
                );
            }
        }
    }

    // =====================================================
    // INICIAR O RECUPERAR CHAT
    // =====================================================

    public long iniciar(long publicacionId, int interesado) {

        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            validarInicio(db, publicacionId, interesado);

            int vendedor;

            String consultaVendedor =
                    "SELECT " + Publicaciones.VENDEDOR_ID + " " +
                            "FROM " + Publicaciones.TABLE + " " +
                            "WHERE " + Publicaciones.ID + " = ?";

            try (Cursor cursor = db.rawQuery(
                    consultaVendedor,
                    new String[]{String.valueOf(publicacionId)}
            )) {
                if (!cursor.moveToFirst()) {
                    throw new IllegalArgumentException(
                            "Publicación inexistente"
                    );
                }

                vendedor = cursor.getInt(0);
            }

            String consultaUsuario =
                    "SELECT " + Usuarios.ID + " " +
                            "FROM " + Usuarios.TABLE + " " +
                            "WHERE " + Usuarios.ID + " = ?";

            try (Cursor cursor = db.rawQuery(
                    consultaUsuario,
                    new String[]{String.valueOf(interesado)}
            )) {
                if (!cursor.moveToFirst()) {
                    throw new IllegalArgumentException(
                            "Sesión inválida"
                    );
                }
            }

            ContentValues valores = new ContentValues();

            valores.put(Chats.PUBLICACION_ID, publicacionId);
            valores.put(Chats.COMPRADOR_ID, interesado);
            valores.put(Chats.VENDEDOR_ID, vendedor);
            valores.put(Chats.CREADO, System.currentTimeMillis());

            // Conserva una conversación existente si ya existe.
            db.insertWithOnConflict(
                    Chats.TABLE,
                    null,
                    valores,
                    SQLiteDatabase.CONFLICT_IGNORE
            );

            String consultaChat =
                    "SELECT " + Chats.ID + " " +
                            "FROM " + Chats.TABLE + " " +
                            "WHERE " + Chats.PUBLICACION_ID + " = ? " +
                            "AND " + Chats.COMPRADOR_ID + " = ?";

            String[] argumentos = {
                    String.valueOf(publicacionId),
                    String.valueOf(interesado)
            };

            long chatId;

            try (Cursor cursor = db.rawQuery(
                    consultaChat,
                    argumentos
            )) {
                if (!cursor.moveToFirst()) {
                    throw new IllegalStateException(
                            "No se pudo iniciar chat"
                    );
                }

                chatId = cursor.getLong(0);
            }

            db.setTransactionSuccessful();

            return chatId;

        } finally {
            db.endTransaction();
        }
    }

    // =====================================================
    // VERIFICAR PARTICIPACIÓN
    // =====================================================

    public boolean participante(long chatId, int usuario) {

        SQLiteDatabase db = helper.getReadableDatabase();

        String consulta =
                "SELECT " + Chats.ID + " " +
                        "FROM " + Chats.TABLE + " " +
                        "WHERE " + Chats.ID + " = ? " +
                        "AND (" +
                        Chats.COMPRADOR_ID + " = ? OR " +
                        Chats.VENDEDOR_ID + " = ?" +
                        ")";

        String[] argumentos = {
                String.valueOf(chatId),
                String.valueOf(usuario),
                String.valueOf(usuario)
        };

        try (Cursor cursor = db.rawQuery(
                consulta,
                argumentos
        )) {
            return cursor.moveToFirst();
        }
    }

    // =====================================================
    // OBTENER TÍTULO DE CONVERSACIÓN
    // =====================================================

    public String titulo(long chatId, int usuario) {

        SQLiteDatabase db = helper.getReadableDatabase();

        String consulta =
                "SELECT " +
                        "p." + Publicaciones.TITLE + ", " +

                        "CASE WHEN c." + Chats.COMPRADOR_ID + " = ? " +
                        "THEN v." + Usuarios.NAME + " " +
                        "ELSE b." + Usuarios.NAME + " END " +

                        "FROM " + Chats.TABLE + " c " +

                        "JOIN " + Publicaciones.TABLE + " p " +
                        "ON p." + Publicaciones.ID +
                        " = c." + Chats.PUBLICACION_ID + " " +

                        "JOIN " + Usuarios.TABLE + " v " +
                        "ON v." + Usuarios.ID +
                        " = c." + Chats.VENDEDOR_ID + " " +

                        "JOIN " + Usuarios.TABLE + " b " +
                        "ON b." + Usuarios.ID +
                        " = c." + Chats.COMPRADOR_ID + " " +

                        "WHERE c." + Chats.ID + " = ? " +
                        "AND (c." + Chats.COMPRADOR_ID + " = ? " +
                        "OR c." + Chats.VENDEDOR_ID + " = ?)";

        String[] argumentos = {
                String.valueOf(usuario),
                String.valueOf(chatId),
                String.valueOf(usuario),
                String.valueOf(usuario)
        };

        try (Cursor cursor = db.rawQuery(
                consulta,
                argumentos
        )) {
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException(
                        "Chat no autorizado"
                );
            }

            return cursor.getString(0)
                    + " · "
                    + cursor.getString(1);
        }
    }

    // =====================================================
    // HISTORIAL DE CONVERSACIONES
    // =====================================================

    public List<Entrada> historial(int usuario) {

        List<Entrada> resultado = new ArrayList<>();

        SQLiteDatabase db = helper.getReadableDatabase();

        String consulta =
                "SELECT " +
                        "c." + Chats.ID + ", " +
                        "p." + Publicaciones.TITLE + ", " +

                        "CASE WHEN c." + Chats.COMPRADOR_ID + " = ? " +
                        "THEN v." + Usuarios.NAME + " " +
                        "ELSE b." + Usuarios.NAME + " END, " +

                        "COALESCE((" +
                        "SELECT m." + Mensajes.TEXTO + " " +
                        "FROM " + Mensajes.TABLE + " m " +
                        "WHERE m." + Mensajes.CHAT_ID +
                        " = c." + Chats.ID + " " +
                        "ORDER BY m." + Mensajes.ID + " DESC " +
                        "LIMIT 1" +
                        "), 'Conversación nueva') " +

                        "FROM " + Chats.TABLE + " c " +

                        "JOIN " + Publicaciones.TABLE + " p " +
                        "ON p." + Publicaciones.ID +
                        " = c." + Chats.PUBLICACION_ID + " " +

                        "JOIN " + Usuarios.TABLE + " v " +
                        "ON v." + Usuarios.ID +
                        " = c." + Chats.VENDEDOR_ID + " " +

                        "JOIN " + Usuarios.TABLE + " b " +
                        "ON b." + Usuarios.ID +
                        " = c." + Chats.COMPRADOR_ID + " " +

                        "WHERE c." + Chats.COMPRADOR_ID + " = ? " +
                        "OR c." + Chats.VENDEDOR_ID + " = ? " +

                        "ORDER BY COALESCE((" +
                        "SELECT MAX(" + Mensajes.ENVIADO + ") " +
                        "FROM " + Mensajes.TABLE + " " +
                        "WHERE " + Mensajes.CHAT_ID +
                        " = c." + Chats.ID +
                        "), c." + Chats.CREADO + ") DESC";

        String[] argumentos = {
                String.valueOf(usuario),
                String.valueOf(usuario),
                String.valueOf(usuario)
        };

        try (Cursor cursor = db.rawQuery(
                consulta,
                argumentos
        )) {
            while (cursor.moveToNext()) {
                resultado.add(new Entrada(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3)
                ));
            }
        }

        return resultado;
    }

    // =====================================================
    // LISTAR MENSAJES
    // =====================================================

    public List<Mensaje> mensajes(long chatId, int usuario) {

        if (!participante(chatId, usuario)) {
            throw new IllegalArgumentException(
                    "Chat no autorizado"
            );
        }

        List<Mensaje> resultado = new ArrayList<>();

        SQLiteDatabase db = helper.getReadableDatabase();

        String[] columnas = {
                Mensajes.ID,
                Mensajes.REMITENTE_ID,
                Mensajes.TEXTO
        };

        try (Cursor cursor = db.query(
                Mensajes.TABLE,
                columnas,
                Mensajes.CHAT_ID + " = ?",
                new String[]{String.valueOf(chatId)},
                null,
                null,
                Mensajes.ID + " ASC"
        )) {

            int indiceId = cursor.getColumnIndexOrThrow(
                    Mensajes.ID
            );

            int indiceRemitente = cursor.getColumnIndexOrThrow(
                    Mensajes.REMITENTE_ID
            );

            int indiceTexto = cursor.getColumnIndexOrThrow(
                    Mensajes.TEXTO
            );

            while (cursor.moveToNext()) {
                resultado.add(new Mensaje(
                        cursor.getLong(indiceId),
                        cursor.getInt(indiceRemitente),
                        cursor.getString(indiceTexto)
                ));
            }
        }

        return resultado;
    }

    // =====================================================
    // ENVIAR MENSAJE
    // =====================================================

    public void enviar(
            long chatId,
            int usuario,
            String texto
    ) {

        if (texto == null) {
            throw new IllegalArgumentException(
                    "El mensaje no puede estar vacío"
            );
        }

        String limpio = texto.trim();

        if (limpio.isEmpty()
                || limpio.length() > MAX_LONGITUD_MENSAJE) {

            throw new IllegalArgumentException(
                    "Mensaje de 1 a 2000 caracteres requerido"
            );
        }

        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            String consulta =
                    "SELECT " +
                            "c." + Chats.ID + ", " +
                            "p." + Publicaciones.MODALIDAD + ", " +
                            "s." + Subastas.FIN + ", " +
                            "s." + Subastas.STATUS + " " +

                            "FROM " + Chats.TABLE + " c " +

                            "JOIN " + Publicaciones.TABLE + " p " +
                            "ON p." + Publicaciones.ID +
                            " = c." + Chats.PUBLICACION_ID + " " +

                            "LEFT JOIN " + Subastas.TABLE + " s " +
                            "ON s." + Subastas.PUBLICACION_ID +
                            " = p." + Publicaciones.ID + " " +

                            "WHERE c." + Chats.ID + " = ? " +
                            "AND (c." + Chats.COMPRADOR_ID + " = ? " +
                            "OR c." + Chats.VENDEDOR_ID + " = ?)";

            String[] argumentos = {
                    String.valueOf(chatId),
                    String.valueOf(usuario),
                    String.valueOf(usuario)
            };

            try (Cursor cursor = db.rawQuery(
                    consulta,
                    argumentos
            )) {
                if (!cursor.moveToFirst()) {
                    throw new IllegalArgumentException(
                            "Chat no autorizado"
                    );
                }

                String modalidad = cursor.getString(1);

                if (Publicaciones.SUBASTA.equals(modalidad)) {

                    boolean sinFechaFin = cursor.isNull(2);

                    boolean noFinalizada = !sinFechaFin
                            && System.currentTimeMillis()
                            < cursor.getLong(2);

                    boolean cancelada = Subastas.CANCELADA.equals(
                            cursor.getString(3)
                    );

                    if (sinFechaFin || noFinalizada || cancelada) {
                        throw new IllegalArgumentException(
                                "La subasta aún no ha finalizado"
                        );
                    }
                }
            }

            ContentValues valores = new ContentValues();

            valores.put(Mensajes.CHAT_ID, chatId);
            valores.put(Mensajes.REMITENTE_ID, usuario);
            valores.put(Mensajes.TEXTO, limpio);
            valores.put(
                    Mensajes.ENVIADO,
                    System.currentTimeMillis()
            );

            db.insertOrThrow(
                    Mensajes.TABLE,
                    null,
                    valores
            );

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }
}

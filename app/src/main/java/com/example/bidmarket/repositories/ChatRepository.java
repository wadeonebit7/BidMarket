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

    /**
     * Prepara el acceso a los chats y mensajes guardados en SQLite.
     *
     * @param context contexto del que se obtiene el contexto de la aplicación.
     */
    public ChatRepository(Context context) {
        helper = new BidMarketDbHelper(
                context.getApplicationContext()
        );
    }

    // =====================================================
    // MODELOS DE RESULTADO
    // =====================================================

    /**
     * Resumen de una conversación que se muestra en el historial.
     */
    public static final class Entrada {

        /**
         * ID de la conversación.
         */
        public final long id;
        /**
         * Título de la publicación asociada.
         */
        public final String titulo;
        /**
         * Nombre del otro participante.
         */
        public final String interlocutor;
        /**
         * Último mensaje o aviso de conversación nueva.
         */
        public final String ultimo;

        /**
         * Crea un resumen de la conversación.
         *
         * @param id ID de la conversación.
         * @param titulo título de la publicación asociada.
         * @param interlocutor nombre del otro participante.
         * @param ultimo texto del último mensaje o el aviso de conversación nueva.
         */
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

    /**
     * Representa un mensaje local con su identificador, autor y texto.
     */
    public static final class Mensaje {

        /**
         * ID del mensaje.
         */
        public final long id;
        /**
         * ID del usuario que lo envió.
         */
        public final int autor;
        /**
         * Contenido del mensaje.
         */
        public final String texto;

        /**
         * Crea un mensaje a partir de los datos consultados.
         *
         * @param id ID del mensaje.
         * @param autor ID del usuario que lo envió.
         * @param texto contenido del mensaje.
         */
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
     * Comprueba si la publicación admite un chat con el usuario que lo solicita.
     * Para una subasta, la fecha de cierre debe haber pasado y la subasta no
     * puede estar cancelada. Para una venta directa, la publicación debe estar
     * activa o vendida.
     *
     * @param db base de datos donde se consulta la publicación.
     * @param publicacionId ID de la publicación.
     * @param solicitante ID del usuario que quiere contactar al vendedor.
     * @throws IllegalArgumentException si la publicación no existe, está borrada,
     *         pertenece al solicitante o no cumple las condiciones para abrir el chat.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
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

    /**
     * Crea un chat local entre el interesado y el vendedor, o recupera el chat
     * que ya existe para esa publicación y ese interesado. Valida las condiciones
     * de acceso incluso cuando el chat ya existe.
     *
     * @param publicacionId ID de la publicación por la que se inicia el chat.
     * @param interesado ID del usuario que quiere contactar al vendedor.
     * @return ID del chat creado o recuperado.
     * @throws IllegalArgumentException si el usuario no existe o la publicación no
     *         permite abrir el chat, por ejemplo, si la subasta aún no termina.
     * @throws IllegalStateException si no se encuentra el chat tras intentar crearlo.
     * @throws android.database.sqlite.SQLiteException si falla una operación de SQLite.
     */
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

    /**
     * Comprueba si el usuario es el comprador o el vendedor del chat.
     *
     * @param chatId ID de la conversación.
     * @param usuario ID del usuario que se quiere comprobar.
     * @return {@code true} si el usuario participa en el chat, o {@code false} si no.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
     */
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

    /**
     * Obtiene el título que se muestra al abrir una conversación.
     *
     * @param chatId ID de la conversación.
     * @param usuario ID del participante que consulta el título.
     * @return título de la publicación y nombre del otro participante, separados por un punto medio.
     * @throws IllegalArgumentException si el chat no existe o el usuario no participa.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
     */
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

    /**
     * Consulta los chats en los que participa el usuario. Para cada chat incluye
     * el último mensaje o un aviso si todavía no tiene mensajes.
     *
     * @param usuario ID del usuario que consulta su historial.
     * @return conversaciones ordenadas de la más reciente a la más antigua según
     *         la actividad del chat, o una lista vacía si no hay conversaciones.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
     */
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

    /**
     * Consulta los mensajes de una conversación después de comprobar que el
     * usuario participa en ella.
     *
     * @param chatId ID de la conversación.
     * @param usuario ID del usuario que consulta los mensajes.
     * @return mensajes ordenados por ID de menor a mayor, o una lista vacía si no hay mensajes.
     * @throws IllegalArgumentException si el chat no existe o el usuario no participa.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
     */
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

    /**
     * Guarda un mensaje local después de validar el texto, la participación
     * del usuario y, si corresponde, el cierre de la subasta.
     *
     * @param chatId ID de la conversación.
     * @param usuario ID del usuario que envía el mensaje.
     * @param texto contenido del mensaje, de 1 a 2000 caracteres después de quitar
     *         los espacios del inicio y del final.
     * @throws IllegalArgumentException si el texto no cumple el límite, el chat
     *         no existe, el usuario no participa o la subasta no permite enviar mensajes.
     * @throws android.database.sqlite.SQLiteException si falla una operación de SQLite.
     */
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

package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bidmarket.database.BidMarketDbHelper;
import com.example.bidmarket.database.BidMarketContract.Categorias;
import com.example.bidmarket.database.BidMarketContract.Publicaciones;
import com.example.bidmarket.database.BidMarketContract.Imagenes;
import com.example.bidmarket.database.BidMarketContract.Subastas;
import com.example.bidmarket.database.BidMarketContract.Pujas;
import com.example.bidmarket.database.BidMarketContract.Compras;
import com.example.bidmarket.database.BidMarketContract.Notificaciones;
import com.example.bidmarket.models.Publicacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de publicaciones, ventas y subastas.
 *
 * Centraliza las operaciones de persistencia relacionadas
 * con publicaciones y sus operaciones asociadas.
 *
 * Las consultas utilizan BidMarketContract para los
 * identificadores del esquema SQLite.
 *
 * Las operaciones de disco deben ejecutarse fuera
 * del hilo principal.
 */
public class PublicacionRepository {

    private final BidMarketDbHelper helper;

    public PublicacionRepository(Context context) {
        helper = new BidMarketDbHelper(
                context.getApplicationContext()
        );
    }

    // =====================================================
    // MODELO AUXILIAR DE CATEGORÍAS
    // =====================================================

    public static class Categoria {

        public final long id;
        public final String nombre;

        Categoria(long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    // =====================================================
    // CONSULTAR CATEGORÍAS
    // =====================================================

    public List<Categoria> categorias() {

        List<Categoria> resultado = new ArrayList<>();

        SQLiteDatabase db = helper.getReadableDatabase();

        String[] columnas = {
                Categorias.ID,
                Categorias.NAME
        };

        try (Cursor cursor = db.query(
                Categorias.TABLE,
                columnas,
                null,
                null,
                null,
                null,
                Categorias.NAME + " ASC"
        )) {

            int indiceId = cursor.getColumnIndexOrThrow(
                    Categorias.ID
            );

            int indiceNombre = cursor.getColumnIndexOrThrow(
                    Categorias.NAME
            );

            while (cursor.moveToNext()) {
                resultado.add(new Categoria(
                        cursor.getLong(indiceId),
                        cursor.getString(indiceNombre)
                ));
            }
        }

        return resultado;
    }

    // =====================================================
    // LISTAR PUBLICACIONES ACTIVAS
    // =====================================================

    public List<Publicacion> listar() {

        List<Publicacion> resultado = new ArrayList<>();

        String sql =
                "SELECT " +
                        "p." + Publicaciones.ID + ", " +
                        "p." + Publicaciones.TITLE + ", " +
                        "p." + Publicaciones.PRICE + ", " +
                        "p." + Publicaciones.CURRENCY + ", " +
                        "p." + Publicaciones.CONDITION + ", " +
                        "i." + Imagenes.IMAGE_URL + ", " +
                        "p." + Publicaciones.MODALIDAD + " " +

                        "FROM " + Publicaciones.TABLE + " p " +

                        "LEFT JOIN " + Imagenes.TABLE + " i " +
                        "ON i." + Imagenes.PUBLICACION_ID +
                        " = p." + Publicaciones.ID + " " +
                        "AND i." + Imagenes.DISPLAY_ORDER + " = ? " +

                        "WHERE p." + Publicaciones.STATUS + " = ? " +

                        "ORDER BY p." + Publicaciones.ID + " DESC";

        String[] argumentos = {
                String.valueOf(Imagenes.ORDEN_PORTADA),
                Publicaciones.ACTIVO
        };

        SQLiteDatabase db = helper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(sql, argumentos)) {

            while (cursor.moveToNext()) {

                Publicacion publicacion = new Publicacion(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getDouble(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6)
                );

                resultado.add(publicacion);
            }
        }

        return resultado;
    }

    // =====================================================
    // CONSULTAR DETALLE DE PUBLICACIÓN
    // =====================================================

    /**
     * Devuelve un Cursor para mantener compatibilidad
     * con DetallePublicacionActivity.
     *
     * La clase que lo recibe debe cerrar el Cursor.
     */
    public Cursor detalle(long id) {

        String sql =
                "SELECT " +
                        "p." + Publicaciones.ID + ", " +
                        "p." + Publicaciones.VENDEDOR_ID + ", " +
                        "p." + Publicaciones.CATEGORIA_ID + ", " +
                        "p." + Publicaciones.TITLE + ", " +
                        "p." + Publicaciones.DESCRIPTION + ", " +
                        "p." + Publicaciones.PRICE + ", " +
                        "p." + Publicaciones.CONDITION + ", " +
                        "p." + Publicaciones.STATUS + ", " +
                        "p." + Publicaciones.LOCATION_NAME + ", " +
                        "p." + Publicaciones.LATITUDE + ", " +
                        "p." + Publicaciones.LONGITUDE + ", " +
                        "p." + Publicaciones.MODALIDAD + ", " +
                        "i." + Imagenes.IMAGE_URL + " " +

                        "FROM " + Publicaciones.TABLE + " p " +

                        "LEFT JOIN " + Imagenes.TABLE + " i " +
                        "ON i." + Imagenes.PUBLICACION_ID +
                        " = p." + Publicaciones.ID + " " +
                        "AND i." + Imagenes.DISPLAY_ORDER + " = ? " +

                        "WHERE p." + Publicaciones.ID + " = ?";

        String[] argumentos = {
                String.valueOf(Imagenes.ORDEN_PORTADA),
                String.valueOf(id)
        };

        return helper.getReadableDatabase().rawQuery(
                sql,
                argumentos
        );
    }

    // =====================================================
    // CREAR O ACTUALIZAR PUBLICACIÓN
    // =====================================================

    public long guardar(
            long id,
            int vendedorId,
            long categoriaId,
            String titulo,
            String descripcion,
            double precio,
            String condicion,
            String ubicacion,
            String imagen,
            boolean subasta,
            long inicio,
            long fin,
            Double lat,
            Double lon,
            List<String> rutasImagenes
    ) {

        if (precio <= 0
                || titulo == null
                || titulo.trim().isEmpty()
                || ubicacion == null
                || ubicacion.trim().isEmpty()
                || vendedorId <= 0) {

            throw new IllegalArgumentException(
                    "Datos de publicación inválidos"
            );
        }

        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            ContentValues valores = new ContentValues();

            valores.put(
                    Publicaciones.VENDEDOR_ID,
                    vendedorId
            );

            valores.put(
                    Publicaciones.CATEGORIA_ID,
                    categoriaId
            );

            valores.put(
                    Publicaciones.TITLE,
                    titulo
            );

            valores.put(
                    Publicaciones.DESCRIPTION,
                    descripcion
            );

            valores.put(
                    Publicaciones.PRICE,
                    precio
            );

            valores.put(
                    Publicaciones.CONDITION,
                    condicion
            );

            valores.put(
                    Publicaciones.LOCATION_NAME,
                    ubicacion
            );

            valores.put(
                    Publicaciones.MODALIDAD,
                    subasta
                            ? Publicaciones.SUBASTA
                            : Publicaciones.VENTA
            );

            // Se conserva el formato usado por el código original.
            valores.put(
                    Publicaciones.UPDATE_AT,
                    System.currentTimeMillis() / 1000
            );

            if (lat != null && lon != null) {
                valores.put(Publicaciones.LATITUDE, lat);
                valores.put(Publicaciones.LONGITUDE, lon);
            }

            long publicacionId = id;

            if (id == 0) {

                valores.put(
                        Publicaciones.STATUS,
                        Publicaciones.ACTIVO
                );

                valores.put(
                        Publicaciones.CURRENCY,
                        Publicaciones.CLP
                );

                publicacionId = db.insertOrThrow(
                        Publicaciones.TABLE,
                        null,
                        valores
                );

            } else {

                String seleccion =
                        Publicaciones.ID + " = ? AND " +
                                Publicaciones.VENDEDOR_ID + " = ? AND " +
                                Publicaciones.STATUS + " IN (?, ?)";

                String[] argumentos = {
                        String.valueOf(id),
                        String.valueOf(vendedorId),
                        Publicaciones.ACTIVO,
                        Publicaciones.PAUSADO
                };

                int actualizados = db.update(
                        Publicaciones.TABLE,
                        valores,
                        seleccion,
                        argumentos
                );

                if (actualizados != 1) {
                    throw new IllegalArgumentException(
                            "Solo el propietario puede editar " +
                                    "una publicación vigente"
                    );
                }
            }

            // -------------------------------------------------
            // FOTOGRAFÍAS
            // -------------------------------------------------

            if (rutasImagenes == null
                    || rutasImagenes.size() < Imagenes.MIN_IMAGENES
                    || rutasImagenes.size() > Imagenes.MAX_IMAGENES) {

                throw new IllegalArgumentException(
                        "Agrega de 1 a 10 imágenes"
                );
            }

            db.delete(
                    Imagenes.TABLE,
                    Imagenes.PUBLICACION_ID + " = ?",
                    new String[]{
                            String.valueOf(publicacionId)
                    }
            );

            int orden = Imagenes.ORDEN_PORTADA;

            for (String ruta : rutasImagenes) {

                ContentValues imagenValores =
                        new ContentValues();

                imagenValores.put(
                        Imagenes.PUBLICACION_ID,
                        publicacionId
                );

                imagenValores.put(
                        Imagenes.IMAGE_URL,
                        ruta
                );

                imagenValores.put(
                        Imagenes.DISPLAY_ORDER,
                        orden++
                );

                db.insertOrThrow(
                        Imagenes.TABLE,
                        null,
                        imagenValores
                );
            }

            // -------------------------------------------------
            // SUBASTAS
            // -------------------------------------------------

            if (subasta) {

                long ahora = System.currentTimeMillis();

                if (fin <= inicio || fin <= ahora) {
                    throw new IllegalArgumentException(
                            "La subasta debe finalizar en el futuro"
                    );
                }

                if (id > 0) {

                    if (tienePujas(db, id)) {
                        throw new IllegalArgumentException(
                                "No es posible modificar " +
                                        "una subasta con pujas"
                        );
                    }

                    eliminarSubasta(db, id);
                }

                ContentValues subastaValores =
                        new ContentValues();

                subastaValores.put(
                        Subastas.PUBLICACION_ID,
                        publicacionId
                );

                subastaValores.put(
                        Subastas.INICIO,
                        inicio
                );

                subastaValores.put(
                        Subastas.FIN,
                        fin
                );

                subastaValores.put(
                        Subastas.MINIMO,
                        (long) Math.ceil(precio)
                );

                subastaValores.put(
                        Subastas.STATUS,
                        inicio > ahora
                                ? Subastas.PROGRAMADA
                                : Subastas.ACTIVA
                );

                db.insertOrThrow(
                        Subastas.TABLE,
                        null,
                        subastaValores
                );

            } else if (id > 0) {

                if (tienePujas(db, id)) {
                    throw new IllegalArgumentException(
                            "No es posible convertir " +
                                    "una subasta con pujas"
                    );
                }

                eliminarSubasta(db, id);
            }

            db.setTransactionSuccessful();

            return publicacionId;

        } finally {
            db.endTransaction();
        }
    }

    // =====================================================
    // MÉTODOS PRIVADOS DE SUBASTAS
    // =====================================================

    private boolean tienePujas(
            SQLiteDatabase db,
            long publicacionId
    ) {

        String sql =
                "SELECT COUNT(*) " +
                        "FROM " + Pujas.TABLE + " " +
                        "WHERE " + Pujas.SUBASTA_ID + " = (" +
                        "SELECT " + Subastas.ID + " " +
                        "FROM " + Subastas.TABLE + " " +
                        "WHERE " + Subastas.PUBLICACION_ID + " = ?" +
                        ")";

        try (Cursor cursor = db.rawQuery(
                sql,
                new String[]{String.valueOf(publicacionId)}
        )) {
            return cursor.moveToFirst()
                    && cursor.getInt(0) > 0;
        }
    }

    private void eliminarSubasta(
            SQLiteDatabase db,
            long publicacionId
    ) {
        db.delete(
                Subastas.TABLE,
                Subastas.PUBLICACION_ID + " = ?",
                new String[]{String.valueOf(publicacionId)}
        );
    }

    // =====================================================
    // CAMBIAR ESTADO DE PUBLICACIÓN
    // =====================================================

    public boolean cambiarEstado(
            long id,
            int propietario,
            String estado
    ) {

        if (!Publicaciones.ACTIVO.equals(estado)
                && !Publicaciones.PAUSADO.equals(estado)
                && !Publicaciones.VENDIDO.equals(estado)
                && !Publicaciones.BORRADO.equals(estado)) {

            return false;
        }

        ContentValues valores = new ContentValues();

        long ahora = System.currentTimeMillis() / 1000;

        valores.put(Publicaciones.STATUS, estado);
        valores.put(Publicaciones.UPDATE_AT, ahora);

        if (Publicaciones.BORRADO.equals(estado)) {
            valores.put(Publicaciones.DELETE_AT, ahora);
        }

        String seleccion =
                Publicaciones.ID + " = ? AND " +
                        Publicaciones.VENDEDOR_ID + " = ? AND " +
                        Publicaciones.STATUS + " IN (?, ?)";

        String[] argumentos = {
                String.valueOf(id),
                String.valueOf(propietario),
                Publicaciones.ACTIVO,
                Publicaciones.PAUSADO
        };

        int actualizados = helper.getWritableDatabase().update(
                Publicaciones.TABLE,
                valores,
                seleccion,
                argumentos
        );

        return actualizados == 1;
    }

    // =====================================================
    // REGISTRAR PUJA
    // =====================================================

    public void ofertar(
            long publicacionId,
            int postorId,
            long monto
    ) {

        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            long subastaId;
            long inicio;
            long fin;
            long minimo;
            long ofertaActual;
            int vendedor;

            String sql =
                    "SELECT " +
                            "s." + Subastas.ID + ", " +
                            "s." + Subastas.INICIO + ", " +
                            "s." + Subastas.FIN + ", " +
                            "s." + Subastas.MINIMO + ", " +
                            "p." + Publicaciones.VENDEDOR_ID + ", " +

                            "COALESCE((" +
                            "SELECT MAX(" + Pujas.MONTO + ") " +
                            "FROM " + Pujas.TABLE + " " +
                            "WHERE " + Pujas.SUBASTA_ID +
                            " = s." + Subastas.ID +
                            "), 0) " +

                            "FROM " + Subastas.TABLE + " s " +

                            "JOIN " + Publicaciones.TABLE + " p " +
                            "ON p." + Publicaciones.ID +
                            " = s." + Subastas.PUBLICACION_ID + " " +

                            "WHERE p." + Publicaciones.ID + " = ? " +
                            "AND p." + Publicaciones.STATUS + " = ?";

            String[] argumentos = {
                    String.valueOf(publicacionId),
                    Publicaciones.ACTIVO
            };

            try (Cursor cursor = db.rawQuery(sql, argumentos)) {

                if (!cursor.moveToFirst()) {
                    throw new IllegalArgumentException(
                            "Subasta no disponible"
                    );
                }

                subastaId = cursor.getLong(0);
                inicio = cursor.getLong(1);
                fin = cursor.getLong(2);
                minimo = cursor.getLong(3);
                vendedor = cursor.getInt(4);
                ofertaActual = cursor.getLong(5);
            }

            long ahora = System.currentTimeMillis();

            if (ahora < inicio || ahora >= fin) {
                throw new IllegalArgumentException(
                        "La subasta no está en curso"
                );
            }

            if (postorId == vendedor) {
                throw new IllegalArgumentException(
                        "No puedes pujar por tu propia publicación"
                );
            }

            if (monto <= ofertaActual || monto < minimo) {
                throw new IllegalArgumentException(
                        "La puja debe superar la mayor oferta " +
                                "y alcanzar el precio inicial"
                );
            }

            // Buscar al anterior postor con la oferta más alta.
            int postorAnterior = -1;

            String consultaAnterior =
                    "SELECT " + Pujas.POSTOR_ID + " " +
                            "FROM " + Pujas.TABLE + " " +
                            "WHERE " + Pujas.SUBASTA_ID + " = ? " +
                            "ORDER BY " + Pujas.MONTO + " DESC, " +
                            Pujas.ID + " DESC LIMIT 1";

            try (Cursor cursor = db.rawQuery(
                    consultaAnterior,
                    new String[]{String.valueOf(subastaId)}
            )) {
                if (cursor.moveToFirst()) {
                    postorAnterior = cursor.getInt(0);
                }
            }

            ContentValues valores = new ContentValues();

            valores.put(Pujas.SUBASTA_ID, subastaId);
            valores.put(Pujas.POSTOR_ID, postorId);
            valores.put(Pujas.MONTO, monto);
            valores.put(Pujas.FECHA, ahora);

            db.insertOrThrow(
                    Pujas.TABLE,
                    null,
                    valores
            );

            if (postorAnterior > 0
                    && postorAnterior != postorId) {

                ContentValues notificacion =
                        new ContentValues();

                notificacion.put(
                        Notificaciones.USUARIO_ID,
                        postorAnterior
                );

                notificacion.put(
                        Notificaciones.MENSAJE,
                        "Tu oferta fue superada en la publicación #"
                                + publicacionId
                );

                notificacion.put(
                        Notificaciones.FECHA,
                        ahora
                );

                db.insertOrThrow(
                        Notificaciones.TABLE,
                        null,
                        notificacion
                );
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    // =====================================================
    // REGISTRAR COMPRA LOCAL
    // =====================================================

    public void comprar(long pubId, int comprador) {

        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            int vendedor;
            double precio;

            String sql =
                    "SELECT " +
                            Publicaciones.VENDEDOR_ID + ", " +
                            Publicaciones.PRICE + " " +
                            "FROM " + Publicaciones.TABLE + " " +
                            "WHERE " + Publicaciones.ID + " = ? " +
                            "AND " + Publicaciones.MODALIDAD + " = ? " +
                            "AND " + Publicaciones.STATUS + " = ?";

            String[] argumentos = {
                    String.valueOf(pubId),
                    Publicaciones.VENTA,
                    Publicaciones.ACTIVO
            };

            try (Cursor cursor = db.rawQuery(sql, argumentos)) {

                if (!cursor.moveToFirst()) {
                    throw new IllegalArgumentException(
                            "La publicación no está disponible para compra"
                    );
                }

                vendedor = cursor.getInt(0);
                precio = cursor.getDouble(1);
            }

            if (comprador == vendedor) {
                throw new IllegalArgumentException(
                        "No puedes comprar tu propio producto"
                );
            }

            long ahora = System.currentTimeMillis();

            ContentValues compra = new ContentValues();

            compra.put(Compras.PUBLICACION_ID, pubId);
            compra.put(Compras.COMPRADOR_ID, comprador);
            compra.put(Compras.VENDEDOR_ID, vendedor);
            compra.put(Compras.PRECIO_ACORDADO, precio);
            compra.put(Compras.FECHA, ahora);

            db.insertOrThrow(
                    Compras.TABLE,
                    null,
                    compra
            );

            ContentValues estado = new ContentValues();

            estado.put(
                    Publicaciones.STATUS,
                    Publicaciones.VENDIDO
            );

            int actualizados = db.update(
                    Publicaciones.TABLE,
                    estado,
                    Publicaciones.ID + " = ? AND " +
                            Publicaciones.STATUS + " = ?",
                    new String[]{
                            String.valueOf(pubId),
                            Publicaciones.ACTIVO
                    }
            );

            if (actualizados != 1) {
                throw new IllegalArgumentException(
                        "La publicación ya no está disponible"
                );
            }

            ContentValues notificacion =
                    new ContentValues();

            notificacion.put(
                    Notificaciones.USUARIO_ID,
                    vendedor
            );

            notificacion.put(
                    Notificaciones.MENSAJE,
                    "Se registró una compra local para " +
                            "la publicación #" + pubId
            );

            notificacion.put(
                    Notificaciones.FECHA,
                    ahora
            );

            db.insertOrThrow(
                    Notificaciones.TABLE,
                    null,
                    notificacion
            );

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    // =====================================================
    // CONSULTAR ESTADO DE SUBASTA
    // =====================================================

    public long[] estadoSubasta(long pubId) {

        String sql =
                "SELECT " +
                        "s." + Subastas.INICIO + ", " +
                        "s." + Subastas.FIN + ", " +
                        "s." + Subastas.MINIMO + ", " +

                        "COALESCE((" +
                        "SELECT MAX(" + Pujas.MONTO + ") " +
                        "FROM " + Pujas.TABLE + " " +
                        "WHERE " + Pujas.SUBASTA_ID +
                        " = s." + Subastas.ID +
                        "), 0) " +

                        "FROM " + Subastas.TABLE + " s " +
                        "WHERE s." + Subastas.PUBLICACION_ID + " = ?";

        SQLiteDatabase db = helper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                sql,
                new String[]{String.valueOf(pubId)}
        )) {

            if (cursor.moveToFirst()) {

                return new long[]{
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getLong(3)
                };
            }
        }

        return null;
    }
}

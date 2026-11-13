package com.example.bidmarket.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.bidmarket.database.BidMarketContract.Cuentas;
import com.example.bidmarket.database.BidMarketContract.Usuarios;
import com.example.bidmarket.database.BidMarketContract.Categorias;
import com.example.bidmarket.database.BidMarketContract.Publicaciones;
import com.example.bidmarket.database.BidMarketContract.Imagenes;
import com.example.bidmarket.database.BidMarketContract.Favoritos;
import com.example.bidmarket.database.BidMarketContract.Subastas;
import com.example.bidmarket.database.BidMarketContract.Pujas;
import com.example.bidmarket.database.BidMarketContract.Compras;
import com.example.bidmarket.database.BidMarketContract.Notificaciones;
import com.example.bidmarket.database.BidMarketContract.Chats;
import com.example.bidmarket.database.BidMarketContract.Mensajes;
import com.example.bidmarket.database.BidMarketContract.AuthIdentities;

/**
 * Administra el esquema SQLite de BidMarket+. Crea las tablas, restricciones
 * e índices, carga las categorías iniciales y aplica las migraciones necesarias.
 * Usa los nombres de tablas y columnas definidos en {@link BidMarketContract}.
 */
public final class BidMarketDbHelper extends SQLiteOpenHelper {

    /**
     * Nombre del archivo de la base de datos local.
     */
    public static final String DATABASE_NAME = "bidmarket.db";
    /**
     * Versión del esquema que usa la aplicación.
     */
    public static final int DATABASE_VERSION = 4;

    /**
     * Prepara el acceso a la base de datos local con su nombre y versión.
     *
     * @param context contexto que Android usa para abrir o crear el archivo SQLite.
     */
    public BidMarketDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    /**
     * Activa las claves foráneas antes de crear, migrar o abrir la base de datos.
     *
     * @param db base de datos que se va a configurar.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // =====================================================
    // CREACIÓN INICIAL
    // =====================================================

    /**
     * Crea las tablas y los índices e inserta las categorías iniciales al abrir
     * la base de datos por primera vez.
     *
     * @param db base de datos recién creada.
     * @throws android.database.sqlite.SQLiteException si SQLite no puede aplicar el esquema o insertar las categorías.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        crearCuentas(db);
        crearUsuarios(db);
        crearCategorias(db);
        crearPublicaciones(db);
        crearImagenes(db);
        crearFavoritos(db);

        crearTablasVentasYSubastas(db);
        crearTablasChat(db);
        crearTablaIdentidades(db);

        crearIndicesPublicaciones(db);
        insertarCategorias(db);
    }

    /**
     * Crea la tabla de cuentas con el estado, el hash de la contraseña y las reglas de
     * unicidad del correo y del nombre de usuario.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearCuentas(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE " + Cuentas.TABLE + " (" +
                        Cuentas.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Cuentas.STATUS + " TEXT NOT NULL DEFAULT '" +
                        Cuentas.ACTIVE + "' CHECK(" +
                        Cuentas.STATUS + " IN ('" +
                        Cuentas.ACTIVE + "','" +
                        Cuentas.SUSPENDED + "','" +
                        Cuentas.DELETED + "')), " +
                        Cuentas.NAME + " TEXT, " +
                        Cuentas.USER_NAME + " TEXT NOT NULL UNIQUE, " +
                        Cuentas.EMAIL + " TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                        Cuentas.PASSWORD_HASH + " TEXT NOT NULL, " +
                        Cuentas.CREATE_AT + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        Cuentas.DELETE_AT + " TEXT" +
                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla de perfiles locales y vincula cada perfil con una cuenta.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearUsuarios(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE " + Usuarios.TABLE + " (" +
                        Usuarios.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Usuarios.CUENTA_ID + " INTEGER NOT NULL UNIQUE " +
                        "REFERENCES " + Cuentas.TABLE +
                        "(" + Cuentas.ID + ") ON DELETE CASCADE, " +

                        Usuarios.PROFILE_PICTURE_URL + " TEXT, " +
                        Usuarios.NAME + " TEXT, " +
                        Usuarios.USER_NAME + " TEXT NOT NULL UNIQUE, " +
                        Usuarios.RATING_SCORE + " REAL" +
                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla de categorías y evita nombres duplicados.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearCategorias(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE " + Categorias.TABLE + " (" +
                        Categorias.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Categorias.NAME + " TEXT NOT NULL UNIQUE" +
                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla del catálogo con su vendedor, categoría, precio, estado, modalidad y
     * datos de ubicación.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearPublicaciones(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE " + Publicaciones.TABLE + " (" +

                        Publicaciones.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Publicaciones.VENDEDOR_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE + "(" + Usuarios.ID + "), " +

                        Publicaciones.CATEGORIA_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Categorias.TABLE + "(" + Categorias.ID + "), " +

                        Publicaciones.TITLE + " TEXT NOT NULL, " +
                        Publicaciones.DESCRIPTION + " TEXT, " +

                        Publicaciones.PRICE +
                        " REAL NOT NULL CHECK(" +
                        Publicaciones.PRICE + " > 0), " +

                        Publicaciones.CURRENCY +
                        " TEXT NOT NULL DEFAULT '" +
                        Publicaciones.CLP + "', " +

                        Publicaciones.CONDITION +
                        " TEXT NOT NULL CHECK(" +
                        Publicaciones.CONDITION + " IN ('" +
                        Publicaciones.NUEVO + "','" +
                        Publicaciones.USADO_COMO_NUEVO + "','" +
                        Publicaciones.BUEN_ESTADO + "','" +
                        Publicaciones.ACEPTABLE + "')), " +

                        Publicaciones.STATUS +
                        " TEXT NOT NULL DEFAULT '" +
                        Publicaciones.ACTIVO + "' CHECK(" +
                        Publicaciones.STATUS + " IN ('" +
                        Publicaciones.ACTIVO + "','" +
                        Publicaciones.VENDIDO + "','" +
                        Publicaciones.PAUSADO + "','" +
                        Publicaciones.BORRADO + "')), " +

                        Publicaciones.LATITUDE + " REAL, " +
                        Publicaciones.LONGITUDE + " REAL, " +

                        Publicaciones.LOCATION_NAME +
                        " TEXT NOT NULL, " +

                        Publicaciones.VIEWS_COUNT +
                        " INTEGER NOT NULL DEFAULT 0, " +

                        Publicaciones.UPDATE_AT + " TEXT, " +

                        Publicaciones.CREATE_AT +
                        " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +

                        Publicaciones.DELETE_AT + " TEXT, " +

                        Publicaciones.MODALIDAD +
                        " TEXT NOT NULL DEFAULT '" +
                        Publicaciones.VENTA + "' CHECK(" +
                        Publicaciones.MODALIDAD + " IN ('" +
                        Publicaciones.VENTA + "','" +
                        Publicaciones.SUBASTA + "'))" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla de imágenes y evita que dos imágenes de una publicación ocupen la
     * misma posición.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearImagenes(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE " + Imagenes.TABLE + " (" +

                        Imagenes.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Imagenes.PUBLICACION_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Publicaciones.TABLE +
                        "(" + Publicaciones.ID + ") ON DELETE CASCADE, " +

                        Imagenes.IMAGE_URL + " TEXT NOT NULL UNIQUE, " +
                        Imagenes.DISPLAY_ORDER + " INTEGER NOT NULL, " +

                        "UNIQUE(" +
                        Imagenes.PUBLICACION_ID + "," +
                        Imagenes.DISPLAY_ORDER + ")" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla que relaciona a cada usuario con sus publicaciones favoritas.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearFavoritos(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE " + Favoritos.TABLE + " (" +

                        Favoritos.USUARIO_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE +
                        "(" + Usuarios.ID + ") ON DELETE CASCADE, " +

                        Favoritos.PUBLICACION_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Publicaciones.TABLE +
                        "(" + Publicaciones.ID + ") ON DELETE CASCADE, " +

                        Favoritos.SAVE_AT +
                        " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +

                        "PRIMARY KEY(" +
                        Favoritos.USUARIO_ID + "," +
                        Favoritos.PUBLICACION_ID + ")" +

                        ")";

        db.execSQL(sql);
    }

    // =====================================================
    // SUBASTAS, PUJAS, COMPRAS Y NOTIFICACIONES
    // =====================================================

    /**
     * Prepara las tablas de subastas, pujas, compras y avisos, junto con el índice de
     * pujas.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearTablasVentasYSubastas(SQLiteDatabase db) {
        crearSubastas(db);
        crearPujas(db);
        crearIndicePujas(db);
        crearCompras(db);
        crearNotificaciones(db);
    }

    /**
     * Crea la tabla que guarda los plazos y el estado de una subasta vinculada a una
     * publicación.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearSubastas(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE IF NOT EXISTS " +
                        Subastas.TABLE + " (" +

                        Subastas.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Subastas.PUBLICACION_ID +
                        " INTEGER NOT NULL UNIQUE REFERENCES " +
                        Publicaciones.TABLE +
                        "(" + Publicaciones.ID + ") ON DELETE CASCADE, " +

                        Subastas.INICIO + " INTEGER NOT NULL, " +
                        Subastas.FIN + " INTEGER NOT NULL, " +

                        Subastas.MINIMO +
                        " INTEGER NOT NULL CHECK(" +
                        Subastas.MINIMO + " > 0), " +

                        Subastas.STATUS +
                        " TEXT NOT NULL DEFAULT '" +
                        Subastas.PROGRAMADA + "' CHECK(" +
                        Subastas.STATUS + " IN ('" +
                        Subastas.PROGRAMADA + "','" +
                        Subastas.ACTIVA + "','" +
                        Subastas.FINALIZADA + "','" +
                        Subastas.CANCELADA + "')), " +

                        "CHECK(" +
                        Subastas.FIN + " > " + Subastas.INICIO + ")" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla que guarda el usuario, el monto y la fecha de cada puja.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearPujas(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE IF NOT EXISTS " +
                        Pujas.TABLE + " (" +

                        Pujas.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Pujas.SUBASTA_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Subastas.TABLE +
                        "(" + Subastas.ID + ") ON DELETE CASCADE, " +

                        Pujas.POSTOR_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE + "(" + Usuarios.ID + "), " +

                        Pujas.MONTO +
                        " INTEGER NOT NULL CHECK(" +
                        Pujas.MONTO + " > 0), " +

                        Pujas.FECHA + " INTEGER NOT NULL" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea un índice por subasta, monto descendente e ID descendente para consultar las
     * pujas de mayor monto.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearIndicePujas(SQLiteDatabase db) {

        String sql =
                "CREATE INDEX IF NOT EXISTS " +
                        Pujas.IDX_SUBASTA +
                        " ON " + Pujas.TABLE + "(" +
                        Pujas.SUBASTA_ID + "," +
                        Pujas.MONTO + " DESC," +
                        Pujas.ID + " DESC)";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla de compras para registrar la publicación, el comprador y los datos de
     * la operación.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearCompras(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE IF NOT EXISTS " +
                        Compras.TABLE + " (" +

                        Compras.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Compras.PUBLICACION_ID +
                        " INTEGER NOT NULL UNIQUE REFERENCES " +
                        Publicaciones.TABLE +
                        "(" + Publicaciones.ID + "), " +

                        Compras.COMPRADOR_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE + "(" + Usuarios.ID + "), " +

                        Compras.VENDEDOR_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE + "(" + Usuarios.ID + "), " +

                        Compras.PRECIO_ACORDADO +
                        " REAL NOT NULL CHECK(" +
                        Compras.PRECIO_ACORDADO + " > 0), " +

                        Compras.FECHA + " INTEGER NOT NULL, " +

                        Compras.STATUS +
                        " TEXT NOT NULL DEFAULT '" +
                        Compras.CONFIRMADA + "' CHECK(" +
                        Compras.STATUS + " IN ('" +
                        Compras.CONFIRMADA + "','" +
                        Compras.CANCELADA + "'))" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla de avisos locales y su estado de lectura.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearNotificaciones(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE IF NOT EXISTS " +
                        Notificaciones.TABLE + " (" +

                        Notificaciones.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Notificaciones.USUARIO_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE +
                        "(" + Usuarios.ID + ") ON DELETE CASCADE, " +

                        Notificaciones.MENSAJE + " TEXT NOT NULL, " +

                        Notificaciones.LEIDA +
                        " INTEGER NOT NULL DEFAULT " +
                        Notificaciones.NO_LEIDA + " CHECK(" +
                        Notificaciones.LEIDA + " IN (" +
                        Notificaciones.NO_LEIDA + "," +
                        Notificaciones.LEIDA_SI + ")), " +

                        Notificaciones.FECHA + " INTEGER NOT NULL" +

                        ")";

        db.execSQL(sql);
    }

    // =====================================================
    // CHATS Y MENSAJES
    // =====================================================

    /**
     * Prepara las tablas de chats y mensajes junto con sus índices.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearTablasChat(SQLiteDatabase db) {
        crearChats(db);
        crearMensajes(db);
        crearIndiceMensajes(db);
        crearIndicesChats(db);
    }

    /**
     * Crea la tabla de conversaciones entre comprador y vendedor asociadas a una
     * publicación.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearChats(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE IF NOT EXISTS " +
                        Chats.TABLE + " (" +

                        Chats.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Chats.PUBLICACION_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Publicaciones.TABLE +
                        "(" + Publicaciones.ID + "), " +

                        Chats.COMPRADOR_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE + "(" + Usuarios.ID + "), " +

                        Chats.VENDEDOR_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE + "(" + Usuarios.ID + "), " +

                        Chats.CREADO + " INTEGER NOT NULL, " +

                        "UNIQUE(" +
                        Chats.PUBLICACION_ID + "," +
                        Chats.COMPRADOR_ID + "), " +

                        "CHECK(" +
                        Chats.COMPRADOR_ID + " <> " +
                        Chats.VENDEDOR_ID + ")" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla que guarda el contenido y el estado de lectura de los mensajes de cada
     * chat.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearMensajes(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE IF NOT EXISTS " +
                        Mensajes.TABLE + " (" +

                        Mensajes.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        Mensajes.CHAT_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Chats.TABLE +
                        "(" + Chats.ID + ") ON DELETE CASCADE, " +

                        Mensajes.REMITENTE_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE + "(" + Usuarios.ID + "), " +

                        Mensajes.TEXTO +
                        " TEXT NOT NULL CHECK(length(trim(" +
                        Mensajes.TEXTO + ")) > 0), " +

                        Mensajes.ENVIADO + " INTEGER NOT NULL, " +

                        Mensajes.LEIDO +
                        " INTEGER NOT NULL DEFAULT " +
                        Mensajes.NO_LEIDO + " CHECK(" +
                        Mensajes.LEIDO + " IN (" +
                        Mensajes.NO_LEIDO + "," +
                        Mensajes.LEIDO_SI + "))" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea la tabla que vincula a un usuario local con el identificador de su proveedor de
     * acceso.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearTablaIdentidades(SQLiteDatabase db) {

        String sql =
                "CREATE TABLE IF NOT EXISTS " +
                        AuthIdentities.TABLE + " (" +

                        AuthIdentities.ID +
                        " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                        AuthIdentities.USUARIO_ID +
                        " INTEGER NOT NULL REFERENCES " +
                        Usuarios.TABLE +
                        "(" + Usuarios.ID + ") ON DELETE CASCADE, " +

                        AuthIdentities.PROVIDER +
                        " TEXT NOT NULL, " +

                        AuthIdentities.PROVIDER_UID +
                        " TEXT NOT NULL, " +

                        "UNIQUE(" +
                        AuthIdentities.PROVIDER + ", " +
                        AuthIdentities.PROVIDER_UID + "), " +

                        "UNIQUE(" +
                        AuthIdentities.USUARIO_ID + ", " +
                        AuthIdentities.PROVIDER + ")" +

                        ")";

        db.execSQL(sql);
    }

    /**
     * Crea un índice por chat e ID para consultar los mensajes de una conversación.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearIndiceMensajes(SQLiteDatabase db) {

        String sql =
                "CREATE INDEX IF NOT EXISTS " +
                        Mensajes.IDX_CHAT +
                        " ON " + Mensajes.TABLE + "(" +
                        Mensajes.CHAT_ID + "," +
                        Mensajes.ID + ")";

        db.execSQL(sql);
    }

    /**
     * Crea índices para buscar conversaciones por comprador o por vendedor.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearIndicesChats(SQLiteDatabase db) {

        String indiceComprador =
                "CREATE INDEX IF NOT EXISTS " +
                        Chats.IDX_COMPRADOR +
                        " ON " + Chats.TABLE + "(" +
                        Chats.COMPRADOR_ID + ")";

        String indiceVendedor =
                "CREATE INDEX IF NOT EXISTS " +
                        Chats.IDX_VENDEDOR +
                        " ON " + Chats.TABLE + "(" +
                        Chats.VENDEDOR_ID + ")";

        db.execSQL(indiceComprador);
        db.execSQL(indiceVendedor);
    }

    /**
     * Crea índices para consultar publicaciones por categoría o por vendedor.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void crearIndicesPublicaciones(SQLiteDatabase db) {

        String indiceCategoria =
                "CREATE INDEX " +
                        Publicaciones.IDX_CATEGORIA +
                        " ON " + Publicaciones.TABLE + "(" +
                        Publicaciones.CATEGORIA_ID + ")";

        String indiceVendedor =
                "CREATE INDEX " +
                        Publicaciones.IDX_VENDEDOR +
                        " ON " + Publicaciones.TABLE + "(" +
                        Publicaciones.VENDEDOR_ID + ")";

        db.execSQL(indiceCategoria);
        db.execSQL(indiceVendedor);
    }

    // =====================================================
    // CATEGORÍAS INICIALES
    // =====================================================

    /**
     * Carga las categorías iniciales sin duplicar las que ya existen.
     *
     * @param db base de datos donde se aplica el cambio.
     */
    private void insertarCategorias(SQLiteDatabase db) {

        String[] nombres = {
                "Electrónica",
                "Hogar",
                "Vehículos",
                "Ropa y Accesorios",
                "Deportes",
                "Libros",
                "Juguetes",
                "Otros"
        };

        String sql =
                "INSERT OR IGNORE INTO " +
                        Categorias.TABLE +
                        "(" + Categorias.NAME + ") VALUES(?)";

        for (String nombre : nombres) {
            db.execSQL(sql, new Object[]{nombre});
        }
    }

    // =====================================================
    // MIGRACIONES
    // =====================================================

    /**
     * Aplica los cambios pendientes según la versión instalada, sin recrear toda
     * la base de datos. La versión 2 incorpora la modalidad de venta, las subastas,
     * las pujas, las compras y los avisos. La versión 3 agrega chats y mensajes.
     * La versión 4 agrega las identidades de los proveedores de acceso.
     *
     * @param db base de datos existente que se va a actualizar.
     * @param oldVersion versión del esquema instalada antes de la actualización.
     * @param newVersion versión de destino solicitada por Android.
     * @throws android.database.sqlite.SQLiteException si falla alguna operación de la migración.
     */
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {

        if (oldVersion < 2) {

            String sql =
                    "ALTER TABLE " + Publicaciones.TABLE +
                            " ADD COLUMN " + Publicaciones.MODALIDAD +
                            " TEXT NOT NULL DEFAULT '" +
                            Publicaciones.VENTA + "' CHECK(" +
                            Publicaciones.MODALIDAD + " IN ('" +
                            Publicaciones.VENTA + "','" +
                            Publicaciones.SUBASTA + "'))";

            db.execSQL(sql);

            crearTablasVentasYSubastas(db);
            insertarCategorias(db);
        }
        if (oldVersion < 3) {
            crearTablasChat(db);
        }

        if (oldVersion < 4) {
            crearTablaIdentidades(db);
        }
    }

    // =====================================================
    // UTILIDAD DE INSPECCIÓN DEL ESQUEMA
    // =====================================================

    /**
     * Consulta el esquema de una tabla para comprobar si contiene una columna
     * con el nombre exacto indicado.
     *
     * @param db base de datos que se quiere consultar.
     * @param tabla nombre de la tabla.
     * @param columna nombre de la columna, respetando mayúsculas y minúsculas.
     * @return {@code true} si la columna existe, o {@code false} si no se encuentra.
     * @throws IllegalArgumentException si un nombre es nulo o no cumple el
     *         formato admitido. Debe empezar con una letra de A a Z o guion bajo
     *         y después solo puede contener letras de A a Z, dígitos o guiones bajos.
     * @throws android.database.sqlite.SQLiteException si falla la consulta del esquema.
     */
    public boolean existeColumna(
            SQLiteDatabase db,
            String tabla,
            String columna
    ) {

        if (tabla == null
                || columna == null
                || !tabla.matches("[A-Za-z_][A-Za-z0-9_]*")
                || !columna.matches("[A-Za-z_][A-Za-z0-9_]*")) {

            throw new IllegalArgumentException(
                    "Identificador de tabla o columna inválido"
            );
        }

        String sql = "PRAGMA table_info(" + tabla + ")";

        try (Cursor cursor = db.rawQuery(sql, null)) {

            int indiceNombre = cursor.getColumnIndexOrThrow(
                    "name"
            );

            while (cursor.moveToNext()) {

                if (columna.equals(
                        cursor.getString(indiceNombre)
                )) {
                    return true;
                }
            }
        }

        return false;
    }
}

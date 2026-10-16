package com.example.bidmarket;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BidMarketDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "bidmarket.db";
    public static final int DATABASE_VERSION = 2;

    public BidMarketDbHelper(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cuentas (id INTEGER PRIMARY KEY AUTOINCREMENT, status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','SUSPENDED','DELETED')), name TEXT, user_name TEXT NOT NULL UNIQUE, email TEXT NOT NULL COLLATE NOCASE UNIQUE, password_hash TEXT NOT NULL, create_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, delete_at TEXT)");
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, cuentas_id INTEGER NOT NULL UNIQUE REFERENCES cuentas(id) ON DELETE CASCADE, profile_picture_url TEXT, name TEXT, user_name TEXT NOT NULL UNIQUE, rating_score REAL)");
        db.execSQL("CREATE TABLE categorias (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE TABLE publicaciones (id INTEGER PRIMARY KEY AUTOINCREMENT, vendedor_id INTEGER NOT NULL REFERENCES usuarios(id), categoria_id INTEGER NOT NULL REFERENCES categorias(id), title TEXT NOT NULL, description TEXT, price REAL NOT NULL CHECK(price > 0), currency TEXT NOT NULL DEFAULT 'CLP', condition TEXT NOT NULL CHECK(condition IN ('Nuevo','Usado como nuevo','Buen Estado','Aceptable')), status TEXT NOT NULL DEFAULT 'Activo' CHECK(status IN ('Activo','Vendido','Pausado','Borrado')), latitude REAL, longitude REAL, location_name TEXT NOT NULL, views_count INTEGER NOT NULL DEFAULT 0, update_at TEXT, create_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, delete_at TEXT, modalidad TEXT NOT NULL DEFAULT 'VENTA' CHECK(modalidad IN ('VENTA','SUBASTA')))");
        db.execSQL("CREATE TABLE imagenes (id INTEGER PRIMARY KEY AUTOINCREMENT, publicacion_id INTEGER NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE, image_url TEXT NOT NULL UNIQUE, display_order INTEGER NOT NULL, UNIQUE(publicacion_id,display_order))");
        db.execSQL("CREATE TABLE favoritos (usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, publicacion_id INTEGER NOT NULL REFERENCES publicaciones(id) ON DELETE CASCADE, save_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(usuario_id,publicacion_id))");
        createNewTables(db);
        db.execSQL("CREATE INDEX idx_publicaciones_categoria ON publicaciones(categoria_id)");
        db.execSQL("CREATE INDEX idx_publicaciones_vendedor ON publicaciones(vendedor_id)");
        insertCategorias(db);
    }

    private void createNewTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS subastas (id INTEGER PRIMARY KEY AUTOINCREMENT, publicacion_id INTEGER NOT NULL UNIQUE REFERENCES publicaciones(id) ON DELETE CASCADE, inicio_ms INTEGER NOT NULL, fin_ms INTEGER NOT NULL, precio_inicial INTEGER NOT NULL CHECK(precio_inicial > 0), status TEXT NOT NULL DEFAULT 'PROGRAMADA' CHECK(status IN ('PROGRAMADA','ACTIVA','FINALIZADA','CANCELADA')), CHECK(fin_ms > inicio_ms))");
        db.execSQL("CREATE TABLE IF NOT EXISTS pujas (id INTEGER PRIMARY KEY AUTOINCREMENT, subasta_id INTEGER NOT NULL REFERENCES subastas(id) ON DELETE CASCADE, postor_id INTEGER NOT NULL REFERENCES usuarios(id), monto INTEGER NOT NULL CHECK(monto > 0), fecha_ms INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pujas_subasta ON pujas(subasta_id,monto DESC,id DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS compras (id INTEGER PRIMARY KEY AUTOINCREMENT, publicacion_id INTEGER NOT NULL UNIQUE REFERENCES publicaciones(id), comprador_id INTEGER NOT NULL REFERENCES usuarios(id), vendedor_id INTEGER NOT NULL REFERENCES usuarios(id), precio_acordado REAL NOT NULL CHECK(precio_acordado > 0), fecha_ms INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'CONFIRMADA' CHECK(status IN ('CONFIRMADA','CANCELADA'))) ");
        db.execSQL("CREATE TABLE IF NOT EXISTS notificaciones (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, mensaje TEXT NOT NULL, leida INTEGER NOT NULL DEFAULT 0 CHECK(leida IN (0,1)), fecha_ms INTEGER NOT NULL)");
    }

    private void insertCategorias(SQLiteDatabase db) {
        String[] nombres = {"Electrónica","Hogar","Vehículos","Ropa y Accesorios","Deportes","Libros","Juguetes","Otros"};
        for (String n : nombres) db.execSQL("INSERT OR IGNORE INTO categorias(name) VALUES(?)",new Object[]{n});
    }

    /** Migra instalaciones de la versión original conservando cuentas, publicaciones e imágenes. */
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE publicaciones ADD COLUMN modalidad TEXT NOT NULL DEFAULT 'VENTA' CHECK(modalidad IN ('VENTA','SUBASTA'))");
            createNewTables(db);
            insertCategorias(db);
        }
    }

    public boolean existeColumna(SQLiteDatabase db, String tabla, String columna) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + tabla + ")", null)) {
            while (c.moveToNext()) if (columna.equals(c.getString(1))) return true;
        }
        return false;
    }
}

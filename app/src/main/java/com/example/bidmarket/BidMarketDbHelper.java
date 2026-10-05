package com.example.bidmarket;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BidMarketDbHelper extends SQLiteOpenHelper {
    // Nombre y versión de la base de datos
    private static final String DATABASE_NAME = "bidmarket.db";
    private static final int DATABASE_VERSION = 1;

    public BidMarketDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Habilitar las claves foráneas como indica el esquema
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Crear tabla cuentas
        db.execSQL("CREATE TABLE cuentas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','DELETED')), " +
                "name TEXT, " +
                "user_name TEXT NOT NULL UNIQUE, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "create_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "delete_at TEXT" +
                ");");

        // 2. Crear tabla usuarios
        db.execSQL("CREATE TABLE usuarios (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "cuentas_id INTEGER NOT NULL UNIQUE, " +
                "profile_picture_url TEXT, " +
                "name TEXT, " +
                "user_name TEXT NOT NULL UNIQUE, " +
                "rating_score REAL, " +
                "FOREIGN KEY (cuentas_id) REFERENCES cuentas(id)" +
                ");");

        // 3. Crear tabla categorias
        db.execSQL("CREATE TABLE categorias (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE" +
                ");");

        // 4. Crear tabla publicaciones
        db.execSQL("CREATE TABLE publicaciones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "vendedor_id INTEGER NOT NULL, " +
                "categoria_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "price REAL NOT NULL, " +
                "currency TEXT NOT NULL DEFAULT 'EUR', " +
                "condition TEXT NOT NULL CHECK (condition IN ('NEW','USED','NOT_APPLICABLE')), " +
                "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PAUSED','SOLD','DELETED')), " +
                "latitude REAL, " +
                "longitude REAL, " +
                "location_name TEXT NOT NULL, " +
                "views_count INTEGER NOT NULL DEFAULT 0, " +
                "update_at TEXT, " +
                "create_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "delete_at TEXT, " +
                "FOREIGN KEY (vendedor_id) REFERENCES usuarios(id), " +
                "FOREIGN KEY (categoria_id) REFERENCES categorias(id)" +
                ");");

        // 5. Crear tabla imagenes
        db.execSQL("CREATE TABLE imagenes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "publicacion_id INTEGER NOT NULL, " +
                "image_url TEXT NOT NULL UNIQUE, " +
                "display_order INTEGER NOT NULL, " +
                "UNIQUE (publicacion_id, display_order), " +
                "FOREIGN KEY (publicacion_id) REFERENCES publicaciones(id) ON DELETE CASCADE" +
                ");");

        // 6. Crear tabla favoritos
        db.execSQL("CREATE TABLE favoritos (" +
                "usuario_id INTEGER NOT NULL, " +
                "publicacion_id INTEGER NOT NULL, " +
                "save_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (usuario_id, publicacion_id), " +
                "FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (publicacion_id) REFERENCES publicaciones(id) ON DELETE CASCADE" +
                ");");

        // 7. Crear índices
        db.execSQL("CREATE INDEX idx_publicaciones_categoria ON publicaciones(categoria_id);");
        db.execSQL("CREATE INDEX idx_publicaciones_vendedor ON publicaciones(vendedor_id);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Lógica para actualizar la base de datos (por ahora eliminamos y recreamos)
        db.execSQL("DROP TABLE IF EXISTS favoritos");
        db.execSQL("DROP TABLE IF EXISTS imagenes");
        db.execSQL("DROP TABLE IF EXISTS publicaciones");
        db.execSQL("DROP TABLE IF EXISTS categorias");
        db.execSQL("DROP TABLE IF EXISTS usuarios");
        db.execSQL("DROP TABLE IF EXISTS cuentas");
        onCreate(db);
    }
}

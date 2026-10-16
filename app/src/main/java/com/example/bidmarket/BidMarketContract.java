package com.example.bidmarket;

import android.provider.BaseColumns;

/** Nombres centralizados del esquema SQLite. */
public final class BidMarketContract {
    private BidMarketContract() {}
    public static final class Cuentas implements BaseColumns {
        public static final String TABLE = "cuentas";
        public static final String EMAIL = "email";
        public static final String STATUS = "status";
    }
    public static final class Usuarios implements BaseColumns {
        public static final String TABLE = "usuarios";
        public static final String CUENTA_ID = "cuentas_id";
    }
    public static final class Categorias implements BaseColumns {
        public static final String TABLE = "categorias";
        public static final String NAME = "name";
    }
    public static final class Publicaciones implements BaseColumns {
        public static final String TABLE = "publicaciones";
        public static final String VENDEDOR_ID = "vendedor_id";
        public static final String CATEGORIA_ID = "categoria_id";
        public static final String TITLE = "title";
        public static final String PRICE = "price";
        public static final String STATUS = "status";
        public static final String MODALIDAD = "modalidad";
    }
    public static final class Imagenes implements BaseColumns {
        public static final String TABLE = "imagenes";
        public static final String PUBLICACION_ID = "publicacion_id";
    }
    public static final class Subastas implements BaseColumns {
        public static final String TABLE = "subastas";
        public static final String PUBLICACION_ID = "publicacion_id";
        public static final String INICIO = "inicio_ms";
        public static final String FIN = "fin_ms";
        public static final String MINIMO = "precio_inicial";
        public static final String STATUS = "status";
    }
    public static final class Pujas implements BaseColumns {
        public static final String TABLE = "pujas";
        public static final String SUBASTA_ID = "subasta_id";
        public static final String POSTOR_ID = "postor_id";
        public static final String MONTO = "monto";
    }
    public static final class Favoritos implements BaseColumns {
        public static final String TABLE = "favoritos";
    }
    public static final class Notificaciones implements BaseColumns {
        public static final String TABLE = "notificaciones";
    }
}


package com.example.bidmarket.database;

import android.provider.BaseColumns;

/**
 * Contrato de la base de datos SQLite de BidMarket+.
 *
 * Centraliza los nombres de tablas, columnas, índices
 * y valores compartidos del esquema.
 *
 * No contiene consultas SQL ni operaciones CRUD.
 *
 * Esquema compatible con bidmarket.db, versión 3.
 */
public final class BidMarketContract {

    private BidMarketContract() {
        throw new AssertionError("Clase no instanciable");
    }

    // =====================================================
    // CUENTAS
    // =====================================================

    public static final class Cuentas implements BaseColumns {

        private Cuentas() {}

        public static final String TABLE = "cuentas";

        public static final String ID = "id";
        public static final String STATUS = "status";
        public static final String NAME = "name";
        public static final String USER_NAME = "user_name";
        public static final String EMAIL = "email";
        public static final String PASSWORD_HASH = "password_hash";
        public static final String CREATE_AT = "create_at";
        public static final String DELETE_AT = "delete_at";

        // Estados admitidos
        public static final String ACTIVE = "ACTIVE";
        public static final String SUSPENDED = "SUSPENDED";
        public static final String DELETED = "DELETED";
    }

    // =====================================================
    // USUARIOS
    // =====================================================

    public static final class Usuarios implements BaseColumns {

        private Usuarios() {}

        public static final String TABLE = "usuarios";

        public static final String ID = "id";
        public static final String CUENTA_ID = "cuentas_id";
        public static final String PROFILE_PICTURE_URL =
                "profile_picture_url";
        public static final String NAME = "name";
        public static final String USER_NAME = "user_name";
        public static final String RATING_SCORE = "rating_score";
    }

    // =====================================================
    // CATEGORÍAS
    // =====================================================

    public static final class Categorias implements BaseColumns {

        private Categorias() {}

        public static final String TABLE = "categorias";

        public static final String ID = "id";
        public static final String NAME = "name";
    }

    // =====================================================
    // PUBLICACIONES
    // =====================================================

    public static final class Publicaciones implements BaseColumns {

        private Publicaciones() {}

        public static final String TABLE = "publicaciones";

        public static final String ID = "id";
        public static final String VENDEDOR_ID = "vendedor_id";
        public static final String CATEGORIA_ID = "categoria_id";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String PRICE = "price";
        public static final String CURRENCY = "currency";
        public static final String CONDITION = "condition";
        public static final String STATUS = "status";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String LOCATION_NAME = "location_name";
        public static final String VIEWS_COUNT = "views_count";
        public static final String UPDATE_AT = "update_at";
        public static final String CREATE_AT = "create_at";
        public static final String DELETE_AT = "delete_at";
        public static final String MODALIDAD = "modalidad";

        // Índices existentes
        public static final String IDX_CATEGORIA =
                "idx_publicaciones_categoria";
        public static final String IDX_VENDEDOR =
                "idx_publicaciones_vendedor";

        // Estados
        public static final String ACTIVO = "Activo";
        public static final String VENDIDO = "Vendido";
        public static final String PAUSADO = "Pausado";
        public static final String BORRADO = "Borrado";

        // Modalidades
        public static final String VENTA = "VENTA";
        public static final String SUBASTA = "SUBASTA";

        // Condiciones
        public static final String NUEVO = "Nuevo";
        public static final String USADO_COMO_NUEVO =
                "Usado como nuevo";
        public static final String BUEN_ESTADO = "Buen Estado";
        public static final String ACEPTABLE = "Aceptable";

        // Moneda predeterminada
        public static final String CLP = "CLP";
    }

    // =====================================================
    // IMÁGENES
    // =====================================================

    public static final class Imagenes implements BaseColumns {

        private Imagenes() {}

        public static final String TABLE = "imagenes";

        public static final String ID = "id";
        public static final String PUBLICACION_ID = "publicacion_id";
        public static final String IMAGE_URL = "image_url";
        public static final String DISPLAY_ORDER = "display_order";

        // Reglas de la galería
        public static final int MIN_IMAGENES = 1;
        public static final int MAX_IMAGENES = 10;
        public static final int ORDEN_PORTADA = 1;
    }

    // =====================================================
    // FAVORITOS
    // Clave primaria compuesta:
    // usuario_id + publicacion_id
    // =====================================================

    public static final class Favoritos {

        private Favoritos() {}

        public static final String TABLE = "favoritos";

        public static final String USUARIO_ID = "usuario_id";
        public static final String PUBLICACION_ID = "publicacion_id";
        public static final String SAVE_AT = "save_at";
    }

    // =====================================================
    // SUBASTAS
    // =====================================================

    public static final class Subastas implements BaseColumns {

        private Subastas() {}

        public static final String TABLE = "subastas";

        public static final String ID = "id";
        public static final String PUBLICACION_ID = "publicacion_id";
        public static final String INICIO = "inicio_ms";
        public static final String FIN = "fin_ms";
        public static final String MINIMO = "precio_inicial";
        public static final String STATUS = "status";

        // Estados
        public static final String PROGRAMADA = "PROGRAMADA";
        public static final String ACTIVA = "ACTIVA";
        public static final String FINALIZADA = "FINALIZADA";
        public static final String CANCELADA = "CANCELADA";
    }

    // =====================================================
    // PUJAS
    // =====================================================

    public static final class Pujas implements BaseColumns {

        private Pujas() {}

        public static final String TABLE = "pujas";

        public static final String ID = "id";
        public static final String SUBASTA_ID = "subasta_id";
        public static final String POSTOR_ID = "postor_id";
        public static final String MONTO = "monto";
        public static final String FECHA = "fecha_ms";

        public static final String IDX_SUBASTA =
                "idx_pujas_subasta";
    }

    // =====================================================
    // COMPRAS
    // =====================================================

    public static final class Compras implements BaseColumns {

        private Compras() {}

        public static final String TABLE = "compras";

        public static final String ID = "id";
        public static final String PUBLICACION_ID = "publicacion_id";
        public static final String COMPRADOR_ID = "comprador_id";
        public static final String VENDEDOR_ID = "vendedor_id";
        public static final String PRECIO_ACORDADO = "precio_acordado";
        public static final String FECHA = "fecha_ms";
        public static final String STATUS = "status";

        // Estados
        public static final String CONFIRMADA = "CONFIRMADA";
        public static final String CANCELADA = "CANCELADA";
    }

    // =====================================================
    // NOTIFICACIONES
    // =====================================================

    public static final class Notificaciones implements BaseColumns {

        private Notificaciones() {}

        public static final String TABLE = "notificaciones";

        public static final String ID = "id";
        public static final String USUARIO_ID = "usuario_id";
        public static final String MENSAJE = "mensaje";
        public static final String LEIDA = "leida";
        public static final String FECHA = "fecha_ms";

        // Valores booleanos almacenados como INTEGER
        public static final int NO_LEIDA = 0;
        public static final int LEIDA_SI = 1;
    }

    // =====================================================
    // CHATS
    // =====================================================

    public static final class Chats implements BaseColumns {

        private Chats() {}

        public static final String TABLE = "chats";

        public static final String ID = "id";
        public static final String PUBLICACION_ID = "publicacion_id";
        public static final String COMPRADOR_ID = "comprador_id";
        public static final String VENDEDOR_ID = "vendedor_id";
        public static final String CREADO = "creado_ms";

        // Índices existentes
        public static final String IDX_COMPRADOR =
                "idx_chats_comprador";
        public static final String IDX_VENDEDOR =
                "idx_chats_vendedor";
    }

    // =====================================================
    // MENSAJES
    // =====================================================

    public static final class Mensajes implements BaseColumns {

        private Mensajes() {}

        public static final String TABLE = "mensajes";

        public static final String ID = "id";
        public static final String CHAT_ID = "chat_id";
        public static final String REMITENTE_ID = "remitente_id";
        public static final String TEXTO = "texto";
        public static final String ENVIADO = "enviado_ms";
        public static final String LEIDO = "leido";

        public static final String IDX_CHAT =
                "idx_mensajes_chat";

        // Valores booleanos almacenados como INTEGER
        public static final int NO_LEIDO = 0;
        public static final int LEIDO_SI = 1;
    }

    // =====================================================
    // AUTHIDENTITIES
    // =====================================================

    public static final class AuthIdentities {

        private AuthIdentities() {}

        public static final String TABLE = "auth_identities";

        public static final String ID = "id";
        public static final String USUARIO_ID = "usuario_id";
        public static final String PROVIDER = "provider";
        public static final String PROVIDER_UID = "provider_uid";

        public static final String GOOGLE = "GOOGLE";
    }
}

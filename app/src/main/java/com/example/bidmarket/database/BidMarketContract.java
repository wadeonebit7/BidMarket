
package com.example.bidmarket.database;

import android.provider.BaseColumns;

/**
 * Reúne los nombres de tablas, columnas e índices de la base de datos SQLite
 * de BidMarket+, junto con los valores compartidos del esquema.
 * No ejecuta consultas ni modifica datos.
 * La creación de tablas y las migraciones se manejan en {@link BidMarketDbHelper}.
 */
public final class BidMarketContract {

    /**
     * Impide crear una instancia del contrato.
     *
     * @throws AssertionError si se intenta ejecutar el constructor.
     */
    private BidMarketContract() {
        throw new AssertionError("Clase no instanciable");
    }

    // =====================================================
    // CUENTAS
    // =====================================================

    /**
     * Columnas y estados de las cuentas de acceso.
     */
    public static final class Cuentas implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Cuentas() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "cuentas";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el estado del registro.
         */
        public static final String STATUS = "status";
        /**
         * Columna con el nombre.
         */
        public static final String NAME = "name";
        /**
         * Columna con el nombre de usuario.
         */
        public static final String USER_NAME = "user_name";
        /**
         * Columna con el correo electrónico de la cuenta.
         */
        public static final String EMAIL = "email";
        /**
         * Columna con el hash de la contraseña.
         */
        public static final String PASSWORD_HASH = "password_hash";
        /**
         * Columna con la fecha de creación.
         */
        public static final String CREATE_AT = "create_at";
        /**
         * Columna con la fecha de eliminación.
         */
        public static final String DELETE_AT = "delete_at";

        // Estados admitidos
        /**
         * Estado de una cuenta activa.
         */
        public static final String ACTIVE = "ACTIVE";
        /**
         * Estado de una cuenta suspendida.
         */
        public static final String SUSPENDED = "SUSPENDED";
        /**
         * Estado de una cuenta eliminada.
         */
        public static final String DELETED = "DELETED";
    }

    // =====================================================
    // USUARIOS
    // =====================================================

    /**
     * Columnas de los perfiles locales vinculados a una cuenta.
     */
    public static final class Usuarios implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Usuarios() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "usuarios";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID de la cuenta vinculada al perfil.
         */
        public static final String CUENTA_ID = "cuentas_id";
        /**
         * Columna con la ruta o URL de la foto de perfil.
         */
        public static final String PROFILE_PICTURE_URL =
                "profile_picture_url";
        /**
         * Columna con el nombre.
         */
        public static final String NAME = "name";
        /**
         * Columna con el nombre de usuario.
         */
        public static final String USER_NAME = "user_name";
        /**
         * Columna con la puntuación del perfil.
         */
        public static final String RATING_SCORE = "rating_score";
    }

    // =====================================================
    // CATEGORÍAS
    // =====================================================

    /**
     * Columnas de las categorías del catálogo.
     */
    public static final class Categorias implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Categorias() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "categorias";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el nombre.
         */
        public static final String NAME = "name";
    }

    // =====================================================
    // PUBLICACIONES
    // =====================================================

    /**
     * Columnas, índices y valores admitidos para las publicaciones del catálogo.
     */
    public static final class Publicaciones implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Publicaciones() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "publicaciones";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID del vendedor.
         */
        public static final String VENDEDOR_ID = "vendedor_id";
        /**
         * Columna con el ID de la categoría.
         */
        public static final String CATEGORIA_ID = "categoria_id";
        /**
         * Columna con el título de la publicación.
         */
        public static final String TITLE = "title";
        /**
         * Columna con la descripción de la publicación.
         */
        public static final String DESCRIPTION = "description";
        /**
         * Columna con el precio publicado.
         */
        public static final String PRICE = "price";
        /**
         * Columna con el código de la moneda.
         */
        public static final String CURRENCY = "currency";
        /**
         * Columna con la condición del producto.
         */
        public static final String CONDITION = "condition";
        /**
         * Columna con el estado del registro.
         */
        public static final String STATUS = "status";
        /**
         * Columna con la latitud de la ubicación.
         */
        public static final String LATITUDE = "latitude";
        /**
         * Columna con la longitud de la ubicación.
         */
        public static final String LONGITUDE = "longitude";
        /**
         * Columna con el nombre de la ubicación.
         */
        public static final String LOCATION_NAME = "location_name";
        /**
         * Columna con el número de visualizaciones.
         */
        public static final String VIEWS_COUNT = "views_count";
        /**
         * Columna con la fecha de actualización.
         */
        public static final String UPDATE_AT = "update_at";
        /**
         * Columna con la fecha de creación.
         */
        public static final String CREATE_AT = "create_at";
        /**
         * Columna con la fecha de eliminación.
         */
        public static final String DELETE_AT = "delete_at";
        /**
         * Columna que distingue una venta directa de una subasta.
         */
        public static final String MODALIDAD = "modalidad";

        // Índices existentes
        /**
         * Nombre del índice de publicaciones por categoría.
         */
        public static final String IDX_CATEGORIA =
                "idx_publicaciones_categoria";
        /**
         * Nombre del índice por vendedor.
         */
        public static final String IDX_VENDEDOR =
                "idx_publicaciones_vendedor";

        // Estados
        /**
         * Estado de una publicación activa.
         */
        public static final String ACTIVO = "Activo";
        /**
         * Estado de una publicación vendida.
         */
        public static final String VENDIDO = "Vendido";
        /**
         * Estado de una publicación pausada.
         */
        public static final String PAUSADO = "Pausado";
        /**
         * Estado de una publicación borrada.
         */
        public static final String BORRADO = "Borrado";

        // Modalidades
        /**
         * Modalidad de venta directa.
         */
        public static final String VENTA = "VENTA";
        /**
         * Modalidad de venta por subasta.
         */
        public static final String SUBASTA = "SUBASTA";

        // Condiciones
        /**
         * Condición de un producto nuevo.
         */
        public static final String NUEVO = "Nuevo";
        /**
         * Condición de un producto usado que está como nuevo.
         */
        public static final String USADO_COMO_NUEVO =
                "Usado como nuevo";
        /**
         * Condición de un producto usado en buen estado.
         */
        public static final String BUEN_ESTADO = "Buen Estado";
        /**
         * Condición de un producto usado en estado aceptable.
         */
        public static final String ACEPTABLE = "Aceptable";

        // Moneda predeterminada
        /**
         * Código del peso chileno y moneda predeterminada.
         */
        public static final String CLP = "CLP";
    }

    // =====================================================
    // IMÁGENES
    // =====================================================

    /**
     * Columnas y reglas de orden y cantidad de las imágenes de una publicación.
     */
    public static final class Imagenes implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Imagenes() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "imagenes";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID de la publicación asociada.
         */
        public static final String PUBLICACION_ID = "publicacion_id";
        /**
         * Columna con la ruta o URL de la imagen.
         */
        public static final String IMAGE_URL = "image_url";
        /**
         * Columna con la posición de la imagen en la galería.
         */
        public static final String DISPLAY_ORDER = "display_order";

        // Reglas de la galería
        /**
         * Cantidad mínima de imágenes por publicación.
         */
        public static final int MIN_IMAGENES = 1;
        /**
         * Cantidad máxima de imágenes por publicación.
         */
        public static final int MAX_IMAGENES = 10;
        /**
         * Posición reservada para la imagen de portada.
         */
        public static final int ORDEN_PORTADA = 1;
    }

    // =====================================================
    // FAVORITOS
    // Clave primaria compuesta:
    // usuario_id + publicacion_id
    // =====================================================

    /**
     * Columnas de los favoritos, identificados por la combinación de usuario y publicación.
     */
    public static final class Favoritos {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Favoritos() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "favoritos";

        /**
         * Columna con el ID del usuario asociado.
         */
        public static final String USUARIO_ID = "usuario_id";
        /**
         * Columna con el ID de la publicación asociada.
         */
        public static final String PUBLICACION_ID = "publicacion_id";
        /**
         * Columna con la fecha en que se guardó el favorito.
         */
        public static final String SAVE_AT = "save_at";
    }

    // =====================================================
    // SUBASTAS
    // =====================================================

    /**
     * Columnas y estados de las subastas asociadas a publicaciones.
     */
    public static final class Subastas implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Subastas() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "subastas";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID de la publicación asociada.
         */
        public static final String PUBLICACION_ID = "publicacion_id";
        /**
         * Columna con el inicio de la subasta en milisegundos.
         */
        public static final String INICIO = "inicio_ms";
        /**
         * Columna con el fin de la subasta en milisegundos.
         */
        public static final String FIN = "fin_ms";
        /**
         * Columna con el precio inicial de la subasta.
         */
        public static final String MINIMO = "precio_inicial";
        /**
         * Columna con el estado del registro.
         */
        public static final String STATUS = "status";

        // Estados
        /**
         * Estado de una subasta programada.
         */
        public static final String PROGRAMADA = "PROGRAMADA";
        /**
         * Estado de una subasta activa.
         */
        public static final String ACTIVA = "ACTIVA";
        /**
         * Estado de una subasta finalizada.
         */
        public static final String FINALIZADA = "FINALIZADA";
        /**
         * Estado que indica que se canceló la operación.
         */
        public static final String CANCELADA = "CANCELADA";
    }

    // =====================================================
    // PUJAS
    // =====================================================

    /**
     * Columnas e índice de las ofertas hechas en una subasta.
     */
    public static final class Pujas implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Pujas() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "pujas";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID de la subasta.
         */
        public static final String SUBASTA_ID = "subasta_id";
        /**
         * Columna con el ID del usuario que hizo la puja.
         */
        public static final String POSTOR_ID = "postor_id";
        /**
         * Columna con el monto de la puja.
         */
        public static final String MONTO = "monto";
        /**
         * Columna con la fecha del registro en milisegundos.
         */
        public static final String FECHA = "fecha_ms";

        /**
         * Nombre del índice de pujas por subasta.
         */
        public static final String IDX_SUBASTA =
                "idx_pujas_subasta";
    }

    // =====================================================
    // COMPRAS
    // =====================================================

    /**
     * Columnas y estados de las operaciones de compra.
     */
    public static final class Compras implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Compras() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "compras";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID de la publicación asociada.
         */
        public static final String PUBLICACION_ID = "publicacion_id";
        /**
         * Columna con el ID del comprador.
         */
        public static final String COMPRADOR_ID = "comprador_id";
        /**
         * Columna con el ID del vendedor.
         */
        public static final String VENDEDOR_ID = "vendedor_id";
        /**
         * Columna con el precio acordado para la compra.
         */
        public static final String PRECIO_ACORDADO = "precio_acordado";
        /**
         * Columna con la fecha del registro en milisegundos.
         */
        public static final String FECHA = "fecha_ms";
        /**
         * Columna con el estado del registro.
         */
        public static final String STATUS = "status";

        // Estados
        /**
         * Estado de una compra confirmada.
         */
        public static final String CONFIRMADA = "CONFIRMADA";
        /**
         * Estado que indica que se canceló la operación.
         */
        public static final String CANCELADA = "CANCELADA";
    }

    // =====================================================
    // NOTIFICACIONES
    // =====================================================

    /**
     * Columnas y marcas de lectura de los avisos locales.
     */
    public static final class Notificaciones implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Notificaciones() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "notificaciones";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID del usuario asociado.
         */
        public static final String USUARIO_ID = "usuario_id";
        /**
         * Columna con el texto del aviso.
         */
        public static final String MENSAJE = "mensaje";
        /**
         * Columna que indica si se leyó el aviso.
         */
        public static final String LEIDA = "leida";
        /**
         * Columna con la fecha del registro en milisegundos.
         */
        public static final String FECHA = "fecha_ms";

        // Valores booleanos almacenados como INTEGER
        /**
         * Valor que indica que el aviso aún no se ha leído.
         */
        public static final int NO_LEIDA = 0;
        /**
         * Valor que indica que el aviso ya se leyó.
         */
        public static final int LEIDA_SI = 1;
    }

    // =====================================================
    // CHATS
    // =====================================================

    /**
     * Columnas e índices de las conversaciones por publicación.
     */
    public static final class Chats implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Chats() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "chats";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID de la publicación asociada.
         */
        public static final String PUBLICACION_ID = "publicacion_id";
        /**
         * Columna con el ID del comprador.
         */
        public static final String COMPRADOR_ID = "comprador_id";
        /**
         * Columna con el ID del vendedor.
         */
        public static final String VENDEDOR_ID = "vendedor_id";
        /**
         * Columna con la fecha de creación del chat en milisegundos.
         */
        public static final String CREADO = "creado_ms";

        // Índices existentes
        /**
         * Nombre del índice por comprador.
         */
        public static final String IDX_COMPRADOR =
                "idx_chats_comprador";
        /**
         * Nombre del índice por vendedor.
         */
        public static final String IDX_VENDEDOR =
                "idx_chats_vendedor";
    }

    // =====================================================
    // MENSAJES
    // =====================================================

    /**
     * Columnas, índice y marcas de lectura de los mensajes de cada chat.
     */
    public static final class Mensajes implements BaseColumns {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private Mensajes() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "mensajes";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID del chat al que pertenece el mensaje.
         */
        public static final String CHAT_ID = "chat_id";
        /**
         * Columna con el ID del usuario que envió el mensaje.
         */
        public static final String REMITENTE_ID = "remitente_id";
        /**
         * Columna con el contenido del mensaje.
         */
        public static final String TEXTO = "texto";
        /**
         * Columna con la fecha de envío en milisegundos.
         */
        public static final String ENVIADO = "enviado_ms";
        /**
         * Columna que indica si se leyó el mensaje.
         */
        public static final String LEIDO = "leido";

        /**
         * Nombre del índice de mensajes por chat.
         */
        public static final String IDX_CHAT =
                "idx_mensajes_chat";

        // Valores booleanos almacenados como INTEGER
        /**
         * Valor que indica que el mensaje aún no se ha leído.
         */
        public static final int NO_LEIDO = 0;
        /**
         * Valor que indica que el mensaje ya se leyó.
         */
        public static final int LEIDO_SI = 1;
    }

    // =====================================================
    // AUTHIDENTITIES
    // =====================================================

    /**
     * Columnas que vinculan un perfil local con una cuenta de un proveedor de acceso.
     */
    public static final class AuthIdentities {

        /**
         * Evita que se creen instancias de esta clase de constantes.
         */
        private AuthIdentities() {}

        /**
         * Nombre de la tabla en SQLite.
         */
        public static final String TABLE = "auth_identities";

        /**
         * Columna con el identificador del registro.
         */
        public static final String ID = "id";
        /**
         * Columna con el ID del usuario asociado.
         */
        public static final String USUARIO_ID = "usuario_id";
        /**
         * Columna con el proveedor de acceso.
         */
        public static final String PROVIDER = "provider";
        /**
         * Columna con el identificador de la cuenta en el proveedor.
         */
        public static final String PROVIDER_UID = "provider_uid";

        /**
         * Valor que identifica a Google como proveedor de acceso.
         */
        public static final String GOOGLE = "GOOGLE";
    }
}

package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import com.example.bidmarket.database.BidMarketContract.AuthIdentities;
import com.example.bidmarket.database.BidMarketContract.Cuentas;
import com.example.bidmarket.database.BidMarketContract.Usuarios;
import com.example.bidmarket.database.BidMarketDbHelper;
import com.example.bidmarket.utils.SecurityUtils;

import java.security.SecureRandom;

/**
 * Vincula las cuentas de Google autenticadas en Firebase con perfiles locales
 * de BidMarket+. Las operaciones de SQLite se deben ejecutar fuera del hilo principal.
 */
public final class GoogleAccountRepository {

    private final BidMarketDbHelper helper;

    /**
     * Prepara el acceso a la base de datos local.
     *
     * @param context contexto del que se obtiene el contexto de la aplicación.
     */
    public GoogleAccountRepository(Context context) {
        helper = new BidMarketDbHelper(context.getApplicationContext());
    }

    /**
     * Busca el perfil local vinculado al UID de Firebase. Si la cuenta local
     * está inactiva, no se considera un perfil nuevo.
     *
     * @param firebaseUid identificador de la cuenta autenticada en Firebase.
     * @return ID del perfil local, o {@code null} si no hay una asociación.
     * @throws IllegalArgumentException si el UID es nulo o está vacío.
     * @throws IllegalStateException si la cuenta local vinculada no está activa.
     * @throws android.database.sqlite.SQLiteException si falla el acceso a la base de datos.
     */
    public Integer buscarPerfil(String firebaseUid) {
        validarUid(firebaseUid);
        return buscarPerfil(helper.getReadableDatabase(), firebaseUid);
    }

    /**
     * Crea la cuenta, el perfil local y el vínculo con Google en una sola
     * transacción. Si el UID ya tiene un perfil activo, devuelve su ID. No
     * vincula automáticamente una cuenta existente por coincidencia de correo.
     *
     * @param firebaseUid identificador del usuario autenticado en Firebase.
     * @param correoVerificado correo verificado obtenido de Firebase, no del formulario.
     * @param nombre nombre que se muestra en el perfil.
     * @param nombreUsuario nombre de usuario elegido para BidMarket+.
     * @param aceptoTerminos indica si el usuario aceptó los términos.
     * @return ID del perfil local creado o recuperado.
     * @throws IllegalArgumentException si faltan datos, no se aceptaron los términos
     *         o el nombre de usuario ya está en uso al crear el perfil.
     * @throws IllegalStateException si el perfil vinculado está inactivo o, al crear
     *         uno nuevo, ya existe una cuenta con el mismo correo.
     * @throws ArithmeticException si el ID del nuevo perfil no cabe en un {@code int}.
     * @throws android.database.sqlite.SQLiteException si falla una operación de SQLite.
     */
    public int crearPerfil(
            String firebaseUid,
            String correoVerificado,
            String nombre,
            String nombreUsuario,
            boolean aceptoTerminos
    ) {
        validarUid(firebaseUid);

        if (!aceptoTerminos) {
            throw new IllegalArgumentException("Debes aceptar los términos");
        }

        String correo = requerido(correoVerificado, "Falta el correo de Google");
        String nombrePerfil = requerido(nombre, "Ingresa tu nombre");
        String usuario = requerido(nombreUsuario, "Elige un nombre de usuario");

        // El esquema actual exige un hash incluso para cuentas Google.
        // El secreto no se muestra ni se guarda en texto plano.
        byte[] secreto = new byte[32];
        new SecureRandom().nextBytes(secreto);
        String hash = SecurityUtils.hashPassword(
                Base64.encodeToString(secreto, Base64.NO_WRAP)
        );

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Integer existente = buscarPerfil(db, firebaseUid);
            if (existente != null) {
                db.setTransactionSuccessful();
                return existente;
            }

            if (existe(db, Cuentas.TABLE, Cuentas.ID,
                    Cuentas.EMAIL + " = ? COLLATE NOCASE", correo)) {
                throw new IllegalStateException(
                        "Ya existe una cuenta con ese correo. "
                                + "Inicia sesión con tu cuenta de BidMarket+."
                );
            }

            if (existe(db, Cuentas.TABLE, Cuentas.ID,
                    Cuentas.USER_NAME + " = ?", usuario)
                    || existe(db, Usuarios.TABLE, Usuarios.ID,
                    Usuarios.USER_NAME + " = ?", usuario)) {
                throw new IllegalArgumentException(
                        "Ese nombre de usuario ya está en uso"
                );
            }

            ContentValues cuenta = new ContentValues();
            cuenta.put(Cuentas.NAME, nombrePerfil);
            cuenta.put(Cuentas.USER_NAME, usuario);
            cuenta.put(Cuentas.EMAIL, correo);
            cuenta.put(Cuentas.PASSWORD_HASH, hash);
            long cuentaId = db.insertOrThrow(Cuentas.TABLE, null, cuenta);

            ContentValues perfil = new ContentValues();
            perfil.put(Usuarios.CUENTA_ID, cuentaId);
            perfil.put(Usuarios.NAME, nombrePerfil);
            perfil.put(Usuarios.USER_NAME, usuario);
            int usuarioId = Math.toIntExact(
                    db.insertOrThrow(Usuarios.TABLE, null, perfil)
            );

            ContentValues identidad = new ContentValues();
            identidad.put(AuthIdentities.USUARIO_ID, usuarioId);
            identidad.put(AuthIdentities.PROVIDER, AuthIdentities.GOOGLE);
            identidad.put(AuthIdentities.PROVIDER_UID, firebaseUid);
            db.insertOrThrow(AuthIdentities.TABLE, null, identidad);

            db.setTransactionSuccessful();
            return usuarioId;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Busca el perfil de Google y comprueba que la cuenta local esté activa.
     *
     * @param db base de datos que se usa para la consulta.
     * @param firebaseUid identificador de Firebase que se quiere buscar.
     * @return ID del perfil asociado, o {@code null} si no hay una asociación.
     * @throws IllegalStateException si la cuenta vinculada no está activa.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
     */
    private Integer buscarPerfil(SQLiteDatabase db, String firebaseUid) {
        String sql =
                "SELECT a." + AuthIdentities.USUARIO_ID + ", c." + Cuentas.STATUS
                        + " FROM " + AuthIdentities.TABLE + " a"
                        + " JOIN " + Usuarios.TABLE + " u ON u." + Usuarios.ID
                        + " = a." + AuthIdentities.USUARIO_ID
                        + " JOIN " + Cuentas.TABLE + " c ON c." + Cuentas.ID
                        + " = u." + Usuarios.CUENTA_ID
                        + " WHERE a." + AuthIdentities.PROVIDER + " = ?"
                        + " AND a." + AuthIdentities.PROVIDER_UID + " = ?";

        try (Cursor cursor = db.rawQuery(
                sql, new String[]{AuthIdentities.GOOGLE, firebaseUid})) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            String estado = cursor.getString(
                    cursor.getColumnIndexOrThrow(Cuentas.STATUS)
            );
            if (!Cuentas.ACTIVE.equals(estado)) {
                throw new IllegalStateException("La cuenta local no está activa");
            }

            return cursor.getInt(
                    cursor.getColumnIndexOrThrow(AuthIdentities.USUARIO_ID)
            );
        }
    }

    /**
     * Comprueba si hay al menos un registro que cumple la condición indicada.
     *
     * @param db base de datos donde se hace la consulta.
     * @param tabla nombre de la tabla que se consulta.
     * @param id nombre de la columna de identificador que se selecciona.
     * @param seleccion condición SQL con un marcador de parámetro.
     * @param valor valor que se pasa a la condición.
     * @return {@code true} si se encuentra un registro, o {@code false} si no.
     * @throws android.database.sqlite.SQLiteException si falla la consulta.
     */
    private static boolean existe(
            SQLiteDatabase db, String tabla, String id,
            String seleccion, String valor
    ) {
        try (Cursor cursor = db.query(
                tabla, new String[]{id}, seleccion, new String[]{valor},
                null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    /**
     * Comprueba que el UID de Firebase tenga contenido.
     *
     * @param uid identificador que se quiere validar.
     * @throws IllegalArgumentException si es nulo o queda vacío al quitar los espacios de los extremos.
     */
    private static void validarUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el UID de Firebase");
        }
    }

    /**
     * Valida un dato obligatorio y quita los espacios del inicio y del final.
     *
     * @param valor dato que se quiere validar.
     * @param mensaje texto del error si falta el dato.
     * @return valor sin los espacios de los extremos.
     * @throws IllegalArgumentException si el valor es nulo o queda vacío.
     */
    private static String requerido(String valor, String mensaje) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }
}
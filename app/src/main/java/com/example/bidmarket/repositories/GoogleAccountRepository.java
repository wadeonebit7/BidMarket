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
 * Asocia identidades Firebase autenticadas con perfiles locales.
 * Ejecutar sus operaciones fuera del hilo principal.
 */
public final class GoogleAccountRepository {

    private final BidMarketDbHelper helper;

    public GoogleAccountRepository(Context context) {
        helper = new BidMarketDbHelper(context.getApplicationContext());
    }

    /**
     * Devuelve el ID local o null si aún no existe una asociación.
     * Una cuenta local inactiva no se trata como un perfil nuevo.
     */
    public Integer buscarPerfil(String firebaseUid) {
        validarUid(firebaseUid);
        return buscarPerfil(helper.getReadableDatabase(), firebaseUid);
    }

    /**
     * Recibe el UID y el correo verificado del resultado de Firebase,
     * no de campos editables del formulario.
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

    private static void validarUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el UID de Firebase");
        }
    }

    private static String requerido(String valor, String mensaje) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }
}
package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bidmarket.database.BidMarketContract.Cuentas;
import com.example.bidmarket.database.BidMarketContract.Usuarios;
import com.example.bidmarket.database.BidMarketDbHelper;
import com.example.bidmarket.utils.SecurityUtils;

/** Acceso local a datos de cuentas y perfiles; sin dependencias de UI. */
public final class AuthRepository {
    private final BidMarketDbHelper helper;

    public AuthRepository(Context context) {
        helper = new BidMarketDbHelper(context.getApplicationContext());
    }

    /** Devuelve el ID del perfil o -1 si las credenciales son incorrectas. */
    public int iniciarSesion(String email, String password) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String sql = "SELECT u." + Usuarios.ID + ", c." + Cuentas.ID +
                ", c." + Cuentas.PASSWORD_HASH + ", c." + Cuentas.STATUS +
                " FROM " + Cuentas.TABLE + " c JOIN " + Usuarios.TABLE + " u" +
                " ON u." + Usuarios.CUENTA_ID + " = c." + Cuentas.ID +
                " WHERE c." + Cuentas.EMAIL + " = ? COLLATE NOCASE";

        int perfilId;
        long cuentaId;
        String hash;
        String estado;
        try (Cursor cursor = db.rawQuery(sql, new String[]{email})) {
            if (!cursor.moveToFirst()) return -1;
            perfilId = cursor.getInt(0);
            cuentaId = cursor.getLong(1);
            hash = cursor.getString(2);
            estado = cursor.getString(3);
        }

        if (!Cuentas.ACTIVE.equals(estado) || !SecurityUtils.verificar(password, hash)) {
            return -1;
        }

        // Conserva la migración de SHA-256 heredado a PBKDF2.
        if (SecurityUtils.requiereMigracion(hash)) {
            ContentValues valores = new ContentValues();
            valores.put(Cuentas.PASSWORD_HASH, SecurityUtils.hashPassword(password));
            db.update(Cuentas.TABLE, valores,
                    Cuentas.ID + " = ? AND " + Cuentas.PASSWORD_HASH + " = ?",
                    new String[]{String.valueOf(cuentaId), hash});
        }
        return perfilId;
    }

    /** Registra cuenta y perfil atómicamente; lanza excepción ante fallos. */
    public long registrarUsuario(String nombre, String usuario, String correo, String password) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cuenta = new ContentValues();
            cuenta.put(Cuentas.NAME, nombre);
            cuenta.put(Cuentas.USER_NAME, usuario);
            cuenta.put(Cuentas.EMAIL, correo);
            cuenta.put(Cuentas.PASSWORD_HASH, SecurityUtils.hashPassword(password));
            long cuentaId = db.insertOrThrow(Cuentas.TABLE, null, cuenta);

            ContentValues perfil = new ContentValues();
            perfil.put(Usuarios.CUENTA_ID, cuentaId);
            perfil.put(Usuarios.NAME, nombre);
            perfil.put(Usuarios.USER_NAME, usuario);
            db.insertOrThrow(Usuarios.TABLE, null, perfil);

            db.setTransactionSuccessful();
            return cuentaId;
        } finally {
            db.endTransaction();
        }
    }
}

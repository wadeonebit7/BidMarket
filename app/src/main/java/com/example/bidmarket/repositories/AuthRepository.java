package com.example.bidmarket.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bidmarket.database.BidMarketContract.Cuentas;
import com.example.bidmarket.database.BidMarketContract.Usuarios;
import com.example.bidmarket.database.BidMarketDbHelper;
import com.example.bidmarket.utils.SecurityUtils;

/**
 * Consulta y guarda cuentas y perfiles locales en SQLite, sin depender de la interfaz.
 */
public final class AuthRepository {
    private final BidMarketDbHelper helper;

    /**
     * Prepara el acceso a la base de datos local.
     *
     * @param context contexto del que se obtiene el contexto de la aplicación.
     */
    public AuthRepository(Context context) {
        helper = new BidMarketDbHelper(context.getApplicationContext());
    }

    /**
     * Busca la cuenta por correo sin distinguir mayúsculas y minúsculas.
     * Comprueba que esté activa y que la contraseña sea correcta. Si el hash
     * todavía usa el formato anterior, lo actualiza a PBKDF2.
     *
     * @param email correo de la cuenta.
     * @param password contraseña ingresada para verificar la cuenta.
     * @return ID del perfil, o {@code -1} si la cuenta no tiene un perfil asociado,
     *         no está activa o las credenciales no coinciden.
     * @throws android.database.sqlite.SQLiteException si falla el acceso a la base de datos.
     */
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

    /**
     * Crea la cuenta y su perfil en una sola transacción. Guarda el hash de la
     * contraseña, no la contraseña en texto plano. Si un paso falla, no confirma
     * ninguno de los dos registros.
     *
     * @param nombre nombre de la persona.
     * @param usuario nombre de usuario para la cuenta y el perfil.
     * @param correo correo electrónico de la cuenta.
     * @param password contraseña que se usa para generar el hash.
     * @return ID de la cuenta creada, no el ID del perfil.
     * @throws android.database.sqlite.SQLiteConstraintException si se incumple una
     *         restricción, por ejemplo, si el correo o el nombre de usuario ya existen.
     * @throws android.database.sqlite.SQLiteException si falla otra operación de la base de datos.
     */
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

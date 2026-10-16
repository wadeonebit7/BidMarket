package com.example.bidmarket;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/** Chats locales: solo usuarios de este dispositivo. No implementa red ni tiempo real. */
public class ChatRepository {
    private final BidMarketDbHelper helper;
    public ChatRepository(Context ctx) { helper = new BidMarketDbHelper(ctx.getApplicationContext()); }

    public static class Entrada {
        public final long id;
        public final String titulo, interlocutor, ultimo;
        public Entrada(long id,String titulo,String interlocutor,String ultimo) {this.id=id;this.titulo=titulo;this.interlocutor=interlocutor;this.ultimo=ultimo;}
    }
    public static class Mensaje {
        public final long id;
        public final int autor;
        public final String texto;
        public Mensaje(long id,int autor,String texto){this.id=id;this.autor=autor;this.texto=texto;}
    }

    /** Comprueba reglas de acceso desde la BD, nunca solo desde la UI. */
    private void validarInicio(SQLiteDatabase db,long publicacionId,int solicitante) {
        String sql="SELECT p.vendedor_id,p.modalidad,p.status,s.fin_ms,s.status " +
                "FROM publicaciones p LEFT JOIN subastas s ON s.publicacion_id=p.id WHERE p.id=?";
        try(Cursor c=db.rawQuery(sql,new String[]{String.valueOf(publicacionId)})) {
            if(!c.moveToFirst()) throw new IllegalArgumentException("Publicación inexistente");
            if(c.getInt(0)==solicitante) throw new IllegalArgumentException("No puedes escribirte a ti mismo");
            String modo=c.getString(1), estado=c.getString(2);
            if("Borrado".equals(estado)) throw new IllegalArgumentException("Publicación eliminada");
            if("SUBASTA".equals(modo)) {
                if(c.isNull(3)||System.currentTimeMillis()<c.getLong(3)||"CANCELADA".equals(c.getString(4)))
                    throw new IllegalArgumentException("El chat estará disponible cuando finalice la subasta");
            } else if(!"Activo".equals(estado) && !"Vendido".equals(estado)) {
                throw new IllegalArgumentException("La publicación no permite nuevas conversaciones");
            }
        }
    }

    public long iniciar(long publicacionId,int interesado) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            validarInicio(db,publicacionId,interesado);
            int vendedor;
            try(Cursor c=db.rawQuery("SELECT vendedor_id FROM publicaciones WHERE id=?",new String[]{""+publicacionId})) {c.moveToFirst();vendedor=c.getInt(0);}
            try(Cursor u=db.rawQuery("SELECT id FROM usuarios WHERE id=?",new String[]{""+interesado})) {
                if(!u.moveToFirst()) throw new IllegalArgumentException("Sesión inválida");
            }
            ContentValues v=new ContentValues();v.put("publicacion_id",publicacionId);v.put("comprador_id",interesado);v.put("vendedor_id",vendedor);v.put("creado_ms",System.currentTimeMillis());
            db.insertWithOnConflict("chats",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            long id;
            try(Cursor c=db.rawQuery("SELECT id FROM chats WHERE publicacion_id=? AND comprador_id=?",new String[]{""+publicacionId,""+interesado})) {
                if(!c.moveToFirst())throw new IllegalStateException("No se pudo iniciar chat"); id=c.getLong(0);
            }
            db.setTransactionSuccessful();return id;
        } finally {db.endTransaction();}
    }

    public boolean participante(long chatId,int usuario) {
        try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT id FROM chats WHERE id=? AND (comprador_id=? OR vendedor_id=?)",new String[]{""+chatId,""+usuario,""+usuario})) {return c.moveToFirst();}
    }

    public String titulo(long chatId,int usuario) {
        String q="SELECT p.title,CASE WHEN c.comprador_id=? THEN v.name ELSE b.name END " +
                 "FROM chats c JOIN publicaciones p ON p.id=c.publicacion_id " +
                 "JOIN usuarios v ON v.id=c.vendedor_id JOIN usuarios b ON b.id=c.comprador_id " +
                 "WHERE c.id=? AND (c.comprador_id=? OR c.vendedor_id=?)";
        try(Cursor c=helper.getReadableDatabase().rawQuery(q,new String[]{""+usuario,""+chatId,""+usuario,""+usuario})) {
            if(!c.moveToFirst())throw new IllegalArgumentException("Chat no autorizado");return c.getString(0)+" · "+c.getString(1);
        }
    }

    public List<Entrada> historial(int usuario) {
        List<Entrada> out=new ArrayList<>();
        String q="SELECT c.id,p.title,CASE WHEN c.comprador_id=? THEN v.name ELSE b.name END, " +
                 "COALESCE((SELECT m.texto FROM mensajes m WHERE m.chat_id=c.id ORDER BY m.id DESC LIMIT 1),'Conversación nueva') " +
                 "FROM chats c JOIN publicaciones p ON p.id=c.publicacion_id " +
                 "JOIN usuarios v ON v.id=c.vendedor_id JOIN usuarios b ON b.id=c.comprador_id " +
                 "WHERE c.comprador_id=? OR c.vendedor_id=? ORDER BY COALESCE((SELECT MAX(enviado_ms) FROM mensajes WHERE chat_id=c.id),c.creado_ms) DESC";
        try(Cursor c=helper.getReadableDatabase().rawQuery(q,new String[]{""+usuario,""+usuario,""+usuario})) {
            while(c.moveToNext())out.add(new Entrada(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)));
        }return out;
    }

    public List<Mensaje> mensajes(long chatId,int usuario) {
        if(!participante(chatId,usuario))throw new IllegalArgumentException("Chat no autorizado");
        List<Mensaje> out=new ArrayList<>();
        try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT id,remitente_id,texto FROM mensajes WHERE chat_id=? ORDER BY id ASC",new String[]{""+chatId})) {
            while(c.moveToNext())out.add(new Mensaje(c.getLong(0),c.getInt(1),c.getString(2)));
        }return out;
    }

    public void enviar(long chatId,int usuario,String texto) {
        String limpio=texto.trim();if(limpio.isEmpty()||limpio.length()>2000)throw new IllegalArgumentException("Mensaje de 1 a 2000 caracteres requerido");
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            try(Cursor c=db.rawQuery("SELECT c.id,p.modalidad,s.fin_ms,s.status FROM chats c JOIN publicaciones p ON p.id=c.publicacion_id LEFT JOIN subastas s ON s.publicacion_id=p.id WHERE c.id=? AND (c.comprador_id=? OR c.vendedor_id=?)",new String[]{""+chatId,""+usuario,""+usuario})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("Chat no autorizado");
                if("SUBASTA".equals(c.getString(1))&&(c.isNull(2)||System.currentTimeMillis()<c.getLong(2)||"CANCELADA".equals(c.getString(3))))
                    throw new IllegalArgumentException("La subasta aún no ha finalizado");
            }
            ContentValues v=new ContentValues();v.put("chat_id",chatId);v.put("remitente_id",usuario);v.put("texto",limpio);v.put("enviado_ms",System.currentTimeMillis());
            db.insertOrThrow("mensajes",null,v);db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
}

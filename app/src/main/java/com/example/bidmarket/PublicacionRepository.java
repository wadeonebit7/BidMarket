package com.example.bidmarket;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.bidmarket.Models.Publicacion;
import java.util.ArrayList;
import java.util.List;

/** Centraliza CRUD y transacciones. Invocar operaciones de disco en ExecutorService. */
public class PublicacionRepository {
    private final BidMarketDbHelper helper;
    public PublicacionRepository(Context context) { helper = new BidMarketDbHelper(context.getApplicationContext()); }

    public static class Categoria {
        public final long id; public final String nombre;
        Categoria(long id, String nombre) { this.id=id; this.nombre=nombre; }
        @Override public String toString() {return nombre;}
    }

    public List<Categoria> categorias() {
        List<Categoria> out = new ArrayList<>();
        try (Cursor c=helper.getReadableDatabase().query("categorias",new String[]{"id","name"},null,null,null,null,"name ASC")) {
            while(c.moveToNext()) out.add(new Categoria(c.getLong(0),c.getString(1)));
        }
        return out;
    }

    public List<Publicacion> listar() {
        List<Publicacion> out = new ArrayList<>();
        String sql = "SELECT p.id,p.title,p.price,p.currency,p.condition,i.image_url,p.modalidad FROM publicaciones p " +
                "LEFT JOIN imagenes i ON i.publicacion_id=p.id AND i.display_order=1 " +
                "WHERE p.status='Activo' ORDER BY p.id DESC";
        try(Cursor c=helper.getReadableDatabase().rawQuery(sql,null)) {
            while(c.moveToNext()) out.add(new Publicacion(c.getInt(0),c.getString(1),c.getDouble(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6)));
        }
        return out;
    }

    public Cursor detalle(long id) {
        return helper.getReadableDatabase().rawQuery("SELECT p.id,p.vendedor_id,p.categoria_id,p.title,p.description,p.price,p.condition,p.status,p.location_name,p.latitude,p.longitude,p.modalidad, i.image_url FROM publicaciones p LEFT JOIN imagenes i ON i.publicacion_id=p.id AND i.display_order=1 WHERE p.id=?",new String[]{String.valueOf(id)});
    }

    public long guardar(long id, int vendedorId, long categoriaId, String titulo, String descripcion, double precio,
                        String condicion, String ubicacion, String imagen, boolean subasta, long inicio, long fin, Double lat, Double lon, List<String> rutasImagenes) {
        if (precio<=0 || titulo.trim().isEmpty() || ubicacion.trim().isEmpty() || vendedorId<=0) throw new IllegalArgumentException("Datos de publicación inválidos");
        SQLiteDatabase db=helper.getWritableDatabase(); db.beginTransaction();
        try {
            ContentValues v=new ContentValues();
            v.put("vendedor_id",vendedorId);v.put("categoria_id",categoriaId);v.put("title",titulo);v.put("description",descripcion);
            v.put("price",precio);v.put("condition",condicion);v.put("location_name",ubicacion);
            v.put("modalidad",subasta?"SUBASTA":"VENTA");v.put("update_at",System.currentTimeMillis()/1000);
            if(lat!=null && lon!=null) {v.put("latitude",lat);v.put("longitude",lon);}
            long pid=id;
            if(id==0) {
                v.put("status","Activo"); v.put("currency","CLP");
                pid=db.insertOrThrow("publicaciones",null,v);
            } else {
                if(db.update("publicaciones",v,"id=? AND vendedor_id=? AND status IN ('Activo','Pausado')",new String[]{""+id,""+vendedorId})!=1)
                    throw new IllegalArgumentException("Solo el propietario puede editar una publicación vigente");
            }
            if (rutasImagenes == null || rutasImagenes.isEmpty() || rutasImagenes.size() > 10)
                throw new IllegalArgumentException("Agrega de 1 a 10 imágenes");
            db.delete("imagenes", "publicacion_id=?", new String[]{Long.toString(pid)});
            int orden=1;
            for (String ruta: rutasImagenes) {
                ContentValues iv = new ContentValues();
                iv.put("publicacion_id", pid);
                iv.put("image_url", ruta);
                iv.put("display_order", orden++);
                db.insertOrThrow("imagenes", null, iv);
            }
            if(subasta) {
                if(fin<=inicio || fin<=System.currentTimeMillis()) throw new IllegalArgumentException("La subasta debe finalizar en el futuro");
                if(id>0) {
                    try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM pujas WHERE subasta_id=(SELECT id FROM subastas WHERE publicacion_id=?)",new String[]{""+id})) {
                        if(c.moveToFirst() && c.getInt(0)>0) throw new IllegalArgumentException("No es posible modificar una subasta con pujas");
                    }
                    db.delete("subastas","publicacion_id=?",new String[]{""+id});
                }
                ContentValues a=new ContentValues();a.put("publicacion_id",pid);a.put("inicio_ms",inicio);a.put("fin_ms",fin);
                a.put("precio_inicial",(long)Math.ceil(precio));a.put("status",inicio>System.currentTimeMillis()?"PROGRAMADA":"ACTIVA");
                db.insertOrThrow("subastas",null,a);
            } else if(id>0) {
                try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM pujas WHERE subasta_id=(SELECT id FROM subastas WHERE publicacion_id=?)",new String[]{""+id})) {
                    if(c.moveToFirst() && c.getInt(0)>0) throw new IllegalArgumentException("No es posible convertir una subasta con pujas");
                }
                db.delete("subastas","publicacion_id=?",new String[]{""+id});
            }
            db.setTransactionSuccessful(); return pid;
        } finally {db.endTransaction();}
    }

    public boolean cambiarEstado(long id,int propietario,String estado) {
        if(!estado.equals("Activo")&&!estado.equals("Pausado")&&!estado.equals("Vendido")&&!estado.equals("Borrado"))return false;
        ContentValues v=new ContentValues();v.put("status",estado);v.put("update_at",System.currentTimeMillis()/1000);
        if(estado.equals("Borrado")) v.put("delete_at",System.currentTimeMillis()/1000);
        return helper.getWritableDatabase().update("publicaciones",v,"id=? AND vendedor_id=? AND status IN ('Activo','Pausado')",new String[]{""+id,""+propietario})==1;
    }

    /** Valida la puja dentro de una transacción, incluyendo vendedor, horario y máximo actual. */
    public void ofertar(long publicacionId, int postorId, long monto) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            long subastaId,fin,inicio,min,actual;int vendedor;
            try(Cursor c=db.rawQuery("SELECT s.id,s.inicio_ms,s.fin_ms,s.precio_inicial,p.vendedor_id,COALESCE((SELECT MAX(monto) FROM pujas WHERE subasta_id=s.id),0) FROM subastas s JOIN publicaciones p ON p.id=s.publicacion_id WHERE p.id=? AND p.status='Activo'",new String[]{""+publicacionId})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("Subasta no disponible");
                subastaId=c.getLong(0);inicio=c.getLong(1);fin=c.getLong(2);min=c.getLong(3);vendedor=c.getInt(4);actual=c.getLong(5);
            }
            long ahora=System.currentTimeMillis();
            if(ahora<inicio || ahora>=fin)throw new IllegalArgumentException("La subasta no está en curso");
            if(postorId==vendedor)throw new IllegalArgumentException("No puedes pujar por tu propia publicación");
            if(monto<=actual || monto<min)throw new IllegalArgumentException("La puja debe superar la mayor oferta y alcanzar el precio inicial");
            int anterior=-1;
            try(Cursor last=db.rawQuery("SELECT postor_id FROM pujas WHERE subasta_id=? ORDER BY monto DESC,id DESC LIMIT 1",new String[]{""+subastaId})){
                if(last.moveToFirst())anterior=last.getInt(0);
            }
            ContentValues v=new ContentValues();v.put("subasta_id",subastaId);v.put("postor_id",postorId);v.put("monto",monto);v.put("fecha_ms",ahora);
            db.insertOrThrow("pujas",null,v);
            if(anterior>0 && anterior!=postorId){
                ContentValues aviso=new ContentValues();aviso.put("usuario_id",anterior);
                aviso.put("mensaje","Tu oferta fue superada en la publicación #"+publicacionId);aviso.put("fecha_ms",ahora);
                db.insertOrThrow("notificaciones",null,aviso);
            }
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }

    /** Compra local: registra acuerdo de venta sin integración de pagos ni comunicación entre teléfonos. */
    public void comprar(long pubId, int comprador) {
        SQLiteDatabase db=helper.getWritableDatabase(); db.beginTransaction();
        try {
            int vendedor;double precio;
            try(Cursor c=db.rawQuery("SELECT vendedor_id,price FROM publicaciones WHERE id=? AND modalidad='VENTA' AND status='Activo'",new String[]{""+pubId})) {
                if(!c.moveToFirst())throw new IllegalArgumentException("La publicación no está disponible para compra");
                vendedor=c.getInt(0);precio=c.getDouble(1);
            }
            if(comprador==vendedor)throw new IllegalArgumentException("No puedes comprar tu propio producto");
            ContentValues v=new ContentValues();v.put("publicacion_id",pubId);v.put("comprador_id",comprador);v.put("vendedor_id",vendedor);v.put("precio_acordado",precio);v.put("fecha_ms",System.currentTimeMillis());
            db.insertOrThrow("compras",null,v);
            ContentValues estado=new ContentValues();estado.put("status","Vendido");
            if(db.update("publicaciones",estado,"id=? AND status='Activo'",new String[]{""+pubId})!=1)throw new IllegalArgumentException("La publicación ya no está disponible");
            ContentValues notif=new ContentValues();notif.put("usuario_id",vendedor);notif.put("mensaje","Se registró una compra local para la publicación #"+pubId);notif.put("fecha_ms",System.currentTimeMillis());
            db.insertOrThrow("notificaciones",null,notif);
            db.setTransactionSuccessful();
        } finally{db.endTransaction();}
    }

    public long[] estadoSubasta(long pubId) {
        try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT s.inicio_ms,s.fin_ms,s.precio_inicial,COALESCE((SELECT MAX(monto) FROM pujas WHERE subasta_id=s.id),0) FROM subastas s WHERE s.publicacion_id=?",new String[]{""+pubId})) {
            if(c.moveToFirst())return new long[]{c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3)};
        }
        return null;
    }
}

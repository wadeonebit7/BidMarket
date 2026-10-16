package com.example.bidmarket;

import com.example.bidmarket.Models.Publicacion;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.List;

public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.ViewHolder> {
    public interface Click {void abrir(Publicacion pub);}
    private final List<Publicacion> datos;
    private final Click click;
    public PublicacionAdapter(List<Publicacion> datos, Click click){this.datos=datos;this.click=click;}
    public void reemplazar(List<Publicacion> otros){datos.clear();datos.addAll(otros);notifyDataSetChanged();}
    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_publicacion,parent,false));}
    @Override public void onBindViewHolder(@NonNull ViewHolder h,int pos){
        Publicacion p=datos.get(pos);h.titulo.setText(p.getTitulo());h.precio.setText("CLP $"+String.format(java.util.Locale.getDefault(),"%,.0f",p.getPrecio()));
        h.condicion.setText(p.getCondicion()+("SUBASTA".equals(p.getModalidad())?" · Subasta":" · Venta directa"));
        h.imagen.setImageDrawable(null);
        if(p.getImageUrl()!=null)h.imagen.setImageURI(Uri.fromFile(new File(p.getImageUrl())));
        h.itemView.setOnClickListener(v->click.abrir(p));
    }
    @Override public int getItemCount(){return datos.size();}
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView titulo,precio,condicion;ImageView imagen;
        ViewHolder(View v){super(v);titulo=v.findViewById(R.id.tvTitulo);precio=v.findViewById(R.id.tvPrecio);condicion=v.findViewById(R.id.tvCondicion);imagen=v.findViewById(R.id.ivProducto);}
    }
}

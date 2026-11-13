package com.example.bidmarket.adapters;

import com.example.bidmarket.R;
import com.example.bidmarket.models.Publicacion;
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

/**
 * Convierte cada publicación en una tarjeta del catálogo y comunica cuál
 * eligió el usuario al tocarla.
 */
public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.ViewHolder> {
    /**
     * Recibe la publicación que el usuario selecciona.
     */
    public interface Click {
        /**
         * Recibe la publicación de la tarjeta que se tocó.
         *
         * @param pub publicación seleccionada.
         */
        void abrir(Publicacion pub);}
    private final List<Publicacion> datos;
    private final Click click;
    /**
     * Prepara el adaptador con la lista y la acción de selección.
     *
     * @param datos lista modificable con las publicaciones del catálogo.
     * @param click acción que recibe la publicación seleccionada.
     */
    public PublicacionAdapter(List<Publicacion> datos, Click click){this.datos=datos;this.click=click;}
    /**
     * Sustituye el contenido de la lista y avisa al catálogo que debe actualizarse.
     *
     * @param otros publicaciones nuevas en una lista distinta de la que usa el adaptador.
     */
    public void reemplazar(List<Publicacion> otros){datos.clear();datos.addAll(otros);notifyDataSetChanged();}
    /**
     * Crea una tarjeta a partir del diseño de una publicación.
     *
     * @param parent contenedor al que se añadirá la tarjeta.
     * @param type tipo de vista indicado por RecyclerView. Se usa el mismo diseño para todas.
     * @return objeto que conserva las vistas de la tarjeta recién creada.
     */
    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_publicacion,parent,false));}
    /**
     * Muestra el título, precio, condición, modalidad e imagen de la publicación.
     * Limpia la imagen anterior y conecta el toque con la publicación actual.
     *
     * @param h objeto que contiene las vistas de la tarjeta.
     * @param pos posición de la publicación en la lista, empezando desde cero.
     */
    @Override public void onBindViewHolder(@NonNull ViewHolder h,int pos){
        Publicacion p=datos.get(pos);h.titulo.setText(p.getTitulo());h.precio.setText("CLP $"+String.format(java.util.Locale.getDefault(),"%,.0f",p.getPrecio()));
        h.condicion.setText(p.getCondicion()+("SUBASTA".equals(p.getModalidad())?" · Subasta":" · Venta directa"));
        h.imagen.setImageDrawable(null);
        if(p.getImageUrl()!=null)h.imagen.setImageURI(Uri.fromFile(new File(p.getImageUrl())));
        h.itemView.setOnClickListener(v->click.abrir(p));
    }
    /**
     * Indica cuántas tarjetas debe mostrar el catálogo.
     *
     * @return número de publicaciones de la lista.
     */
    @Override public int getItemCount(){return datos.size();}
    /**
     * Conserva las vistas de una tarjeta para poder reutilizarla.
     */
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView titulo,precio,condicion;ImageView imagen;
        /**
         * Localiza las vistas que se rellenan al mostrar una publicación.
         *
         * @param v vista raíz de la tarjeta.
         */
        ViewHolder(View v){super(v);titulo=v.findViewById(R.id.tvTitulo);precio=v.findViewById(R.id.tvPrecio);condicion=v.findViewById(R.id.tvCondicion);imagen=v.findViewById(R.id.ivProducto);}
    }
}

package com.example.bidmarket;

import com.example.bidmarket.Models.Publicacion;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PublicacionAdapter extends RecyclerView.Adapter<PublicacionAdapter.ViewHolder> {
    private List<Publicacion> listaPublicaciones;

    public PublicacionAdapter(List<Publicacion> listaPublicaciones) {
        this.listaPublicaciones = listaPublicaciones;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Publicacion pub = listaPublicaciones.get(position);

        holder.tvTitulo.setText(pub.getTitulo());
        holder.tvPrecio.setText(String.format("%s %.2f", pub.getMoneda(), pub.getPrecio()));

        String condicion = pub.getCondicion().equals("NEW") ? "Nuevo" : "Usado";
        holder.tvCondicion.setText(condicion);

        // NOTA: Para cargar la URL de la imagen en holder.ivProducto,
        // lo ideal es usar una librería como Glide o Picasso.
        // Por ahora, dejamos el ImageView con su color por defecto.
    }

    @Override
    public int getItemCount() {
        return listaPublicaciones.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvPrecio, tvCondicion;
        ImageView ivProducto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvCondicion = itemView.findViewById(R.id.tvCondicion);
            ivProducto = itemView.findViewById(R.id.ivProducto);
        }
    }
}

package com.example.bidmarket.models;

/**
 * Representa los datos básicos de una publicación que se muestra en el catálogo.
 */
public class Publicacion {
    private int id;
    private String titulo;
    private double precio;
    private String moneda;
    private String condicion;
    private String modalidad;
    private String imageUrl; // Tomaremos la imagen con display_order = 1

    /**
     * Crea una publicación con los datos que se muestran en el catálogo.
     *
     * @param id identificador de la publicación.
     * @param titulo título que se muestra en el catálogo.
     * @param precio precio publicado.
     * @param moneda código de la moneda del precio, como {@code CLP}.
     * @param condicion estado físico del producto, como nuevo o usado.
     * @param imageUrl ruta o URL de la imagen de portada, o {@code null} si no hay imagen.
     * @param modalidad modalidad de la publicación, como venta directa o subasta.
     */
    public Publicacion(int id, String titulo, double precio, String moneda, String condicion, String imageUrl, String modalidad) {
        this.id = id;
        this.titulo = titulo;
        this.precio = precio;
        this.moneda = moneda;
        this.condicion = condicion;
        this.imageUrl = imageUrl;
        this.modalidad = modalidad;
    }

    // Agrega los Getters para cada propiedad
    /**
     * Obtiene el identificador de la publicación.
     *
     * @return ID de la publicación.
     */
    public int getId() { return id; }
    /**
     * Obtiene el título para mostrar en el catálogo.
     *
     * @return título de la publicación.
     */
    public String getTitulo() { return titulo; }
    /**
     * Obtiene el precio sin formato.
     *
     * @return precio en la moneda de la publicación.
     */
    public double getPrecio() { return precio; }
    /**
     * Indica en qué moneda se expresa el precio.
     *
     * @return código de la moneda.
     */
    public String getMoneda() { return moneda; }
    /**
     * Indica el estado físico del producto.
     *
     * @return condición del producto.
     */
    public String getCondicion() { return condicion; }
    /**
     * Obtiene la referencia a la portada.
     *
     * @return ruta o URL de la imagen con {@code display_order = 1}, o {@code null} si no hay imagen.
     */
    public String getImageUrl() { return imageUrl; }
    /**
     * Indica si la publicación es de venta directa o subasta.
     *
     * @return valor que identifica la modalidad de la publicación.
     */
    public String getModalidad() { return modalidad; }
}

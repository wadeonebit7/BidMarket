package com.example.bidmarket.Models;

public class Publicacion {
    private int id;
    private String titulo;
    private double precio;
    private String moneda;
    private String condicion;
    private String imageUrl; // Tomaremos la imagen con display_order = 1

    public Publicacion(int id, String titulo, double precio, String moneda, String condicion, String imageUrl) {
        this.id = id;
        this.titulo = titulo;
        this.precio = precio;
        this.moneda = moneda;
        this.condicion = condicion;
        this.imageUrl = imageUrl;
    }

    // Agrega los Getters para cada propiedad
    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public double getPrecio() { return precio; }
    public String getMoneda() { return moneda; }
    public String getCondicion() { return condicion; }
    public String getImageUrl() { return imageUrl; }
}

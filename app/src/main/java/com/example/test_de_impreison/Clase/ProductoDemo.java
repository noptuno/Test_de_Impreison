package com.example.test_de_impreison.Clase;

public class ProductoDemo {

    private Integer idProducto;
    private String codigoProducto;
    private String codigoEan;
    private String descripcion;
    private String precio;


    public ProductoDemo() {
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getCodigoEan() {
        return codigoEan;
    }

    public void setCodigoEan(String codigoEan) {
        this.codigoEan = codigoEan;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }



    public ProductoDemo(String codigoProducto, String codigoEan, String descripcion, String precio) {
        this.codigoProducto = codigoProducto;
        this.codigoEan = codigoEan;
        this.descripcion = descripcion;
        this.precio = precio;

    }

    public String toString() {
        return "Producto{" +
                "idproducto=" +         idProducto +
                ", codigoProducto='" +  codigoProducto + '\'' +
                ", codigoEan='" +       codigoEan + '\'' +
                ", descripcion='" +     descripcion + '\'' +
                ", precio='" +        precio + '\'' +
                '}';
    }
}

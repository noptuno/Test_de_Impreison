package com.example.test_de_impreison.BaseDatos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.test_de_impreison.Clase.Producto;
import com.example.test_de_impreison.Clase.ProductoDemo;

import java.util.ArrayList;


public class ProductosDBdemo {

        private SQLiteDatabase db;
        private ProductosDBdemo.DBHelper dbHelper;

        public ProductosDBdemo(Context context) {
            dbHelper = new ProductosDBdemo.DBHelper(context);
        }


        private void openReadableDB() {
            db = dbHelper.getReadableDatabase();
        }

        private void openWriteableDB() {
            db = dbHelper.getWritableDatabase();
        }

        private void closeDB() {
            if (db != null) {
                db.close();
            }
        }

    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {

            super(context, ConstantsDB.DB_NAME, null, ConstantsDB.DB_VERSION);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(ConstantsDB.TABLA_PRODUCTO_SQL);
            db.execSQL(ConstantsDB.TABLA_LIST_PRODUCTO_SQL);
            db.execSQL(ConstantsDB.TABLA_PRODUCTOAPI_SQL);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private ContentValues clienteMapperContentValuesDemo(ProductoDemo producto) {

        ContentValues cv = new ContentValues();
        cv.put(ConstantsDB.PRO_IDPRODUCTOAPI, producto.getIdProducto());
        cv.put(ConstantsDB.PRO_CODIGOPRODUCTOAPI, producto.getCodigoProducto());
        cv.put(ConstantsDB.PRO_CODIGOEANAPI, producto.getCodigoEan());
        cv.put(ConstantsDB.PRO_DESCRIPCIONAPI, producto.getDescripcion());
        cv.put(ConstantsDB.PRO_PRECIOAPI, producto.getPrecio());
        return cv;

    }

    public ProductoDemo Buscar_tabla_producto(String codigoetiqueta) {

        boolean error ;

        ProductoDemo producto = new ProductoDemo();
        this.openReadableDB();
        String where = ConstantsDB.PRO_CODIGOPRODUCTOAPI+ "= ?" + " OR " + ConstantsDB.PRO_CODIGOEANAPI + "= ?" ;
        String[] campos = new String[]{ConstantsDB.PRO_IDPRODUCTOAPI,ConstantsDB.PRO_CODIGOPRODUCTOAPI,ConstantsDB.PRO_CODIGOEANAPI, ConstantsDB.PRO_DESCRIPCIONAPI, ConstantsDB.PRO_PRECIOAPI};
        String[] whereArgs = {codigoetiqueta,codigoetiqueta};

        Cursor c = db.query(ConstantsDB.TABLA_PRODUCTOAPI, campos, where, whereArgs, null, null, null, null);

        try{
            if( c != null ) {
                c.moveToFirst();
                producto.setIdProducto(c.getInt(0));
                producto.setCodigoProducto(c.getString(1));
                producto.setCodigoEan(c.getString(2));
                producto.setDescripcion(c.getString(3));
                producto.setPrecio(c.getString(4));

                c.close();
                error = false;
            }else{
                error = true;
            }

        }catch (Exception ex){
            Log.e("ProductoDB," +
                    "", "error al leer");
            error = true;
        }

        if (error){
            return null;
        }else {
            return producto;
        }
    }
    public ProductoDemo loadProductoDemo2(String codigoetiqueta) {

        ProductoDemo producto = new ProductoDemo();
        this.openReadableDB();
        String[] campos = new String[]{ConstantsDB.PRO_IDPRODUCTOAPI,ConstantsDB.PRO_CODIGOPRODUCTOAPI,ConstantsDB.PRO_CODIGOEANAPI, ConstantsDB.PRO_DESCRIPCIONAPI, ConstantsDB.PRO_PRECIOAPI};
        Cursor c = db.query(ConstantsDB.TABLA_PRODUCTOAPI, campos, null, null, null, null, null);

        try {
            while (c.moveToNext()) {

                producto.setIdProducto(c.getInt(0));
                producto.setCodigoProducto(c.getString(1));
                producto.setCodigoEan(c.getString(2));
                producto.setDescripcion(c.getString(3));
                producto.setPrecio(c.getString(4));

            }
        } finally {
            c.close();
        }
        this.closeDB();
        return producto;
    }


    public ArrayList loadProductoDemo() {

        ArrayList<ProductoDemo> list = new ArrayList<>();
        this.openReadableDB();
        String[] campos = new String[]{ConstantsDB.PRO_IDPRODUCTOAPI,ConstantsDB.PRO_CODIGOPRODUCTOAPI,ConstantsDB.PRO_CODIGOEANAPI, ConstantsDB.PRO_DESCRIPCIONAPI, ConstantsDB.PRO_PRECIOAPI};
        Cursor c = db.query(ConstantsDB.TABLA_PRODUCTOAPI, campos, null, null, null, null, null);

        try {
            while (c.moveToNext()) {
                ProductoDemo producto = new ProductoDemo();
                producto.setIdProducto(c.getInt(0));
                producto.setCodigoProducto(c.getString(1));
                producto.setCodigoEan(c.getString(2));
                producto.setDescripcion(c.getString(3));
                producto.setPrecio(c.getString(4));
                list.add(producto);
            }
        } finally {
            c.close();
        }
        this.closeDB();
        return list;
    }

    public long insertarProductoDemo(ProductoDemo producto) {
        this.openWriteableDB();
        long rowID = db.insert(ConstantsDB.TABLA_PRODUCTOAPI, null, clienteMapperContentValuesDemo(producto));
        this.closeDB();
        return rowID;
    }

    public Boolean validarDemo(String codigo) {

        boolean error= false;
        this.openReadableDB();
        String where = ConstantsDB.PRO_CODIGOPRODUCTOAPI+ "= ?";

        Cursor c = db.query(ConstantsDB.TABLA_PRODUCTOAPI, null, where, new String[]{codigo}, null, null, null, null);
        try{
            if( c.getCount()>0) {
                c.close();
                error = true;
            }

        }catch (Exception ex){
            Log.e("valiando" + "", "error al leer");
            error = true;
        }

        return error;
    }




    public void eliminarAllDemo() {
        this.openWriteableDB();
        db.delete(ConstantsDB.TABLA_PRODUCTOAPI, null, null);
        this.closeDB();

    }



    }



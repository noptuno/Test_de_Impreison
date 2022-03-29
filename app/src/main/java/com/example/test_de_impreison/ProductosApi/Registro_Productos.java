package com.example.test_de_impreison.ProductosApi;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.test_de_impreison.Adapter.AdapterProductoDemo;
import com.example.test_de_impreison.BaseDatos.ProductosDB;
import com.example.test_de_impreison.BaseDatos.ProductosDBdemo;
import com.example.test_de_impreison.Clase.ProductoDemo;
import com.example.test_de_impreison.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class Registro_Productos extends AppCompatActivity {
    private AdapterProductoDemo adapter;
    private ProductosDBdemo db;
    private Button Cargar, Eliminar;
    private Handler m_handler = new Handler();
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro__productos);

        Cargar = findViewById(R.id.btn_cargar);
        Cargar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try{
                            AbrirDialog(true,"Cargando Datos...");
                            cargarscv();
                            cargarListaRunable();
                            AbrirDialog(false,"Cargando Datos...");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        });


        Eliminar = findViewById(R.id.btn_eliminar);
        Eliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try{

                            AbrirDialog(true,"Cargando Datos...");
                            eliminarRunable();
                            cargarListaRunable();
                            AbrirDialog(false,"Cargando Datos...");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        });


        adapter = new AdapterProductoDemo();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recicler_view_productos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        cargarLista();

    }

    public void AbrirDialog (final boolean value, final String mensaje) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                if (value){
                    createCancelProgressDialog("Cargando...", mensaje, "Cancelar");
                }else{
                    if (dialog!=null)
                        dialog.dismiss();
                }
            }
        });
    }


    private void createCancelProgressDialog(String title, String message, String buttonText)
    {
        dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButton(buttonText, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {

            }
        });
        dialog.show();
    }

    public void DisplayToastNormal(final String mensaje) {

        Toast.makeText(Registro_Productos.this, mensaje, Toast.LENGTH_SHORT).show();

    }

    private void cargarscv() {

        InputStream is = getResources().openRawResource(R.raw.iistaarticulosagostoandroid);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String line = "";
        try {
            // step over header
            reader.readLine();
            while ((line = reader.readLine()) != null) {

                String[] tokens = line.split(",");
                ProductoDemo productocsv = new ProductoDemo();
                productocsv.setCodigoProducto(tokens[0].trim());
                productocsv.setCodigoEan(tokens[2].trim());
                productocsv.setDescripcion(tokens[1].trim());
                productocsv.setPrecio(tokens[3]);


                if (!valdiarproducto(productocsv.getCodigoProducto(), productocsv.getCodigoEan())) {
                    if (registrarProdcuto(productocsv.getCodigoProducto(), productocsv.getCodigoEan(), productocsv.getDescripcion(), productocsv.getPrecio())) {

                    }
                }
                Log.d("basedatos", "base" + tokens);
            }

        } catch (IOException e) {
            Log.wtf("Myactivity", "error" + line, e);
            e.printStackTrace();
        }
    }

    private boolean valdiarproducto(String codigoproducto, String codigoean) {
        boolean validar = false;

        try {
            db = new ProductosDBdemo(Registro_Productos.this);
            if(db.validarDemo(codigoproducto)){
                validar = true;
            }

        } catch (Exception e) {
            Log.e("error", "mensajeA");
            validar = false;
        }

        return validar;
    }

    public boolean registrarProdcuto(String codigoproducto, String codigoean, String descripcion, String precio) {

        try {
            db = new ProductosDBdemo(Registro_Productos.this);
            ProductoDemo producto = new ProductoDemo(codigoproducto, codigoean, descripcion, precio);
            db.insertarProductoDemo(producto);
            return true;

        } catch (Exception e) {
            Log.e("error", "mensajeb");
            return false;
        }
    }

    private void cargarListaRunable() {

        m_handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    db = new ProductosDBdemo(Registro_Productos.this);
                    ArrayList<ProductoDemo> list = db.loadProductoDemo();
                    for (ProductoDemo producto : list) {

                        Log.i("---> Base de datos: ", producto.toString());

                    }
                    adapter.setNotes(list);
                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e("error", "mensajec");
                }
            }
        });
    }

    private void cargarLista() {

                try {
                    db = new ProductosDBdemo(Registro_Productos.this);
                    ArrayList<ProductoDemo> list = db.loadProductoDemo();
                    for (ProductoDemo producto : list) {
                        Log.i("---> Base de datos: ", producto.toString());

                    }
                    adapter.setNotes(list);
                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e("error", "mensajed");
                }
    }

    private void eliminarRunable() {


        m_handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    db = new ProductosDBdemo(Registro_Productos.this);
                    ArrayList<ProductoDemo> list = db.loadProductoDemo();
                    db = new ProductosDBdemo(Registro_Productos.this);
                    db.eliminarAllDemo();
                    adapter.setNotes(list);
                    adapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Log.e("error", "mensajee");
                }
            }
        });
    }

}

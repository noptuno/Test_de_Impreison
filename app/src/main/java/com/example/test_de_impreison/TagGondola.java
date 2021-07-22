package com.example.test_de_impreison;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.test_de_impreison.Adapter.ProductoAdapter;
import com.example.test_de_impreison.BaseDatos.ProductosDB;
import com.example.test_de_impreison.Clase.Producto;
import com.example.test_de_impreison.Clase.Producto2;
import com.example.test_de_impreison.configuracion.DMRPrintSettings;
import com.example.test_de_impreison.sdkstar.Communication;

import com.google.gson.Gson;


import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.StarIoExt;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;



import honeywell.connection.ConnectionBase;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class TagGondola extends AppCompatActivity {

    String ultimocodigo;
    private OkHttpClient Pickinghttp;
    private Request RequestPicking;

    ConnectionBase conn = null;
    String ApplicationConfigFilename = "applicationconfigg.dat";
    private String m_printerMode = null;
    private String m_printerMAC = null;
    private String m_ip = null;
    private String m_sucursal;
    private TextView txtsucursal;
    private String password;
    private SharedPreferences sharedPref;
    private int m_printerPort = 515;
    private static final int REQUEST_PICK_CONFIGURACION = 2;
    DMRPrintSettings g_appSettings = new DMRPrintSettings("", 0, 0, "0", "0", "0", 0);
    private int m_configurado;
    private Handler m_handler = new Handler(); // Main thread
    byte[] printData;
    private Context mContext;
    private boolean mIsForeground;
    private ProgressDialog mProgressDialog;
    private int a = 1;
    private Button btnagregar;
    private Button btnprintall;
    private ProductoAdapter adapter;
    private ProductosDB db;

    private ArrayList<Producto> list;

    private Producto lista_producto_seleccionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_gondola);

        mProgressDialog = new ProgressDialog(TagGondola.this);
        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);


        btnagregar = findViewById(R.id.btnagregar);

        btnagregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //cargarproducto();

                String a = "{\n" +
                        "\"Codigo\": \"250490000\",\n" +
                        "\"Nombre\": \"YERBA COMPUESTA SERENA CANARIA 1.00 K\",\n" +
                        "\"Moneda\": \"moneda\",\n" +
                        "\"Precio\": \"195.00\",\n" +
                        "\"CodBarras\": \"7730241010294\",\n" +
                        "\"Prioridad\": \"C\",\n" +
                        "\"Aux\": \"\"\n" +
                        "}";

                cargarproductojson(a);

            }
        });

        btnprintall = findViewById(R.id.btnimprimirall);

        btnprintall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printall();
            }
        });


        adapter = new ProductoAdapter();

        adapter.setOnNoteSelectedListener(new ProductoAdapter.OnNoteSelectedListener() {
            @Override
            public void onClick(final Producto producto) {
                                print(producto);
            }
        });



        RecyclerView recyclerView = findViewById(R.id.reciclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        cargarLista();


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                try {
                    db = new ProductosDB(TagGondola.this);
                    db.eliminarProducto(adapter.getNoteAt(viewHolder.getAdapterPosition()).getCodigoProducto());
                    cargarLista();
                } catch (Exception e) {

                }
            }
        }).attachToRecyclerView(recyclerView);



    }

    @Override
    public void onPause() {
        super.onPause();

        mIsForeground = false;
    }



    private void cargarproducto(){

        try {
            db = new ProductosDB(TagGondola.this);
            a++;
            Producto prod = new Producto("Descripción producto " + a,"Descripcion secundaria"+ a,"123456789" + a,"CodBarras"+ a,"120","Stock","NO","Producto","Suc","Mensaje","Estado","Off_available","Txt_oferta");
            db.insertarProducto(prod);
            cargarLista();

        } catch (Exception e) {
            Log.e("errorXMLA", "mensaje");
        }
    }

    private void cargarproductojson(String response) {


        Gson g = new Gson();

        Producto2 p = g.fromJson(response, Producto2.class);


        Log.d("TAG1111", p.getCodBarras());
        //db = new ProductosDB(TagGondola.this);

        //db.insertarProducto(p);
        cargarLista();
    }





    public void requesthttptest(final String codigoverificar) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final String codigocaptrado = codigoverificar;

                if (!ultimocodigo.equals(codigocaptrado)) {

                    Pickinghttp = new OkHttpClient();
                    MediaType mediaType = MediaType.parse("text/xml");

                    RequestBody body = RequestBody.create(mediaType, "");

                    RequestPicking = new Request.Builder()
                            .url("https://precioonline.tata.com.uy/articulos.php?tipocodigo=2&codigo=7730241010294")
                            .method("POST", body)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    //cancelar dialog

                    Pickinghttp.newCall(RequestPicking).enqueue(new Callback() {

                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            //cancelar dialog
                            //conexion fallo
                        }

                        @Override
                        public void onResponse(Call call, final Response response) throws IOException {

                            if (response.isSuccessful()) {

                                try {

                                    final String myResponse = response.body().string();

                                    Gson g = new Gson();

                                    Producto p = g.fromJson(myResponse, Producto.class);

                                } catch (IOException e) {

                                    e.printStackTrace();

                                }
                                //cerrar dialog

                            } else {

                               //error conexion
                                //cerrar dialog

                            }
                        }

                    });

                } else {
                   //ya consulto este codigo
                }

            }

        });

    }

    private void print(Producto producto) {

        mProgressDialog.show();
        lista_producto_seleccionada = producto;
        Charset encoding = Charset.forName("CP437");

        byte[] nombreproducto= lista_producto_seleccionada.getDescArticulo_1().getBytes(encoding);
        byte[] descripcionproducto = lista_producto_seleccionada.getDescArticulo_2().getBytes(encoding);
        byte[] codigointerno = ("Codigo Interno: "+lista_producto_seleccionada.getCodigoProducto().toString()).getBytes(encoding);
        byte[] codigobarra = ("1234567895215").getBytes();
        byte[] precio = ("$:"+producto.getPrecio().toString()).getBytes();
        ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.StarPRNTL);

        builder.beginDocument();
        //builder.appendTopMargin(1);
        builder.appendCodePage(ICommandBuilder.CodePageType.CP437);

        //*********************************
        builder.appendAlignment(ICommandBuilder.AlignmentPosition.Left);

        builder.appendAbsolutePosition(nombreproducto,10);
        builder.appendLineFeed();
        builder.appendLineSpace(24);

        builder.appendAbsolutePosition(descripcionproducto,10);
        builder.appendLineFeed();

        builder.appendAbsolutePosition(codigointerno,10);
        builder.appendMultiple(2, 3);
        builder.appendAbsolutePosition(precio,250);
        builder.appendLineFeed();

        builder.appendAlignment(ICommandBuilder.AlignmentPosition.Center);
        builder.appendBarcodeWithAbsolutePosition(codigobarra, ICommandBuilder.BarcodeSymbology.JAN13, ICommandBuilder.BarcodeWidth.Mode1, 50, false, 10);

        //**********************
        builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);
        builder.endDocument();

        printData = builder.getCommands();

        Communication.sendCommands(this, printData, "BT:" + m_printerMAC, "Portable", 10000, 30000, TagGondola.this, mCallback);     // 10000mS!!!
    }


    private void printall() {
        mProgressDialog.show();

        Charset encoding = Charset.forName("CP437");

        ICommandBuilder builder = StarIoExt.createCommandBuilder(StarIoExt.Emulation.StarPRNTL);
        for (Producto prod : list) {

            byte[] nombreproducto= (prod.getDescArticulo_1()).getBytes(encoding);
            byte[] descripcionproducto = (prod.getDescArticulo_2()).getBytes(encoding);
            byte[] precio = ("$:"+prod.getPrecio()).getBytes();
            byte[] codigointerno = ("Codigo Interno: "+prod.getCodigoProducto()).getBytes(encoding);
            byte[] codigobarra = "1234567895215".getBytes();

            builder.beginDocument();
            builder.appendTopMargin(1);
            builder.appendCodePage(ICommandBuilder.CodePageType.CP437);

            //*********************************
            builder.appendAlignment(ICommandBuilder.AlignmentPosition.Left);

            builder.appendAbsolutePosition(nombreproducto,10);
            builder.appendLineFeed();
            builder.appendLineSpace(24);

            builder.appendAbsolutePosition(descripcionproducto,10);
            builder.appendLineFeed();


            builder.appendAbsolutePosition(codigointerno,10);
            builder.appendMultiple(2, 3);
            builder.appendAbsolutePosition(precio,250);
            builder.appendLineFeed();

            builder.appendAlignment(ICommandBuilder.AlignmentPosition.Center);
            builder.appendBarcodeWithAbsolutePosition(codigobarra, ICommandBuilder.BarcodeSymbology.JAN13, ICommandBuilder.BarcodeWidth.Mode1, 50, false, 10);
            builder.appendLineFeed();
            //*****************************************

            builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed);

        }

        builder.endDocument();
        printData = builder.getCommands();

        Communication.sendCommands(this, printData, "BT:" + m_printerMAC, "Portable", 10000, 30000, TagGondola.this, mCallback);     // 10000mS!!!

    }



    private final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(Communication.CommunicationResult communicationResult) {

            String a =  communicationResult.getResult().toString();;
            Log.e("Mensaje", a);

            if (a.equals("Success")){

                Toast.makeText(TagGondola.this,
                        "Impresión Correcta",
                        Toast.LENGTH_SHORT).show();

            }else if (a.equals("ErrorOpenPort")){

                Toast.makeText(TagGondola.this,
                        "Error por Bluetooth",
                        Toast.LENGTH_SHORT).show();
            }else if(a.equals("ErrorBeginCheckedBlock")){

                Toast.makeText(TagGondola.this,
                        "Error Tapa Abierta",
                        Toast.LENGTH_SHORT).show();
            }else if(a.equals("ErrorEndCheckedBlock")) {

                Toast.makeText(TagGondola.this,
                        "Error por interrupción",
                        Toast.LENGTH_SHORT).show();

            }else{

                Toast.makeText(TagGondola.this,
                        "Error Desconocido",
                        Toast.LENGTH_SHORT).show();
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                Log.e("mensaje", "cerrar dialog");
            }

        }
    };



    private void cargarLista() {
        try {
            db = new ProductosDB(TagGondola.this);
            list = db.loadProducto();
            adapter.setNotes(list);
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            Log.e("errorW", "mensaje");
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        cargardatos();
        mIsForeground = true;
    }

    public void cargardatos() {


        DMRPrintSettings appSettings = ReadApplicationSettingFromFile();

        if (appSettings != null) {

            g_appSettings = appSettings;
            m_printerMAC = g_appSettings.getPrinterMAC();
            m_printerMode = g_appSettings.getSelectedPrintMode();//2018 PH
            m_sucursal = g_appSettings.getSuc();
            m_ip = g_appSettings.getIpwebservice();
            m_configurado = g_appSettings.getConfigurado();

        } else {

            Toast.makeText(TagGondola.this,
                    "Debe configurar las opciones de la aplicación",
                    Toast.LENGTH_SHORT).show();

        }

    }

    DMRPrintSettings ReadApplicationSettingFromFile() {
        DMRPrintSettings ret = null;
        InputStream instream;

        try {

            instream = openFileInput(ApplicationConfigFilename);

        } catch (FileNotFoundException e) {

            Log.e("DOPrint", e.getMessage(), e);
            showToast("Configurar");

            return null;
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(instream);

            try {

                ret = (DMRPrintSettings) ois.readObject();

            } catch (ClassNotFoundException e) {

                Log.e("DOPrint", e.getMessage(), e);
                ret = null;

            }

        } catch (Exception e) {

            Log.e("DOPrint", e.getMessage(), e);
            ret = null;

        } finally {

            try {

                if (instream != null)
                    instream.close();

            } catch (IOException ignored) {
            }

        }
        return ret;

    }

    public void showToast(final String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
    }


}
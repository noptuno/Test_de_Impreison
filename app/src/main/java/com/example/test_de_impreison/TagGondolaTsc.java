package com.example.test_de_impreison;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test_de_impreison.Adapter.ProductoAdapter;
import com.example.test_de_impreison.BaseDatos.ProductosDB;
import com.example.test_de_impreison.BaseDatos.ProductosDBdemo;
import com.example.test_de_impreison.Clase.Producto;
import com.example.test_de_impreison.Clase.ProductoDemo;
import com.example.test_de_impreison.ProductosApi.Registro_Productos;
import com.example.test_de_impreison.configuracion.DMRPrintSettings;
import com.example.test_de_impreison.sdkstar.Communication;
import com.example.tscdll.TSCActivity;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import honeywell.connection.ConnectionBase;


public class TagGondolaTsc extends AppCompatActivity implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {

    private com.honeywell.aidc.BarcodeReader barcodeReader;

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
    TSCActivity TscDll = new TSCActivity();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_gondolatsc);
        cargarbarcoder();
        mProgressDialog = new ProgressDialog(TagGondolaTsc.this);

        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);


        btnagregar = findViewById(R.id.btnagregar);

        btnagregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarproducto();
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
                    db = new ProductosDB(TagGondolaTsc.this);
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



    public void request(final String codigoverificar) {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {

                    ProductosDBdemo bdproducto = new ProductosDBdemo(TagGondolaTsc.this);
                  //  ProductoDemo productoActual = bdproducto.Buscar_tabla_producto(codigoverificar);
                    ProductoDemo productoActual = bdproducto.loadProductoDemo2(codigoverificar);

                    if (productoActual!=null){

                            db = new ProductosDB(TagGondolaTsc.this);
                            Producto prod = new Producto(productoActual.getDescripcion(),productoActual.getDescripcion(),productoActual.getCodigoProducto(),productoActual.getCodigoEan(),productoActual.getPrecio(),"Stock","NO","Producto","Suc","Mensaje","Estado","Off_available","Txt_oferta");
                            db.insertarProducto(prod);

                    }else{

                    }

                } catch (Exception e) {
                    e.printStackTrace();

                }
                cargarLista();
            }
        };

        thread.start();

    }






    private void cargarbarcoder() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            barcodeReader = MainActivity.getBarcodeObject();

        if (barcodeReader != null) {
            barcodeReader.addBarcodeListener(this);

            try {
                barcodeReader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE, BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
            } catch (UnsupportedPropertyException e) {
                Toast.makeText(this, "Failed to apply properties", Toast.LENGTH_SHORT).show();
            }

            barcodeReader.addTriggerListener(this);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
            properties.put(BarcodeReader.PROPERTY_UPC_A_CHECK_DIGIT_TRANSMIT_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_UPC_E_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_UPC_E_CHECK_DIGIT_TRANSMIT_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_GOOD_READ_ENABLED, true);
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_VIBRATE_ENABLED, true);
            barcodeReader.setProperties(properties);

        }
    }

    private void cargarproducto(){

        try {
            db = new ProductosDB(TagGondolaTsc.this);
            a++;
            Producto prod = new Producto("Descripción producto " + a,"Descripcion secundaria"+ a,"123456789" + a,"CodBarras"+ a,"120","Stock","NO","Producto","Suc","Mensaje","Estado","Off_available","Txt_oferta");
            db.insertarProducto(prod);
            cargarLista();

        } catch (Exception e) {
            Log.e("errorXMLA", "mensaje");
        }


    }




    private void print(final Producto producto) {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    EnableDialog(true, "Enviando Documento...",true);
                    lista_producto_seleccionada = producto;

                    TscDll.openport(m_printerMAC);
                    TscDll.setup(76, 35, 2, 1, 1, 4, 0);
                    TscDll.clearbuffer();
                    TscDll.sendcommand("CODEPAGE UTF-8\n");
                    TscDll.sendcommand("SET TEAR ON\n");
                    TscDll.sendcommand("DIRECTION 0\n");
                    TscDll.sendcommand("TEXT 70,20,\"arial_na.TTF\",0,11,11,\"" + lista_producto_seleccionada.getDescArticulo_1() + "\"\n");
                    TscDll.sendcommand("TEXT 70,55,\"arial_na.TTF\",0,08,08,\"" + lista_producto_seleccionada.getDescArticulo_2() + "\"\n");
                    TscDll.sendcommand("TEXT 280,100,\"arial_bl.TTF\",0,20,20,\"" + "$ " + lista_producto_seleccionada.getPrecio() + "\"\n");
                    TscDll.sendcommand("TEXT 400,190,\"arial_na.TTF\",0,11,11,\"" + lista_producto_seleccionada.getCodProd() + "\"\n");
                    TscDll.barcode(70, 180, "128", 30, 1, 0, 1, 1, lista_producto_seleccionada.getCodBarras());
                    TscDll.printlabel(1, 1);
                    TscDll.closeport(500);
                    EnableDialog(false, "Enviando terminando...",false);

                } catch (Exception e) {
                    e.printStackTrace();
                    EnableDialog(false, "Enviando terminando...",false);
                }

            }
        };

        thread.start();
    }


    private void printall() {


            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        EnableDialog(true, "Enviando Documento...",true);

                        TscDll.openport(m_printerMAC);
                        TscDll.setup(76, 35, 2, 1, 1, 4, 0);

                        for (final Producto prod : list) {

                            lista_producto_seleccionada = prod;

                            TscDll.clearbuffer();
                            TscDll.sendcommand("CODEPAGE UTF-8\n");
                            TscDll.sendcommand("SET TEAR ON\n");
                            TscDll.sendcommand("DIRECTION 0\n");
                            TscDll.sendcommand("TEXT 70,20,\"arial_na.TTF\",0,11,11,\"" + lista_producto_seleccionada.getDescArticulo_1() + "\"\n");
                            TscDll.sendcommand("TEXT 70,55,\"arial_na.TTF\",0,08,08,\"" + lista_producto_seleccionada.getDescArticulo_2() + "\"\n");
                            TscDll.sendcommand("TEXT 280,100,\"arial_bl.TTF\",0,20,20,\"" + "$ " + lista_producto_seleccionada.getPrecio() + "\"\n");
                            TscDll.sendcommand("TEXT 400,190,\"arial_na.TTF\",0,11,11,\"" + lista_producto_seleccionada.getCodProd() + "\"\n");
                            TscDll.barcode(70, 180, "128", 30, 1, 0, 1, 1, lista_producto_seleccionada.getCodBarras());
                            TscDll.printlabel(1, 1);

                            EnableDialog(false, "Enviando terminando...",false);
                        }
                        TscDll.closeport(500);

                    } catch (Exception e) {
                        e.printStackTrace();
                        EnableDialog(false, "Enviando terminando...",false);
                    }

                }
            };

            thread.start();





    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent event) {
        request(event.getBarcodeData());

    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {
        DisplayPrintingStatusMessage("Codigo no valido");
    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {
        DisplayPrintingStatusMessage("verificar excepcion");
    }

    public void DisplayPrintingStatusMessage(final String MsgStr) {

        m_handler.post(new Runnable() {
            public void run() {
                showToast(MsgStr);//2018 PH
            }// run()
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (barcodeReader != null) {
            barcodeReader.removeBarcodeListener(this);
            barcodeReader.removeTriggerListener(this);
        }
    }


    private ProgressDialog dialog;
    public void EnableDialog(final boolean value, final String mensaje, final Boolean cancelar) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                if (value) {
                    createCancelProgressDialog("Cargando: ", mensaje, "Cancelar",cancelar);
                } else {
                    if (dialog != null)
                        dialog.dismiss();
                }
            }
        });
    }

    private void createCancelProgressDialog(String title, String message, String buttonText,Boolean cancelar) {
        dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        if (cancelar) {
            dialog.setButton(buttonText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                }
            });
        }
        dialog.show();
    }

    private final Communication.SendCallback mCallback = new Communication.SendCallback() {
        @Override
        public void onStatus(Communication.CommunicationResult communicationResult) {

            String a =  communicationResult.getResult().toString();;
            Log.e("Mensaje", a);

            if (a.equals("Success")){

                Toast.makeText(TagGondolaTsc.this,
                        "Impresión Correcta",
                        Toast.LENGTH_SHORT).show();

            }else if (a.equals("ErrorOpenPort")){

                Toast.makeText(TagGondolaTsc.this,
                        "Error por Bluetooth",
                        Toast.LENGTH_SHORT).show();
            }else if(a.equals("ErrorBeginCheckedBlock")){

                Toast.makeText(TagGondolaTsc.this,
                        "Error Tapa Abierta",
                        Toast.LENGTH_SHORT).show();
            }else if(a.equals("ErrorEndCheckedBlock")) {

                Toast.makeText(TagGondolaTsc.this,
                        "Error por interrupción",
                        Toast.LENGTH_SHORT).show();

            }else{

                Toast.makeText(TagGondolaTsc.this,
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
            db = new ProductosDB(TagGondolaTsc.this);
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

        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }
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

            Toast.makeText(TagGondolaTsc.this,
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
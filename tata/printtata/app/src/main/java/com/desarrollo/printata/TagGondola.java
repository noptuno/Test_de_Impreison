package com.desarrollo.printata;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.desarrollo.printata.Adapter.ProductoAdapter;
import com.desarrollo.printata.BaseDatos.ProductosDB;
import com.desarrollo.printata.Clases.Producto;
import com.desarrollo.printata.ParsearXML.ParserXml;
import com.example.tscdll.TSCActivity;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;
import com.honeywell.aidc.TriggerStateChangeEvent;
import com.honeywell.aidc.UnsupportedPropertyException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TagGondola extends AppCompatActivity implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener, Runnable {

    private com.honeywell.aidc.BarcodeReader barcodeReader;
    private Handler m_handler = new Handler(); // Main thread
    private TextView textView2;
    private EditText codigomanual;
    private Button buscarcodigomaual;
    TSCActivity TscDll = new TSCActivity();
    private String sucursal;
    private String m_printerMAC = null;
    private String m_ip = null;
    private ProductoAdapter adapter;
    private ProductosDB db;
    private Button boton, buttonxml;
    private String ultimocodigo = "ultimocodigo";
    private boolean tagimprimirall = false;
    private OkHttpClient Pickinghttp;
    private Request RequestPicking;
    private Boolean continuarimprimiendo;
    private Boolean cancelarrun = false;
    private ArrayList<Producto> list;
    private Producto lista_producto_seleccionada;
    private ProgressDialog dialog;
    private boolean imprimirofertas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_gondola);

        cargarbarcoder();

        cargardatospreference();

        botonesyadapter();

        cargarLista();

    }

    private void botonesyadapter() {

        codigomanual = findViewById(R.id.txt_codigomanual);
        buscarcodigomaual = findViewById(R.id.btn_buscarcodigomanual);

        codigomanual.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean procesado = false;

                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    presionarboton();

                    procesado = true;
                }
                return procesado;
            }
        });

        buscarcodigomaual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                presionarboton();
            }
        });

        buttonxml = findViewById(R.id.buttonxml);
        boton = findViewById(R.id.button);
        buttonxml.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CargarPicking();
            }
        });

        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    db = new ProductosDB(TagGondola.this);
                    Producto pro = new Producto("PRODUCTO DE PRUEBA TAMAÑOS", "SEGUNDA DESCRIPCION DEL PRODUCTO", "116663", "1111690712025", "12.000", "6", "7", "8", "9", "10", "11", "N", "OFERTA");
                    db.insertarProducto(pro);
                    cargarLista();
                } catch (Exception e) {
                    Log.e("errorO", "mensaje");
                }
            }
        });

        adapter = new ProductoAdapter();
        adapter.setOnNoteSelectedListener(new ProductoAdapter.OnNoteSelectedListener() {
            @Override
            public void onClick(final Producto producto) {

                lista_producto_seleccionada = producto;

                new Thread(TagGondola.this, "PrintingTask").start();
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.reciclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

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
                    ultimocodigo = "ultimocodigo";
                    cargarLista();
                } catch (Exception e) {

                }
            }
        }).attachToRecyclerView(recyclerView);
    }


    private void cargardatospreference() {
        Bundle parametros = getIntent().getExtras();
        if (parametros != null) {
            sucursal = (parametros.getString("suc"));
            m_printerMAC = (parametros.getString("mac"));
            m_ip = (parametros.getString("ip"));

        } else {
            Toast.makeText(getApplicationContext(), "No hay datos a mostrar", Toast.LENGTH_LONG).show();
        }
    }

    private void cargarbarcoder() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        barcodeReader = MenuPrincipal.getBarcodeObject();

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


    private void presionarboton() {

        String codigoimprimir = codigomanual.getText().toString().trim();
        if (!codigoimprimir.equals("")) {
            imprimirCodigo(codigoimprimir);
            codigomanual.setText("");
            ocultarteclado();
        }
        if (codigomanual.isFocused()) {
            ocultarteclado();
        }
    }

    public void ocultarteclado() {

        View view = this.getCurrentFocus();

        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();   
                Toast.makeText(this, "Scanner unavailable", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void imprimirCodigo(final String codigoverificar) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final String codigocaptrado = codigoverificar;

                if (!ultimocodigo.equals(codigocaptrado)) {

                    Pickinghttp = new OkHttpClient();
                    MediaType mediaType = MediaType.parse("text/xml");

                    RequestBody body = RequestBody.create(mediaType,
                            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" +
                                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                                    "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n " +
                                    " <soap:Body>\r\n  " +
                                    "  <ObtenerDatosArticuloEtiquetas xmlns=\"http://tempuri.org/\">\r\n " +
                                    "     <p_suc>" + sucursal + "</p_suc>\r\n    " +
                                    "  <p_producto>" + codigocaptrado + "</p_producto>\r\n " +
                                    "   </ObtenerDatosArticuloEtiquetas>\r\n " +
                                    " </soap:Body>\r\n" +
                                    "</soap:Envelope>\r\n\r\n");

                    RequestPicking = new Request.Builder()
                            .url("http://" + m_ip + "/WSSREtiquetas/EtiquetaService.asmx")
                            .post(body)
                            .addHeader("Content-Type", "text/xml")
                            .addHeader("User-Agent", "PostmanRuntime/7.18.0")
                            .addHeader("Accept", "*/*")
                            .addHeader("Cache-Control", "no-cache")
                            .addHeader("Postman-Token", "a76f7625-a2c8-4806-853a-877dff25011f,faebb6f7-d421-44eb-8a54-eb1786e95136")
                            .addHeader("Host", m_ip)
                            .addHeader("Accept-Encoding", "gzip, deflate")
                            .addHeader("Content-Length", "428")
                            .addHeader("Connection", "keep-alive")
                            .addHeader("cache-control", "no-cache")
                            .build();

                    EnableDialog(true, "Conectando", false);

                    Pickinghttp.newCall(RequestPicking).enqueue(new Callback() {

                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            EnableDialog(false, "",false);
                            DisplayPrintingStatusMessage("Conexion Fallo");
                        }

                        @Override
                        public void onResponse(Call call, final Response response) throws IOException {

                            if (response.isSuccessful()) {
                                try {

                                    final String myResponse = response.body().string();

                                    ParserXml parserXmlPicking = new ParserXml(TagGondola.this);

                                    Document doc = toXmlDocument(myResponse);
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    Source xmlSource = new DOMSource(doc);
                                    Result outputTarget = new StreamResult(outputStream);
                                    TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
                                    InputStream is = new ByteArrayInputStream(outputStream.toByteArray());

                                    if (parserXmlPicking.parsear(is)) {

                                        DisplayPrintingStatusMessage("Correcto");
                                        cargarLista2();
                                        ultimocodigo = codigocaptrado;

                                    } else {

                                        DisplayPrintingStatusMessage("No disponible");

                                    }

                                } catch (XmlPullParserException e) {
                                    e.printStackTrace();
                                } catch (TransformerConfigurationException e) {
                                    e.printStackTrace();
                                } catch (ParserConfigurationException e) {
                                    e.printStackTrace();
                                } catch (SAXException e) {
                                    e.printStackTrace();
                                } catch (TransformerException e) {
                                    e.printStackTrace();
                                }

                                EnableDialog(false, "Conectando", false);

                            } else {

                                DisplayPrintingStatusMessage("Error con la conexion Wifi.. Reintentar");
                                EnableDialog(false, "Conectando", false);

                            }
                        }

                    });

                } else {
                    DisplayPrintingStatusMessage("Ya Consulto este código");
                }

            }

        });

    }

    @Override
    public void onBarcodeEvent(BarcodeReadEvent event) {
        imprimirCodigo(event.getBarcodeData());

    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {
        DisplayPrintingStatusMessage("Codigo no valido");
    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent triggerStateChangeEvent) {
        DisplayPrintingStatusMessage("verificar excepcion");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeReader != null) {
            // release the scanner claim so we don't get any scanner
            // notifications while paused.
            barcodeReader.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (barcodeReader != null) {
            barcodeReader.removeBarcodeListener(this);
            barcodeReader.removeTriggerListener(this);
        }
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


                    cancelarrun = true;
                }
            });
        }
        dialog.show();
    }

    public void Asignar(final String numero, final String mensaje) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {

                dialog.setTitle(numero);
                dialog.setMessage(mensaje);

            }
        });
    }


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


    private void restaurarimpresion() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    EnableDialog(true, "Restaurando Lista..",false);
                    if (list.size() > 0) {

                        for (Producto prod : list) {

                            if (prod.getIP().equals("SI")) {
                                prod.setIP("NO");
                                db.updatecodigoproduto(prod);
                            }

                        }
                        cargarLista2();
                    }
                    EnableDialog(false, "Enviando terminando...",false);
                    DisplayPrintingStatusMessage("Reimprimir restaurado");

                } catch (Exception e) {
                    Log.e("errorW", "mensaje");
                    EnableDialog(false, "Enviando terminando...",false);
                    DisplayPrintingStatusMessage("Fallo la base de datos.");
                }

            }
        };

        thread.start();


    }


    private static Document toXmlDocument(String str) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(new InputSource(new StringReader(str)));
        return document;

    }

    public void DisplayPrintingStatusMessage(final String MsgStr) {

        m_handler.post(new Runnable() {
            public void run() {
                showToast(MsgStr);//2018 PH
            }// run()
        });

    }

    public void showToast(final String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
    }

    private void CargarPicking() {

        dialog = new ProgressDialog(this);
        dialog.setTitle("Titulo");
        dialog.setMessage("Mensjae");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        ParserXml par = new ParserXml(TagGondola.this);
        List<Producto> entries = null;

        try {
            InputStream asset = getResources().getAssets().open("responsexmlproducto1.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document docc = dBuilder.parse(asset);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(docc);
            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
            par.parsear(is);
            cargarLista();

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        dialog.dismiss();
    }

    private void cargarLista2() {

        m_handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    db = new ProductosDB(TagGondola.this);
                    list = db.loadProducto();
                    adapter.setNotes(list);
                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e("errorW", "mensaje");
                }
            }
        });

    }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menutag, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public void Limpiar() {
        try {
            db = new ProductosDB(TagGondola.this);
            db.eliminarProductos();
            ultimocodigo = "ultimocodigo";
            cargarLista();
            Toast.makeText(TagGondola.this, "Se Limpio la Lista", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("error", "mensaje");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.limpiar:

                AlertDialog.Builder build4 = new AlertDialog.Builder(TagGondola.this);
                build4.setMessage("¿Desea borrar lista? ").setPositiveButton("Borrar lista", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Limpiar();

                    }


                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                AlertDialog alertDialog4 = build4.create();
                alertDialog4.show();


                break;

            case R.id.test_impresion:

                AlertDialog.Builder build3 = new AlertDialog.Builder(TagGondola.this);
                build3.setMessage("¿Desea imprimir un test de impresión? ").setPositiveButton("Imprimir test", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    EnableDialog(true, "Enviando Documento...",true);
                                    TscDll.openport(m_printerMAC);
                                    TscDll.setup(76, 35, 2, 1, 1, 4, 0);
                                    TscDll.clearbuffer();
                                    TscDll.sendcommand("CODEPAGE UTF-8\n");
                                    TscDll.sendcommand("SET TEAR ON\n");
                                    TscDll.sendcommand("DIRECTION 0\n");
                                    TscDll.sendcommand("TEXT 70,20,\"arial_na.TTF\",0,11,11,\"" + "descripcion1" + "\"\n");
                                    TscDll.sendcommand("TEXT 70,55,\"arial_na.TTF\",0,08,08,\"" + "descripcion2" + "\"\n");
                                    TscDll.sendcommand("TEXT 280,100,\"arial_bl.TTF\",0,20,20,\"" + "$ " + "1.230" + "\"\n");
                                    TscDll.sendcommand("TEXT 400,190,\"arial_na.TTF\",0,11,11,\"" + "123456" + "\"\n");
                                    TscDll.barcode(70, 180, "128", 30, 1, 0, 1, 1, "123456");
                                    TscDll.printlabel(1, 1);

                                    TscDll.closeport(500);
                                    EnableDialog(false, "Enviando terminando...",false);
                                    DisplayPrintingStatusMessage("Impresión Exitosa.");

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    EnableDialog(false, "Enviando terminando...",false);
                                    DisplayPrintingStatusMessage("Fallo conexion Bluetooth.");
                                }

                            }
                        };

                        thread.start();

                    }


                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                    }
                });

                AlertDialog alertDialog3 = build3.create();
                alertDialog3.show();

                break;

            case R.id.Imprimir_todo:

                AlertDialog.Builder build = new AlertDialog.Builder(TagGondola.this);

                build.setMessage("¿Desea imprimir toda la lista? ").setPositiveButton("Imprimir Sin Ofertas", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        tagimprimirall = true;
                        imprimirofertas = false;

                        new Thread(TagGondola.this, "PrintingTask").start();

                    }

                }).setNeutralButton("Imprimir Ofertas", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        tagimprimirall = true;
                        imprimirofertas = true;

                        new Thread(TagGondola.this, "PrintingTask").start();

                    }

                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                    }
                });

                AlertDialog alertDialog = build.create();
                alertDialog.show();


                break;

            case R.id.Restaurar:

                AlertDialog.Builder build2 = new AlertDialog.Builder(TagGondola.this);
                build2.setMessage("¿Desea restaurar tags impresos? ").setPositiveButton("Restaurar tags impresos", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        restaurarimpresion();

                    }


                }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                    }
                });
                AlertDialog alertDialog2 = build2.create();
                alertDialog2.show();

                break;

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void run() {

        try {

            EnableDialog(true, "Imprimiendo...",true);
            String estado = "00";
            TscDll.openport(m_printerMAC);
            TscDll.setup(76, 35, 2, 1, 1, 4, 0);

            if (!tagimprimirall) {

                Asignar("Imprimiendo: ", lista_producto_seleccionada.getDescArticulo_1());
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
                lista_producto_seleccionada.setIP("SI");
                db.updatecodigoproduto(lista_producto_seleccionada);


            } else {

                db = new ProductosDB(TagGondola.this);
                for (Producto prod : list) {

                    if (cancelarrun) {
                        break;
                    }


                    if (!prod.getIP().equals("SI")){

                        if (imprimirofertas){
                            //todo SI ES OFERTA

                            if (prod.getOff_available().equals("S")) {


                                Asignar("Imprimiendo: ", prod.getDescArticulo_1());

                                TscDll.clearbuffer();
                                TscDll.sendcommand("CODEPAGE UTF-8\n");
                                TscDll.sendcommand("SET TEAR ON\n");
                                TscDll.sendcommand("DIRECTION 0\n");
                                TscDll.sendcommand("TEXT 70,20,\"arial_na.TTF\",0,11,11,\"" + prod.getDescArticulo_1() + "\"\n");
                                TscDll.sendcommand("TEXT 70,55,\"arial_na.TTF\",0,08,08,\"" + prod.getDescArticulo_2() + "\"\n");
                                TscDll.sendcommand("TEXT 280,100,\"arial_bl.TTF\",0,20,20,\"" + "$ " + prod.getPrecio() + "\"\n");
                                TscDll.sendcommand("TEXT 400,190,\"arial_na.TTF\",0,11,11,\"" + prod.getCodProd() + "\"\n");
                                TscDll.barcode(70, 180, "128", 30, 1, 0, 1, 1, prod.getCodBarras());
                                estado = TscDll.printerstatus();
                                TscDll.printlabel(1, 1);

                                prod.setIP("SI");
                                db.updatecodigoproduto(prod);
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }


                            }

                        }else{
                            //todo SI NO ES OFERTA

                            if (prod.getOff_available().equals("N")) {

                                Asignar("Imprimiendo: ", prod.getDescArticulo_1());

                                TscDll.clearbuffer();
                                TscDll.sendcommand("CODEPAGE UTF-8\n");
                                TscDll.sendcommand("SET TEAR ON\n");
                                TscDll.sendcommand("DIRECTION 0\n");
                                TscDll.sendcommand("TEXT 70,20,\"arial_na.TTF\",0,11,11,\"" + prod.getDescArticulo_1() + "\"\n");
                                TscDll.sendcommand("TEXT 70,55,\"arial_na.TTF\",0,08,08,\"" + prod.getDescArticulo_2() + "\"\n");
                                TscDll.sendcommand("TEXT 280,100,\"arial_bl.TTF\",0,20,20,\"" + "$ " + prod.getPrecio() + "\"\n");
                                TscDll.sendcommand("TEXT 400,190,\"arial_na.TTF\",0,11,11,\"" + prod.getCodProd() + "\"\n");
                                TscDll.barcode(70, 180, "128", 30, 1, 0, 1, 1, prod.getCodBarras());
                                estado = TscDll.printerstatus();
                                TscDll.printlabel(1, 1);

                                prod.setIP("SI");
                                db.updatecodigoproduto(prod);

                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }


                            }

                        }
                    }

                    int timepo = 50;
                    while (!estado.equals("00") && !estado.equals("20")) {

                        Asignar("Tapa Abierta", prod.getDescArticulo_1() + "\n\n" +
                                "Cambiar Rollo antes de: " + timepo);
                        estado = TscDll.printerstatus();
                        timepo--;

                        if (cancelarrun || timepo == 0) {
                            cancelarrun = true;
                            break;
                        }
                    }
                }
            }

            TscDll.closeport(500);
            cargarLista2();
            EnableDialog(false, "",false);
            DisplayPrintingStatusMessage("Finalizado.");
            cancelarrun = false;
            tagimprimirall = false;


        } catch (Exception e) {

            e.printStackTrace();
            EnableDialog(false, "",false);
            DisplayPrintingStatusMessage("Fallo conexion Bluetooth.");
            tagimprimirall = false;
            cancelarrun = false;

        }

    }

}

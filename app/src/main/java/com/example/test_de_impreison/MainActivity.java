package com.example.test_de_impreison;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bixolon.commonlib.BXLCommonConst;
import com.bixolon.commonlib.connectivity.ConnectivityManager;
import com.bixolon.commonlib.emul.image.LabelImage;
import com.bixolon.labelprinter.BixolonLabelPrinter;
import com.bixolon.labelprinter.service.ServiceManager;
import com.bxl.config.editor.BXLConfigLoader;

import com.example.test_de_impreison.ProductosApi.Registro_Productos;
import com.example.test_de_impreison.configuracion.DMRPrintSettings;
import com.example.test_de_impreison.configuracion.DOPrintMainActivity;
import com.example.tscdll.TSCActivity;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeReader;
import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.neptunelite.api.NeptuneLiteUser;
import com.rt.printerlibrary.cmd.Cmd;
import com.rt.printerlibrary.cmd.EscFactory;
import com.rt.printerlibrary.enumerate.BmpPrintMode;
import com.rt.printerlibrary.enumerate.CommonEnum;
import com.rt.printerlibrary.exception.SdkException;
import com.rt.printerlibrary.factory.cmd.CmdFactory;
import com.rt.printerlibrary.printer.RTPrinter;
import com.rt.printerlibrary.setting.BitmapSetting;
import com.rt.printerlibrary.setting.CommonSetting;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.graphics.internal.CompressedBitmapOutputStreamCpcl;
import com.zebra.sdk.graphics.internal.DitheredImageProvider;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.internal.PrinterConnectionOutputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Set;

import honeywell.connection.ConnectionBase;
import honeywell.connection.Connection_Bluetooth;
import honeywell.printer.DocumentDPL;
import honeywell.printer.DocumentExPCL_LP;
import honeywell.printer.ParametersDPL;



import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.config.JposEntry;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.DirectIOEvent;
import jpos.events.DirectIOListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class MainActivity extends AppCompatActivity implements Runnable, OutputCompleteListener, StatusUpdateListener, DirectIOListener, DataListener, ErrorListener {
    ConnectionBase conn = null;
    String ApplicationConfigFilename = "applicationconfigg.dat";
    private String m_printerMode = null;
    private String m_printerMAC = null;
    private String m_ip = null;
    private String m_sucursal, pathpdf;
    private TextView txtsucursal,txtpathpdf;
    private String password;
    private Button btnbluetooth, btnpaxprint, apiprodcutos,btnbixolon;
    private Button btnTagSml300, btnPdfZebraAlpha, btnBmpTsc, btnbuscarpdf,btn_MPD31,btn_gondolatsc, btndpletiqueta,btnescpos
            ;
    private Button btnPdfR410;
    private SharedPreferences sharedPref;
    private int m_printerPort = 515;
    private TextView textbluetooth;
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 1;
    DMRPrintSettings g_appSettings = new DMRPrintSettings("", 0, 0, "0", "0", "0", 0);
    private int m_configurado;
    private Handler m_handler = new Handler(); // Main thread
    byte[] printData;
    private Context mContext;
    private boolean mIsForeground;
    private ProgressDialog mProgressDialog;
    private Boolean cancelarrun = false;
    private POSPrinter posPrinter = null;
    private int mPortType;
    private String mAddress;
    private int portType = BXLConfigLoader.DEVICE_BUS_BLUETOOTH;
    private ProgressDialog dialog;
    String productNamer410 = BXLConfigLoader.PRODUCT_NAME_SPP_R410;
    File file;
    String productNamerr310 = BXLConfigLoader.PRODUCT_NAME_SPP_R310;
    private BXLConfigLoader bxlConfigLoader = null;
    private File tempFile;
    private Bitmap mBitmap = null;
    private static final int REQUEST_PICK_FILE = 1; //for File browsing

    static final String FOLDER_NAME_KEY = "com.honeywell.doprint.Folder_Name_Key";
    static final String FOLDER_PATH_KEY = "com.honeywell.doprint.Folder_Path_Key";
    byte[] printdatos;
    TSCActivity TscDll = new TSCActivity();
    private static IDAL dal;
    private static Context appContext;
    public static IDAL idal = null;
    private IPrinter printer;
    private static BarcodeReader barcodeReader;
    private AidcManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        pedir_permiso_escritura();


        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Communicating...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);


        long start = System.currentTimeMillis();


        try {
            idal = NeptuneLiteUser.getInstance().getDal(getApplicationContext());
       } catch (Exception e) {
            e.printStackTrace();
        }

        if (null == idal) {
            Toast.makeText(MainActivity.this, "error occurred,DAL is null.", Toast.LENGTH_LONG).show();
        }

        printer = idal.getPrinter();


        txtpathpdf = findViewById(R.id.txtpathpdff);
        txtpathpdf.setText("seleccionarpdf");

        textbluetooth = findViewById(R.id.txtbluetooth);

        btnbluetooth = findViewById(R.id.btn_bluetooth);
        btnbluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DOPrintMainActivity.class);
                startActivity(intent);
            }
        });
        btn_gondolatsc = findViewById(R.id.btngondolatsc);
        btn_gondolatsc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TagGondolaTsc.class);
                startActivity(intent);
            }
        });


        btnbixolon =findViewById(R.id.btnBixolon);
        btnbixolon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                printBixolon2();


            }
        });

        apiprodcutos = findViewById(R.id.btnapiproductos);

        apiprodcutos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, Registro_Productos.class);
                startActivity(intent);

            }
        });

        btndpletiqueta = findViewById(R.id.btndpl);

        btndpletiqueta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printdpl();
            }
        });


        btn_MPD31 = findViewById(R.id.btnMPD31);
        btn_MPD31.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PrintPD31();
            }
        });

        btnTagSml300 = findViewById(R.id.btn_tag_sml300);
        btnTagSml300.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent2 = new Intent(MainActivity.this, TagGondola.class);
                startActivity(intent2);
            }
        });

        btnPdfZebraAlpha = findViewById(R.id.btn_pdf_zebra_alpha);
        btnPdfZebraAlpha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (valdiarpdf()){
                    PrintPdfZebraAlpha();
                }else{
                    Toast.makeText(MainActivity.this, "Seleccione un documento pdf", Toast.LENGTH_SHORT).show();
                }

            }
        });


        btnPdfR410 = findViewById(R.id.btn_pdf_r410);
        btnPdfR410.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (valdiarpdf()){
                    PrintPdfR410();
                }else{
                    Toast.makeText(MainActivity.this, "Seleccione un documento pdf", Toast.LENGTH_SHORT).show();
                }

            }
        });


        btnBmpTsc = findViewById(R.id.btn_bmp_tsc);
        btnBmpTsc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (valdiarpdf()){
                    PrintBmpTsc();
                }else{
                    Toast.makeText(MainActivity.this, "Seleccione un documento pdf", Toast.LENGTH_SHORT).show();
                }

            }
        });



        btnescpos = findViewById(R.id.btnescpos2);
        btnescpos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (valdiarpdf()){
                    try {

                        // EnableDialog(true, "Enviando Documento...",true);
                        CmdFactory cmdFactory = new EscFactory();
                        Cmd cmd = cmdFactory.create();

                        BitmapSetting bitmapSetting = new BitmapSetting();
                        bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_SINGLE_COLOR);
                        bitmapSetting.setBimtapLimitWidth(72 * 8);

                        mBitmap = generateImageFromPdf(pathpdf, 0, 576);

                        cmd.append(cmd.getBitmapCmd(bitmapSetting, mBitmap));
                        printdatos = cmd.getAppendCmds();

                        escPrint();


                    } catch (SdkException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(MainActivity.this, "Seleccione un documento pdf", Toast.LENGTH_SHORT).show();
                }

            }
        });



        btnpaxprint = findViewById(R.id.btnpax);


        btnpaxprint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {
                    public void run() {

                        mBitmap = generateImageFromPdf(pathpdf, 0, 490);
                        if (mBitmap != null) {
                            byte[] bitmapData = convertTo1BPP(mBitmap, 128);

                            final Bitmap bitt = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);


                            try {
                                printer.init();
                            } catch (PrinterDevException e) {
                                e.printStackTrace();
                            }

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inScaled = true;

                            try {
                                printer.printBitmap(bitt);
                            } catch (PrinterDevException e) {
                                e.printStackTrace();
                            }

                            try {
                                final int status =  printer.start();
                            } catch (PrinterDevException e) {
                                e.printStackTrace();
                            }
                        }




                    }
                }).start();
            }
        });


        btnbuscarpdf = findViewById(R.id.btnPDF);
        btnbuscarpdf.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                //==========Start file browsing activity==================//
                Intent intent = new Intent("com.honeywell.doprint.FileBrowseActivity");
                startActivityForResult(intent, REQUEST_PICK_FILE);
            }
        });



        posPrinter = new POSPrinter(MainActivity.this);
        posPrinter.addStatusUpdateListener(MainActivity.this);
        posPrinter.addErrorListener(MainActivity.this);
        posPrinter.addOutputCompleteListener(MainActivity.this);
        posPrinter.addDirectIOListener(MainActivity.this);
        portType = BXLConfigLoader.DEVICE_BUS_BLUETOOTH;

        bxlConfigLoader = new BXLConfigLoader(this);
        try {
            bxlConfigLoader.openFile();
        } catch (Exception e) {
            bxlConfigLoader.newFile();
        }


        AidcManager.create(this, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
            }
        });

    }
    static BarcodeReader getBarcodeObject() {
        return barcodeReader;
    }


    private void escPrint() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                        EnableDialog(true, "Enviando Documento...",true);
                        Looper.prepare();
                        conn = Connection_Bluetooth.createClient(m_printerMAC, true);
                        if (!conn.getIsOpen()) {
                            if (conn.open()) {
                                int bytesWritten = 0;
                                int bytesToWrite = 1024;
                                int totalBytes = printdatos.length;
                                int remainingBytes = totalBytes;
                                while (bytesWritten < totalBytes) {
                                    if (remainingBytes < bytesToWrite)
                                        bytesToWrite = remainingBytes;
                                    //Send data, 1024 bytes at a time until all data sent
                                    conn.write(printdatos, bytesWritten, bytesToWrite);
                                    bytesWritten += bytesToWrite;
                                    remainingBytes = remainingBytes - bytesToWrite;
                                }
                                EnableDialog(false, "Enviando Documento...",true);
                                conn.close();
                            }
                        }

                    } catch (PrinterDevException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    EnableDialog(false, "Enviando Documento...",true);

            }
        }).start();
    }



        private void printdpl(){

        String connectionType;
        DocumentDPL docDPL;
        DocumentExPCL_LP docExPCL_LP;
        ParametersDPL paramDPL;

        docDPL = new DocumentDPL();
        paramDPL = new ParametersDPL();
        printData = new byte[]{0};



        try {

            docDPL.writePDF(pathpdf, 834, 0, 0);
            printData = docDPL.getDocumentData();

        } catch (Exception e) {
            e.printStackTrace();
        }


        new Thread(new Runnable() {
            public void run() {
                try {
                    EnableDialog(true, "Enviando Documento...",true);
                    Looper.prepare();
                    conn = Connection_Bluetooth.createClient(m_printerMAC, false);
                    if (!conn.getIsOpen()) {
                        if (conn.open()) {
                            int bytesWritten = 0;
                            int bytesToWrite = 1024;
                            int totalBytes = printData.length;
                            int remainingBytes = totalBytes;
                            while (bytesWritten < totalBytes) {
                                if (remainingBytes < bytesToWrite)
                                    bytesToWrite = remainingBytes;
                                //Send data, 1024 bytes at a time until all data sent
                                conn.write(printData, bytesWritten, bytesToWrite);
                                bytesWritten += bytesToWrite;
                                remainingBytes = remainingBytes - bytesToWrite;
                                Thread.sleep(100);
                            }
                            EnableDialog(false, "Enviando Documento...",true);
                            conn.close();
                        }
                    }

                } catch (PrinterDevException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                EnableDialog(false, "Enviando Documento...",true);
            }
        }).start();
    }

    private void pedir_permiso_escritura() {

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowHomeEnabled(true);

        int readExternalPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeExternalPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (writeExternalPermission != PackageManager.PERMISSION_GRANTED || readExternalPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE_PERMISSION);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int readExternalPermission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writeExternalPermission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (writeExternalPermission2 != PackageManager.PERMISSION_GRANTED || readExternalPermission2 != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE_PERMISSION);
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    Toast.makeText(MainActivity.this, "Permission permitido to read your External storage", Toast.LENGTH_SHORT).show();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mIsForeground = false;
    }


    private void PrintBmpTsc() {

        final int heigth_calculator,wigth_calculator;

        try {

            mBitmap = generateImageFromPdf(pathpdf, 0, 630);
            heigth_calculator = (int) (mBitmap.getHeight()/8);
            wigth_calculator = (int) (mBitmap.getWidth()/8);

            File f = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/" + "/temp2.BMP");
            byte[] bitmapData = convertTo1BPP(mBitmap, 128);
            ByteArrayInputStream bs = new ByteArrayInputStream(bitmapData);

                InputStream is = bs;
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(buffer);
                fos.close();

            Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {

                            EnableDialog(true, "Enviando Documento...",true);
                            TscDll.openport(m_printerMAC);
                            TscDll.downloadbmp("temp2.BMP");
                            TscDll.setup(wigth_calculator, heigth_calculator, 4, 10, 0, 0, 0);
                            TscDll.clearbuffer();
                            TscDll.sendcommand("PUTBMP 10,10,\"temp2.BMP\"\n");
                            TscDll.printlabel(1, 1);
                            TscDll.closeport(5000);

                            EnableDialog(false, "Enviando terminando...",false);

                        } catch (Exception e) {
                            e.printStackTrace();
                            EnableDialog(false, "Enviando terminando...",false);
                        }
                    }
                };

                thread.start();



        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private final Handler mHandlerer = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case BixolonLabelPrinter.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1)
                    {
                        case BixolonLabelPrinter.STATE_CONNECTED:
                            Toast.makeText(getApplicationContext(), "conectado", Toast.LENGTH_SHORT).show();
                            break;

                        case BixolonLabelPrinter.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(), "conectando", Toast.LENGTH_SHORT).show();
                            break;

                        case BixolonLabelPrinter.STATE_NONE:
                            Toast.makeText(getApplicationContext(), "none", Toast.LENGTH_SHORT).show();
                            invalidateOptionsMenu();
                            break;
                    }
                    break;

                case BixolonLabelPrinter.MESSAGE_READ:
                    //MainActivity.this.dispatchMessage(msg);
                    break;

                case BixolonLabelPrinter.MESSAGE_DEVICE_NAME:
                   // mConnectedDeviceName = msg.getData().getString(BixolonLabelPrinter.DEVICE_NAME);
                 //   Toast.makeText(getApplicationContext(), mConnectedDeviceName, Toast.LENGTH_LONG).show();

                    break;

                case BixolonLabelPrinter.MESSAGE_TOAST:
                   // mListView.setEnabled(false);
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BixolonLabelPrinter.TOAST), Toast.LENGTH_SHORT).show();
                    break;

                case BixolonLabelPrinter.MESSAGE_LOG:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BixolonLabelPrinter.LOG), Toast.LENGTH_SHORT).show();
                    break;

                case BixolonLabelPrinter.MESSAGE_BLUETOOTH_DEVICE_SET:
                    if(msg.obj == null)
                    {
                        Toast.makeText(getApplicationContext(), "No paired device", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                      //  DialogManager.showBluetoothDialog(MainActivity.this, (Set<BluetoothDevice>) msg.obj);
                    }
                    break;

                case BixolonLabelPrinter.MESSAGE_USB_DEVICE_SET:
                    if(msg.obj == null)
                    {
                        Toast.makeText(getApplicationContext(), "No connected device", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                    //    DialogManager.showUsbDialog(MainActivity.this, (Set<UsbDevice>) msg.obj, mUsbReceiver);
                    }
                    break;

                case BixolonLabelPrinter.MESSAGE_NETWORK_DEVICE_SET:
                    if(msg.obj == null)
                    {
                        Toast.makeText(getApplicationContext(), "No connectable device", Toast.LENGTH_SHORT).show();
                    }
                   // DialogManager.showNetworkDialog(MainActivity.this, msg.obj.toString());
                    break;

            }
        }
    };


    private ServiceManager mServiceManager;
    private void printBixolon2(){

        Bitmap mBitmap = generateImageFromPdf(pathpdf, 0, 500);
        byte[] bitmapData = convertTo1BPP(mBitmap, 128);
        final Bitmap bitt = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);

        mServiceManager = new ServiceManager(this, mHandlerer,  Looper.getMainLooper());


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    EnableDialog(true, "Enviando Documento...",true);


                    if (bitt != null) {
                        LabelImage image = new LabelImage();
                        if (image.Load(bitt)) {
                            image.MakeLD(0, 0, 500,  0, 0, 50, 30);

                            //Looper.prepare();

                            mServiceManager.connect(m_printerMAC, BXLCommonConst._PORT_BLUETOOTH);
                            mServiceManager.Write(image.PopAll());
                            mServiceManager.executeCommand("P" + 1 + "," + 1, false);

                        }
                    }



                    EnableDialog(false, "Enviando terminando...",false);

                } catch (Exception e) {
                    e.printStackTrace();
                    EnableDialog(false, "Enviando terminando...",false);
                }
            }
        };

        thread.start();










    }


    private void printBixolon(){


       Bitmap mBitmap = generateImageFromPdf(pathpdf, 0, 500);
        if (mBitmap != null) {

            byte[] bitmapData = convertTo1BPP(mBitmap, 128);
           final Bitmap bitt = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);

           // BixolonLabelPrinter mBixolonLabelPrinter;
           // mBixolonLabelPrinter = new BixolonLabelPrinter(this, mHandlerer, Looper.getMainLooper());

            LabelImage image = new LabelImage();

            ConnectivityManager connectivityManager = null;

            if (image.Load(bitt)) {
              //  image.MakeLD(horizontalStartPosition, verticalStartPosition, width, dithering ? 1 : 0, 0, level, 30);
                image.MakeLD(0, 0, 500, 0, 0, 50, 30);


               connectivityManager.write( image.PopAll());
                byte[] readBuffer =  image.PopAll();
               // printdatos = image.PopAll();
            }

          //  mBixolonLabelPrinter.drawBitmap(bitt, horizontalStartPosition, verticalStartPosition, width, level, dithering);


            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        EnableDialog(true, "Enviando Documento...",true);





                        EnableDialog(false, "Enviando terminando...",false);

                    } catch (Exception e) {
                        e.printStackTrace();
                        EnableDialog(false, "Enviando terminando...",false);
                    }
                }
            };

            thread.start();

        }

        }




    private void PrintPdfZebraAlpha() {

            /*
            File f = new File(getCacheDir() + "/tempp.pdf");
        if (!f.exists()) {
            InputStream is = getAssets().open("ejemplofacturacompleta.pdf");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        }

        */
            mBitmap = generateImageFromPdf(pathpdf, 0, 490);
            if (mBitmap != null) {
                byte[] bitmapData = convertTo1BPP(mBitmap, 128);

               // creamos el bitmap del pdf
                final Bitmap bitt = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                // nuestro propio bitmap

                new Thread(new Runnable() {
                    public void run() {
                        try {

                            EnableDialog(true, "Imprimiendo test de prueba");
                            Looper.prepare();
                            Connection connection = new BluetoothConnection(m_printerMAC);
                            connection.open();

                            ZebraImageAndroid imagenandroid = new ZebraImageAndroid(bitt);

                            int var2 = 0;
                            int var3 = 0;
                            int AnchoTotal = (imagenandroid.getWidth()+7) / 8;

                            try {

                                ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
                                String var10 = "! 0 200 200 " + imagenandroid.getHeight() + " 1\r\n";

                                String contrast = "CONTRAST 0\r\n";
                                String tone = "TONE 0\r\n";
                                String speed = "SPEED 3\r\n";

                                outputstream.write(contrast.getBytes());
                                outputstream.write(tone.getBytes());
                                outputstream.write(speed.getBytes());
                                outputstream.write(var10.getBytes());


                                outputstream.write("CG ".getBytes());
                                outputstream.write(String.valueOf(AnchoTotal).getBytes());
                                outputstream.write(" ".getBytes());
                                outputstream.write(String.valueOf(imagenandroid.getHeight()).getBytes());
                                outputstream.write(" ".getBytes());
                                outputstream.write(String.valueOf(var2).getBytes());
                                outputstream.write(" ".getBytes());
                                outputstream.write(String.valueOf(var3).getBytes());
                                outputstream.write(" ".getBytes());

                                connection.write(outputstream.toByteArray());

                                PrinterConnectionOutputStream var12 = new PrinterConnectionOutputStream(connection);
                                CompressedBitmapOutputStreamCpcl var13 = new CompressedBitmapOutputStreamCpcl(var12);
                                DitheredImageProvider.getDitheredImage(imagenandroid, var13);

                                var13.close();
                                var12.close();

                                connection.write("\r\n".getBytes());

                                String var11 = "PRINT\r\n";
                                connection.write(var11.getBytes());


                            } catch (Exception var14) {
                                throw new ConnectionException(var14.getMessage());
                            }

                            connection.close();
                            EnableDialog(false, "Imprimiendo test de prueba");

                        } catch (ConnectionException e) {

                        } finally {
                            bitt.recycle();
                            Looper.myLooper().quit();
                        }
                    }
                }).start();

            }


    }

    private void PrintPD31() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {

                    EnableDialog(true, "Enviando Documento...",true);
                    TscDll.openport(m_printerMAC);
                   //TscDll.setup(500, 500, 4, 0, 0, 0, 0);

                    File file = new File(pathpdf);
                    byte[] readBuffer = new byte[(int) file.length()];
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                    if(inputStream.read(readBuffer) == 0)
                        throw new Exception("Unable to read file or file is empty.");
                    inputStream.close();

                    TscDll.sendcommand(readBuffer);
                    //printData = readBuffer;


/*
                    TscDll.sendcommand(
                            "!ETSC ESC/POS Ticket"+"\r\n"+
                            "! E 3040 Saturn Street Suite #200. Brea, CA 92821"+"\r\n"+
                            "E-2Comestibles-0"+"\r\n"+
                            "E"+"\r\n"+
                            "Bananas	   $2.99/LB  "+"\r\n"+
                            "Manzanas	   $1.99/LB  "+"\r\n"+
                            "Zanahorias	   $0.99/LB  "+"\r\n"+
                            "E-2CarnesE -0      "+"\r\n"+
                            "Chuletón	   $9.99/LB  "+"\r\n"+
                            "Filete	   $8.99/LB      "+"\r\n"+
                            "ESubtotal	  $24.95 "+"\r\n"+
                            "Imp. (9%)	   $2.25     "+"\r\n"+
                            "                        "+"\r\n"
                    );
*/
                    TscDll.printlabel(1, 1);
                    TscDll.closeport(5000);

                    EnableDialog(false, "Enviando terminando...",false);

                } catch (Exception e) {
                    e.printStackTrace();
                    EnableDialog(false, "Enviando terminando...",false);
                }
            }
        };

        thread.start();


    }

    private void PrintPdfR410() {

        Thread thread = new Thread() {
            @Override
            public void run() {

                EnableDialog(true, "imprimiendo");
                if (setTargetDevice(portType, productNamerr310, BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER, m_printerMAC)) {
                    try {
                        posPrinter.open(productNamer410);
                        posPrinter.claim(5000 * 2);
                        posPrinter.setDeviceEnabled(true);
                        posPrinter.setAsyncMode(false);
                        mPortType = portType;
                        mAddress = m_printerMAC;

                        if (posPrinter.getDeviceEnabled()) {
                            ByteBuffer buffer = ByteBuffer.allocate(4);
                            buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
                            buffer.put((byte) 50); // brightness
                            buffer.put((byte) 0x01); // compress
                            buffer.put((byte) 0); // dither
                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bixolonlogo);
                            posPrinter.printBitmap(buffer.getInt(0), bitmap, 576, POSPrinterConst.PTR_BM_CENTER);
                        }

                        EnableDialog(false, "imprimiendo");

                    } catch (JposException e) {
                        e.printStackTrace();
                        EnableDialog(false, "imprimiendo");
                        try {
                            posPrinter.close();
                        } catch (JposException e1) {
                            e1.printStackTrace();
                        }
                    }
                    try {
                        posPrinter.close();
                    } catch (JposException e1) {
                        e1.printStackTrace();
                    }
                }


            }
        };

        thread.start();


    }


//bixolon para ejecutar unos servicios 114 del sdk Y recordar la impresora
    private boolean setTargetDevice(int portType, String logicalName, int deviceCategory, String address) {
        try {
            for (Object entry : bxlConfigLoader.getEntries()) {
                JposEntry jposEntry = (JposEntry) entry;
                if (jposEntry.getLogicalName().equals(logicalName)) {
                    bxlConfigLoader.removeEntry(jposEntry.getLogicalName());
                }
            }
            bxlConfigLoader.addEntry(logicalName, deviceCategory, logicalName, portType, address);
            bxlConfigLoader.saveFile();
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }

        return true;
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


    @Override
    public void run() {
        try {
            EnableDialog(true, "Enviando Documento...");

            EnableDialog(false, "Enviando terminando...");

        } catch (Exception e) {
            e.printStackTrace();
            EnableDialog(false, "Enviando terminando...");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargardatos();
        mIsForeground = true;
    }


    public void EnableDialog(final boolean value, final String mensaje) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                if (value) {

                    createCancelProgressDialog("Imprimiendo...", mensaje, "Cancelar");
                } else {
                    if (dialog != null)
                        dialog.dismiss();
                }

            }
        });
    }



    private void createCancelProgressDialog(String title, String message, String buttonText) {
        dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButton(buttonText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Use either finish() or return() to either close the activity or just the dialog

            }
        });
        dialog.show();
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

            textbluetooth.setText("BT:"+ m_printerMAC);

        } else {

            Toast.makeText(MainActivity.this,
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

    //Bixolon
    @Override
    public void dataOccurred(DataEvent dataEvent) {
        Log.e("dataOccurred", dataEvent.toString());
    }

    @Override
    public void directIOOccurred(DirectIOEvent directIOEvent) {
        Log.e("directIOOccurred", directIOEvent.toString());
    }

    @Override
    public void outputCompleteOccurred(OutputCompleteEvent outputCompleteEvent) {
        Log.e("outputCompleteOccurred", outputCompleteEvent.toString());
    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {
        Log.e("statusUpdateOccurred", statusUpdateEvent.toString());
    }

    @Override
    public void errorOccurred(ErrorEvent errorEvent) {
        Log.e("errorOccurred", errorEvent.toString());
    }



    private Bitmap generateImageFromPdf(String assetFileName, int pageNumber, int printHeadWidth) {

        PdfiumCore pdfiumCore = new PdfiumCore(MainActivity.this);
        try {
            File f = new File(assetFileName);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfiumCore.openPage(pdfDocument, pageNumber);
            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);

            float scale = (float) printHeadWidth / width;
            float scaledWidth = width * scale;
            float scaledHeight = height * scale;
            Bitmap bmp = Bitmap.createBitmap((int) scaledWidth, (int) scaledHeight, Bitmap.Config.ARGB_8888);
            
            int width2 = (int) scaledWidth;
            int height2 = (int) scaledHeight;

            pdfiumCore.getClass();
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width2, height2);
            pdfiumCore.closeDocument(pdfDocument);

            return bmp;

        } catch (Exception e) {
            //todo with exception
        }
        return null;
    }

    private byte[] intToDWord(int parValue) {
        byte[] retValue = new byte[]{(byte) (parValue & 255), (byte) (parValue >> 8 & 255), (byte) (parValue >> 16 & 255), (byte) (parValue >> 24 & 255)};
        return retValue;
    }

    private byte[] intToWord(int parValue) {
        byte[] retValue = new byte[]{(byte) (parValue & 255), (byte) (parValue >> 8 & 255)};
        return retValue;
    }

    private byte[] convertTo1BPP(Bitmap inputBitmap, int darknessThreshold) {
        int width = inputBitmap.getWidth();
        int height = inputBitmap.getHeight();
        ByteArrayOutputStream mImageStream = new ByteArrayOutputStream();
        int BITMAPFILEHEADER_SIZE = 14;
        int BITMAPINFOHEADER_SIZE = 40;
        short biPlanes = 1;
        short biBitCount = 1;
        int biCompression = 0;
        int biSizeImage = (width * biBitCount + 31 & -32) / 8 * height;
        int biXPelsPerMeter = 0;
        int biYPelsPerMeter = 0;
        int biClrUsed = 2;
        int biClrImportant = 2;
        byte[] bfType = new byte[]{66, 77};
        short bfReserved1 = 0;
        short bfReserved2 = 0;
        int bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE + 8;
        int bfSize = bfOffBits + biSizeImage;
        byte[] colorPalette = new byte[]{0, 0, 0, -1, -1, -1, -1, -1};
        int monoBitmapStride = (width + 31 & -32) / 8;
        byte[] newBitmapData = new byte[biSizeImage];

        try {
            mImageStream.write(bfType);
            mImageStream.write(this.intToDWord(bfSize));
            mImageStream.write(this.intToWord(bfReserved1));
            mImageStream.write(this.intToWord(bfReserved2));
            mImageStream.write(this.intToDWord(bfOffBits));
            mImageStream.write(this.intToDWord(BITMAPINFOHEADER_SIZE));
            mImageStream.write(this.intToDWord(width));
            mImageStream.write(this.intToDWord(height));
            mImageStream.write(this.intToWord(biPlanes));
            mImageStream.write(this.intToWord(biBitCount));
            mImageStream.write(this.intToDWord(biCompression));
            mImageStream.write(this.intToDWord(biSizeImage));
            mImageStream.write(this.intToDWord(biXPelsPerMeter));
            mImageStream.write(this.intToDWord(biYPelsPerMeter));
            mImageStream.write(this.intToDWord(biClrUsed));
            mImageStream.write(this.intToDWord(biClrImportant));
            mImageStream.write(colorPalette);
            int[] imageData = new int[height * width];
            inputBitmap.getPixels(imageData, 0, width, 0, 0, width, height);

            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    int pixelIndex = y * width + x;
                    int mask = 128 >> (x & 7);
                    int pixel = imageData[pixelIndex];
                    int R = Color.red(pixel);
                    int G = Color.green(pixel);
                    int B = Color.blue(pixel);
                    int A = Color.alpha(pixel);
                    boolean set = A < darknessThreshold || R + G + B > darknessThreshold * 3;
                    if (set) {
                        int index = (height - y - 1) * monoBitmapStride + (x >>> 3);
                        newBitmapData[index] = (byte) (newBitmapData[index] | mask);
                    }
                }
            }

            mImageStream.write(newBitmapData);
        } catch (Exception var36) {
            var36.printStackTrace();
        }

        return mImageStream.toByteArray();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_FILE: {
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        //========Get the file path===============//
                        pathpdf = extras.getString(FOLDER_PATH_KEY);
                        txtpathpdf.setText("Direccion Path: "+pathpdf);
                        }
                    }
                }
                break;
            }
    }


    boolean valdiarpdf(){
        boolean exist = false;
        if (txtpathpdf.getText().equals("seleccionarpdf")){

            exist = false;
        }else{
            exist = true;
        }


        return exist;
    }

}
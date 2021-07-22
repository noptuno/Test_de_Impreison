package com.desarrollo.printata;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.desarrollo.printata.Configuracion.DMRPrintSettings;
import com.desarrollo.printata.Configuracion.DOPrintMainActivity;
import com.desarrollo.printata.Constantes.Constante;


import com.example.tscdll.TSCActivity;
import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;



public class MenuPrincipal extends AppCompatActivity {

    private int condicion = 0;
    TextView text,txtversion;
    String idlogin;



    private Button btnTag, btnEti,btnScann;
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

    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    TSCActivity TscDll = new TSCActivity();
    private int m_configurado;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        txtsucursal = findViewById(R.id.txt_sucursal);
        txtversion = findViewById(R.id.txtversion);

        btnEti = findViewById(R.id.btneti);
        btnEti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (m_configurado != 0) {

                    Intent intent2 = new Intent(MenuPrincipal.this, ListTagGondola.class);
                    intent2.putExtra("mac", m_printerMAC);
                    intent2.putExtra("suc", m_sucursal);
                    intent2.putExtra("ip", m_ip);
                    startActivity(intent2);

                } else {

                    Toast.makeText(MenuPrincipal.this, "Debe configurar las opciones de la aplicacion", Toast.LENGTH_SHORT).show();

                }

            }
        });

        btnTag = findViewById(R.id.btnTag);

        btnTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (m_configurado != 0) {
                    Intent intent2 = new Intent(MenuPrincipal.this, TagGondola.class);
                    intent2.putExtra("mac", m_printerMAC);
                    intent2.putExtra("suc", m_sucursal);
                    intent2.putExtra("ip", m_ip);
                    startActivity(intent2);
                } else {
                    Toast.makeText(MenuPrincipal.this,
                            "Debe configurar las opciones de la aplicación",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnScann = findViewById(R.id.btn_scan);
        btnScann.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (m_configurado != 0) {
                    Intent intent2 = new Intent(MenuPrincipal.this, ScannerElo.class);
                    intent2.putExtra("mac", m_printerMAC);
                    intent2.putExtra("suc", m_sucursal);
                    intent2.putExtra("ip", m_ip);
                    startActivity(intent2);
                } else {
                    Toast.makeText(MenuPrincipal.this,
                            "Debe configurar las opciones de la aplicación",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });

        AidcManager.create(this, new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();

            }
        });

        PackageInfo pinfo;
        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);

        txtversion.setText("Version: "+pinfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }




    @Override
    protected void onResume() {
        super.onResume();
        cargardatos();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

    }

    static BarcodeReader getBarcodeObject() {
        return barcodeReader;
    }

    public void showToast(final String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
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
            txtsucursal.setText(m_sucursal);

        } else {

            Toast.makeText(MenuPrincipal.this,
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

    @Override
    public void onBackPressed() {

        super.onBackPressed();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            AlertDialog.Builder mBuilder = new AlertDialog.Builder(MenuPrincipal.this);
            View mView = getLayoutInflater().inflate(R.layout.alerdiaglog, null);
            final EditText mEmail = (EditText) mView.findViewById(R.id.etEmail);
            final EditText mPassword = (EditText) mView.findViewById(R.id.etPassword);
            final TextView text = (TextView) mView.findViewById(R.id.txt_sucursal);
            Button mLogin = (Button) mView.findViewById(R.id.btnLogin);



            mBuilder.setView(mView);
            final AlertDialog dialog = mBuilder.create();
            dialog.show();



            mLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mPassword.getText().toString().isEmpty()) {

                        sharedPref = getSharedPreferences(Constante.LOGINNAME, Context.MODE_PRIVATE);

                            String PASSWORD = sharedPref.getString(Constante.PASSWORD, Constante.PASSWORDROOT);

                            if (mPassword.getText().toString().equals(PASSWORD) || mPassword.getText().toString().equals(Constante.PASSWORDROOT) ){

                                Intent intent = new Intent(MenuPrincipal.this, DOPrintMainActivity.class);
                                startActivityForResult(intent, REQUEST_PICK_CONFIGURACION);
                                dialog.dismiss();

                            }else{

                                Toast.makeText(MenuPrincipal.this,"Acceso Denegado", Toast.LENGTH_SHORT).show();

                            }

                    } else {

                        mPassword.setError("Faltan Datos");
                        mPassword.requestFocus();

                    }
                }
            });


            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

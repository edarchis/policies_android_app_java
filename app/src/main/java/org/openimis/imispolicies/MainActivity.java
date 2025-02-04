//Copyright (c) 2016-%CurrentYear% Swiss Agency for Development and Cooperation (SDC)
//
//The program users must agree to the following terms:
//
//Copyright notices
//This program is free software: you can redistribute it and/or modify it under the terms of the GNU AGPL v3 License as published by the 
//Free Software Foundation, version 3 of the License.
//This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of 
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU AGPL v3 License for more details www.gnu.org.
//
//Disclaimer of Warranty
//There is no warranty for the program, to the extent permitted by applicable law; except when otherwise stated in writing the copyright 
//holders and/or other parties provide the program "as is" without warranty of any kind, either expressed or implied, including, but not 
//limited to, the implied warranties of merchantability and fitness for a particular purpose. The entire risk as to the quality and 
//performance of the program is with you. Should the program prove defective, you assume the cost of all necessary servicing, repair or correction.
//
//Limitation of Liability 
//In no event unless required by applicable law or agreed to in writing will any copyright holder, or any other party who modifies and/or 
//conveys the program as permitted above, be liable to you for damages, including any general, special, incidental or consequential damages 
//arising out of the use or inability to use the program (including but not limited to loss of data or data being rendered inaccurate or losses 
//sustained by you or third parties or a failure of the program to operate with any other programs), even if such holder or other party has been 
//advised of the possibility of such damages.
//
//In case of dispute arising out or in relation to the use of the program, it is subject to the public law of Switzerland. The place of jurisdiction is Berne.

package org.openimis.imispolicies;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.exact.general.General;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import static org.openimis.imispolicies.BuildConfig.SHOW_PAYMENT_MENU;
import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String PREFS_NAME = "CMPref";
    private NavigationView navigationView;

    private SQLHandler sqlHandler;
    private WebView wv;
    private final Context context = this;
    private String selectedFilePath;
    static Global global;
    //private General general;
    private static final int MENU_LANGUAGE_1 = Menu.FIRST;
    private static final int MENU_LANGUAGE_2 = Menu.FIRST + 1;
    private String Language1 = "";
    private String Language2 = "";
    private String LanguageCode1 = "";
    private String LanguageCode2 = "";
    private String selectedLanguage;
    public String ImagePath;
    public String InsureeNumber;
    static TextView Login;
    static TextView OfficerName;
    ClientAndroidInterface ca;
    String aBuffer = "";
    String calledFrom = "java";
    public File f;

    General _General = new General(AppInformation.DomainInfo.getDomain());

    final String VersionField = "AppVersionImis";
    final String ApkFileLocation = AppInformation.DomainInfo.getDomain() + "/Apps/IMIS.apk";
    final int SIMPLE_NOTFICATION_ID = 98029;

    NotificationManager mNotificationManager;
    Vibrator vibrator;
    public String etRarPassword = "";
    boolean isUserLogged = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ClientAndroidInterface.RESULT_LOAD_IMG && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            wv.evaluateJavascript(String.format("selectImageCallback(\"%s\");", selectedImage), null);
        } else if (requestCode == ClientAndroidInterface.RESULT_SCAN && resultCode == RESULT_OK && data != null) {
            this.InsureeNumber = data.getStringExtra("SCAN_RESULT");
        } else if (requestCode == 4 && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    byte[] bytes = IOUtils.toByteArray(getContentResolver().openInputStream(uri));
                    String path = global.getSubdirectory("Database");
                    f = new File(path, "MasterData.rar");
                    if (f.exists() || f.createNewFile())
                        new FileOutputStream(f).write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ShowDialogTex2();
            }
        } else if (requestCode == 4 && resultCode == 0) {
            finish();
        } else {
            //if user cancels
            ClientAndroidInterface.inProgress = false;
            this.InsureeNumber = null;
            this.ImagePath = null;
        }
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Check if user has language set
        SharedPreferences spHF = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedLanguage = spHF.getString("Language", "en");
        changeLanguage(selectedLanguage, false);
        super.onCreate(savedInstanceState);
        //Request for camera permission//
        //By Herman 20/2/2018
        requestPermision();

        setContentView(R.layout.activity_main);

        sqlHandler = new SQLHandler(this);

        sqlHandler.isPrivate = true;
        global = (Global) getApplicationContext();
        //Set the Image folder path
        global.setImageFolder(getApplicationContext().getApplicationInfo().dataDir + "/Images/");
        CreateFolders();
        //Check if database exists
        File database = getApplicationContext().getDatabasePath(SQLHandler.DBNAME);
        if (!database.exists()) {
            sqlHandler.getReadableDatabase();
            if (copyDatabase(this)) {
                Toast.makeText(this, "Copy database success", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Copy database failed", Toast.LENGTH_SHORT).show();
                return;
            }
        } else
            sqlHandler.getReadableDatabase();

        //Create image folder
        createImageFolder();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //noinspection deprecation
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);
        wv = findViewById(R.id.webview);
        WebSettings settings = wv.getSettings();
        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        settings.setJavaScriptEnabled(true);
        //noinspection deprecation
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAppCacheEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setUseWideViewPort(true);
        settings.setSaveFormData(true);
        settings.setAllowFileAccess(true);
        //noinspection deprecation
        settings.setEnableSmoothTransition(true);
        settings.setLoadWithOverviewMode(true);
        wv.addJavascriptInterface(new ClientAndroidInterface(this), "Android");

        //Register for context acquire_menu
        registerForContextMenu(wv);

        wv.loadUrl("file:///android_asset/pages/Home.html");
        wv.setWebViewClient(new MyWebViewClient(MainActivity.this));

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                //noinspection ConstantConditions
                getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
                getSupportActionBar().setSubtitle(title);
            }
        });
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerview = navigationView.getHeaderView(0);
        Login = headerview.findViewById(R.id.tvLogin);
        OfficerName = headerview.findViewById(R.id.tvOfficerName);

        Login.setOnClickListener(v -> {
            wv.loadUrl("file:///android_asset/pages/Login.html?s=3");
            DrawerLayout drawer1 = findViewById(R.id.drawer_layout);
            drawer1.closeDrawer(GravityCompat.START);
            SetLoggedIn(getApplication().getResources().getString(R.string.Login), getApplication().getResources().getString(R.string.Logout));
        });
        ca = new ClientAndroidInterface(context);
        if (ca.isMasterDataAvailable() > 0) {
            loadLanguages();
        }

        SetLoggedIn(getResources().getString(R.string.Login), getResources().getString(R.string.Logout));

        navigationView.setCheckedItem(R.id.nav_home);
        if (TextUtils.isEmpty(global.getOfficerCode())) {
            //Edited By HERMAN
            ShowDialogTex();
        }
        _General.isSDCardAvailable();

        setVisibilityOfPaymentMenu();
    }

    private void setVisibilityOfPaymentMenu() {
        navigationView = findViewById(R.id.nav_view);
        MenuItem navPayment = navigationView.getMenu().findItem(R.id.nav_payment);
        navPayment.setVisible(AppInformation.MenuInfo.getShowPaymentNumberMenu());
    }

    @Override
    protected void onResume() {
        super.onResume();
        OfficerName.setText(global.getOfficerName());
    }

    public static void SetLoggedIn(String Lg, String Lo) {
        if (global.isLoggedIn()) {
            Login.setText(Lo);
        } else {
            Login.setText(Lg);
        }
    }

    public void CreateFolders() {
        boolean dirsCreated = !"".equals(global.getMainDirectory());
        String[] subdirectories = {"Enrolment", "Photos", "Database", "Authentications", "AcceptedFeedback", "RejectedFeedback", "AcceptedRenewal", "RejectedRenewal", "Family", "EnrolmentXML"};
        for (String dir : subdirectories) {
            dirsCreated &= !"".equals(global.getSubdirectory(dir));
        }

        if (!dirsCreated) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.ImisDirNotCreated), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLanguages() {
        ClientAndroidInterface ca = new ClientAndroidInterface(context);
        JSONArray Languages = ca.getLanguage();
        JSONObject LanguageObject = null;

        try {
            LanguageObject = Languages.getJSONObject(0);
            Language1 = (LanguageObject.getString("LanguageName"));
            LanguageCode1 = (LanguageObject.getString("LanguageCode"));
            if (Languages.length() > 1) {
                LanguageObject = Languages.getJSONObject(1);
                Language2 = (LanguageObject.getString("LanguageName"));
                LanguageCode2 = (LanguageObject.getString("LanguageCode"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void openDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(getResources().getString(R.string.NoInternetTitle))
                .setMessage(getResources().getString(R.string.DoImport))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes),
                        (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("*/*");
                            try {
                                startActivityForResult(intent, 4);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.NoFileExporerInstalled), Toast.LENGTH_SHORT).show();
                            }
                        }).setNegativeButton(getResources().getString(R.string.No),
                (dialog, id) -> {
                    dialog.cancel();
                    finish();
                }).show();
    }

    public void openDialogFromPage() {
        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                MainActivity.this);

// Setting Dialog Title
        alertDialog2.setTitle(getResources().getString(R.string.NoInternetTitle));
        alertDialog2.setMessage(getResources().getString(R.string.DoImport));

// Setting Icon to Dialog
        // alertDialog2.setIcon(R.drawable.delete);

// Setting Positive "Yes" Btn
        alertDialog2.setPositiveButton(getResources().getString(R.string.Yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        try {
                            startActivityForResult(intent, 4);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.NoFileExporerInstalled), Toast.LENGTH_SHORT).show();
                        }
                        // Write your code here to execute after dialog
                    }
                }).setNegativeButton(getResources().getString(R.string.No),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

// Showing Alert Dialog
        alertDialog2.show();
    }

    public void ConfirmDialog(String filename) {
        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                MainActivity.this);

// Setting Dialog Title
        alertDialog2.setTitle(getResources().getString(R.string.LoadFile));
        alertDialog2.setMessage(filename);
        alertDialog2.setCancelable(false);

// Setting Icon to Dialog
        // alertDialog2.setIcon(R.drawable.delete);

// Setting Positive "Yes" Btn
        alertDialog2.setPositiveButton(getResources().getString(R.string.Ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MasterDataLocalAsync masterDataLocalAsync = new MasterDataLocalAsync();
                        masterDataLocalAsync.execute();
                        // Write your code here to execute after dialog
                    }
                }).setNegativeButton(getResources().getString(R.string.Quit),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });

// Showing Alert Dialog
        alertDialog2.show();
    }

    public void ConfirmDialogPage(String filename) {
        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                MainActivity.this);

// Setting Dialog Title
        alertDialog2.setTitle(getResources().getString(R.string.LoadFile));
        alertDialog2.setMessage(filename);
        alertDialog2.setCancelable(false);

// Setting Icon to Dialog
        // alertDialog2.setIcon(R.drawable.delete);

// Setting Positive "Yes" Btn
        alertDialog2.setPositiveButton(getResources().getString(R.string.Ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MasterDataLocalAsync masterDataLocalAsync = new MasterDataLocalAsync();
                        masterDataLocalAsync.execute();
                        // Write your code here to execute after dialog
                    }
                }).setNegativeButton(getResources().getString(R.string.Quit),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

// Showing Alert Dialog
        alertDialog2.show();
    }

    public void openDialogMsg(String msg) {
        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                MainActivity.this);

// Setting Dialog Title
        alertDialog2.setTitle(getResources().getString(R.string.Incomplete));
        alertDialog2.setMessage(msg);

// Setting Icon to Dialog
        // alertDialog2.setIcon(R.drawable.delete);

// Setting Positive "Yes" Btn
        alertDialog2.setPositiveButton(getResources().getString(R.string.Ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog
                    }
                });

// Showing Alert Dialog
        alertDialog2.show();
    }

    public void ShowDialogTex() {
        final ClientAndroidInterface ca = new ClientAndroidInterface(context);
        final int MasterData = ca.isMasterDataAvailable();

//      OfficerCode= ca.ShowDialogText() ;
        LayoutInflater li = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View promptsView = li.inflate(R.layout.dialog, null);

        android.support.v7.app.AlertDialog alertDialog = null;

        final android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(
                context);

        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.txtOfficerCode);
        final TextView tVDialogTile = (TextView) promptsView.findViewById(R.id.tvDialogTitle);

        if (MasterData == 0) {
            tVDialogTile.setText(getResources().getString(R.string.MasterDataNotFound));
            userInput.setVisibility(View.GONE);
        }


        // set dialog message
        final String result = "";

        int positiveButton, negativeButton;
        if (MasterData > 0) {
            positiveButton = R.string.Ok;
            negativeButton = R.string.Cancel;

        } else {
            positiveButton = R.string.Yes;
            negativeButton = R.string.No;
        }

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getResources().getString(positiveButton),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    if (MasterData > 0) {
                                        if (ca.isOfficerCodeValid(userInput.getText().toString())) {
                                            global.setOfficerCode(userInput.getText().toString());
                                            OfficerName.setText(global.getOfficerName());
//                                            if(_General.isNetworkAvailable(MainActivity.this)){
//                                                ca.getOfficerVillages(userInput.getText().toString());
//                                            }
                                        } else {
                                            ShowDialogTex();
                                            ca.ShowDialog(getResources().getString(R.string.IncorrectOfficerCode));
                                        }
                                    } else {
                                        if (!_General.isNetworkAvailable(MainActivity.this)) {
                                            openDialog();
                                        } else {
                                            MasterDataAsync masterDataAsync = new MasterDataAsync();
                                            masterDataAsync.execute();

                                        }
                                        //ca.downloadMasterData();
                                        //ShowDialogTex();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .setNegativeButton(getResources().getString(negativeButton),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                finish();
                            }
                        });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    public void ShowDialogTex2() {

        final ClientAndroidInterface ca = new ClientAndroidInterface(context);
        LayoutInflater li = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View promptsView = li.inflate(R.layout.rar_pass_dialog, null);

        android.support.v7.app.AlertDialog alertDialog = null;

        final android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(
                context);

        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.etRarPass);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes),
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    etRarPassword = userInput.getText().toString();
                                    getMasterDataText2(f.getPath(), etRarPassword);

                                    if (calledFrom == "java") {
                                        ConfirmDialog((f.getName()).toString());
                                    } else {
                                        ConfirmDialogPage((f.getName()).toString());
                                    }
                                } catch (Exception e) {
                                    e.getMessage();
                                }
                            }

                        })
                .setNegativeButton(getResources().getString(R.string.No),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                finish();
                            }
                        });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    public String getMasterDataText2(String fileName, String password) {
        ca.unZipWithPassword(fileName, password);
        String fname = "MasterData.txt";
        try {
            String dir = global.getSubdirectory("Database");
            File myFile = new File(dir, fname);//"/"+dir+"/MasterData.txt"
//            BufferedReader myReader = new BufferedReader(
//                    new InputStreamReader(
//                            new FileInputStream(myFile), "UTF32"));
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            aBuffer = myReader.readLine();

            myReader.close();
/*            Scanner in = new Scanner(new FileReader("/"+dir+"/MasterData.txt"));
            StringBuilder sb = new StringBuilder();
            while(in.hasNext()) {
                sb.append(in.next());
            }
            in.close();
            aBuffer = sb.toString();*/
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aBuffer;
    }

    public String getMasterDataText(String filename) {
        ca.unZip(filename);
        String fname = "MasterData.txt";
        try {
            String dir = global.getSubdirectory("Database");
            File myFile = new File(dir, fname);//"/"+dir+"/MasterData.txt"
//            BufferedReader myReader = new BufferedReader(
//                    new InputStreamReader(
//                            new FileInputStream(myFile), "UTF32"));
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            aBuffer = myReader.readLine();

            myReader.close();
/*            Scanner in = new Scanner(new FileReader("/"+dir+"/MasterData.txt"));
            StringBuilder sb = new StringBuilder();
            while(in.hasNext()) {
                sb.append(in.next());
            }
            in.close();
            aBuffer = sb.toString();*/
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aBuffer;
    }

    private int getCheckedMenuItem() {
        Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.isChecked())
                return menuItem.getItemId();
        }
        return -1;
    }


    private boolean copyDatabase(Context context) {
        try {
            InputStream inputStream = getApplicationContext().getAssets().open("database/" + SQLHandler.DBNAME);
            String outFileName = getApplicationContext().getApplicationInfo().dataDir + "/databases/" + SQLHandler.DBNAME;
            OutputStream outputStream = new FileOutputStream(outFileName);
            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();

            Log.w("MainActivity", "DB Copied");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createImageFolder() {
        File imageFolder = new File(global.getImageFolder());
        if (!imageFolder.exists())
            imageFolder.mkdir();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the acquire_menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.acquire_menu.language, acquire_menu);
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_LANGUAGE_1, 0, Language1);
        if (!Language2.equals("")) {
            menu.add(0, MENU_LANGUAGE_2, 0, Language2);
        }
        return true;
    }

    private void changeLanguage(String LanguageCode, boolean withRefresh) {

        //General gen = new General();
        _General.ChangeLanguage(this, LanguageCode);

        if (withRefresh) {
            //Restart the activity for change to be affected
            Intent refresh = new Intent(MainActivity.this, MainActivity.class);
            startActivity(refresh);
            finish();
        }
        setPreferences();
        //OfficerName.setText(global.getOfficerName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case MENU_LANGUAGE_1:
                selectedLanguage = LanguageCode1;
                changeLanguage(selectedLanguage, true);

                return true;
            case MENU_LANGUAGE_2:
                selectedLanguage = LanguageCode2;
                changeLanguage(selectedLanguage, true);
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();

        if (id == R.id.nav_home) {
            wv.loadUrl("file:///android_asset/pages/Home.html");
        } else if (id == R.id.nav_acquire) {
            Intent intent = new Intent(this, Acquire.class);
            startActivity(intent);
        } else if (id == R.id.nav_enrolment) {
            wv.loadUrl("file:///android_asset/pages/Enrollment.html");
        } else if (id == R.id.nav_modify_family) {
            global = (Global) getApplicationContext();
            if (global.isLoggedIn()) {
                wv.loadUrl("file:///android_asset/pages/Search.html");
            } else {
                wv.loadUrl("file:///android_asset/pages/Login.html?s=1");
            }

        } else if (id == R.id.nav_renewal) {
            Intent i = new Intent(this, RenewList.class);
            startActivity(i);

        } else if (id == R.id.nav_reports) {
            Global global = (Global) getApplicationContext();
            if (global.isLoggedIn()) {
                Intent i = new Intent(this, Reports.class);
                startActivity(i);
            } else {
                wv.loadUrl("file:///android_asset/pages/Login.html?s=4");
            }
        } else if (id == R.id.nav_feedback) {
            Intent intent = new Intent(this, FeedbackList.class);
            startActivity(intent);
        } else if (id == R.id.nav_sync) {
            wv.loadUrl("file:///android_asset/pages/Sync.html");
        } else if (id == R.id.nav_about) {
            wv.loadUrl("file:///android_asset/pages/About.html");
        } else if (id == R.id.nav_settings) {
            wv.loadUrl("file:///android_asset/pages/Settings.html");
        } else if (id == R.id.nav_quit) {
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.QuitAppQuestion))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.Yes), (dialogInterface, i) -> {
                        finish();
                        System.exit(0);
                    })
                    .setNegativeButton(getResources().getString(R.string.No), (dialogInterface, i) -> {
                    }).create().show();
        } else if (id == R.id.nav_enquire) {
            global = (Global) getApplicationContext();
            if (global.isLoggedIn()) {
                Intent intent = new Intent(this, Enquire.class);
                startActivity(intent);
            } else {
                wv.loadUrl("file:///android_asset/pages/Login.html?s=5");
            }
        } else if (id == R.id.nav_payment) {
            ClientAndroidInterface ca = new ClientAndroidInterface(context);
            ca.launchPayment();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (wv.canGoBack()) {
                        if (global.getCurrentUrl() != null)
                            wv.loadUrl("file:///android_asset/pages/" + global.getCurrentUrl());
                        else
                            wv.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public class MasterDataAsync extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd = null;

        @Override
        protected void onPreExecute() {

            pd = ProgressDialog.show(context, context.getResources().getString(R.string.Sync), context.getResources().getString(R.string.DownloadingMasterData));
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ClientAndroidInterface ca = new ClientAndroidInterface(context);
            try {
                ca.startDownloading();
            } catch (JSONException | UserException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            pd.dismiss();
            ShowDialogTex();

            Intent refresh = new Intent(MainActivity.this, MainActivity.class);
            startActivity(refresh);
            finish();
            setPreferences();
        }
    }

    public class MasterDataLocalAsync extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd = null;

        @Override
        protected void onPreExecute() {

            pd = ProgressDialog.show(context, context.getResources().getString(R.string.Sync), context.getResources().getString(R.string.DownloadingMasterData));
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ClientAndroidInterface ca = new ClientAndroidInterface(context);
            try {
                ca.importMasterData(aBuffer);
            } catch (JSONException | UserException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            pd.dismiss();
            ShowDialogTex();

            Intent refresh = new Intent(MainActivity.this, MainActivity.class);
            startActivity(refresh);
            finish();
            setPreferences();
        }
    }


    private void setPreferences() {
        SharedPreferences Lang = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = Lang.edit();
        editor.putString("Language", selectedLanguage);
        editor.apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        setPreferences();
    }

    private void CheckForUpdates() {
        if (_General.isNetworkAvailable(MainActivity.this)) {
            if (_General.isNewVersionAvailable(VersionField, MainActivity.this, getApplicationContext().getPackageName())) {
                //Show notification bar
                mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                //final Notification NotificationDetails = new Notification(R.drawable.ic_launcher_policies, getResources().getString(R.string.NotificationAlertText), System.currentTimeMillis());
                //NotificationDetails.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
                //NotificationDetails.setLatestEventInfo(context, ContentTitle, ContentText, intent);
                //mNotificationManager.notify(SIMPLE_NOTFICATION_ID, NotificationDetails);
                Context context = getApplicationContext();
                CharSequence ContentTitle = getResources().getString(R.string.ContentTitle);
                CharSequence ContentText = getResources().getString(R.string.ContentText);

                Intent NotifyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ApkFileLocation));

                PendingIntent intent = PendingIntent.getActivity(MainActivity.this, 0, NotifyIntent, 0);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "M_CH_ID");
                builder.setAutoCancel(false);
                builder.setContentTitle(ContentTitle);
                builder.setContentText(ContentText);
                builder.setSmallIcon(R.drawable.ic_statistics);
                builder.setContentIntent(intent);
                builder.setOngoing(false);

                mNotificationManager.notify(SIMPLE_NOTFICATION_ID, builder.build());
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(500);
            }
        }
    }

    //Ask for permission
    public void requestPermision() {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.VIBRATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CHANGE_WIFI_STATE};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        // Ask for "Manage External Storage" permission, required in Android 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String[] Android11Permissions = {Manifest.permission.MANAGE_EXTERNAL_STORAGE};
            if (!hasPermissions(this, Android11Permissions)) {
                ActivityCompat.requestPermissions(this, Android11Permissions, PERMISSION_ALL);
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void LoginDialogBox(final String page) {
        if (!ca.CheckInternetAvailable())
            return;

        global = (Global) MainActivity.this.getApplicationContext();
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.login_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final TextView username = (TextView) promptsView.findViewById(R.id.UserName);
        final TextView password = (TextView) promptsView.findViewById(R.id.Password);

        username.setText(global.getOfficerCode());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(MainActivity.this.getResources().getString(R.string.button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (!username.getText().toString().equals("") && !password.getText().toString().equals("")) {

                                    new Thread() {
                                        public void run() {
                                            ClientAndroidInterface cai = new ClientAndroidInterface(context);
                                            isUserLogged = cai.LoginToken(username.getText().toString(), password.getText().toString());

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SetLoggedIn(MainActivity.this.getResources().getString(R.string.Login), MainActivity.this.getResources().getString(R.string.Logout));
                                                }
                                            });

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isUserLogged) {
                                                        if (page.equals("Enquire")) {
                                                            Intent intent = new Intent(MainActivity.this, Enquire.class);
                                                            startActivity(intent);
                                                            Toast.makeText(MainActivity.this, MainActivity.this.getResources().getString(R.string.Login_Successful), Toast.LENGTH_LONG).show();
                                                        }
                                                        if (page.equals("Reports")) {
                                                            Intent intent = new Intent(MainActivity.this, Reports.class);
                                                            startActivity(intent);
                                                            Toast.makeText(MainActivity.this, MainActivity.this.getResources().getString(R.string.Login_Successful), Toast.LENGTH_LONG).show();
                                                        }

                                                    } else {
                                                        ca.ShowDialog(MainActivity.this.getResources().getString(R.string.LoginFail));
                                                    }
                                                }
                                            });

                                        }
                                    }.start();


                                } else {
                                    Toast.makeText(MainActivity.this, MainActivity.this.getResources().getString(R.string.Enter_Credentials), Toast.LENGTH_LONG).show();
                                }
                            }
                        }).setNegativeButton(MainActivity.this.getResources().getString(R.string.button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public String getSelectedLanguage() {
        return selectedLanguage;
    }
}

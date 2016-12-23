package com.disarm.sanna.pdm.DisarmConnect;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.disarm.sanna.pdm.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sanna on 16-02-2016.
 */
public class MyService extends Service {

    public static WifiManager wifi;
    public static String checkWifiState="0x";
    public static int level;
    public static BatteryLevel bl;
    public static WifiScanReceiver wifiReciever;
    public static boolean isHotspotOn,c;
    public static WifiInfo wifiInfo;
    public static List<String> IpAddr;
    public static String mobileAPName = "DH";
    public static String dbAPName = "DisarmHotspotDB";
    public static String dbPass = "DisarmDB";
    public FileReader fr = null;
    public static int count=0,startwififirst = 1;
    public static Handler handler;
    public static double wifiState;
    public static int macCount = 0;
    public static String TAG1 = "Timer_Toggle";
    public static String TAG2 = "WifiConnect";
    public static String TAG3 = "Toggler";
    public static String TAG4 = "Searching DB";
    public Timer_Toggler tt;
    public SearchingDisarmDB sDDB;
    public WifiConnect wifiC;
    private final IBinder myServiceBinder = new MyServiceBinder();
    public BufferedReader br = null;
    private Logger logger;
    public static String phoneVal;
    public static String presentState="wifi";
    public static List<ScanResult> wifiScanList;
    public static int bestAvailableChannel;

    @Override
    public IBinder onBind(Intent intent) {

        return myServiceBinder;
    }
    public class MyServiceBinder extends Binder {
        public MyService getService() {
            // Return this instance of SyncService so activity can call public methods
            return MyService.this;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();

        // DisarmConnect Started
        Log.v("MyService:", "DisarmConnect Started");

        // WifiScanReceiver registered
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiReciever = new WifiScanReceiver();
        registerReceiver(wifiReciever, filter);

        // Start scan for Wifi List
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifi.startScan();

        // Logger Initiated
        logger = new Logger();

        // Battery Level Indicator registered
        bl = new BatteryLevel();
        IntentFilter batfilter = new IntentFilter();
        batfilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(bl, batfilter);

        // Read Source to generate DH
        File file = new File(MainActivity.TARGET_DMS_PATH,"source.txt");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            // phoneVal storing the source from source.txt
            phoneVal = new String(data, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // DisarmConnect Service started
        logger.addRecordToLog("DisarmConnect Started");

        // Acquired WakeLock
        WakeLockHelper.keepCpuAwake(getApplicationContext(), true);
        WakeLockHelper.keepWiFiOn(getApplicationContext(), true);

        // Handler started
        handler = new Handler();
        tt = new Timer_Toggler(handler,getApplicationContext());
        wifiC = new WifiConnect(handler,getApplicationContext());
        sDDB = new SearchingDisarmDB(handler,getApplicationContext());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregistering receivers
        unregisterReceiver(wifiReciever);
        unregisterReceiver(bl);

        // Disabling hotspot and enabling WiFi Mode on app destroy
        isHotspotOn = ApManager.isApOn(MyService.this);
        if(isHotspotOn){
            ApManager.configApState(MyService.this);
            wifi.setWifiEnabled(true);
            Logger.addRecordToLog("Stopping DisarmConnect Hotspot Disabled");
        }

        // Stopping all services
        handler.removeCallbacksAndMessages(null);

        // Release lock
        WakeLockHelper.keepCpuAwake(getApplicationContext(), false);
        WakeLockHelper.keepWiFiOn(getApplicationContext(), false);

        // Adding stop record to log
        logger.addRecordToLog("DisarmConnect Stopped");
        Log.v("MyService:", "DisarmConnect Stopped");
    }

}
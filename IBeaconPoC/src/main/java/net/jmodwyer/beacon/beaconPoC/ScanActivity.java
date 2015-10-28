package net.jmodwyer.beacon.beaconPoC;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

//import com.google.android.gms.common.GooglePlayServicesClient;
//import com.google.android.gms.location.LocationClient;

import net.jmodwyer.ibeacon.ibeaconPoC.R;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

//import com.mapbox.mapboxsdk.overlay.GeoJSONPainter;
//import com.mapbox.mapboxsdk.overlay.Icon;
//import com.mapbox.mapboxsdk.overlay.Marker;
//import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.geometry.LatLng;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Adapted from original code written by D Young of Radius Networks.
 *
 * @author dyoung, jodwyer
 */
public class ScanActivity extends Activity implements BeaconConsumer,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        SensorEventListener {

    // Constant Declaration
    private static final String PREFERENCE_SCANINTERVAL = "scanInterval";
    private static final String PREFERENCE_TIMESTAMP = "timestamp";
    private static final String PREFERENCE_POWER = "power";
    private static final String PREFERENCE_PROXIMITY = "proximity";
    private static final String PREFERENCE_RSSI = "rssi";
    private static final String PREFERENCE_MAJORMINOR = "majorMinor";
    private static final String PREFERENCE_UUID = "uuid";
    private static final String PREFERENCE_INDEX = "index";
    private static final String PREFERENCE_LOCATION = "location";
    private static final String PREFERENCE_REALTIME = "realTimeLog";
    private static final String MODE_SCANNING = "Stop Scanning";
    private static final String MODE_STOPPED = "Start Scanning";
    protected static final String TAG = "ScanActivity";

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private FileHelper fileHelper;
    private BeaconManager beaconManager;
    private Region region;
    private int eventNum = 1;

    // This StringBuffer will hold the scan data for any given scan.  
    private StringBuffer logString;

    // Preferences - will actually have a boolean value when loaded.
    private Boolean index;
    private Boolean location;
    private Boolean uuid;
    private Boolean majorMinor;
    private Boolean rssi;
    private Boolean proximity;
    private Boolean power;
    private Boolean timestamp;
    private String scanInterval;
    // Added following a feature request from D.Schmid.
    private Boolean realTimeLog;
    private ImageView image;
    float mDeclination;
    private SensorManager mSensorManager;
    Sensor sensor;
    boolean isCompassOn = false;


    // LocationClient for Google Play Location Services
    GoogleApiClient mlocationClient;

    private ScrollView scroller;
    private EditText editText;
    private LatLng POS = new LatLng(0, 0);
    private LatLng POSCH = new LatLng(0, 0);
    private GoogleMap map;
    private double GPOSX, GPOSY, GPOSXCH, GPOSYCH;
    private Long IConvX, IConvY;
    private Float FConvX, FConvY;
    float oldRSSI, newRSSI, Cangle;

    private MapView mMapboxView;
    private String mString;

//    private MarkerOptions marker = new MarkerOptions().position(POS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        /**
         * Read Json from assets folder
         */
//        new DrawGeoJSON().execute();

        /**
         * Google MAP
         */
//        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
//        MarkerOptions marker = new MarkerOptions().position(POS);
//        map.addMarker(marker);
//        //POS=new LatLng(121,121);
//        // Move the camera instantly to NKUT with a zoom of 16.
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(POS, 16));
//        map.getUiSettings().setCompassEnabled(true);
//        map.getUiSettings().setMyLocationButtonEnabled(true);

        /**
         * MapboxView 2.1
         */
        mMapboxView = (com.mapbox.mapboxsdk.views.MapView) findViewById(R.id.mapview);
        setMapView();
        mMapboxView.onCreate(savedInstanceState);
        /**
         * Android SensorManager
         */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        //image=(ImageView) findViewById(R.id.imageViewCompass);
        //image.setMaxWidth(10);
        //image.setMaxHeight(10);

        /**
         * verifyBluetooth
         */
        verifyBluetooth();


        /**
         * Preferences
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        /**
         * IBeaconPoC libraries
         */
        BeaconScannerApp app = (BeaconScannerApp) this.getApplication();
        beaconManager = app.getBeaconManager();
        //beaconManager.setForegroundScanPeriod(10);
        region = app.getRegion();
        beaconManager.bind(this);
        fileHelper = app.getFileHelper();

        /**
         * Google Play Services - LocationClient
         */
        mlocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        /**
         * Initialise Scrolview and EditText
         */
        scroller = (ScrollView) ScanActivity.this.findViewById(R.id.scanScrollView);
//        editText = (EditText) ScanActivity.this.findViewById(R.id.scanText);

        /**
         * Initialise scan button.
         */
        getScanButton().setText(MODE_STOPPED);
    }

    private void setMapView() {

        mMapboxView.setStyleUrl(Style.LIGHT);
        mMapboxView.setCenterCoordinate(new LatLng(25.04129, 121.6152));
        mMapboxView.setZoomLevel(18);
        mMapboxView.setAccessToken("pk.eyJ1IjoiYm9va2phbiIsImEiOiJjaWV1MHRzcnYwZXA2bGVtMHM2aHU4NTkxIn0.v4UuMNZfBTmWrCy0B1zgug");

        new DrawGeoJSON().execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onBeaconServiceConnect() {
    }

    /**
     *
     * @param view
     */
    public void onScanButtonClicked(View view) {
        toggleScanState();
    }

    // Handle the user selecting "Settings" from the action bar.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.Settings:
                // Show settings
                startActivityForResult(new Intent(this, AppPreferenceActivity.class), 0);
                return true;
            case R.id.action_listfiles:
                // Launch list files activity
                startActivity(new Intent(this, FileHandlerActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Start and stop scanning, and toggle button label appropriately.
     */
    private void toggleScanState() {
        Button scanButton = getScanButton();
        String currentState = scanButton.getText().toString();
        if (currentState.equals(MODE_SCANNING)) {
            stopScanning(scanButton);
        } else {
            startScanning(scanButton);
        }
    }

    /**
     * start looking for beacons.
     */
    private void startScanning(final Button scanButton) {

        // Set UI elements to the correct state.
        scanButton.setText(MODE_SCANNING);
//        ((EditText) findViewById(R.id.scanText)).setText("");

        // Reset event counter
        eventNum = 1;
        // Get current values for logging preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.putAll(sharedPrefs.getAll());

        index = (Boolean) prefs.get(PREFERENCE_INDEX);
        location = (Boolean) prefs.get(PREFERENCE_LOCATION);
        uuid = (Boolean) prefs.get(PREFERENCE_UUID);
        majorMinor = (Boolean) prefs.get(PREFERENCE_MAJORMINOR);
        rssi = (Boolean) prefs.get(PREFERENCE_RSSI);
        proximity = (Boolean) prefs.get(PREFERENCE_PROXIMITY);
        power = (Boolean) prefs.get(PREFERENCE_POWER);
        timestamp = (Boolean) prefs.get(PREFERENCE_TIMESTAMP);
        scanInterval = (String) prefs.get(PREFERENCE_SCANINTERVAL);
        realTimeLog = (Boolean) prefs.get(PREFERENCE_REALTIME);

        // Get current background scan interval (if specified)
        if (prefs.get(PREFERENCE_SCANINTERVAL) != null) {
            beaconManager.setBackgroundBetweenScanPeriod(Long.parseLong(scanInterval));
        }

//        logToDisplay("Scanning...");

        // Initialise scan log
        logString = new StringBuffer();

        //Start scanning again.
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Iterator<Beacon> beaconIterator = beacons.iterator();
                    while (beaconIterator.hasNext()) {
                        Beacon beacon = beaconIterator.next();
                        logBeaconData(beacon);
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            // TODO - OK, what now then?
        }
    }

    /**
     * Stop looking for beacons.
     */
    private void stopScanning(Button scanButton) {

        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            // TODO - OK, what now then?
        }
        String scanData = logString.toString();
        if (scanData.length() > 0) {
            // Write file
            fileHelper.createFile(scanData);
            // Display file created message.
            Toast.makeText(getBaseContext(),
                    "File saved to:" + getFilesDir().getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            scanButton.setText(MODE_STOPPED);
        } else {
            // We didn't get any data, so there's no point writing an empty file.
            Toast.makeText(getBaseContext(),
                    "No data captured during scan, output file will not be created.",
                    Toast.LENGTH_SHORT).show();
            scanButton.setText(MODE_STOPPED);
        }
    }

    /**
     * @return reference to the start/stop scanning button
     */
    private Button getScanButton() {
        return (Button) findViewById(R.id.scanButton);
    }

    /**
     * @param beacon The detected beacon
     */
    private void logBeaconData(Beacon beacon) {

        StringBuilder scanString = new StringBuilder();

        if (index) {
            scanString.append(eventNum++);
        }
        if (beacon.getServiceUuid() == 0xfeaa) {
            if (beacon.getBeaconTypeCode() == 0x00) {
                scanString.append(" Eddystone-UID -> ");
                scanString.append(" Namespace : ").append(beacon.getId1());
                scanString.append(" Identifier : ").append(beacon.getId2());
                int switchs = 0;
                double posX = 0.0;
                double posY = 0.0;
                double counter = 1;
                char Id1[] = beacon.getId1().toString().toCharArray();
                char Id2[] = beacon.getId2().toString().toCharArray();
                int c = Id2[Id2.length - 1] - 48;
                scanString.append("char:").append(c);
                for (int i = 0; i < 10; i++) {
                    if (Id1[Id1.length - (i + 1)] != 'a') {
                        if (switchs == 0)
                            posX = posX / 10 + (Id1[Id1.length - (i + 1)] - 48);
                        if (switchs == 1) {
                            posX = posX + (Id1[Id1.length - (i + 1)] - 48) * counter;
                            counter *= 10;
                        }
                    } else {
                        posX /= 10;
                        switchs = 1;
                        counter = 1;
                    }
                }
                counter = 1;
                switchs = 0;
                for (int i = 0; i < 10; i++) {
                    if (Id2[Id2.length - (i + 1)] != 'a') {
                        if (switchs == 0)
                            posY = posY / 10 + (Id2[Id2.length - (i + 1)] - 48);
                        if (switchs == 1) {
                            posY = posY + (Id2[Id2.length - (i + 1)] - 48) * counter;
                            counter *= 10;
                        }
                    } else {
                        posY /= 10;
                        switchs = 1;
                        counter = 1;
                    }
                }
                newRSSI = beacon.getRssi();
                float distance = Math.abs(Math.abs(oldRSSI) - Math.abs(newRSSI));
                if (distance > 6) {
                    GPOSX += Math.cos(Cangle) * distance * 0.000001;
                    GPOSY += Math.sin(Cangle) * distance * 0.000001;
                    oldRSSI = newRSSI;
                }
                if (beacon.getRssi() > -65) {
                    GPOSX = posX;
                    GPOSY = posY;
                    POS = new LatLng(posX, posY);
                }
                scanString.append("X").append(posX).append("Y").append(posY);
                logEddystoneTelemetry(scanString, beacon);
            } else if (beacon.getBeaconTypeCode() == 0x10) {
                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                //scanString.append("URL::").append(beacon.getId1());
                //scanString.append(" Eddystone-URL -> " + url);
            } else if (beacon.getBeaconTypeCode() == 0x20) {
                //scanString.append(" Eddystone-TLM -> ");
                logEddystoneTelemetry(scanString, beacon);
            }
        } else {
            // Just an old fashioned iBeacon or AltBeacon...
            logGenericBeacon(scanString, beacon);
        }

//        logToDisplay(scanString.toString());
        //scanString.append("\n");

        // Code added following a feature request by D.Schmid - writes a single entry to a file
        // every time a beacon is detected, the file will only ever have one entry as it will be
        // recreated on each call to this method.
        // Get current background scan interval (if specified)
        if (realTimeLog) {
            // We're in realtime logging mode, create a new log file containing only this entry.
            fileHelper.createFile(scanString.toString(), "realtimelog.txt");
        }
        logString.append(scanString.toString());
    }

    /**
     * Logs iBeacon & AltBeacon data.
     */
    private void logGenericBeacon(StringBuilder scanString, Beacon beacon) {

        if (location) {
            //scanString.append(" Location: ").append(getCurrentLocation()).append(" ");
        }

        if (uuid) {
            //scanString.append(" UUID: ").append(beacon.getId1());
        }

        if (majorMinor) {
            scanString.append(" Maj. Mnr.: ");
            if (beacon.getId2() != null) {
                // scanString.append(beacon.getId2());
            }
            scanString.append("-");
            if (beacon.getId3() != null) {
                scanString.append(beacon.getId3());
                //map.moveCamera(CameraUpdateFactory.newLatLngZoom(POS, 16));
                /*
                newRSSI= beacon.getRssi();
                float distance= Math.abs(Math.abs(oldRSSI)-Math.abs(newRSSI));
                if (distance>6){
                    GPOSX+=Math.cos(Cangle)*distance*0.0000011;
                    GPOSY+=Math.sin(Cangle)*distance*0.0000011;
                    oldRSSI=newRSSI;
                }
                */
                if (beacon.getRssi() > -65) {
                    // GPOSX=25.04+beacon.getId2().toInt()*0.000001; //Major
                    //GPOSY=121.61+beacon.getId3().toInt()*0.000001; //Minor
                    String SConvX = "", SConvY = "";

                    char CConvX[], CConvY[];
                    CConvX = beacon.getId2().toString().toCharArray();
                    CConvY = beacon.getId3().toString().toCharArray();

                    for (int i = 0; i < 8; i++) {
                        SConvX += CConvX[i + 2];
                        SConvY += CConvY[i + 2];
                    }
                    IConvX = Long.parseLong(SConvX, 16);
                    IConvY = Long.parseLong(SConvY, 16);
                    FConvX = Float.intBitsToFloat(IConvX.intValue());
                    FConvY = Float.intBitsToFloat(IConvY.intValue());
                    GPOSX = Double.parseDouble(Float.toString(FConvX));
                    GPOSY = Double.parseDouble(Float.toString(FConvY));

                    Log.i("messageMajor", String.valueOf(GPOSX));
                    Log.i("messageMinor", String.valueOf(GPOSY));

                }

            }
            //POS=new LatLng(25.04,121.61);

        }

        if (rssi) {
            //scanString.append(" RSSI: ").append(beacon.getRssi());
        }

        if (proximity) {
            // scanString.append(" Proximity: ").append(BeaconHelper.getProximityString(beacon.getDistance()));
        }

        if (power) {
            //scanString.append(" Power: ").append(beacon.getTxPower());
        }

        if (timestamp) {
            //scanString.append(" Timestamp: ").append(BeaconHelper.getCurrentTimeStamp());
        }
        POS = new LatLng(GPOSX, GPOSY);
    }

    private void logEddystoneTelemetry(StringBuilder scanString, Beacon beacon) {
        // Do we have telemetry data?
        if (beacon.getExtraDataFields().size() > 0) {
            long telemetryVersion = beacon.getExtraDataFields().get(0);
            long batteryMilliVolts = beacon.getExtraDataFields().get(1);
            long pduCount = beacon.getExtraDataFields().get(3);
            long uptime = beacon.getExtraDataFields().get(4);

            scanString.append(" Telemetry version : " + telemetryVersion);
            scanString.append(" Uptime (sec) : " + uptime);
            scanString.append(" Battery level (mv) " + batteryMilliVolts);
            scanString.append(" Tx count: " + pduCount);
        }
    }

//    /**
//     *
//     * @param line
//     */
//    private void logToDisplay(final String line) {
//        if (GPOSX != GPOSXCH && GPOSYCH != GPOSY) {
//            GPOSXCH = GPOSX;
//            GPOSYCH = GPOSY;
//            System.out.println("aaa");
//            runOnUiThread(new Runnable() {
//                public void run() {
//
//                    //editText.append(line + "\n");
//
//                    // Temp code - don't really want to do this for every line logged, will look for a
//                    // workaround.
//                    //Linkify.addLinks(editText, Linkify.WEB_URLS);
//
//                    //scroller.fullScroll(View.FOCUS_DOWN);
//
//                    CameraPosition oldPos = map.getCameraPosition();
//                    CameraPosition pos = new CameraPosition.Builder(oldPos).target(POS).zoom(20).build();
//                    //CameraPosition cameraPosition = new CameraPosition.Builder(oldPos)
//                    //.target(POS).bearing(bearing).zoom(20).build();
//                    GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {
//                        @Override
//                        public void onFinish() {
//                            map.clear();
//                            marker.position(POS);
//                            map.addMarker(marker);
//                        }
//
//                        @Override
//                        public void onCancel() {
//                        }
//                    };
//                    //map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
//                    map.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 24, callback);
//
//                }
//
//
//                // map.moveCamera(CameraUpdateFactory.newLatLngZoom(POS, 20));
//
//
//            });
//
//        }
//
//    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();
        }
    }

    /* Location services code follows */

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mlocationClient.connect();
        mMapboxView.onStart();
    }

    @Override
    protected void onStop() {
        // Disconnect the client.
        mlocationClient.disconnect();
        super.onStop();
        mMapboxView.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        beaconManager.bind(this);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        mMapboxView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mMapboxView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapboxView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapboxView.onSaveInstanceState(outState);
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Uncomment the following line to display the connection status.
        // Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

//    @Override
//    public void onDisconnected() {
//        // Display the connection status
//        Toast.makeText(this, "Disconnected. Please re-connect.",
//                Toast.LENGTH_SHORT).show();
//    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

         /* Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Toast.makeText(getBaseContext(),
                    "Location services not available, cannot track device location.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        //updateCamera(event.values[0] + mDeclination);
        System.out.print(event.values[0] + mDeclination);
        Cangle = (float) Math.toRadians(Math.round(event.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public String getCurrentLocation() {
        /** Default "error" value is set for location, will be overwritten with the correct lat and
         *  long values if we're ble to connect to location services and get a reading.
         */
        String location = "Unavailable";
        if (mlocationClient.isConnected()) {
            Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mlocationClient);
            if (currentLocation != null) {
                location = Double.toString(currentLocation.getLatitude()) + "," +
                        Double.toString(currentLocation.getLongitude());
            }
        }
        return location;
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    private void updateCamera(float bearing) {
    /*
    CameraPosition oldPos = map.getCameraPosition();
    CameraPosition pos = new CameraPosition.Builder(oldPos).target(POS).bearing(bearing).zoom(20).build();
    //CameraPosition cameraPosition = new CameraPosition.Builder(oldPos)
            //.target(POS).bearing(bearing).zoom(20).build();
    GoogleMap.CancelableCallback callback = new GoogleMap.CancelableCallback() {
        @Override
        public void onFinish() {
            map.clear();
            marker.position(POS);
            map.addMarker(marker);
        }

        @Override
        public void onCancel() {

        }
    };

    //map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();




    map.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 24, callback);

*/
    }

    private class DrawGeoJSON extends AsyncTask<Void, Void, List<LatLng>> {
        @Override
        protected List<LatLng> doInBackground(Void... voids) {

            ArrayList<LatLng> points = new ArrayList<LatLng>();

            try {
                // Load GeoJSON file
                InputStream inputStream = getAssets().open("IIS_indoorMap.geojson");
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
                StringBuilder sb = new StringBuilder();
                int cp;
                while ((cp = rd.read()) != -1) {
                    sb.append((char) cp);
                }

                Log.i("DrawGeoJSON","1");
                inputStream.close();

                // Parse JSON
                JSONObject json = new JSONObject(sb.toString());
                JSONArray features = json.getJSONArray("features");
                JSONObject feature = features.getJSONObject(0);
                JSONObject geometry = feature.getJSONObject("geometry");
                if (geometry != null) {
                    String type = geometry.getString("type");

                    // Our GeoJSON only has one feature: a line string
                    if (!TextUtils.isEmpty(type) && type.equalsIgnoreCase("LineString")) {

                        // Get the Coordinates
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        for (int lc = 0; lc < coords.length(); lc++) {
                            JSONArray coord = coords.getJSONArray(lc);
                            LatLng latLng = new LatLng(coord.getDouble(1), coord.getDouble(0));
                            points.add(latLng);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception Loading GeoJSON: " + e.toString());
            }

            return points;
        }

        @Override
        protected void onPostExecute(List<LatLng> points) {
            super.onPostExecute(points);

            if (points.size() > 0) {
                LatLng[] pointsArray = points.toArray(new LatLng[points.size()]);

                Log.i("DrawGeoJSON","2");
                Log.i("DrawGeoJSON", points.toString());
                // Draw Points on MapView
                mMapboxView.addPolyline(new PolylineOptions()
                        .add(pointsArray)
                        .color(Color.parseColor("#3bb2d0"))
                        .width(2));
            }
        }
    }
}



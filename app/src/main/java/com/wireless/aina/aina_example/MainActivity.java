/*
    ©2017 Aina Wireless Inc., All rights reserved.
    ----------------------------------------------

    This file is part of Aina Wireless Inc's Pairing example.

    Pairing example is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Pairing example is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wireless.aina.aina_example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Typeface;
import android.Manifest;
import android.util.SparseArray;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;



public class MainActivity extends AppCompatActivity
{
    private static final int PHOTO_REQUEST = 10;
    private TextView         scanResults;
    private TextView         scanHeader;
    private BarcodeDetector  detector;
    private Uri              imageUri;

    private static final int    REQUEST_WRITE_PERMISSION = 20;

    /* BLE GATT Messages*/
    private final static String ACTION_GATT_CONNECTED            = "com.wireless.aina.ACTION_GATT_CONNECTED";
    private final static String ACTION_GATT_DISCONNECTED         = "com.wireless.aina.ACTION_GATT_DISCONNECTED";
    private final static String ACTION_GATT_SERVICES_DISCOVERED  = "com.wireless.aina.ACTION_GATT_SERVICES_DISCOVERED";
    private final static String ACTION_DATA_AVAILABLE            = "com.wireless.aina.ACTION_DATA_AVAILABLE";
    private final static String EXTRA_DATA                       = "com.wireless.aina.EXTRA_DATA";

    /* Classic BT UUID */
    private static final UUID     MY_UUID    = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /* BLE Service UUID */
    private static final UUID     AINA_SERV  = UUID.fromString("127FACE1-CB21-11E5-93D0-0002A5D5C51B");

    /* BLE Characteristics UUIDs */
    private static final UUID     KEYS       = UUID.fromString("127FBEEF-CB21-11E5-93D0-0002A5D5C51B"); /* Key press' */
    private static final UUID     SW_VERS    = UUID.fromString("127FC0FF-CB21-11E5-93D0-0002A5D5C51B"); /* SW Versions */


    private static final String  TAG = "Aina_Pairing";
    private NfcAdapter          mNfcAdapter;
    private Activity            mNfcActivity;
    private TextView            mText_classicMac;
    private TextView            mText_bleMac;
    private TextView            mText_charVal;
    private TextView            mText_keysString;
    private TextView            mText_sw_versions;
    private Button              mButton_scan;
    private BluetoothAdapter    mAdapter;
    private int                 ble_char;
    private int                 mac_cnt = 0;
    private String              ble_sw_char;
    private BluetoothGatt       mBluetoothGatt;
    private BluetoothDevice     mmDevice_classic;
    private BluetoothSocket     mmSocket;
    private BluetoothA2dp       mBluetoothA2DP;
    private BluetoothHeadset    mBluetoothHeadset;
    private boolean             classic_paired = false;
    private boolean             ble_connected = false;
    private boolean             update_ble = false;
    private boolean             tryBTC = false;
    private boolean             tryBLE= false;
    private boolean             failedConnect= false;
    private boolean             clearAll = false;
    private boolean             ReadVersions = true;
    private boolean             UpdateVersions = false;
    private final String[]      MACs = new String[2]; // 0 = BLE MAC,  1 = Classic MAC
    private final Handler       read_ble_handler = new Handler();
    private final Handler       TextUpdateHandler = new Handler();
    private final BluetoothAdapter  mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    /* Read BLE characteristics */
    private final Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {

            if(!ble_connected)
            {
                ble_connected = true;

                /* Get BLE device */
                BluetoothDevice mmDevice_ble = mAdapter.getRemoteDevice(MACs[0]);

                //final Set<BluetoothDevice> bonded = mAdapter.getBondedDevices();

                /* Connect to BLEs GATT */
                mBluetoothGatt = mmDevice_ble.connectGatt(getApplicationContext(), false, mGattCallback);
            }
            else
            {

                BluetoothGattService mService = mBluetoothGatt.getService(AINA_SERV);

                if (!mButton_scan.isEnabled()) mButton_scan.setEnabled(true);

                if (mService != null) {
                    if (!ReadVersions) {
                        BluetoothGattCharacteristic mChar = mService.getCharacteristic(KEYS);

                        if (mChar != null) {
                            mBluetoothGatt.readCharacteristic(mChar);
                        }
                    } else {
                        BluetoothGattCharacteristic mChar = mService.getCharacteristic(SW_VERS);

                        if (mChar != null) {
                            mBluetoothGatt.readCharacteristic(mChar);
                        }
                    }
                }
            }
        }
    };



    /* Update TextView elements of the UI */
    private final Runnable updateRunnable = new Runnable()
    {
        public void run()
        {
            if(classic_paired)
            {
                if(!mText_classicMac.getText().toString().contains("Paired"))
                {
                    mText_classicMac.append(" (Paired)");
                }
            }

            if(update_ble)
            {
                if((ble_char & 0xff) < 16)
                    mText_charVal.setText("GATT Characteristic value: 0x0" + Integer.toHexString(ble_char & 0xff));
                else
                    mText_charVal.setText("GATT Characteristic value: 0x" + Integer.toHexString(ble_char & 0xff));

                mText_keysString.setText("");

                if ((ble_char & 1) == 1) mText_keysString.append("APPT1 ");
                if ((ble_char & 2) == 2) mText_keysString.append("EMERG ");
                if ((ble_char & 4) == 4) mText_keysString.append("APPT2 ");
                if ((ble_char & 8) == 8) mText_keysString.append("SOFT1 ");
                if ((ble_char & 16) == 16) mText_keysString.append("SOFT2 ");
                if ((ble_char & 32) == 32) mText_keysString.append("CALL ");

                update_ble = false;
            }

            if(tryBTC)
            {
                mText_charVal.setText("Trying to connect with Classic BT...");
                mText_keysString.setText("");
                tryBTC = false;
            }

            if(tryBLE)
            {
                mText_charVal.setText("Trying to connect with BLE...");
                mText_keysString.setText("");
                tryBLE = false;
            }

            if(failedConnect)
            {
                mText_charVal.setText("Failed to connect!");

                mButton_scan.setEnabled(true);

                mText_keysString.setText("Read new NFC-Tag or QR-code and try again...");
                failedConnect = false;
            }

            if(UpdateVersions)
            {
                mText_sw_versions.setText(ble_sw_char);
                UpdateVersions = false;
            }

            if(clearAll)
            {
                ReadVersions = true;
                classic_paired = false;
                ble_connected = false;

                mText_charVal.setText("");
                mText_keysString.setText("");
                mText_bleMac.setText("");
                mText_classicMac.setText("");
                mText_sw_versions.setText("");

                scanResults.setText("");

                clearAll = false;
            }

        }
    };



    /* Setup example on application creation */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mText_classicMac  = (TextView) findViewById(R.id.textView_classicMac);
        mText_bleMac      = (TextView) findViewById(R.id.textView_bleMac);
        mText_charVal     = (TextView) findViewById(R.id.textView_charVal);
        mText_keysString  = (TextView) findViewById(R.id.textView_keysString);
        mText_sw_versions = (TextView) findViewById(R.id.sw_versions);
        mButton_scan      = (Button)   findViewById(R.id.ScanButton);

        /* Get bluetooth adapter */
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mAdapter == null)
        {
            Toast.makeText(this, "Bluetooth not supported.", Toast.LENGTH_LONG).show();
            finish();

            return;
        }

        /* Check if bluetooth is enabled */
        if (!mAdapter.isEnabled())
        {
            /* Request to start BT */
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableBtIntent, 1);
        }

        /* Needed by some Android versions */
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);

        scanResults   = (TextView) findViewById(R.id.scan_results);
        scanHeader    = (TextView) findViewById(R.id.scan_header);

        mButton_scan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });

        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                .build();

        if (!detector.isOperational())
        {
            Toast.makeText(this, "Could not set up the QR-scanner!", Toast.LENGTH_LONG).show();

            mButton_scan.setEnabled(false);

            return;
        }

        /* Get NFC adapter */
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null)
        {
            Toast.makeText(this, "NFC not supported.", Toast.LENGTH_LONG).show();
            finish();

            return;
        }

        /* Check if NFC is enabled */
        if (!mNfcAdapter.isEnabled())
        {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
        }
        else
        {
            handleIntent(getIntent());

            mNfcActivity = this;
        }

        scanHeader.setText("QR-Code or NFC data not yet read...");


    }



    @Override
    protected void onResume()
    {
        super.onResume();

        if (mNfcAdapter != null) setupForegroundDispatch(mNfcActivity, mNfcAdapter);
    }



    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        stopForegroundDispatch(mNfcActivity, mNfcAdapter);

        if(mBluetoothGatt != null) mBluetoothGatt.close();

        try
        {
            if(mmSocket != null)
            {
                if (mmSocket.isConnected()) mmSocket.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        mAdapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2DP);
        mAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);

        mAdapter = null;

        finish();
    }



    @Override
    protected void onPause()
    {
        super.onPause();

        if (mNfcAdapter != null) mNfcAdapter.disableForegroundDispatch(mNfcActivity);

    }



    @Override
    public void onStart()

    {
        super.onStart();
    }



    @Override
    public void onStop()
    {
        super.onStop();
    }



    @Override
    protected void onNewIntent(Intent intent)

    {
        handleIntent(intent);
    }



    /* Setup NFC foreground dispatcher */
    private static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter)
    {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        try
        {
            /* Mime type for BT pairing */
            filters[0].addDataType("application/vnd.bluetooth.ep.oob");
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            throw new RuntimeException("Unsupported mime type!");
        }

        /* Start Foreground dispatcher */
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }



    /* Stop Foreground dispatcher */
    private static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter)
    {
        adapter.disableForegroundDispatch(activity);
    }



    /* Check NFC Tags type */
    private void handleIntent(Intent intent)
    {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action))
        {

            String type = intent.getType();

            if (type.equals("application/vnd.bluetooth.ep.oob"))
            {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            }
            else
            {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        }
    }



    /* Read NFC Tags messages */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String>
    {
        @Override
        protected String doInBackground(Tag... params)
        {
            Tag tag = params[0];
            String result;

            mac_cnt = 0;

            Ndef ndef = Ndef.get(tag);

            if (ndef == null)
            {
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();

            for (NdefRecord record : records) {
                if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA) {
                    /* Read the NFC Tags payload */
                    readText(record);
                }
            }
            result = "OK";

            return result;
        }



        /* Read NFC Tags payload and parse MAC from the payload */
        private void readText(NdefRecord record) {
            int start;
            String payload = record.toString();
            String MAC;

            start = payload.indexOf("payload=");
            MAC = payload.substring(start + 12);

            MACs[mac_cnt] = MAC.substring(10, 12) + ":";
            MACs[mac_cnt] += MAC.substring(8, 10) + ":";
            MACs[mac_cnt] += MAC.substring(6, 8) + ":";
            MACs[mac_cnt] += MAC.substring(4, 6) + ":";
            MACs[mac_cnt] += MAC.substring(2, 4) + ":";
            MACs[mac_cnt] += MAC.substring(0, 2);

            mac_cnt++;

        }



        @Override
        protected void onPostExecute(String result)
        {
            if (result != null)
            {
                if(!MACs[1].equals("")) mText_classicMac.setText("BTC MAC address = " + MACs[1]);
                if(!MACs[0].equals("")) mText_bleMac.setText("BLE MAC address = " + MACs[0]);

                scanHeader.setText("NFC Tag read successfully...");
            }

            tryBTC = true;
            TextUpdateHandler.post(updateRunnable);

            mButton_scan.setEnabled(false);

            Runnable r = new ConnectThread(MACs[1]);
            new Thread(r).start();
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    clearAll = true;
                    TextUpdateHandler.post(updateRunnable);

                    takePicture();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Permission Denied to take pictures!", Toast.LENGTH_SHORT).show();
                }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK)
        {
            mButton_scan.setEnabled(false);
            launchMediaScanIntent();

            try
            {
                Bitmap bitmap = decodeBitmapUri(this, imageUri);

                if (detector.isOperational() && bitmap != null)
                {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<Barcode> QRcodes = detector.detect(frame);

                    for (int index = 0; index < QRcodes.size(); index++)
                    {
                        int tmp;
                        mac_cnt = 0;

                        scanHeader.setTypeface(null, Typeface.BOLD);
                        scanHeader.setText("QR-Code content:");

                        Barcode code = QRcodes.valueAt(index);
                        scanResults.setText(code.displayValue);

                        tmp = code.displayValue.indexOf("/");

                        String MAC = code.displayValue.substring(0, tmp);

                        MACs[mac_cnt]  = MAC.substring(0, 2) + ":";
                        MACs[mac_cnt] += MAC.substring(2, 4) + ":";
                        MACs[mac_cnt] += MAC.substring(4, 6) + ":";
                        MACs[mac_cnt] += MAC.substring(6, 8) + ":";
                        MACs[mac_cnt] += MAC.substring(8, 10) + ":";
                        MACs[mac_cnt] += MAC.substring(10, 12);

                        mac_cnt++;

                        tmp = code.displayValue.indexOf("/") + 1;

                        MAC = code.displayValue.substring(tmp, code.displayValue.length());

                        MACs[mac_cnt]  = MAC.substring(0, 2) + ":";
                        MACs[mac_cnt] += MAC.substring(2, 4) + ":";
                        MACs[mac_cnt] += MAC.substring(4, 6) + ":";
                        MACs[mac_cnt] += MAC.substring(6, 8) + ":";
                        MACs[mac_cnt] += MAC.substring(8, 10) + ":";
                        MACs[mac_cnt] += MAC.substring(10, 12);

                        if(!MACs[1].equals("")) mText_classicMac.setText("BTC MAC address = " + MACs[1]);
                        if(!MACs[0].equals("")) mText_bleMac.setText("BLE MAC address = " + MACs[0]);

                        tryBTC = true;
                        TextUpdateHandler.post(updateRunnable);

                        Runnable r = new ConnectThread(MACs[1]);
                        new Thread(r).start();

                    }

                    if (QRcodes.size() == 0)
                    {
                        mButton_scan.setEnabled(true);

                        scanHeader.setText("Scan Failed: Did not found valid QR-Code");
                    }

                }
                else
                {
                    mButton_scan.setEnabled(true);

                    scanHeader.setText("Could not set up the QR-scanner!");
                }

            }
            catch (Exception e)
            {
                Toast.makeText(this, "Failed to load QR-code file", Toast.LENGTH_SHORT).show();
            }

            File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");

            if(!photo.delete())
            {
                Toast.makeText(this, "Failed to delete qr-code picture file", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void takePicture()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            imageUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", photo);
        else
            imageUri = Uri.fromFile(photo);

        clearAll = true;
        TextUpdateHandler.post(updateRunnable);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQUEST);
    }



    private void launchMediaScanIntent()
    {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }



    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException
    {
        int targetW = 600;
        int targetH = 600;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
    }



    /* Connect class */
    public class ConnectThread extends Thread
    {
        /* Connect to bluetooth classic */
        public ConnectThread(String MAC)
        {
            BluetoothDevice device;
            BluetoothSocket tmp = null;

            device = mAdapter.getRemoteDevice(MAC);
            mmDevice_classic = device;

            try
            {
                /* Try to connect to BT classic according to our UUID */
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            mmSocket = tmp;
        }



        /* Finish classic BT connection and start BLE connection process */
        public void run()
        {
            /* In example we don't start discovery, but any case if it is discovering, stop it */
            mAdapter.cancelDiscovery();

            /* Create profile listeners for A2DP and Headset profiles */
            mBluetoothAdapter.getProfileProxy(getApplicationContext(), mProfileListener, BluetoothProfile.A2DP);
            mBluetoothAdapter.getProfileProxy(getApplicationContext(), mProfileListener, BluetoothProfile.HEADSET);

            try
            {
                if(!mmSocket.isConnected())
                {
                    mmDevice_classic.getName();

                    /* Connect to Classic BTs socket */
                    mmSocket.connect();
                }
            }
            catch (IOException connectException)
            {
                try
                {
                    mmSocket.close();
                }
                catch (IOException closeException)
                {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }

                failedConnect = true;
                TextUpdateHandler.post(updateRunnable);

                return;
            }

            /* Paired with Classic BT, update UI */
            classic_paired = true;
            tryBLE = true;
            TextUpdateHandler.post(updateRunnable);

            read_ble_handler.postDelayed(runnable, 1000);

        }


    }


    /* Profile listeners for A2DP and Headset profiles */
    private final BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener()
    {
        public void onServiceConnected(int profile, BluetoothProfile proxy)
        {
            if (profile == BluetoothProfile.A2DP)
            {
                mBluetoothA2DP = (BluetoothA2dp) proxy;

                Method connect = null;
                try
                {
                    connect = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
                }
                catch (NoSuchMethodException e)
                {
                    e.printStackTrace();
                }

                assert connect != null;
                connect.setAccessible(true);
                try
                {
                    connect.invoke(mBluetoothA2DP, mmDevice_classic);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    e.printStackTrace();
                }
            }

            if (profile == BluetoothProfile.HEADSET)
            {
                mBluetoothHeadset = (BluetoothHeadset) proxy;

                Method connect = null;
                try
                {
                    connect = BluetoothHeadset.class.getDeclaredMethod("connect", BluetoothDevice.class);
                }
                catch (NoSuchMethodException e)
                {
                    e.printStackTrace();
                }

                assert connect != null;
                connect.setAccessible(true);
                try
                {
                    connect.invoke(mBluetoothHeadset, mmDevice_classic);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    e.printStackTrace();
                }
            }
        }



        /* Services were disconnected... */
        public void onServiceDisconnected(int profile)
        {
            if (profile == BluetoothProfile.A2DP)
            {
                mBluetoothA2DP = null;
            }

            if (profile == BluetoothProfile.HEADSET)
            {
                mBluetoothHeadset = null;
            }
        }
    };



    /* Handle BLE's messages */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            String intentAction;

            /* Connected to BLE */
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (newState == BluetoothProfile.STATE_CONNECTED)
                {
                    intentAction = ACTION_GATT_CONNECTED;

                    broadcastUpdate(intentAction);

                    /* Start BLE's Service discovery process */
                    gatt.discoverServices();

                }
                /* BLE was disconnected... */
                else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    intentAction = ACTION_GATT_DISCONNECTED;

                    broadcastUpdate(intentAction);

                    failedConnect = true;
                    TextUpdateHandler.post(updateRunnable);
                }

            }
            else
            {
                intentAction = ACTION_GATT_CONNECTED;

                broadcastUpdate(intentAction);

                /* Start BLE's Service discovery process */
                gatt.discoverServices();

            }
        }



        /* BLE's Services found */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }

            //   - After bonding is supported by AINA PTT Voice responder, use following API call to create bonding with BLE
            //mmDevice_ble.createBond();

            /* Start BLE's characteristics reading thread  */
            read_ble_handler.postDelayed(runnable, 200);
        }



        /* Read BLE's characteristics */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                broadcastUpdate(characteristic);

                if(ReadVersions)
                {
                    /* Build Classic BT firmware string*/
                    ble_sw_char = "BTC Ver: ";
                    ble_sw_char += Integer.toHexString((characteristic.getValue()[0] & 0xff));
                    ble_sw_char += Integer.toHexString((characteristic.getValue()[1] & 0xff));
                    ble_sw_char += Integer.toHexString((characteristic.getValue()[2] & 0xff)).toUpperCase();

                    if(ble_sw_char.substring(ble_sw_char.length() - 1, ble_sw_char.length()).equals("0"))
                    {
                        ble_sw_char = ble_sw_char.substring(0, ble_sw_char.length()-1);
                    }

                    /* Build BLE firmware string*/
                    ble_sw_char += "   -   BLE Ver: ";
                    ble_sw_char += Integer.toHexString((characteristic.getValue()[3] & 0xff));
                    ble_sw_char += Integer.toHexString((characteristic.getValue()[4] & 0xff));
                    ble_sw_char += Integer.toHexString((characteristic.getValue()[5] & 0xff)).toUpperCase();

                    if(ble_sw_char.substring(ble_sw_char.length() - 1, ble_sw_char.length()).equals("0"))
                    {
                        ble_sw_char = ble_sw_char.substring(0, ble_sw_char.length() - 1);
                    }

                    ReadVersions = false;
                    UpdateVersions = true;
                }
                else
                {
                    /* Get first value of characteristic (keycode)*/
                    ble_char = ((int) characteristic.getValue()[0]);

                    update_ble = true;
                }

                /* Update UI */
                TextUpdateHandler.post(updateRunnable);

                /* Start characteristic read again... */
                read_ble_handler.postDelayed(runnable, 100);

            }
        }



        private void broadcastUpdate(final String action)
        {
            final Intent intent = new Intent(action);
            sendBroadcast(intent);
        }



        private void broadcastUpdate(final BluetoothGattCharacteristic characteristic)
        {
            final Intent intent = new Intent(MainActivity.ACTION_DATA_AVAILABLE);

            final byte[] data = characteristic.getValue();

            if (data != null && data.length > 0)
            {
                final StringBuilder stringBuilder = new StringBuilder(data.length);

                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }

            sendBroadcast(intent);

        }
    };

    public void openBrowser(View view)
    {
        String url = view.getTag().toString();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        intent.setData(Uri.parse(url));

        startActivity(intent);
    }
}


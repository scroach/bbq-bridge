package patamon.at.bbqbridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import patamon.at.bbqbridge.bluetooth.GattClient;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.log);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(GattClient.BROADCAST_TEMP_RECEIVED);

        // Instantiates a new DownloadStateReceiver
        MyBroadcastReceiver downloadStateReceiver = new MyBroadcastReceiver();
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadStateReceiver, statusIntentFilter);


    }

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner scanner;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private Context context;
    private GattClient gattClient = new GattClient();

    private ScanCallback myScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() != null && result.getDevice().getName().startsWith("iBBQ")) {
                Toast.makeText(context, "found device:" + result.getDevice().getName(), Toast.LENGTH_SHORT).show();
                log("found device:" + result.getDevice().getName());
                gattClient.onCreate(context, result.getDevice().getAddress());
                stopScan();
            } else {
                log("IGNORING DEVICE: " + result.getDevice().getName());
            }

            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            log("onBatchScanResults");
            Toast.makeText(context, "found deviceS:", Toast.LENGTH_SHORT).show();
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            log("onScanFailed: " + errorCode);
            super.onScanFailed(errorCode);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        scanLeDevice();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    private void scanLeDevice() {
        // Stops scanning after a pre-defined scan period.
        context = this;

        Toast.makeText(this, "starting BLE scan...", Toast.LENGTH_SHORT).show();
        log("STAAART!");

        if (scanner == null) {
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        scanner.startScan(myScanCallback);
    }

    private void stopScan() {
        scanner.stopScan(myScanCallback);
        Toast.makeText(this, "stopping BLE scan...", Toast.LENGTH_SHORT).show();
    }

    private void log(String message) {
        Log.w(TAG, message);
        textView.append(message + "\r\n");

        textView.setMovementMethod(new ScrollingMovementMethod());
        while (textView.canScrollVertically(1)) {
            textView.scrollBy(0, 10);
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int probe = intent.getIntExtra(GattClient.INFO_PROBE, -10);
            int temp = intent.getIntExtra(GattClient.INFO_TEMP, -10);
            boolean isConnected = intent.getBooleanExtra(GattClient.INFO_CONNECTED, false);

            TextView labelView = null;
            TextView tempView = null;

            switch (probe) {
                case 1:
                    labelView = ((TextView) findViewById(R.id.probe_label_1));
                    tempView = ((TextView) findViewById(R.id.probe_temp_1));
                    break;
                case 2:
                    labelView = ((TextView) findViewById(R.id.probe_label_2));
                    tempView = ((TextView) findViewById(R.id.probe_temp_2));
                    break;
                case 3:
                    labelView = ((TextView) findViewById(R.id.probe_label_3));
                    tempView = ((TextView) findViewById(R.id.probe_temp_3));
                    break;
                case 4:
                    labelView = ((TextView) findViewById(R.id.probe_label_4));
                    tempView = ((TextView) findViewById(R.id.probe_temp_4));
                    break;
                default:
                    return;
            }

            if (isConnected) {
                labelView.setTextColor(Color.BLACK);
                labelView.setPaintFlags(0);
                tempView.setText(temp + "°C");
            } else {
                labelView.setTextColor(Color.RED);
                labelView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tempView.setText("N/A");
            }

        }
    }
}


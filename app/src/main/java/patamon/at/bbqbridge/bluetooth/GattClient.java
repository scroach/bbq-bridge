package patamon.at.bbqbridge.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;

public class GattClient extends JobIntentService {

    private static final String TAG = GattClient.class.getSimpleName();

    public static final String BROADCAST_TEMP_RECEIVED = GattClient.class.getPackage() + "_TEMP";
    public static final String INFO_PROBE = "PROBE";
    public static final String INFO_TEMP = "TEMP";
    public static final String INFO_CONNECTED = "CONNECTED";

    private final byte[] unitCelsius = new byte[]{(byte) 2, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] unitFahrenheit = new byte[]{(byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] autoPairData = new byte[]{(byte) 33, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1, (byte) -72, (byte) 34, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] handPairData = new byte[]{(byte) 32, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] loginIbbq = new byte[]{(byte) 33, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1, (byte) 182, (byte) 34, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};

    private final byte[] readVersionsValue = new byte[]{(byte) 8, (byte) 35, (byte) 0, (byte) 0, (byte) 0, (byte) 0};

    private final byte[] f1323a = new byte[]{(byte) 32, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] f1323e = new byte[]{(byte) 32, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] f1324c = new byte[]{(byte) 33, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1, (byte) -72, (byte) 34, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private final byte[] f1324f = new byte[]{(byte) 33, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1, (byte) -72, (byte) 34, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};

    private Context mContext;
    private String mDeviceAddress;

    private Queue<QueueItem> queue = new ArrayDeque<>();

    private class QueueItem {

        BluetoothGattCharacteristic characteristic;
        BluetoothGattDescriptor descriptor;

        byte[] value;

        public QueueItem(BluetoothGattCharacteristic characteristic, byte[] value) {
            this.characteristic = characteristic;
            this.value = value;
        }

        public QueueItem(BluetoothGattDescriptor descriptor, byte[] value) {
            this.descriptor = descriptor;
            this.value = value;
        }
    }

    private BluetoothGattCharacteristic versionInfoCharacteristic;
    private BluetoothGattCharacteristic pairCharacteristic;
    private BluetoothGattCharacteristic historyDataCharacteristic;
    private BluetoothGattCharacteristic realtimeCharacteristic;
    private BluetoothGattCharacteristic settingsCharacteristic;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.w(TAG, "CONNECTED!!!!!");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT client. Attempting to start service discovery");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT client");
                startClient();
            }
        }

        private void handPair() {
            pairCharacteristic.setValue(handPairData);
            mBluetoothGatt.writeCharacteristic(pairCharacteristic);
        }

        private void autoPair() {
            queue.add(new QueueItem(pairCharacteristic, f1324c));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.w(TAG, "onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(BluetoothGattCharasteristics.GENERAL_SERVICE_UUID);
                if (service != null) {

                    versionInfoCharacteristic = service.getCharacteristic(BluetoothGattCharasteristics.VERSION_INFO_UUID);
                    pairCharacteristic = service.getCharacteristic(BluetoothGattCharasteristics.PAIRING_UUID);
                    historyDataCharacteristic = service.getCharacteristic(BluetoothGattCharasteristics.HISTORY_DATA_UUID);
                    realtimeCharacteristic = service.getCharacteristic(BluetoothGattCharasteristics.REALTIME_NOTIFY_UUID);
                    settingsCharacteristic = service.getCharacteristic(BluetoothGattCharasteristics.SETTINGS_UUID);

                    autoPair();
                    setTargetTemp((byte) 0, (short) 300, (short) 330);
//                    setTargetTemp((byte) 2, (short) 10, (short) 200);
                    setUnitCelsius();
                    subscribeRealtimeData();
                    enableRealtimeTemps();

                    processQueue();
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        private void enableDisableNotification(BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z) {
            mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, z);
            BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString(BluetoothGattCharasteristics.CLIENT_CHARACTERISTIC_CONFIG));
            if (z) {
                queue.add(new QueueItem(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
            } else {
                queue.add(new QueueItem(descriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE));
            }
        }

        private void subscribeRealtimeData() {
            enableDisableNotification(realtimeCharacteristic, true);
        }

        private void subscribeHistoryData() {
            enableDisableNotification(historyDataCharacteristic, true);
        }

        private void enableRealtimeTemps() {
            byte[] enableRealtimeTemps = new byte[]{(byte) 11, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
            queue.add(new QueueItem(settingsCharacteristic, enableRealtimeTemps));
        }

        private void setUnitCelsius() {
            byte[] celsius = new byte[]{(byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
            queue.add(new QueueItem(settingsCharacteristic, celsius));
        }

        private void setUnitFahrenheit() {
            byte[] fahrenheit = new byte[]{(byte) 2, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
            queue.add(new QueueItem(settingsCharacteristic, fahrenheit));
        }

        private void setTargetTemp(byte mightBeProbeIndicator, short lowerTemp, short targetTemp) {
            byte[] bArr = new byte[]{(byte) 1, mightBeProbeIndicator, (byte) (lowerTemp & 255), (byte) (lowerTemp >> 8), (byte) (targetTemp & 255), (byte) (targetTemp >> 8)};
            queue.add(new QueueItem(settingsCharacteristic, bArr));
        }

        private void writeDianya(BluetoothGatt gatt) {
            BluetoothGattService service = gatt.getService(BluetoothGattCharasteristics.GENERAL_SERVICE_UUID);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(BluetoothGattCharasteristics.SETTINGS_UUID);

            byte[] readDianyaValue = new byte[]{(byte) 8, (byte) 36, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
            characteristic.setValue(readDianyaValue);
            gatt.writeCharacteristic(characteristic);
        }

        private void readVersions() {
            BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(BluetoothGattCharasteristics.GENERAL_SERVICE_UUID)
                    .getCharacteristic(BluetoothGattCharasteristics.SETTINGS_UUID);

            byte[] readVersionsValue = new byte[]{(byte) 8, (byte) 35, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
            characteristic.setValue(readVersionsValue);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readCounterCharacteristic(characteristic);
            Log.v("", "");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            processQueue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            readCounterCharacteristic(characteristic);
            Log.v("", "");

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            processQueue();
            Log.v("", "");
        }

        private void readCounterCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (BluetoothGattCharasteristics.REALTIME_NOTIFY_UUID.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                Log.i("GAATTT", Arrays.toString(data));
                int value = fromByteArray(data);

                broadcastTemp(1, (short) ((int) Math.round(((double) readTempFromByteArray(data, 0)) / 10.0d)), data[1] == 1);
                broadcastTemp(2, (short) ((int) Math.round(((double) readTempFromByteArray(data, 2)) / 10.0d)), data[3] == 1);
                broadcastTemp(3, (short) ((int) Math.round(((double) readTempFromByteArray(data, 4)) / 10.0d)), data[5] == 1);
                broadcastTemp(4, (short) ((int) Math.round(((double) readTempFromByteArray(data, 6)) / 10.0d)), data[7] == 1);
                broadcastTemp(5, (short) ((int) Math.round(((double) readTempFromByteArray(data, 8)) / 10.0d)), data[9] == 1);
                broadcastTemp(6, (short) ((int) Math.round(((double) readTempFromByteArray(data, 10)) / 10.0d)), data[11] == 1);
            }
        }

        public short readTempFromByteArray(byte[] bArr, int i) {
            return bArr.length <= i ? (short) 0 : (short) ((bArr[i + 1] << 8) | (bArr[i + 0] & 255));
        }

    };

    private void broadcastTemp(int probe, int temp, boolean isConnected) {
        Intent localIntent = new Intent(BROADCAST_TEMP_RECEIVED)
                .putExtra(INFO_PROBE, probe)
                .putExtra(INFO_TEMP, temp)
                .putExtra(INFO_CONNECTED, isConnected);

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void processQueue() {
        if (queue.peek() != null) {
            QueueItem item = queue.remove();

            if (item.characteristic != null) {
                item.characteristic.setValue(item.value);
                boolean result = mBluetoothGatt.writeCharacteristic(item.characteristic);
                Log.i(TAG, "written characteristic" + item.characteristic.getUuid() + "; result: " + result);
            } else {
                item.descriptor.setValue(item.value);
                boolean result = mBluetoothGatt.writeDescriptor(item.descriptor);
                Log.i(TAG, "written descriptor" + item.descriptor.getUuid() + "; result: " + result);
            }

        }
    }


    public static int fromByteArray(byte[] bytes) {
        return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    private static int fromBytes(byte b1, byte b2, byte b3, byte b4) {
        return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startClient();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopClient();
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
    };

    public void onCreate(Context context, String deviceAddress) throws RuntimeException {
        mContext = context;
        mDeviceAddress = deviceAddress;

        mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            throw new RuntimeException("GATT client requires Bluetooth support");
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothReceiver, filter);
        if (!mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is currently disabled... enabling");
            mBluetoothAdapter.enable();
        } else {
            Log.i(TAG, "Bluetooth enabled... starting client");
            startClient();
        }
    }

    public void onDestroy() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopClient();
        }

        mContext.unregisterReceiver(mBluetoothReceiver);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

    }

    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    private void startClient() {
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback);

        if (mBluetoothGatt == null) {
            Log.w(TAG, "Unable to create GATT client");
        }
    }

    private void stopClient() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = null;
        }
    }
}
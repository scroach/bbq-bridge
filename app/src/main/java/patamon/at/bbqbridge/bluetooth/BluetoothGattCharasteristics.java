package patamon.at.bbqbridge.bluetooth;

import java.util.UUID;

class BluetoothGattCharasteristics {

    static final UUID GENERAL_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");

    static final UUID VERSION_INFO_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    static final UUID PAIRING_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    static final UUID HISTORY_DATA_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    static final UUID REALTIME_NOTIFY_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    static final UUID SETTINGS_UUID = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb");

    static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
}

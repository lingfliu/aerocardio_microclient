package com.uteamtec.bletool;

import android.content.Context;
import android.util.Log;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattCharacter;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by liulingfeng on 2017/7/8.
 */

public class BleComm {
    public static final int STATE_OFF = 0;
    public static final int STATE_DISCONNECTED = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private static BleComm comm;

    static int cnt = 0;
    AtomicBoolean isReceiving = new AtomicBoolean(false);

    /*
     * initialize BleComm by customized uuids
     */
    //initialize only once
    @Deprecated
    public static void init(Context ctx, String serviceUuid, String txUuid, String rxUuid){
        if (comm == null){
            synchronized (BleComm.class){
                if (comm == null){
                    comm = new BleComm(ctx, serviceUuid, txUuid, rxUuid);
                }
            }
        }
    }

    /*
     * initialize BleComm by application default uuids
     */
    //initialize only once
    public static void init(Context ctx){
        if (comm == null){
            synchronized (BleComm.class){
                if (comm == null){
                    comm = new BleComm(ctx, BleConstants.SERVICE_UUID, BleConstants.TX_UUID, BleConstants.RX_UUID);
                }
            }
        }
    }

//    @Deprecated
//    public void setUUID(String service, String tx, String rx){
//        serviceUuid = UUID.fromString(service);
//        txUuid = UUID.fromString(tx);
//        rxUuid = UUID.fromString(rx);
//    }

    public static BleComm getInstance(){
        return comm;
    }

    public int getState() {
        return state.get();
    }

    private AtomicInteger state;
    private BluetoothClient client;
    private String bleMac;
    private boolean isNotifyReceive;
    private UUID serviceUuid;
    private UUID txUuid;
    private UUID rxUuid;

    private BleStateListener stateListener;
    public void setStateListener(BleStateListener stateListener) {
        this.stateListener = stateListener;
    }
    private BleDataListener dataListener;
    public void setDataListener(BleDataListener dataListener){
        this.dataListener = dataListener;
    }
    private BleScanListener scanListener;
    public void setScanListener(BleScanListener scanListener){
        this.scanListener = scanListener;
    }

    private final BluetoothStateListener bluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            if (openOrClosed){
                state.set(STATE_DISCONNECTED);
                client.unregisterBluetoothStateListener(bluetoothStateListener);
            }
            else {
                state.set(STATE_OFF);
            }

            notifyStateChanged();
        }
    };

    private final BleConnectStatusListener bleConnectStatusListener = new BleConnectStatusListener() {
            @Override
            public void onConnectStatusChanged(String mac, int status) {
                if(mac.equals(bleMac)){
                    if (status == Constants.STATUS_CONNECTED){
                        state.set(STATE_CONNECTED);
                    }
                    else if (status == Constants.STATUS_DISCONNECTED){
                        Log.i("A", "blecomm: on disconnected detected");
                        state.set(STATE_DISCONNECTED);
                    }
                }

                notifyStateChanged();
            }
    };

    private BleComm(Context context, String serviceUuid, String txUuid, String rxUuid){
        this.client = new BluetoothClient(context);
        this.serviceUuid = UUID.fromString(serviceUuid);
        this.txUuid = UUID.fromString(txUuid);
        this.rxUuid = UUID.fromString(rxUuid);
        this.state = new AtomicInteger(STATE_OFF);

        client.registerBluetoothStateListener(bluetoothStateListener);

        client.registerConnectStatusListener(bleMac, bleConnectStatusListener);

        if (client.isBluetoothOpened()) {
            this.state.set(STATE_DISCONNECTED);
        }
        else {
            this.state.set(STATE_OFF);
            this.bleOn();
        }


    }

    public boolean isConfiged() {
        return client != null && serviceUuid != null && txUuid != null && rxUuid != null;
    }

    public boolean bleOn(){
        return client.openBluetooth();
    }

    public boolean bleOff(){
        return client.closeBluetooth();
    }

    public void connect(String mac){
        //do not double connect, or double response will be registered in the client
        if (state.get() == STATE_CONNECTING) return;


        if (isConfiged()) {

            if (state.get() == STATE_CONNECTED) {
                client.disconnect(bleMac);
            }

            this.bleMac = mac;
            state.set(STATE_CONNECTING);
            notifyStateChanged();

            client.connect(mac, new BleConnectResponse() {
                @Override
                public void onResponse(int code, BleGattProfile data) {
                    if (code == Constants.REQUEST_SUCCESS){
                        boolean containRxUuid = false;
                        boolean containTxUuid = false;
                        if (data.getService(serviceUuid) != null){
                            List<BleGattCharacter> characters = data.getService(serviceUuid).getCharacters();
                            for (BleGattCharacter character : characters){
                                if (character.getUuid().toString().equals(rxUuid.toString())) containRxUuid = true;
                                if (character.getUuid().toString().equals(txUuid.toString())) containTxUuid = true;
                            }
                        }

                        if (containRxUuid && containTxUuid){
                            Log.i("A", "blecomm: ble connected, uuid matched");
                            state.set(STATE_CONNECTED);
                            notifyStateChanged();
                            if (!isReceiving.get()) {
                                isReceiving.set(true);
                                startReceive(true);
                            }
                        }
                        else {
                            reconnect();
                        }
                    }
                    else {
                        Log.i("A", "blecomm: connect failed");
                        state.set(STATE_DISCONNECTED);
                        notifyStateChanged();
                    }

                }
            });

        }
    }

    public void reconnect(){
        if(bleMac != null) {
            Log.i("A", "blecomm: reconnect");
            disconnect(false);
            connect(bleMac);
        }
    }

    /*
    * isActiveDisconnect: set true for manual disconnect, bleMac will be wiped
    * */
    public void disconnect(boolean isActiveDisconnect){
        if(! (state.get() == STATE_DISCONNECTED)) {

            Log.i("A", "blecomm: disconnect and unnotify: " + String.valueOf(isActiveDisconnect));
            client.disconnect(bleMac);
            client.unnotify(bleMac, serviceUuid, txUuid, new BleUnnotifyResponse() {
                @Override
                public void onResponse(int code) {
                }
            });

            isReceiving.set(false);

            cnt -- ;

            state.set(STATE_DISCONNECTED);
            notifyStateChanged();
        }

        if (isActiveDisconnect) {
            bleMac = null;
        }
    }

    public void receiveOnce() {
        client.read(bleMac, serviceUuid, rxUuid, new BleReadResponse() {
            @Override
            public void onResponse(int code, byte[] data) {
                if (code == Constants.REQUEST_SUCCESS) {
                    if (dataListener != null){
                        dataListener.onDataReceived(data);
                    }
                }
                else {
                    onDisconnected();
                }
            }
        });
    }


    /*
    send data by 20 bytes slicing
    */
    public synchronized void send(final byte[] data, boolean isNoRsp){
//        String str = "";
//        for (byte b : data)
//            str += " " + String.valueOf(b & 0x00ff);
//        Log.i("A", "blecomm: data send len = " + data.length + " content = " + str);

        if (data.length < 20){
            if (!isNoRsp){
                client.write(bleMac, serviceUuid, txUuid, data, new BleWriteResponse() {
                    @Override
                    public void onResponse(int code) {
                        if (code != Constants.REQUEST_SUCCESS){
//                            Log.i("A", "blecomm: disconnected while sending rsp");
                        }
                    }
                });
            }
            else {
                client.writeNoRsp(bleMac, serviceUuid, txUuid, data, new BleWriteResponse() {
                    @Override
                    public void onResponse(int code) {
                        if (code != Constants.REQUEST_SUCCESS){
//                            Log.i("A", "blecomm: disconnected while sending noresp");
                        }
                    }
                });
            }
        }
        else {
            for (int m = 0; m+20 < data.length; m +=20){
                byte[] bytes = null;
                if (m+20 < data.length){
                    bytes = new byte[data.length - m];
                    System.arraycopy(data, m, bytes, 0, bytes.length);
                    if (!isNoRsp){
                        client.write(bleMac, serviceUuid, txUuid, bytes, new BleWriteResponse() {
                            @Override
                            public void onResponse(int code) {
                                if (code != Constants.REQUEST_SUCCESS){
//                                    Log.i("A", "blecomm: disconnected while sending rsp data len = " + String.valueOf(data.length));
                                }
                            }
                        });
                    }
                    else {
                        client.writeNoRsp(bleMac, serviceUuid, txUuid, bytes, new BleWriteResponse() {
                            @Override
                            public void onResponse(int code) {
                                if (code != Constants.REQUEST_SUCCESS){
//                                    Log.i("A", "blecomm: disconnected while sending norsp data len = " + String.valueOf(data.length));
                                }
                            }
                        });
                    }
                }
            }
        }

        if (state.get()==STATE_CONNECTED) {
            if(dataListener!=null){
                dataListener.onDataSent(data);
            }
        }
    }

    public void startReceive(boolean isNotify){
        cnt ++;
        Log.i("A", "blecomm: start receive " + String.valueOf(cnt));
        isNotifyReceive = isNotify;

        if (isNotifyReceive){

            client.notify(bleMac, serviceUuid, rxUuid, new BleNotifyResponse() {
                @Override
                public void onNotify(UUID service, UUID character, byte[] value) {
                    if (dataListener != null){
                        dataListener.onDataReceived(value);
//                        String str = "";
//                        for (byte b : value)
//                            str += " " + String.valueOf(b&0x00ff);
//                        Log.i("A", "blecomm: data received len = " + value.length + " content = " + str);
                    }
                }

                @Override
                public void onResponse(int code) {
                    if (code != Constants.REQUEST_SUCCESS){
                        Log.i("A", "blecomm: disconnect while receiving");
                        onDisconnected();
                    }
                }
            });
        }
        else {
             client.indicate(bleMac, serviceUuid, rxUuid, new BleNotifyResponse() {
                @Override
                public void onNotify(UUID service, UUID character, byte[] value) {
                    if (dataListener != null){
                        dataListener.onDataReceived(value);
                    }
                }

                @Override
                public void onResponse(int code) {
                    if (code != Constants.REQUEST_SUCCESS){
                        Log.i("A", "blecomm: disconnected while receiving");
                        onDisconnected();
                    }
                }
            });
        }
    }

    /*
     * stop receive but not disconnect
     */
    public void stopReceive(){
        if (isNotifyReceive){
            client.unnotify(bleMac, serviceUuid, rxUuid, new BleUnnotifyResponse() {
                @Override
                public void onResponse(int code) {
//                    if (code != Constants.REQUEST_SUCCESS) {
//                        onDisconnected();
//                    }
                }
            });
        }
        else {
            client.unindicate(bleMac, serviceUuid, rxUuid, new BleUnnotifyResponse() {
                @Override
                public void onResponse(int code) {
//                    if (code != Constants.REQUEST_SUCCESS) {
//                        onDisconnected();
//                    }
                }
            });
        }
    }

    public String getBleMac() {
        return bleMac;
    }

    /*
     * used to re-enable auto-reconnect
     */
    public void setBleMac(String bleMac) {
        this.bleMac = bleMac;
    }

    public void startScan(){
        SearchRequest searchRequest = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000,3)
                .build();

        client.search(searchRequest, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                if (scanListener != null) scanListener.onScanStarted();
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
//                Log.i("A", "scan device found: " + device.getAddress() + ":" + device.getName());
                if (scanListener != null) scanListener.onDeviceFound(device.getAddress(), device.getName(), device.rssi);

            }

            @Override
            public void onSearchStopped() {
//                Log.i("A", "scan finished");
                if (scanListener != null) scanListener.onScanFinished();

            }

            @Override
            public void onSearchCanceled() {
//                Log.i("A", "scan finished");
                if (scanListener != null) scanListener.onScanFinished();

            }
        });
    }

    public void stopScan(){
        client.stopSearch();
        if (scanListener != null) scanListener.onScanFinished();
    }

    public void onDisconnected(){
        Log.i("A", "blecomm: ondisconnected");
        state.set(STATE_DISCONNECTED);
        notifyStateChanged();
        disconnect(false);
    }

    private void notifyStateChanged(){
        if(stateListener != null){
            stateListener.onBleStateChanged(state.get());
        }
    }

    public interface BleScanListener{
        void onDeviceFound(String mac, String name, int rssi);

        void onScanStarted();
        void onScanFinished();
    }

    public interface BleStateListener{
        void onBleStateChanged(int state);
    }

    public interface BleDataListener{
        void onDataReceived(byte[] data);
        void onDataSent(byte[] data);
    }
}

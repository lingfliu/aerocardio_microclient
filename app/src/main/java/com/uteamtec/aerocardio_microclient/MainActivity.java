package com.uteamtec.aerocardio_microclient;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.uteamtec.aerocardio_microserver.MainRemoteCall;
import com.uteamtec.aerocardio_microserver.MainRemoteCallback;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteBytes;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcg;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcgMark;
import com.uteamtec.bletool.BleComm;

public class MainActivity extends AppCompatActivity {
    //调用微服务的信令，每个app拥有各自的信令
    private final String token = "1d23b3235c2396e37bc060179e1598fe2d685e370833f7dfe9af1904344ce3ed";
    String mac = "80:EA:CA:BD:7A:00"; //待连接的mac地址(举例)，可以通过blecomm.startScan并注册scanListerner获得扫描结果

    TextView txtView;
    MainRemoteCall remoteCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtView = findViewById(R.id.txt);

        Intent serviceIntent = new Intent()
                .setComponent(new ComponentName(
                        "com.uteamtec.aero_cardio_microserver",
                        "com.uteamtec.aerocardio_microserver.MainService"));

        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            remoteCall = MainRemoteCall.Stub.asInterface(iBinder);
            int res = 0;
            try {
                res = remoteCall.signin(token);
                if (res >= 0) {
                    remoteCall.coreOn();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            BleComm.getInstance().setDataListener(new BleComm.BleDataListener() {
                @Override
                public void onDataReceived(byte[] bytes) {
                    try {
                        remoteCall.sendBytes(new RemoteBytes(bytes));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDataSent(byte[] bytes) {

                }
            });

            try {
                remoteCall.registerRemoteCallback(mainRemoteCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            try {
                remoteCall.signout();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    private final MainRemoteCallback mainRemoteCallback = new MainRemoteCallback() {
        @Override
        public void onBleBytesOut(RemoteBytes bytes) throws RemoteException {
            BleComm.getInstance().send(bytes.getBytes(), true);
        }

        @Override
        public void onEcgOut(RemoteEcg ecg) throws RemoteException {
            //TODO: 这里获取ecg数据显示
        }

        @Override
        public void onEcgMarkOut(RemoteEcgMark mark) throws RemoteException {
            //TODO: 这里获取ecgmark数据显示
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    };
}

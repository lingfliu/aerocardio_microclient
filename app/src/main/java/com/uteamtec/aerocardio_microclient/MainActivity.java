package com.uteamtec.aerocardio_microclient;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uteamtec.aerocardio_microserver.MainRemoteCall;
import com.uteamtec.aerocardio_microserver.MainRemoteCallback;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteBytes;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcg;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcgMark;
import com.uteamtec.bletool.BleComm;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 简单的基于aidl的本地跨进程核心服务调用
 */
public class MainActivity extends AppCompatActivity {
    //调用微服务的信令，每个app拥有各自的信令，这个token是com.uteamtec.aerocardio_microclient的包所用的
    private final String TEST_TOKEN = "1d23b3235c2396e37bc060179e1598fe2d685e370833f7dfe9af1904344ce3ed";

    //这两个不要修改
    private final String REMOTE_SERVER_PACKAGE = "com.uteamtec.aerocardio_microserver";
    private final String REMOTE_SERVER_SERVICE = "com.uteamtec.aerocardio_microserver.MainService";


    //待连接的mac地址，请自行获取，保存
    String mac = "80:EA:CA:BD:7A:00";

    //用户登陆用id与pass，请自行获取，保存
    String uid = "000012";
    String pass = "000012";

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    //这是核心调用的接口，此处不再做二次封装
    MainRemoteCall remoteCall;

    @BindView(R.id.app_info)
    TextView appInfo;
    @BindView(R.id.mark)
    TextView markOutput;
    @BindView(R.id.ecg)
    TextView ecgOutput;


    @OnClick(R.id.set_mac)
    void setMac(){
        registerDevice(mac);
    }

    @OnClick(R.id.set_user)
    void setUser(){
        registerUser(uid, pass);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (BuildConfig.DEBUG) {
            CrashHandler.getInstance().init(this);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        BleComm.init(getApplicationContext());

        Intent serviceIntent = new Intent()
                .setComponent(new ComponentName(
                        REMOTE_SERVER_PACKAGE,
                        REMOTE_SERVER_SERVICE));

        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            int res = 0;

            remoteCall = MainRemoteCall.Stub.asInterface(iBinder);
            if (remoteCall == null) return;

            try {

                remoteCall.registerRemoteCallback(mainRemoteCallback);

                res = remoteCall.signin(TEST_TOKEN);
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

            BleComm.getInstance().setStateListener(new BleComm.BleStateListener() {
                @Override
                public void onBleStateChanged(int i) {
                    switch (i) {
                        case BleComm.STATE_DISCONNECTED:
                            //TODO: 上端在这里处理蓝牙断开事件
                            appInfo.setText("蓝牙已断开");
                            break;
                        case BleComm.STATE_CONNECTED:
                            //TODO： 这里处理蓝牙连接事件
                            appInfo.setText("蓝牙已连接上");
                            break;
                        case BleComm.STATE_CONNECTING:
                            //TODO：如果处于正在连接的状态，请避免再次连接
                            appInfo.setText("蓝牙已正在连接");
                            break;
                        default:
                            break;
                    }
                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (remoteCall == null) return;

            try {
                remoteCall.signout();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Intent serviceIntent = new Intent()
                    .setComponent(new ComponentName(
                            REMOTE_SERVER_PACKAGE,
                            REMOTE_SERVER_SERVICE));
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        }
    };

    private final MainRemoteCallback mainRemoteCallback = new MainRemoteCallback.Stub() {
        @Override
        public void onUserRegistered(String uid) throws RemoteException {
            //TODO: 这里处理用户注册通知
            appInfo.setText("用户注册成功");
        }

        @Override
        public void onDeviceRegistered(String mac) throws RemoteException {
            //TODO: 这里处理设备注册通知
            appInfo.setText("设备注册成功");
        }

        @Override
        public void onBleBytesOut(RemoteBytes bytes) throws RemoteException {
            //TODO: 这里转发字节流给蓝牙连接（上层不需要进行管理）
            BleComm.getInstance().send(bytes.getBytes(), true);
        }

        @Override
        public void onEcgOut(RemoteEcg ecg) throws RemoteException {
            //TODO: 这里获取ecg数据显示
            Gson gson = new GsonBuilder().create();
            String str = gson.toJson(ecg);

            ecgOutput.setText(str);
//            iwebView.evalJs("addEcg(" + ecg.toJson + ")");
        }

        @Override
        public void onEcgMarkOut(RemoteEcgMark mark) throws RemoteException {
            //TODO: 这里获取ecgmark数据显示
            Gson gson = new GsonBuilder().create();
            String str = gson.toJson(mark);

            markOutput.setText(str);
//            iwebView.evalJs("addEcgMark(" + mark.toJson + ")");
        }

        @Override
        public void onServerStateChanged(String state) throws RemoteException {
            if (state.equals("disconnected")) {
                //TODO: 这里处理远程服务断开
                appInfo.setText("远程服务器断开");
            }
            else if (state.equals("connected")) {
                //TODO: 这里处理远程服务正常连接
                appInfo.setText("远程服务器已连接");
            }
            else if (state.equals("login")) {
                //TODO: 这里处理设备连接用户已登陆
                appInfo.setText("远程服务器已登陆");
            }
        }

        @Override
        public void onBleDead() throws RemoteException {
            //TODO: 这里通知应用设备无响应
            appInfo.setText("设备无反应");
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    };

    /**
     * 向远程注册用户
     * @param uid
     * @param pass
     */
    private void registerUser(String uid, String pass) {
        try {
            remoteCall.userRegister(uid, pass);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 向远程注册设备mac
     * @param mac
     */
    private void registerDevice(String mac) {
        try {
            remoteCall.deviceRegister(mac);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

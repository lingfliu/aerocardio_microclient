package com.HBuilder.integrate;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.uteamtec.aerocardio_microserver_commons.MainRemoteCall;
import com.uteamtec.aerocardio_microserver_commons.MainRemoteCallback;
import com.uteamtec.aerocardio_utils.CrashHandler;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteBytes;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcg;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcgMark;
import com.uteamtec.bletool.BleComm;


/**
 * Created by liulingfeng on 2017/12/25.
 */

public class BaseActivity extends Activity{
    //调用微服务的信令，每个app拥有各自的信令，这个token是com.HBuilder.integrate包所用的
    private final String TOKEN = "d53a8f0acf62abb1547dd5e02c91dc59138ce8c37aab133ffbc67a723f993927";

    //这两个不要修改
    private final String REMOTE_SERVER_PACKAGE = "com.uteamtec.aerocardio_microserver";
    private final String REMOTE_SERVER_SERVICE = "com.uteamtec.aerocardio_microserver.MainService";

    String uid = "56490283";
    String pass = "0000000000000000";
    String mac = "80:EA:CA:BC:7A:11";

    void setUser(String uid, String pass) {
        this.uid = uid;
        this.pass = pass;
    }

    void setMac(String mac) {
        this.mac = mac;
    }

    //这是核心调用的接口，此处不再做二次封装
    MainRemoteCall remoteCall;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            CrashHandler.getInstance().init(this);
        }

        //沉浸式设置
        //设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏导航键
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
        }

        //Initialize base classes
        BleComm.init(getApplicationContext());
        BleComm.getInstance().setDataListener(new BleComm.BleDataListener() {
            @Override
            public void onDataReceived(byte[] data) {
                try {
                    Log.i("A", "microclient: send bytes to remote server");
                    if (remoteCall != null) {
                        remoteCall.sendBytes(new RemoteBytes(data));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    onRemoteServerDisconnected();
                }
            }

            @Override
            public void onDataSent(byte[] data) {

            }
        });

        BleComm.getInstance().setStateListener(new BleComm.BleStateListener() {
            @Override
            public void onBleStateChanged(int i) {
                Log.i("A", "blecomm: state changed = " + i);
                switch (i) {
                    case BleComm.STATE_DISCONNECTED:
                        //TODO: 上端在这里处理蓝牙断开事件
                        if (listener != null) {
                            listener.onBleStateChanged("disconnected");
                        }

                        if (mac != null) {
                            BleComm.getInstance().reconnect();
                        }
                        break;
                    case BleComm.STATE_CONNECTED:
                        //TODO： 这里处理蓝牙连接事件
                        if (listener != null) {
                            listener.onBleStateChanged("connected");
                        }
                        break;
                    case BleComm.STATE_CONNECTING:
                        //TODO：如果处于正在连接的状态，请避免再次连接
                        if (listener != null) {
                            listener.onBleStateChanged("connecting");
                        }
                        break;
                    default:
                        break;
                }
            }
        });

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

            try {
                res = remoteCall.signin(TOKEN);
                if (res >= 0) {
                    remoteCall.coreOn();
                }
                else {
                    Log.i("A", "microclient: signin failed");
                }

                res = remoteCall.registerRemoteCallback(mainRemoteCallback);
                if (res < 0) {
                    Log.i("A", "microclient: register callback failed");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            Log.i("A", "microclient: register ble listeners");




            BleComm.getInstance().connect(mac);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (remoteCall == null) return;

            try {
                remoteCall.signout();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            //断开自动重连服务，可选设置
//            Intent serviceIntent = new Intent()
//                    .setComponent(new ComponentName(
//                            REMOTE_SERVER_PACKAGE,
//                            REMOTE_SERVER_SERVICE));
//            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            //TODO: 这里处理连接中断的策略
            if(listener != null) {
                listener.onRemoteException(new RemoteException("remote connection died"));
            }
        }
    };

    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private class MainRemoteCallbackImpl extends MainRemoteCallback.Stub {
        @Override
        public void onUserRegistered(final String uid) throws RemoteException {
            //TODO: 这里处理用户注册通知
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(listener != null) {
                        listener.onUserRegistered(uid);
                    }
                }
            });
        }

        @Override
        public void onDeviceRegistered(final String mac) throws RemoteException {
            //TODO: 这里处理设备注册通知
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onDeviceRegistered(mac);
                    }
                }
            });
        }

        @Override
        public void onBleBytesOut(RemoteBytes bytes) throws RemoteException {
            //TODO: 这里转发字节流给蓝牙连接（上层不需要进行管理）
            String str = "";
            for (byte b : bytes.getBytes())
                str += " " + String.valueOf(b&0x00ff);
            Log.i("A", "microclient: server sent bytes back, length = " + str.length() + " content = " + str);
            BleComm.getInstance().send(bytes.getBytes(), true);
        }

        @Override
        public void onEcgOut(final RemoteEcg ecg) throws RemoteException {
            //TODO: 这里获取ecg数据显示
            Log.i("A", "microclient: server send ecg back, start_time = " + ecg.getStartTime());
            if (listener != null) {
                listener.onEcgOut(ecg);
            }
        }

        @Override
        public void onEcgMarkOut(RemoteEcgMark mark) throws RemoteException {
            //TODO: 这里获取ecgmark数据显示
            if (listener != null) {
                listener.onEcgMarkOut(mark);
            }
        }

        @Override
        public void onServerStateChanged(String state) throws RemoteException {
            if (state.equals("disconnected")) {
                //TODO: 这里处理远程服务断开
                if (listener != null) {
                    listener.onServerStateChanged(state);
                }
            }
            else if (state.equals("connected")) {
                //TODO: 这里处理远程服务正常连接
                if (listener != null) {
                    listener.onServerStateChanged(state);
                }
            }
            else if (state.equals("login")) {
                //TODO: 这里处理设备连接用户已登陆
                if (listener != null) {
                    listener.onServerStateChanged(state);
                }
            }
        }

        @Override
        public void onBleDead() throws RemoteException {
            //TODO: 这里通知应用设备无响应
            if (listener != null) {
                listener.onBleDead();
            }
        }
    };

    private final MainRemoteCallbackImpl mainRemoteCallback = new MainRemoteCallbackImpl();

    /**
     * 向远程注册用户
     * @param uid
     * @param pass
     */
    private void registerUser(String uid, String pass) {
        if (remoteCall == null) return;
        try {
            remoteCall.userRegister(uid, pass);
        } catch (RemoteException e) {
            e.printStackTrace();
            onRemoteServerDisconnected();
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
            onRemoteServerDisconnected();
        }
    }


    //处理连接断开的异常
    private void onRemoteServerDisconnected(){
//        remoteCall = null;
//        Intent serviceIntent = new Intent()
//                    .setComponent(new ComponentName(
//                            REMOTE_SERVER_PACKAGE,
//                            REMOTE_SERVER_SERVICE));
//            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private BaseListener listener;

    public void setListener(BaseListener listener) {
        this.listener = listener;
    }

    public interface BaseListener {
        void onBleDead();
        void onUserRegistered(String uid);
        void onDeviceRegistered(String mac);
        void onServerStateChanged(String state);
        void onBleStateChanged(String state);
        void onRemoteException(Exception e);
        void onEcgOut(RemoteEcg ecg);
        void onEcgMarkOut(RemoteEcgMark mark);
    }
}

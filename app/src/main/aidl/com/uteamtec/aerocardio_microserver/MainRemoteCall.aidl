// MainRemoteCall.aidl
package com.uteamtec.aerocardio_microserver;

import com.uteamtec.aerocardio_microserver_commons.types.RemoteBytes;
import com.uteamtec.aerocardio_microserver.MainRemoteCallback;

interface MainRemoteCall {

    int signin(String token);
    int signout();
    String fetchState();
    int coreOn();
    int coreReset();
    int coreOff();

    int sendBytes(in RemoteBytes remoteBytes);

    void userRegister(String uid, String pass);
    void deviceRegister(String mac);

    String getAppServerState();

    int registerRemoteCallback(MainRemoteCallback cb);
    int unregisterRemoteCallback(MainRemoteCallback cb);
}

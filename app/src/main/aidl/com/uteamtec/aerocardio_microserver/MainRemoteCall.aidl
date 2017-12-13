// MainRemoteCall.aidl
package com.uteamtec.aerocardio_microserver;

import com.uteamtec.aerocardio_microserver_commons.types.RemoteBytes;
import com.uteamtec.aerocardio_microserver.MainRemoteCallback;

interface MainRemoteCall {

    int signin(in String token);
    int signout();
    String fetchState();
    int coreOn();
    int coreOff();

    int sendBytes(in RemoteBytes remoteBytes);

    void registerRemoteCallback(in MainRemoteCallback cb);
    void unregisterRemoteCallback(in MainRemoteCallback cb);
}

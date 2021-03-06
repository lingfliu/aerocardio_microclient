// MainRemoteCallback.aidl
package com.uteamtec.aerocardio_microserver;

import com.uteamtec.aerocardio_microserver_commons.types.RemoteBytes;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcg;
import com.uteamtec.aerocardio_microserver_commons.types.RemoteEcgMark;

interface MainRemoteCallback {

    void onUserRegistered(String uid);
    void onDeviceRegistered(String mac);

    void onBleBytesOut(out RemoteBytes bytes);
    void onEcgOut(out RemoteEcg ecg);
    void onEcgMarkOut(out RemoteEcgMark mark);

    void onServerStateChanged(String state);
    void onBleDead();
}

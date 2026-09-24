package de.blinkt.openvpn.core;

import de.blinkt.openvpn.VpnProfile;

public class OpenVPNThreadv3 implements Runnable, OpenVPNManagement {
    public OpenVPNThreadv3(OpenVPNService service, VpnProfile profile) {}
    @Override public void run() {}
    @Override public boolean stopVPN(boolean replace) { return false; }
    @Override public void reconnect() {}
    @Override public void pause(pauseReason reason) {}
    @Override public void resume() {}
    @Override public void networkChange(boolean sameNetwork) {}
    @Override public void setPauseCallback(PausedStateCallback callback) {}
    @Override public void sendCRResponse(String response) {}
    @Override public void sendAccMessage(AccMessage msg) {}
}

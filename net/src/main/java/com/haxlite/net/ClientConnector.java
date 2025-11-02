package com.goalrift.football.net;

import com.goalrift.football.net.dto.InputMessage;
import com.goalrift.football.net.dto.Snapshot;

public interface ClientConnector {
    void connect(Listener listener);
    void sendInput(InputMessage msg);
    interface Listener {
        void onConnected();
        void onSnapshot(Snapshot snap);
        void onDisconnected(String reason);
    }
}

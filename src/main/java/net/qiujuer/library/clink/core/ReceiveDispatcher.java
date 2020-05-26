package net.qiujuer.library.clink.core;

import java.io.Closeable;

public interface ReceiveDispatcher extends Closeable {

    void start();

    void stop();
    interface ReceivePacketCallback{
        void onReceivePacketCompleted(ReceivePacket receivePacket);
    }
}

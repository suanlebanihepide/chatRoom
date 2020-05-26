/*
 * @Author: shenzheng
 * @Date: 2020/5/26 16:38
 */

package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.ReceiveDispatcher;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {

    private final AtomicBoolean isClose = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback receivePacketCallback;
    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket packetTemp;
    private byte[] buffer;
    private int total;
    private int position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receivePacketCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(ioArgsEventListener);
        this.receivePacketCallback = receivePacketCallback;
    }

    @Override
    public void start() {
        registerReceive();
    }

    private void registerReceive() {
        try {
            receiver.receiveAsync(ioArgs);
        } catch (IOException e) {
            closeAndNotify();
        }

    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {
        if (isClose.compareAndSet(false, true)) {
            ReceivePacket packet = this.packetTemp;
            if(packet!=null){
                packetTemp= null;
                CloseUtils.close(packet);
            }
        }

    }

    private IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

            int receiveSize;
            if (packetTemp == null) {
                receiveSize = 4;

            } else {
                receiveSize = Math.min(total - position, args.capacity());
            }
            args.setLimit(receiveSize);
        }
        @Override
        public void onCompleted(IoArgs args) {
            //解析数据然后继续接收下一个数据
            assemblePacket(args);
            registerReceive();
        }
    };

    //解析数据到Packet
    private void assemblePacket(IoArgs args) {
        if (packetTemp == null) {
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }
        int count = args.writeTo(buffer, 0);
        if (count > 0) {
            packetTemp.save(buffer, count);
            System.out.println("解析到Packet");
            position += count;
            if (position == total) {
                completePacket();
                packetTemp = null;
            }
        }

    }

    //完成数据接收
    private void completePacket() {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        receivePacketCallback.onReceivePacketCompleted(packet);
    }
}

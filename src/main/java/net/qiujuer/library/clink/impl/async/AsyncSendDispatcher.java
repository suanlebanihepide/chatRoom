/*
 * @Author: shenzheng
 * @Date: 2020/5/26 15:44
 */

package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendDispatcher;
import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private SendPacket packetTemp;
    private IoArgs ioArgs = new IoArgs();
    private int total;//packet大小
    private int position;//当前packet发送了多长

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);

        if(isSending.compareAndSet(false,true)){
            sendNextPacket();
        }
    }

    private void sendNextPacket() {

        if (packetTemp != null) {
            CloseUtils.close(packetTemp);
        }
        SendPacket packet = packetTemp=queue.poll();
        if (packet == null) {
            //队列为空
            isSending.set(false);
            return;
        }
        total = packet.length();
        position = 0;
        sendCurrentPacket();

    }

    private void sendCurrentPacket() {
        IoArgs args = ioArgs;
        args.startWriting();
        if (position >= total) {
            sendNextPacket();
            return;
        } else if (position == 0) {
            //首包，需要携带长度信息
            args.writeLength(total);
        }
        byte[] bytes = packetTemp.bytes();
        //吧bytes吸入IoArgs
        int count = args.readFrom(bytes, position);
        position += count;
        //完成封装
        args.finishWriting();
        try {
            sender.sendAsync(args,ioArgsEventListener);
        } catch (IOException e) {
            closeAndNotify();
            e.printStackTrace();
        }
    }

    private void closeAndNotify() {
        //关闭自己并唤醒
        CloseUtils.close();
    }
    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false ,true)){
            isSending.set(false);
            SendPacket packet = packetTemp;
            if(packet!=null){
                packet=null;
                CloseUtils.close(packet);
            }
        }
    }
    private final IoArgs.IoArgsEventListener ioArgsEventListener =new IoArgs.IoArgsEventListener() {
    @Override
    public void onStarted(IoArgs args) {

    }

    @Override
    public void onCompleted(IoArgs args) {
        //继续发送当前包
        sendCurrentPacket();
    }
};

    @Override
    public void cancel(SendPacket packet) {

    }


}

/*
 * @Author: shenzheng
 * @Date: 2020/5/25 22:18
 */

package net.qiujuer.library.clink.impl;

import com.sun.org.apache.xpath.internal.operations.String;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.IoProvider;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Cloneable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventListener receiveIoEventListener;
    private IoArgs.IoArgsEventListener sendIoEventListener;

    private IoArgs receiveArgsTemp;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;
        this.receiveArgsTemp = new IoArgs();
        channel.configureBlocking(false);
    }


    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()) {
            throw new IOException("current channel closed");
        }
        sendIoEventListener = listener;
        //当前发送的数据附加到回调中

        outputCallback.setAttach(args);
        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public void close() throws IOException {

//        if (isClosed.compareAndSet(false, true)) {
//            ioProvider.unRegisterInput(channel);
//            ioProvider.unRegisterOutput(channel);
//            CloseUtils.close(channel);
//            listener.onChannelClosed(channel);
//        }
    }

    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {


            IoArgs args = receiveArgsTemp;
            IoArgs.IoArgsEventListener receiveIoEventListener = SocketChannelAdapter.this.receiveIoEventListener;
            receiveIoEventListener.onStarted(args);
            try {
                if (args.readFrom(channel) > 0) {
                    //读取完成
                    receiveIoEventListener.onCompleted(args);
                } else {
                    throw new IOException("can not read data");
                }

            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };
    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }
            IoArgs args = (IoArgs) getAttach();
            IoArgs.IoArgsEventListener listener = sendIoEventListener;
            listener.onStarted(args);
            try {
                if (args.writeTo(channel) > 0) {
                    //读取完成回调
                    listener.onCompleted(args);
                } else {
                    throw new IOException("can not write data");
                }
            } catch (IOException e) {
                CloseUtils.close(SocketChannelAdapter.this);
            }

        }
    };


    @Override
    public void setReceiveListener(IoArgs.IoArgsEventListener listener) {
        receiveIoEventListener = listener;
    }

    @Override
    public boolean receiveAsync(IoArgs ioArgs) throws IOException {
        if (isClosed.get()) {
            throw new IOException("current channel closed");
        }
        receiveArgsTemp = ioArgs;
        return ioProvider.registerInput(channel, inputCallback);
    }


    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}

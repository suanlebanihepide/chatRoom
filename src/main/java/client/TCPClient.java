package client;

import client.bean.ServerInfo;
import net.qiujuer.library.clink.core.Connector;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TCPClient extends Connector {


    public TCPClient(SocketChannel socketChannel) throws IOException {
       setup(socketChannel);
    }

    public void exit() {

        CloseUtils.close(this);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        System.out.println("连接已经关闭");
    }



    public static TCPClient startWith(ServerInfo info) throws IOException {
       SocketChannel socketChannel = SocketChannel.open();

        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());

        try {

            return new TCPClient(socketChannel);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }

        return null;
    }

//
//    static class ReadHandler extends Thread {
//        private boolean done = false;
//        private final InputStream inputStream;
//
//        ReadHandler(InputStream inputStream) {
//            this.inputStream = inputStream;
//        }
//
//        @Override
//        public void run() {
//            super.run();
//            try {
//                // 得到输入流，用于接收数据
//                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));
//
//                do {
//                    String str;
//                    try {
//                        // 客户端拿到一条数据
//                        str = socketInput.readLine();
//                    } catch (SocketTimeoutException e) {
//                        continue;
//                    }
//                    if (str == null) {
//                        System.out.println("连接已关闭，无法读取数据！");
//                        break;
//                    }
//                    // 打印到屏幕
//                    System.out.println(str);
//                } while (!done);
//            } catch (Exception e) {
//                if (!done) {
//                    System.out.println("连接异常断开：" + e.getMessage());
//                }
//            } finally {
//                // 连接关闭
//                CloseUtils.close(inputStream);
//            }
//        }
//
//        void exit() {
//            done = true;
//            CloseUtils.close(inputStream);
//        }
//    }
}

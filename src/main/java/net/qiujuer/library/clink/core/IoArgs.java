/*
 * @Author: shenzheng
 * @Date: 2020/5/25 21:53
 */

package net.qiujuer.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IoArgs {
    private int limit = 6;
    public byte[]byteBuffer = new byte[256];
    public ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int readFrom(byte [] bytes,int offset){
        int size = Math.min(bytes.length-offset,buffer.remaining());
        buffer.put(bytes,offset,size);
        return size ;
    }
    public int writeTo(byte[]bytes,int offset){
        int size = Math.min(bytes.length-offset,buffer.remaining());
        buffer.get(bytes,offset,size);
        return size ;
    }
    //从channel读取数据
    public int readFrom(SocketChannel channel) throws IOException {
        startWriting();
        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.read(buffer);
            if(len<0)
            {
                break;
            }
            bytesProduced+=len;
        }
        finishWriting();
        return bytesProduced;
    }
    public int writeTo(SocketChannel channel) throws IOException {

        int bytesProduced = 0;
        while(buffer.hasRemaining()){
            int len = channel.write(buffer);
            if(len<0)
            {
                throw new EOFException();
            }
            bytesProduced+=len;
        }

        return bytesProduced;
    }
    public void startWriting(){
        buffer.clear();
        buffer.limit(limit);
    }
    public void setLimit(int limit){
        this.limit = limit;
    }
    public void finishWriting(){
        buffer.flip();
    }

    public void writeLength(int total) {
    buffer.putInt(total);

    }
    public int readLength(){
       return  buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }


    public interface  IoArgsEventListener{
        void onStarted(IoArgs args);
        void onCompleted(IoArgs args);
    }

}

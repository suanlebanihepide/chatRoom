/*
 * @Author: shenzheng
 * @Date: 2020/5/26 15:21
 */

package net.qiujuer.library.clink.core;

import java.io.IOException;

public abstract  class ReceivePacket extends  Packet{
    public abstract void save(byte[] bytes,int count);

    @Override
    public void close() throws IOException {
        super.close();
    }
}

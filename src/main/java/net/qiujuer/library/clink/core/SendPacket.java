/*
 * @Author: shenzheng
 * @Date: 2020/5/26 15:20
 */

package net.qiujuer.library.clink.core;

import java.io.IOException;

public abstract  class SendPacket extends Packet{
    public boolean isCanceled;
    public abstract byte[] bytes();
    public boolean isCanceled(){
        return isCanceled;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}

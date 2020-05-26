/*
 * @Author: shenzheng
 * @Date: 2020/5/26 15:18
 */

package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

//公共数据封装，提供了类型以及基本的长度定义
public abstract  class Packet implements Closeable {
    protected byte type;
    protected int length;
    public byte type(){
        return type;
    }
    public int length(){
        return length;
    }

    @Override
    public void close() throws IOException {

    }
}

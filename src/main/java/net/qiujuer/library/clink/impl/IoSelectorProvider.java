/*
 * @Author: shenzheng
 * @Date: 2020/5/25 22:17
 */

package net.qiujuer.library.clink.impl;

import net.qiujuer.library.clink.core.IoProvider;
import net.qiujuer.library.clink.utils.CloseUtils;
import sun.dc.pr.PRError;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IoSelectorProvider implements IoProvider {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    //作为锁判断是否处于注册过程
    private final AtomicBoolean isRegInput = new AtomicBoolean(false);
    private final AtomicBoolean isRegOutput = new AtomicBoolean(false);

    private final Selector readSelector;
    private final Selector writeSelector;
    private final HashMap<SelectionKey, Runnable> inputCallBackMap = new HashMap<SelectionKey, Runnable>();
    private final HashMap<SelectionKey, Runnable> outputCallBackMap = new HashMap<SelectionKey, Runnable>();
    private final ExecutorService inputHandlPool;
    private final ExecutorService outputHandlPool;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();
        inputHandlPool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlPool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        //开始读写监听
        startRead();
        startWrite();
    }



    private void startRead() {
        Thread thread = new Thread("Clink IoSelectorProvider readSelector Thread") {
            @Override
            public void run() {
                super.run();
                System.out.println("开始监听读取");
                while (!isClosed.get()) {

                    try {
                        if (readSelector.select() == 0) {
                            waitSelection(isRegInput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();

                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallBackMap, inputHandlPool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();

    }

    private void startWrite() {
        Thread thread = new Thread("Clink IoSelectorProvider WriteSelector Thread") {
            @Override
            public void run() {
                System.out.println("开始监听写入");
                super.run();
                while (!isClosed.get()) {

                    try {
                        if (writeSelector.select() == 0) {
                            waitSelection(isRegOutput);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();

                        for (SelectionKey selectionKey : selectionKeys) {
                            if (selectionKey.isValid()) {
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallBackMap, outputHandlPool);
                            }
                        }
                        selectionKeys.clear();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }


    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {

        return registerSelection(channel, readSelector, SelectionKey.OP_READ,
                isRegInput, inputCallBackMap, callback) != null;


    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return registerSelection(channel, writeSelector, SelectionKey.OP_WRITE,
                isRegOutput, outputCallBackMap, callback) != null;


    }

    @Override
    public void unRegisterInput(SocketChannel channel) {

        unRegisterSelection(channel,readSelector,inputCallBackMap);

    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel,writeSelector,outputCallBackMap);
    }

    @Override
    public void close() throws IOException {

        if(isClosed.compareAndSet(false,true)){
            inputHandlPool.shutdownNow();
            outputHandlPool.shutdownNow();
            inputCallBackMap.clear();;
            outputCallBackMap.clear();;
            readSelector.wakeup();
            writeSelector.wakeup();
            CloseUtils.close(readSelector,writeSelector);
        }
    }
    private static void waitSelection(final AtomicBoolean locker ){

        synchronized (locker){
            if(locker.get()){
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SelectionKey registerSelection(SocketChannel channel, Selector selector, int registerOps,
                                                  AtomicBoolean locker, HashMap<SelectionKey, Runnable> map, Runnable runnable) {

        synchronized (locker) {
            //设置锁定状态
            locker.set(true);
            try {
                //唤醒当前selector 让其不处于select()状态
                selector.wakeup();
                SelectionKey key = null;
                if (channel.isRegistered()) {
                    //查询是否已经注册
                    key = channel.keyFor(selector);
                    if (key != null) {
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }
                if (key == null) {
                    //注册selector的到key
                    key =channel.register(selector, registerOps);
                    //注册回调
                    map.put(key, runnable);
                }
                return key;

            } catch (ClosedChannelException e) {
                return null;
            } finally {
                locker.set(false);
                try {
                    locker.notify();
                } catch (Exception e) {

                }
            }
        }

    }

    private static void unRegisterSelection (SocketChannel channel, Selector selector, Map<SelectionKey,Runnable> map){

        if(channel.isRegistered()){
            SelectionKey key =  channel.keyFor(selector);
            if(key!=null){
                //取消监听
                key.cancel();
                map.remove(key);
                selector.wakeup();
            }
        }

    }
    private static void handleSelection(SelectionKey selectionKey, int opRead, HashMap<SelectionKey, Runnable> inputCallBackMap, ExecutorService inputHandlPool) {

        //重点
        //取消监听
        selectionKey.interestOps(selectionKey.readyOps() & ~opRead);
        Runnable runnable = null;

        try {
            runnable = inputCallBackMap.get(selectionKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //异步调度
        if (runnable != null && !inputHandlPool.isShutdown()) {

            inputHandlPool.execute(runnable);
        }
    }

    static class IoProviderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}

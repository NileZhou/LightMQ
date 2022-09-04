package org.light;

import org.light.constant.ServerConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BizWorker implements Runnable {

    private Thread thread;

    private Selector selector;

    private String name;

    private volatile boolean initialized = false;

    private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    public BizWorker(String name) {
        this.name = name;
    }

    /**
     * register the open event
     */
    public void register(SocketChannel clientChannel) throws IOException {
        // if not initialized, init the thread and selector
        if (!initialized) {
            selector = Selector.open();
            thread = new Thread(this, name);
            thread.start();
            initialized = true;
        }

        // add a task (not execute it) to queue
        queue.add(() -> {
            try {
                clientChannel.register(selector, SelectionKey.OP_READ, null);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        });

        selector.wakeup();
    }

    @Override
    public void run() {
        while (true) {
            try {
                // get r/w event. selector will be blocked if no read/write event
                selector.select();

                Runnable taskThread = queue.poll();
                if (taskThread != null) {
                    taskThread.run();
                }

                // iterate the selector keys
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();

                    if (selectionKey.isReadable()) {
                        ByteBuffer buffer = ByteBuffer.allocate(ServerConfig.ServerByteBufferSize);
                        SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                        int read = clientChannel.read(buffer);
                        if (read != -1) {
                            buffer.flip();
                            System.out.println(Thread.currentThread().getName() + "is reading..." + clientChannel.getRemoteAddress());
                            System.out.println(Charset.defaultCharset().decode(buffer));

                        } else {
                            System.out.println("client closed " + clientChannel.getRemoteAddress());
                            selectionKey.cancel();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

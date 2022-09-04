package org.light;

import org.light.constant.ServerConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hello world!
 *
 */
public class Server
{

    public static void main( String[] args ) throws IOException
    {
        Thread.currentThread().setName("LightMQServer");
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.bind(new InetSocketAddress(ServerConfig.HOST, ServerConfig.PORT));

        Selector lightServer = Selector.open();
        // wtf is this ?
        server.register(lightServer, SelectionKey.OP_ACCEPT, null);

        // create the workers
        System.out.println("available processors of current node machine: " + Runtime.getRuntime().availableProcessors());
        BizWorker[] bizWorkers = new BizWorker[ServerConfig.WORKER_NUM];
        for (int i=0; i< bizWorkers.length; ++i) {
            bizWorkers[i] = new BizWorker(ServerConfig.PROVIDER_WORKER_NAME + i);
        }

        AtomicInteger atomicIndex = new AtomicInteger();

        while (true) {
            lightServer.select();
            Iterator<SelectionKey> iterator = lightServer.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    if (key.isAcceptable()) {
                        // get r/w channel
                        SocketChannel clientChannel = server.accept();
                        clientChannel.configureBlocking(false);
                        System.out.println("connected: " + clientChannel.getRemoteAddress());

                        // associate channel with business worker (round robin)
                        bizWorkers[atomicIndex.getAndIncrement() % bizWorkers.length].register(clientChannel);
                        System.out.println("after register: " + clientChannel.getRemoteAddress());
                    }

                    if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(ServerConfig.ServerByteBufferSize);
                        int read = clientChannel.read(buffer);
                        if (read == -1) {
                            key.cancel();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // ensure the client can be closed
                    key.cancel();
                }
            }
        }
    }
}

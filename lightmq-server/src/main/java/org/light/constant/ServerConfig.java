package org.light.constant;

public class ServerConfig {
    public static final String HOST = "localhost";

    public static final Integer PORT = 9090;

    public static final Integer ServerByteBufferSize = 1 << 8;

    public static final Integer WORKER_NUM = 4;

    public static final String PROVIDER_WORKER_NAME = "provider-worker-";

    public static final String CONSUMER_WORKER_NAME = "consumer-worker-";
}

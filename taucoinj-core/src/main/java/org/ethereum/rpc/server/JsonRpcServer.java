package org.ethereum.rpc.server;

import org.ethereum.facade.Ethereum;

public abstract class JsonRpcServer {
    public JsonRpcServer(Ethereum ethereum) {};
    public abstract void start(int port) throws Exception;
    public abstract void stop();
}
package org.ethereum.rpc.server;

import org.ethereum.facade.Ethereum;

public abstract class JsonRpcServer {
    public JsonRpcServer(Ethereum ethereum) {};
    public abstract void start() throws Exception;
    public abstract void stop();
}
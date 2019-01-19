package org.ethereum.rpc.server;

import org.ethereum.facade.Ethereum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ethereum.config.SystemProperties.CONFIG;

public class JsonRpcServerFactory {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public static JsonRpcServer createJsonRpcServer(Ethereum ethereum) {
        String type = CONFIG.getRpcServerType();
        logger.info("Json rpc serer type {}", type);
        if ("full".equals(type)) {
            return new org.ethereum.rpc.server.full.JsonRpcServer(ethereum);
        } else {
            return new org.ethereum.rpc.server.light.JsonRpcServer(ethereum);
        }
    }
}

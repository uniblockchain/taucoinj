package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.net.eth.EthVersion;
import io.taucoin.net.eth.handler.EthHandler;

public class eth_protocolVersion extends JsonRpcServerMethod {

    public eth_protocolVersion (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        String tmp = Byte.toString(EthVersion.LOWER);
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}
package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.core.Block;
import io.taucoin.core.ImportResult;
import io.taucoin.facade.Taucoin;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class tau_sendNewBlock extends JsonRpcServerMethod {
    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public tau_sendNewBlock (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() < 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }

        ArrayList<String> result = new ArrayList<String>();
        Block block = new Block(jsToByteArray((String)params.get(0)), true);

        ImportResult importResult = taucoin.getBlockchain().tryToConnect(block);

        result.add("result: " + importResult.toString());

        JSONRPC2Response res = new JSONRPC2Response(result, req.getID());
        return res;
    }

}

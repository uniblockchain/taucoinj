package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.config.MainNetParams;
import io.taucoin.core.Account;
import io.taucoin.core.Address;
import io.taucoin.core.Base58;
import io.taucoin.crypto.ECKey;
import io.taucoin.facade.Taucoin;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class tau_importprikey extends JsonRpcServerMethod {
    private static final Logger log = LoggerFactory.getLogger("rpc");
    public tau_importprikey (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() < 1 ) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }
        //String prikey = Hex.toHexString(jsToByteArray((String)params.get(0)));
        String prikey = (String)params.get(0);
        log.info("privkey is {}",prikey);
        taucoin.getWallet().importKey(jsToByteArray((String)params.get(0)));
        JSONRPC2Response res = new JSONRPC2Response(req.getID());
        return res;

    }
}

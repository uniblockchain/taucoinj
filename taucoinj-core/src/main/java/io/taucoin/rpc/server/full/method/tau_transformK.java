package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.config.MainNetParams;
import io.taucoin.core.Address;
import io.taucoin.core.DumpedPrivateKey;
import io.taucoin.core.Utils;
import io.taucoin.core.VersionedChecksummedBytes;
import io.taucoin.crypto.ECKey;
import io.taucoin.facade.Taucoin;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;

public class tau_transformK extends JsonRpcServerMethod{
    public tau_transformK (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        String aimString = "";
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            String privateKey = (String) params.get(0);
            boolean type = (boolean) params.get(1);
            ECKey key;
            if(type){
                DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(MainNetParams.get(),privateKey);
                key = dumpedPrivateKey.getKey();
                aimString = Hex.toHexString(key.getPrivKeyBytes());
            }else {
                BigInteger privKey = new BigInteger(privateKey,16);
                key = ECKey.fromPrivate(privKey);
                aimString = key.getPrivateKeyAsWiF(MainNetParams.get());
            }
        }
        JSONRPC2Response res = new JSONRPC2Response(aimString, req.getID());
        return res;
    }
}

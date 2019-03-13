package io.taucoin.rpc.server.full.method;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.config.MainNetParams;
import io.taucoin.core.*;
import io.taucoin.facade.Taucoin;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

public class tau_transformS2T extends JsonRpcServerMethod {
    public tau_transformS2T (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        String aimString = "";
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            String addr = (String) params.get(0);
            boolean type = (boolean) params.get(1);
            if(type){
               Address temp = new Address(MainNetParams.get(),Utils.parseAsHexOrBase58(addr));
               aimString = temp.toBase58();
            }else {
                VersionedChecksummedBytes toEncoedAddress= new VersionedChecksummedBytes(addr);
                byte[] to = toEncoedAddress.getBytes();
                aimString = Hex.toHexString(to);
            }
        }
        JSONRPC2Response res = new JSONRPC2Response(aimString, req.getID());
        return res;
    }
}

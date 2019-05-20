package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.config.MainNetParams;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.core.*;
import io.taucoin.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

public class tau_newaccount extends JsonRpcServerMethod {

    public tau_newaccount (Taucoin taucoin) {
    super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        Account account = new Account();
        account.init();
        taucoin.getWallet().addNewAccount(account);
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add("priKey: "+Hex.toHexString(account.getEcKey().getPrivKey().toByteArray()));
        tmp.add("WiFKey: "+account.getEcKey().getPrivateKeyAsWiF(MainNetParams.get()));
        tmp.add("pubKey: "+ Hex.toHexString(account.getEcKey().getPubKey()));
        tmp.add("oriAdd: " + account.getEcKey().getAddress());
        tmp.add("tauAdd: " + account.getEcKey().getAccountAddress().toBase58());
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}

package io.taucoin.rpc.server.light.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.light.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;

import java.util.ArrayList;

public class proxy extends JsonRpcServerMethod {

    ArrayList<String> proxyMethods;
    ArrayList<String> deprecatedMethods;

    public proxy (Taucoin taucoin)
    {
        super(taucoin);
        proxyMethods = new ArrayList<>();
        proxyMethods.add("web3_clientVersion");
        proxyMethods.add("web3_sha3");
        proxyMethods.add("net_version");
        proxyMethods.add("net_listening");
        proxyMethods.add("net_peerCount");
        proxyMethods.add("tau_protocolVersion");
        proxyMethods.add("tau_gasPrice");
        proxyMethods.add("tau_blockNumber");
        proxyMethods.add("tau_getBalance");
        proxyMethods.add("tau_getStorageAt");
        proxyMethods.add("tau_getTransactionCount");
        proxyMethods.add("tau_getBlockTransactionCountByHash");
        proxyMethods.add("tau_getBlockTransactionCountByNumber");
        proxyMethods.add("tau_getUncleCountByBlockHash");
        proxyMethods.add("tau_getUncleCountByBlockNumber");
        proxyMethods.add("tau_getCode");
        proxyMethods.add("tau_getBlockByHash");
        proxyMethods.add("tau_getBlockByNumber");
        proxyMethods.add("tau_getTransactionByHash");
        proxyMethods.add("tau_getTransactionByBlockHashAndIndex");
        proxyMethods.add("tau_getTransactionByBlockNumberAndIndex");
        proxyMethods.add("tau_getTransactionReceipt");
        proxyMethods.add("tau_getUncleByBlockHashAndIndex");
        proxyMethods.add("tau_getUncleByBlockNumberAndIndex");
        proxyMethods.add("tau_getCompilers");
        proxyMethods.add("tau_compileSolidity");
        proxyMethods.add("tau_compileLLL");
        proxyMethods.add("tau_compileSerpent");
        proxyMethods.add("tau_newFilter");
        proxyMethods.add("tau_newBlockFilter");
        proxyMethods.add("tau_newPendingTransactionFilter");
        proxyMethods.add("tau_uninstallFilter");
        proxyMethods.add("tau_getFilterChanges");
        proxyMethods.add("tau_getFilterLogs");
        proxyMethods.add("tau_getLogs");

        proxyMethods.add("shh_version");
        proxyMethods.add("shh_post");
        proxyMethods.add("shh_newIdentity");
        proxyMethods.add("shh_hasIdentity");
        proxyMethods.add("shh_newGroup");
        proxyMethods.add("shh_addToGroup");
        proxyMethods.add("shh_newFilter");
        proxyMethods.add("shh_uninstallFilter");
        proxyMethods.add("shh_getFilterChanges");
        proxyMethods.add("shh_getMessages");

        //TODO: issue methods - they generate transaction but must call them in blockchain.
        proxyMethods.add("tau_call");
        proxyMethods.add("tau_estimateGas");


        deprecatedMethods = new ArrayList<>();
        //db - deprecated in specification
        deprecatedMethods.add("db_getHex");
        deprecatedMethods.add("db_getString");
        deprecatedMethods.add("db_putHex");
        deprecatedMethods.add("db_putString");
        //mining - deprecated because will be mess over global.
        deprecatedMethods.add("tau_getWork");
        deprecatedMethods.add("tau_hashrate");
        deprecatedMethods.add("tau_mining");
        deprecatedMethods.add("tau_submitWork");
    }

    @Override
    public String[] handledRequests() {
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.addAll(proxyMethods);
        tmp.addAll(deprecatedMethods);
        return tmp.toArray(new String[tmp.size()]);
    }

    @Override
    public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
        if (proxyMethods.contains(req.getMethod())) {
            return worker(req, ctx);
        } else if (deprecatedMethods.contains(req.getMethod())) {
            return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
        } else {
            return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
        }
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        JSONRPC2Response res = getRemoteData(req);
        if (res == null) {
            return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
        }
        return res;
    }
}
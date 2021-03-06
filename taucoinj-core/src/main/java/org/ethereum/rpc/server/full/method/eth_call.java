package org.ethereum.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import net.minidev.json.JSONObject;
import org.ethereum.rpc.server.full.JsonRpcServerMethod;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.facade.Ethereum;
import org.ethereum.vm.VM;
import org.ethereum.vm.program.Program;
import org.spongycastle.util.encoders.Hex;
import java.util.List;

public class eth_call extends JsonRpcServerMethod {

    public eth_call (Ethereum ethereum) {
        super(ethereum);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            JSONObject obj = (JSONObject)params.get(0);
            Transaction tx;
            try {
                tx = jsToTransaction(obj);
            } catch (Exception e) {
                return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
            }

            String height = (String)params.get(1);
            long blockNumber = getBlockNumber(height);
            byte[] root = ethereum.getBlockchain().getBestBlock().getStateRoot();

            if (blockNumber >= 0) {
                Repository repository = (Repository)ethereum.getRepository();
                repository.syncToRoot(ethereum.getBlockchain().getBlockByNumber(blockNumber).getStateRoot());
            }

            VM vm = new VM();
            Program program = new Program(tx.getData(), null);
            vm.play(program);
            byte[] result = program.getResult().getHReturn();

            if (blockNumber >= 0) {
                Repository repository = (Repository)ethereum.getRepository();
                repository.syncToRoot(root);
            }

            String tmp = "0x" + Hex.toHexString(result);
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}
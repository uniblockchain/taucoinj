package io.taucoin.rpc.server.light;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.server.*;
import net.minidev.json.JSONObject;
import io.taucoin.core.Account;
import io.taucoin.core.Transaction;
import io.taucoin.core.transaction.TransactionVersion;
import io.taucoin.core.transaction.TransactionOptions;
import io.taucoin.facade.Taucoin;
import io.taucoin.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class JsonRpcServerMethod implements RequestHandler {

    private String name = "";
    protected Taucoin taucoin;
    private JSONRPC2Session jpSession;
    private String currentUrl;

    public JsonRpcServerMethod(Taucoin taucoin) {
        this.taucoin = taucoin;
        name = this.getClass().getSimpleName();
    }

    public String[] handledRequests() {
        return new String[]{name};
    }

    public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
        if (req.getMethod().equals(name)) {
            return worker(req, ctx);
        } else {
            return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
        }
    }

    protected abstract JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx);

    protected long getBlockNumber(String height) {
        long blockNumber = 0;
        switch (height) {
            case "earliest":
                blockNumber = 0;
                break;
            case "latest":
                blockNumber = -1;
                break;
            case "pending":
                blockNumber = -2;
                break;
            default:
                blockNumber = jsToLong(height);
        }
        if (blockNumber >= 0)
            blockNumber = -1;
        return blockNumber;
    }

    protected String clearJSString(String data) {
        if (data.substring(0, 2).equals("0x"))
            return data.substring(2);
        return data;
    }

    protected byte[] jsToAddress(String data) {
        return Hex.decode(clearJSString(data));
    }

    protected byte[] jsToByteArray(String data) {
        return Hex.decode(clearJSString(data));
    }

    protected int jsToInt(String data) {
        return Integer.parseInt(clearJSString(data), 16);
    }

    protected long jsToLong(String data) {
        return Long.parseLong(clearJSString(data), 16);
    }

    protected BigInteger jsToBigInteger(String data) {
        return new BigInteger(clearJSString(data), 16);
    }

    protected BigInteger jsToBigIntegerDecimal(String data) {
        return new BigInteger(data, 10);
    }

    /**
     * Create transaction by rpc method parameters.
     * {
     *     "to" : <toAddress>,
     *     "value": <value>,
     *     "fee": <tx fee>,
     *     "privkey": <sender private key>
     * }
     */
    protected Transaction jsToTransaction(JSONObject obj) throws Exception {
        if ((!obj.containsKey("to") || ((String)obj.get("to")).equals(""))   || (!obj.containsKey("value") || ((String)obj.get("value")).equals(""))
                || (!obj.containsKey("privkey") || ((String)obj.get("privkey")).equals(""))) {
            throw new Exception("Invalid params");
        }

        byte[] to = null;
        if (obj.containsKey("to") && !((String)obj.get("to")).equals("")) {
            to = jsToAddress((String) obj.get("to"));
        }

        BigInteger value = BigInteger.ZERO;
        if (obj.containsKey("value") && !((String)obj.get("value")).equals("")) {
            value = jsToBigIntegerDecimal((String) obj.get("value"));
        }

        BigInteger fee = BigInteger.ZERO;
        if (obj.containsKey("fee") && !((String)obj.get("fee")).equals("")) {
            value = jsToBigIntegerDecimal((String) obj.get("fee"));
        }

        byte[] senderPrivkey = null;
        if (obj.containsKey("privkey") && !((String)obj.get("privkey")).equals("")) {
            senderPrivkey = jsToByteArray((String) obj.get("privkey"));
        }

        long timeStamp = System.currentTimeMillis();

        Transaction tx = taucoin.createTransaction(TransactionVersion.V01.getCode(),
                TransactionOptions.TRANSACTION_OPTION_DEFAULT, ByteUtil.longToBytes(timeStamp),
                to, ByteUtil.bigIntegerToBytes(value), ByteUtil.bigIntegerToBytes(fee));

        tx.sign(senderPrivkey);

        return tx;
    }


    protected JSONRPC2Response getRemoteData(JSONRPC2Request req) {
        URL url = JsonRpcServer.getRemoteServer();
        boolean isChanged = !url.toString().equals(currentUrl);
        if (isChanged) {
            currentUrl = url.toString();
        }
        try {
            if (jpSession == null) {
                jpSession = new JSONRPC2Session(url);
            } else {
                if (isChanged) {
                    currentUrl = url.toString();
                    jpSession = new JSONRPC2Session(url);
                }
            }

            return jpSession.send(req);
        } catch (Exception e) {
            System.out.println("Exception getting remote rpc data: " + e.getMessage());
            //taucoin.getListener().trace("Exception getting remote rpc data: " + e.getMessage());
            if (!JsonRpcServer.IsRemoteServerRecuring) {
                return getRemoteData(req);
            } else {
                return null;
            }
        }
    }

    protected byte[] getCoinBase() {
        return ((Account) taucoin.getWallet().getAccountCollection().toArray()[0]).getEcKey().getAddress();
    }

    protected BigInteger getBalance(byte[] account) {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("0x" + Hex.toHexString(account));
        params.add("latest");
        JSONRPC2Request req = new JSONRPC2Request("tau_getBalance", params, 1000);
        JSONRPC2Response res = getRemoteData(req);
        if (res == null || !res.indicatesSuccess()) {
            return BigInteger.ZERO;
        } else {
            return jsToBigInteger(res.getResult().toString());
        }
    }

    protected BigInteger getLatestBlockGasRemaining() {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("latest");
        params.add(false);
        JSONRPC2Request req = new JSONRPC2Request("tau_getBlockByNumber", params, 1000);
        JSONRPC2Response res = getRemoteData(req);
        if (res == null || !res.indicatesSuccess()) {
            return BigInteger.ZERO;
        } else {
            JSONObject block = (JSONObject)res.getResult();
            return jsToBigInteger((String)block.get("gasLimit")).add(jsToBigInteger((String) block.get("gasUsed")).negate());
        }
    }

    protected BigInteger getTransactionCount(byte[] account) {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add("0x" + Hex.toHexString(account));
        params.add("latest");
        JSONRPC2Request req = new JSONRPC2Request("tau_getTransactionCount", params, 1000);
        JSONRPC2Response res = getRemoteData(req);
        if (res == null || !res.indicatesSuccess()) {
            return BigInteger.ZERO;
        } else {
            return jsToBigInteger(res.getResult().toString());
        }
    }

    protected BigInteger getGasPrice() {
        ArrayList<Object> params = new ArrayList<Object>();
        JSONRPC2Request req = new JSONRPC2Request("tau_gasPrice", params, 1000);
        JSONRPC2Response res = getRemoteData(req);
        if (res == null || !res.indicatesSuccess()) {
            return BigInteger.ZERO;
        } else {
            return jsToBigInteger(res.getResult().toString());
        }
    }

    protected BigInteger getEstimateGas(JSONObject obj) {
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(obj);
        JSONRPC2Request req = new JSONRPC2Request("tau_estimateGas", params, 1000);
        JSONRPC2Response res = getRemoteData(req);
        if (res == null || !res.indicatesSuccess()) {
            return BigInteger.valueOf(90000);
        } else {
            return jsToBigInteger(res.getResult().toString());
        }
    }

}

package io.taucoin.rpc.server.full;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

//import io.taucoin.android.db.BlockTransactionVO;
//import io.taucoin.android.db.OrmLiteBlockStoreDatabase;
import io.taucoin.rpc.server.full.JsonRpcServer;
import io.taucoin.core.Account;
import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.Blockchain;
import io.taucoin.core.Transaction;
import io.taucoin.crypto.HashUtil;
import io.taucoin.facade.Taucoin;
import io.taucoin.util.RLP;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static io.taucoin.core.Denomination.SZABO;

public abstract class JsonRpcServerMethod implements RequestHandler {

    private String name = "";
    protected Taucoin taucoin;

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

    protected int jsToInt(String data) {
        return Integer.parseInt(clearJSString(data), 16);
    }

    protected long jsToLong(String data) {
        return Long.parseLong(clearJSString(data), 16);
    }

    protected BigInteger jsToBigInteger(String data) {
        return new BigInteger(clearJSString(data), 16);
    }

    protected Transaction jsToTransaction(JSONObject obj) throws Exception {
        if ((!obj.containsKey("to") || ((String)obj.get("to")).equals("")) && (!obj.containsKey("data") || ((String)obj.get("data")).equals(""))) {
            throw new Exception("");
        }

        byte[] from = getCoinBase();
        if (obj.containsKey("from") && !((String)obj.get("from")).equals("")) {
            from = jsToAddress((String) obj.get("from"));
        }
        Account acc = null;
        for (Account ac : taucoin.getWallet().getAccountCollection()) {
            if (Arrays.equals(ac.getAddress(), from)) {
                acc = ac;
                break;
            }
        }
        if (acc == null) {
            throw new Exception("");
        }

        byte[] senderPrivKey = acc.getEcKey().getPrivKeyBytes();

        // default - from taucoinj-studio
        byte[] to = null;
        if (obj.containsKey("to") && !((String)obj.get("to")).equals("")) {
            to = jsToAddress((String) obj.get("to"));
        }

        // default - from taucoinj-studio
        BigInteger gasPrice = SZABO.value().multiply(BigInteger.TEN);
        if (obj.containsKey("gasPrice") && !((String)obj.get("gasPrice")).equals("")) {
            gasPrice = jsToBigInteger((String) obj.get("gasPrice"));
        }

        // default - from cpp-taucoin
        BigInteger gas = acc.getBalance().divide(gasPrice);
        //BigInteger gasBBRemaining = new BigInteger(Long.toString((taucoin.getBlockchain().getBestBlock().getGasLimit() - taucoin.getBlockchain().getBestBlock().getGasUsed()) / 5));
        BigInteger gasBBRemaining = BigInteger.ZERO;
        if (gasBBRemaining.compareTo(gas) < 0)
            gas = gasBBRemaining;
        if (obj.containsKey("gas") && !((String)obj.get("gas")).equals("")) {
            gas = jsToBigInteger((String) obj.get("gas"));
        }

        // default - from taucoinj-studio
        BigInteger value = new BigInteger("1000");
        if (obj.containsKey("value") && !((String)obj.get("value")).equals("")) {
            value = jsToBigInteger((String) obj.get("value"));
        }

        // default - from taucoinj-studio
        BigInteger nonce = BigInteger.ONE;//taucoin.getRepository().getNonce(acc.getAddress());
        if (obj.containsKey("nonce") && !((String)obj.get("nonce")).equals("")) {
            nonce = jsToBigInteger((String) obj.get("nonce"));
        }

        // default - from taucoinj-studio
        byte[] data = new byte[]{};
        if (obj.containsKey("data") && !((String)obj.get("data")).equals("")) {
            data = jsToAddress((String) obj.get("data"));
        }

        Transaction tx = taucoin.createTransaction((byte)0x01, (byte)0x00, null/*gas*/, null/*to*/, null/*value*/, null/*data*/);;

        tx.sign(senderPrivKey);

        return tx;
    }

    protected JSONObject blockToJS (Block block, Boolean detailed) {
        JSONObject res = new JSONObject();
        if (block == null)
            return null;

        res.put("number", "0x" + Long.toHexString(block.getNumber()));

        res.put("hash", "0x" + Hex.toHexString(block.getHash()));

        res.put("parentHash", "0x" + Hex.toHexString(block.getPreviousHeaderHash()));

        //res.put("nonce", "0x" + Hex.toHexString(block.getNonce()));

       // res.put("sha3Uncles", "0x" + Hex.toHexString(block.getUnclesHash()));

        //res.put("logsBloom", "0x" + Hex.toHexString(block.getLogBloom()));

        //res.put("transactionsRoot", "0x" + Hex.toHexString(block.getHeader().getTxTrieRoot()));

        //res.put("stateRoot", "0x" + Hex.toHexString(block.getStateRoot()));

        //res.put("miner", "0x" + Hex.toHexString(block.getCoinbase()));

        //res.put("difficulty", "0x" + block.getDifficultyBI().toString(16));

        res.put("totalDifficulty", "0x" + block.getCumulativeDifficulty().toString(16));

        //res.put("extraData", "0x" + Hex.toHexString(block.getExtraData()));

        // No way to get size of block in bytes, so I try calculate it using formula from  getEncoded
        byte[] header = block.getHeader().getEncoded();
        byte[] transactions = RLP.encodeList();
        //byte[][] unclesEncoded = new byte[block.getUncleList().size()][];
        /*
        int i = 0;
        for (BlockHeader uncle : block.getUncleList()) {
            unclesEncoded[i] = uncle.getEncoded();
            ++i;
        }
        byte[] uncles = RLP.encodeList(unclesEncoded);
        byte[] rlpEncoded = RLP.encodeList(header, transactions, uncles);
        res.put("size", "0x" + Integer.toHexString(rlpEncoded.length));

        res.put("gasLimit", "0x" /*+ Long.toHexString(block.getGasLimit())*//*);

        res.put("gasUsed", "0x" + Long.toHexString(block.getGasUsed())); */
        

        //res.put("timestamp", "0x" + Long.toHexString(block.getTimestamp()));

        JSONArray transactionsJA = new JSONArray();
        int i = 0;
        for (Transaction transaction : block.getTransactionsList()) {
            if (detailed) {
                JSONObject tx = transactionToJS(block, transaction);
                tx.put("transactionIndex", "0x" + Integer.toHexString(i));
                tx.put("blockHash", "0x" + Hex.toHexString(block.getHash()));
                tx.put("blockNumber", "0x" + Long.toHexString(block.getNumber()));
                transactionsJA.add(tx);
            } else {
                transactionsJA.add("0x" + Hex.toHexString(transaction.getHash()));
            }
            ++i;
        }
        res.put("transactions", transactionsJA);

        /*
        JSONArray unclesJA = new JSONArray();
        for (BlockHeader uncle : block.getUncleList()) {
            unclesJA.add("0x" + Hex.toHexString(HashUtil.sha3(uncle.getEncoded())));
        }
        res.put("uncles", unclesJA);
        */

        return res;
    }

    protected JSONObject transactionToJS (Block block, Transaction transaction) {
        JSONObject res = new JSONObject();

        res.put("hash", "0x" + Hex.toHexString(transaction.getHash()));

        //res.put("nonce", "0x" + Hex.toHexString(transaction.getNonce()));

        res.put("from", "0x" + Hex.toHexString(transaction.getSender()));

        res.put("to", "0x" + Hex.toHexString(transaction.getReceiveAddress()));

        //res.put("value", "0x" + Hex.toHexString(transaction.getValue()));

        //res.put("gasPrice", "0x" + Hex.toHexString(transaction.getGasPrice()));

        //res.put("gas", "0x" + Hex.toHexString(transaction.getGasLimit()));

        //res.put("input", "0x" + Hex.toHexString(transaction.getData()));

        /*
        if (block == null) {
            OrmLiteBlockStoreDatabase db = OrmLiteBlockStoreDatabase.getHelper(null);
            BlockTransactionVO relation = db.getTransactionLocation(transaction.getHash());
            if (relation == null) {
                res.put("transactionIndex", null);
                res.put("blockHash", null);
                res.put("blockNumber", null);
            } else {
                block = taucoin.getBlockchain().getBlockByHash(relation.getBlockHash());
            }
        }
        if (block != null) {
            long txi = 0;
            for (Transaction tx : block.getTransactionsList()) {
                if (Arrays.equals(tx.getHash(), transaction.getHash()))
                    break;
                txi++;
            }
            res.put("transactionIndex", "0x" + Long.toHexString(txi));
            res.put("blockHash", "0x" + Hex.toHexString(block.getHash()));
            res.put("blockNumber", "0x" + Long.toHexString(block.getNumber()));
        }
        */

        return res;
    }

    protected byte[] getCoinBase() {
        return ((Account) taucoin.getWallet().getAccountCollection().toArray()[0]).getEcKey().getAddress();
    }
}

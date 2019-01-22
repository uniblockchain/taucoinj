package io.taucoin.core.genesis;

import com.google.common.io.ByteStreams;
import io.taucoin.core.Transaction;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.Genesis;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.jsontestsuite.Utils;
import io.taucoin.trie.SecureTrie;
import io.taucoin.trie.Trie;
import io.taucoin.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;

import static java.math.BigInteger.ZERO;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.core.Genesis.ZERO_HASH_2048;
import static io.taucoin.crypto.HashUtil.EMPTY_LIST_HASH;
import static io.taucoin.util.ByteUtil.wrap;

public class GenesisLoader {

    public static Block loadGenesis() {
        return loadGenesis(CONFIG);
    }

    public static Genesis loadGenesis(SystemProperties config)  {
        String genesisFile = config.genesisInfo();

        InputStream is = GenesisLoader.class.getResourceAsStream("/genesis/" + genesisFile);
        return loadGenesis(is);
    }

    public static Genesis loadGenesis(InputStream genesisJsonIS)  {
        try {

            String json = new String(ByteStreams.toByteArray(genesisJsonIS));

            ObjectMapper mapper = new ObjectMapper();
            JavaType type = mapper.getTypeFactory().constructType(GenesisJson.class);

            GenesisJson genesisJson  = new ObjectMapper().readValue(json, type);

            Genesis genesis = createBlockForJson(genesisJson);

            Map<ByteArrayWrapper, AccountState> premine = generatePreMine(genesisJson.getAlloc());
            genesis.setPremine(premine);

            byte[] rootHash = generateRootHash(premine);
            //genesis.setStateRoot(rootHash);


            return genesis;
        } catch (Throwable e) {
            System.err.println("Genesis block configuration is corrupted or not found ./resources/genesis/...");
            e.printStackTrace();
            System.exit(-1);
        }

        System.err.println("Genesis block configuration is corrupted or not found ./resources/genesis/...");
        System.exit(-1);
        return null;
    }


    private static Genesis createBlockForJson(GenesisJson genesisJson){

        byte version       = Utils.parseByte(genesisJson.version);
        byte[] baseTarget  = Utils.parseData(genesisJson.geneBasetarget);
        byte[] preheaderHash     = Utils.parseData(genesisJson.previousHeaderHash);
        byte[] coinbase    = Utils.parseData(genesisJson.coinbase);

        byte[] timestampBytes = Utils.parseData(genesisJson.timestamp);
        long   timestamp         = ByteUtil.byteArrayToLong(timestampBytes);

        byte[] genePubkey  = Utils.parseData(genesisJson.generatorPublicKey);
        byte[] geneSig   = Utils.parseData(genesisJson.blockSignature);

        byte option    = Utils.parseByte(genesisJson.option);
        //here is temporary method...
        List<Transaction> tr = new ArrayList<Transaction>();
        tr.add(new Transaction(coinbase));
        return new Genesis(version, timestampBytes, preheaderHash, genePubkey,
                            geneSig, option, tr);
    }


    private static Map<ByteArrayWrapper, AccountState> generatePreMine(Map<String, AllocatedAccount> alloc){

        Map<ByteArrayWrapper, AccountState> premine = new HashMap<>();
        for (String key : alloc.keySet()){

            BigInteger balance = new BigInteger(alloc.get(key).getBalance());
            AccountState acctState = new AccountState(ZERO, balance);

            premine.put(wrap(Hex.decode(key)), acctState);
        }

        return premine;
    }

    private static byte[] generateRootHash(Map<ByteArrayWrapper, AccountState> premine){

        Trie state = new SecureTrie(null);

        for (ByteArrayWrapper key : premine.keySet()) {
            state.update(key.getData(), premine.get(key).getEncoded());
        }

        return state.getRootHash();
    }

}

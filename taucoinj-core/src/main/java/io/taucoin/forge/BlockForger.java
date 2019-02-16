package io.taucoin.forge;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.taucoin.facade.TaucoinImpl;
import io.taucoin.util.ByteUtil;
import org.apache.commons.collections4.CollectionUtils;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.db.IndexedBlockStore;
import io.taucoin.facade.Taucoin;
import io.taucoin.listener.CompositeEthereumListener;
import io.taucoin.listener.EthereumListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.BigIntegers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.Math.max;


/**
 * Created by Anton Nashatyrev on 10.12.2015.
 * Modified by Taucoin Core Developers on 01.29.2019.
 */
@Component
public class BlockForger {
    private static final Logger logger = LoggerFactory.getLogger("forge");

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    private  Repository repository;

    @Autowired
    private Blockchain blockchain;

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private Taucoin taucoin;

    @Autowired
    private CompositeEthereumListener listener;

    @Autowired
    private SystemProperties config;

    @Autowired
    protected PendingState pendingState;

    private List<ForgerListener> listeners = new CopyOnWriteArrayList<>();

    private byte[] minerPubkey;
    private byte[] minerCoinbase;

    private Block miningBlock;

    private volatile boolean stopForge = false;

    private volatile boolean isForging = false;

    private static final int TNO = 50;

    @PostConstruct
    private void init() {
        minerPubkey = config.getForgerPubkey();
        minerCoinbase = config.getForgerCoinbase();
        listener.addListener(new EthereumListenerAdapter() {

            @Override
            public void onBlock(Block block) {
                BlockForger.this.onNewBlock(block);
            }

            @Override
            public void onSyncDone() {
                if (config.forgerStart() && config.isSyncEnabled()) {
                    logger.info("Sync complete, start forging...");
                    startForging((long)config.getForgedAmount());
                }
            }
        });

        if (config.forgerStart() && !config.isSyncEnabled()) {
            logger.info("Start forging now...");
            startForging((long)config.getForgedAmount());
        }
    }

    public void startForging() {
        startForging(-1);
    }

    public void startForging(long amount) {
        if (isForging()) {
            return;
        }

        executor.submit(new ForgeTask(this, amount));
        fireForgerStarted();
        this.isForging = true;
        this.stopForge = false;
    }

    public void stopForging() {
        this.isForging = false;
        this.stopForge = true;
        executor.shutdownNow();
        fireForgerStopped();
    }

    public void onForgingStopped() {
        this.isForging = false;
        this.stopForge = true;
        fireForgerStopped();
    }

    public boolean isForging() {
        return this.isForging;
    }


    protected List<Transaction> getAllPendingTransactions() {
        List<Transaction> txList = new ArrayList<Transaction>();
        txList.addAll(pendingState.getPendingTransactions());
        txList.addAll(pendingState.getWireTransactions());

        if (txList.size() <= TNO) {
            return txList;
        } else {
            // Order, Transaction Fee, Time
            Collections.sort(txList);
            return txList.subList(0, TNO);
        }
    }

    private void onNewBlock(Block newBlock) {
        logger.info("On new block {}", newBlock.getNumber());

        // TODO: wakeup forging sleep thread or interupt forging process.
    }

    public void restartMining() {

        Block bestBlock;
        BigInteger baseTarget;
        byte[] generationSignature;
        BigInteger cumulativeDifficulty;

        while (true) {
            bestBlock = blockchain.getBestBlock();

            baseTarget = ProofOfTransaction.calculateRequiredBaseTarget(bestBlock, blockStore);
            logger.info("forging... baseTarget {}", baseTarget);

            BigInteger forgingPower = repository.getforgePower(minerCoinbase);
            logger.info("forging... forging power {}", forgingPower);
            if (forgingPower.longValue() < 0) {
                logger.error("Forging Power < 0!!!");
                return;
            }

            logger.info("forging... base target {}, forging power {}", baseTarget, forgingPower);

            generationSignature = ProofOfTransaction.
                    calculateNextBlockGenerationSignature(bestBlock.getGenerationSignature(), minerPubkey);
            logger.info("forging... generationSignature {}", generationSignature);

            BigInteger hit = ProofOfTransaction.calculateRandomHit(generationSignature);
            logger.info("forging... hit {}", hit.longValue());

            long timeInterval = ProofOfTransaction.calculateForgingTimeInterval(hit, baseTarget, forgingPower);
            logger.info("forging... timeInterval {}", timeInterval);
            BigInteger targetValue = ProofOfTransaction.calculateMinerTargetValue(baseTarget, forgingPower, timeInterval);
            logger.info("forging... hit {}, target value {}", hit.longValue(), targetValue);
            long timeNow = System.currentTimeMillis() / 1000;
            long timePreBlock = new BigInteger(bestBlock.getTimestamp()).longValue();
            logger.info("forging... forging time {}", timePreBlock + timeInterval);

            if (timeNow < timePreBlock + timeInterval) {
                long sleepTime = timePreBlock + timeInterval - timeNow;
                logger.debug("Sleeping " + sleepTime + " s before importing...");
                synchronized (blockchain.getLockObject()) {
                    try {
                        blockchain.getLockObject().wait(sleepTime * 1000);
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }

            if (stopForge) {
                logger.info("~~~~~~~~~~~~~~~~~~Stop forging!!!~~~~~~~~~~~~~~~~~~");
                return;
            }

            cumulativeDifficulty = ProofOfTransaction.
                    calculateCumulativeDifficulty(bestBlock.getCumulativeDifficulty(), baseTarget);

            if (bestBlock.equals(blockchain.getBestBlock())) {
                logger.info("~~~~~~~~~~~~~~~~~~Forging a new block...~~~~~~~~~~~~~~~~~~");
                break;
            } else {
                logger.info("~~~~~~~~~~~~~~~~~~Got a new best block, continue forging...~~~~~~~~~~~~~~~~~~");
            }

        }

        miningBlock = blockchain.createNewBlock(bestBlock, baseTarget,
                generationSignature, cumulativeDifficulty, getAllPendingTransactions());

        try {
            // wow, block mined!
            blockForged(miningBlock);
        } catch (InterruptedException | CancellationException e) {
            // OK, we've been cancelled, just exit
        } catch (Exception e) {
            logger.warn("Exception during mining: ", e);
        }

        fireBlockStarted(miningBlock);
    }

    protected void blockForged(Block newBlock) throws InterruptedException {

        fireBlockForged(newBlock);
        logger.info("Wow, block mined !!!: {}", newBlock.toString());

        miningBlock = null;

        // broadcast the block
        logger.debug("Importing newly mined block " + newBlock.getShortHash() + " ...");
        ImportResult importResult =  taucoin.addNewMinedBlock(newBlock);
        logger.debug("Mined block import result is " + importResult + " : " + newBlock.getShortHash());
    }

    /*****  Listener boilerplate  ******/

    public void addListener(ForgerListener l) {
        listeners.add(l);
    }

    public void removeListener(ForgerListener l) {
        listeners.remove(l);
    }

    protected void fireForgerStarted() {
        for (ForgerListener l : listeners) {
            l.forgingStarted();
        }
    }

    protected void fireForgerStopped() {
        for (ForgerListener l : listeners) {
            l.forgingStopped();
        }
    }

    protected void fireBlockStarted(Block b) {
        for (ForgerListener l : listeners) {
            l.blockForgingStarted(b);
        }
    }

    protected void fireBlockCancelled(Block b) {
        for (ForgerListener l : listeners) {
            l.blockForgingCanceled(b);
        }
    }

    protected void fireBlockForged(Block b) {
        for (ForgerListener l : listeners) {
            l.blockForged(b);
        }
    }

    // Forge task implementation.
    private static class ForgeTask implements Runnable, ForgerListener {

        BlockForger forger;

        private long forgeTargetAmount = -1;
        private long forgedBlockAmount = 0;

        public ForgeTask(BlockForger forger) {
            this.forger = forger;
            registerForgeListener();
        }

        public ForgeTask(BlockForger forger, long forgeTargetAmount) {
           this.forger = forger;
           this.forgeTargetAmount = forgeTargetAmount;
           registerForgeListener();
        }

        private void registerForgeListener() {
            forger.addListener(this);
        }

        @Override
        public void run() {
            while (forgeTargetAmount == -1
                    || (forgeTargetAmount > 0 && forgedBlockAmount < forgeTargetAmount)) {
               forger.restartMining();
            }

            forger.onForgingStopped();
        }

        @Override
        public void forgingStarted() {
            logger.info("Forging started...");
        }

        @Override
        public void forgingStopped() {
            logger.info("Forging stopped...");
        }

        @Override
        public void blockForgingStarted(Block block) {
            logger.info("Block forging started...");
        }

        @Override
        public void blockForged(Block block) {
            forgedBlockAmount++;
            logger.info("New Block: {}", Hex.toHexString(block.getHash()));
        }

        @Override
        public void blockForgingCanceled(Block block) {
            logger.info("Block froging canceled: {}", Hex.toHexString(block.getHash()));
        }
     }
}

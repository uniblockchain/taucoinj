package io.taucoin.forge;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.taucoin.facade.TaucoinImpl;
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
 */
@Component
public class BlockMiner {
    private static final Logger logger = LoggerFactory.getLogger("mine");

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

    private List<MinerListener> listeners = new CopyOnWriteArrayList<>();

    private long minBlockTimeout;
    private int cpuThreads;
    private boolean fullMining = true;

    private boolean isMining;

    private byte[] minerPubkey;
    private byte[] minerCoinbase;

    private Block miningBlock;
    private ListenableFuture<Long> ethashTask;
    private long lastBlockMinedTime;

    private boolean stopForge;

    private volatile boolean isForging = false;

    @PostConstruct
    private void init() {
        minBlockTimeout = config.getMineMinBlockTimeoutMsec();
        cpuThreads = config.getMineCpuThreads();
        fullMining = config.isMineFullDataset();
        minerPubkey = config.getMinerPubkey();
        minerCoinbase = config.getMinerCoinbase();
        listener.addListener(new EthereumListenerAdapter() {

            public void onPendingStateChanged(PendingState pendingState) {
                BlockMiner.this.onPendingStateChanged();
            }

            @Override
            public void onSyncDone() {
                if (config.minerStart() && config.isSyncEnabled()) {
                    logger.info("Sync complete, start mining...");
                    startMining();
                }
            }
        });

        if (config.minerStart() && !config.isSyncEnabled()) {
            logger.info("Start mining now...");
            startMining();
        }
    }

    public void setFullMining(boolean fullMining) {
        this.fullMining = fullMining;
    }

    public void setCpuThreads(int cpuThreads) {
        this.cpuThreads = cpuThreads;
    }

    public void setStopForge(boolean stopForge) {
        this.stopForge = stopForge;
    }

    public boolean isStopForge() {
        return stopForge;
    }

    public void startMining() {
        isMining = true;
        fireMinerStarted();
        logger.info("Miner started");
        restartMining();
    }

    public void stopMining() {
        isMining = false;
        this.isForging = false;
        cancelCurrentBlock();
        fireMinerStopped();
        logger.info("Miner stopped");
    }

    public void startForging() {
        startForging(-1);
    }

    public void startForging(long amount) {
        if (isForging()) {
            return;
        }

        executor.submit(new ForgeTask(this, amount));
        fireMinerStarted();
        this.isForging = true;
    }

    public void stopForging() {
        this.isForging = false;
        executor.shutdownNow();
        fireMinerStopped();
    }

    public boolean isForging() {
        return this.isForging;
    }


    protected List<Transaction> getAllPendingTransactions() {
        List<Transaction> txList = new ArrayList<Transaction>();
        txList.addAll(pendingState.getPendingTransactions());
        txList.addAll(pendingState.getWireTransactions());

        if (txList.size() > 50) {
            return txList.subList(0, 50);
        }
        else {
            return txList;
        }

//        PendingStateImpl.TransactionSortedSet ret = new PendingStateImpl.TransactionSortedSet();
//        ret.addAll(pendingState.getPendingTransactions());
//        ret.addAll(pendingState.getWireTransactions());
//        Iterator<Transaction> it = ret.iterator();
//        while(it.hasNext()) {
//            Transaction tx = it.next();
//            if (!isAcceptableTx(tx)) {
//                logger.debug("Miner excluded the transaction: {}", tx);
//                it.remove();
//            }
//        }
//        return new ArrayList<>(ret);
    }

    private void onPendingStateChanged() {
        if (!isMining) return;

        logger.debug("onPendingStateChanged()");
        if (miningBlock == null) {
            restartMining();
        } else if (miningBlock.getNumber() <= ((PendingStateImpl) pendingState).getBestBlock().getNumber()) {
            logger.debug("Restart mining: new best block: " + blockchain.getBestBlock().getShortDescr());
            restartMining();
        } else if (!CollectionUtils.isEqualCollection(miningBlock.getTransactionsList(), getAllPendingTransactions())) {
            logger.debug("Restart mining: pending transactions changed");
            restartMining();
        } else {
            if (logger.isDebugEnabled()) {
                String s = "onPendingStateChanged() event, but pending Txs the same as in currently mining block: ";
                for (Transaction tx : getAllPendingTransactions()) {
                    s += "\n    " + tx;
                }
                logger.debug(s);
            }
        }
    }

    protected boolean isAcceptableTx(Transaction tx) {
        return true;
    }

    protected synchronized void cancelCurrentBlock() {
        if (ethashTask != null && !ethashTask.isCancelled()) {
            ethashTask.cancel(true);
            fireBlockCancelled(miningBlock);
            logger.debug("Tainted block mining cancelled: {}", miningBlock.getShortHash());
            ethashTask = null;
            miningBlock = null;
        }
    }

    public void restartMining() {
        Block /*bestBlockchain*/ bestPendingState = blockchain.getBestBlock();
//        Block bestPendingState = ((PendingStateImpl) pendingState).getBestBlock();

        BigInteger baseTarget = ProofOfTransaction.calculateRequiredBaseTarget(bestPendingState, blockStore);
        logger.info("baseTarget:", baseTarget);
        BigInteger forgingPower = repository.getforgePower(minerCoinbase);
        logger.info("forgingPower:", forgingPower);
        System.out.println("wwwwwwwww" + forgingPower);
        forgingPower = new BigInteger("1");
        if (forgingPower.longValue() < 0) {
            logger.error("Forging Power < 0!!!");
            return;
        }

        byte[] generationSignature = ProofOfTransaction.
                calculateNextBlockGenerationSignature(bestPendingState.getGenerationSignature().toByteArray(), minerPubkey);
        logger.info("generationSignature:", generationSignature);

        BigInteger hit = ProofOfTransaction.calculateRandomHit(generationSignature);
        logger.info("hit:", hit);

        long timeInterval = ProofOfTransaction.calculateForgingTimeInterval(hit, baseTarget, forgingPower);
        logger.info("timeInterval:", timeInterval);
        long timeNow = System.currentTimeMillis() / 1000;
        long timePreBlock = new BigInteger(bestPendingState.getTimestamp()).longValue();
        if (timeNow < timePreBlock + timeInterval) {
            long sleepTime = timePreBlock + timeInterval - timeNow;
            logger.debug("Sleeping " + sleepTime + " s before importing...");
            try {
                Thread.sleep(sleepTime * 1000);
            } catch (InterruptedException e) {
                //
            }
        }

        BigInteger cumulativeDifficulty = ProofOfTransaction.
                calculateCumulativeDifficulty(bestPendingState.getCumulativeDifficulty(), baseTarget);

        if (!stopForge) {
            miningBlock = blockchain.createNewBlock(bestPendingState, baseTarget,
                    BigIntegers.fromUnsignedByteArray(generationSignature),
                    cumulativeDifficulty, getAllPendingTransactions());
            System.out.println(miningBlock.toString());
            try {
                // wow, block mined!
                blockMined(miningBlock);
            } catch (InterruptedException | CancellationException e) {
                // OK, we've been cancelled, just exit
            } catch (Exception e) {
                logger.warn("Exception during mining: ", e);
            }
        }

//        Block newMiningBlock = blockchain.createNewBlock(bestPendingState, getAllPendingTransactions());
//
//        synchronized(this) {
//            cancelCurrentBlock();
//           // miningBlock = newMiningBlock;
//            ethashTask = fullMining ?
//                    Ethash.getForBlock(miningBlock.getNumber()).mine(miningBlock, cpuThreads) :
//                    Ethash.getForBlock(miningBlock.getNumber()).mineLight(miningBlock, cpuThreads);
//            ethashTask.addListener(new Runnable() {
//                //            private final Future<Long> task = ethashTask;
//                @Override
//                public void run() {
//                    try {
//                        ethashTask.get();
//                        // wow, block mined!
//                        blockMined(miningBlock);
//                    } catch (InterruptedException | CancellationException e) {
//                        // OK, we've been cancelled, just exit
//                    } catch (Exception e) {
//                        logger.warn("Exception during mining: ", e);
//                    }
//                }
//            }, MoreExecutors.sameThreadExecutor());
//        }
        fireBlockStarted(miningBlock);
//        logger.debug("New block mining started: {}", miningBlock.getShortHash());
    }

    protected void blockMined(Block newBlock) throws InterruptedException {

//        long t = System.currentTimeMillis();
//        if (t - lastBlockMinedTime < minBlockTimeout) {
//            long sleepTime = minBlockTimeout - (t - lastBlockMinedTime);
//            logger.debug("Last block was mined " + (t - lastBlockMinedTime) + " ms ago. Sleeping " +
//                    sleepTime + " ms before importing...");
//            Thread.sleep(sleepTime);
//        }

        fireBlockMined(newBlock);
        logger.info("Wow, block mined !!!: {}", newBlock.toString());

//        lastBlockMinedTime = t;
        ethashTask = null;
        miningBlock = null;

        // broadcast the block
        logger.debug("Importing newly mined block " + newBlock.getShortHash() + " ...");
        ImportResult importResult = ((TaucoinImpl) taucoin).addNewMinedBlock(newBlock);
        logger.debug("Mined block import result is " + importResult + " : " + newBlock.getShortHash());
    }

    /*****  Listener boilerplate  ******/

    public void addListener(MinerListener l) {
        listeners.add(l);
    }

    public void removeListener(MinerListener l) {
        listeners.remove(l);
    }

    protected void fireMinerStarted() {
        for (MinerListener l : listeners) {
            l.miningStarted();
        }
    }
    protected void fireMinerStopped() {
        for (MinerListener l : listeners) {
            l.miningStopped();
        }
    }
    protected void fireBlockStarted(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningStarted(b);
        }
    }
    protected void fireBlockCancelled(Block b) {
        for (MinerListener l : listeners) {
            l.blockMiningCanceled(b);
        }
    }
    protected void fireBlockMined(Block b) {
        for (MinerListener l : listeners) {
            l.blockMined(b);
        }
    }

        // Forge task implementation.
    private static class ForgeTask implements Runnable, MinerListener {

        BlockMiner forger;

        private long forgeTargetAmount = -1;
        private long forgedBlockAmount = 0;

        public ForgeTask(BlockMiner forger) {
            this.forger = forger;
            registerForgeListener();
        }

        public ForgeTask(BlockMiner forger, long forgeTargetAmount) {
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

            forger.stopMining();
        }

        @Override
        public void miningStarted() {
            logger.info("Forging started...");
        }

        @Override
        public void miningStopped() {
            logger.info("Forging stopped...");
        }

        @Override
        public void blockMiningStarted(Block block) {
            logger.info("Block forging started...");
        }

        @Override
        public void blockMined(Block block) {
            forgedBlockAmount++;
            logger.info("New Block: {}", Hex.toHexString(block.getHash()));
        }

        @Override
        public void blockMiningCanceled(Block block) {
            logger.info("Block froging canceled: {}", Hex.toHexString(block.getHash()));
        }
     }
}

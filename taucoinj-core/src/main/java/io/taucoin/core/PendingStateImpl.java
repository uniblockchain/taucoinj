package io.taucoin.core;

//import io.taucoin.listener.TaucoinListener;
import io.taucoin.util.FastByteComparisons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
/*
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
*/
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeSet;

import static java.math.BigInteger.ZERO;
import org.apache.commons.collections4.map.LRUMap;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.util.BIUtil.toBI;
import io.taucoin.db.ByteArrayWrapper;

/**
 * Keeps logic providing pending state management
 *
 * @author Mikhail Kalinin
 * @since 28.09.2015
 */
//@Singleton
@Component
public class PendingStateImpl implements PendingState {

    /*
    public static class TransactionSortedSet extends TreeSet<Transaction> {

        public TransactionSortedSet() {

            super(public int compareFee(Transaction tx1, Transaction tx2){
                      return FastByteComparisons.compareTo(tx1.getFee(), 0, 2, tx2.getFee(), 0, 2);
	            }
			);
        }
    }
    */
    private static final Logger logger = LoggerFactory.getLogger("state");

    // Private EthereumListener listener;
    private Repository repository;
    private Blockchain blockchain;

    //@Resource
    private final List<Transaction> wireTransactions = new ArrayList<>();

    // To filter out the transactions we have already processed
    // transactions could be sent by peers even if they were already included into blocks
    private final Map<ByteArrayWrapper, Object> redceivedTxs = new LRUMap<>(500000);

    //@Resource
    private final List<Transaction> pendingStateTransactions = new ArrayList<>();

    private Repository pendingState;

    private Block best = null;

    public PendingStateImpl() {
    }

    //constructor
    //public PendingStateImpl(EthereumListener listener, Repository repository) {
    @Autowired
    public PendingStateImpl(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void init() {
        this.pendingState = repository.startTracking();
    }

    public Repository getRepository() {
        if (this.pendingState == null) {
            init();
        }
        return pendingState;
    }

    // Return Transaction Received From Network
    public List<Transaction> getWireTransactions() {

        List<Transaction> txs = new ArrayList<>();

        for (Transaction tx : wireTransactions) {
            txs.add(tx);
        }

        return txs;
    }

    public Block getBestBlock() {
        if (best == null) {
            best = blockchain.getBestBlock();
        }
        return best;
    }

    private boolean addNewTxIfNotExist(Transaction tx) {
        ByteArrayWrapper hash = new ByteArrayWrapper(tx.getHash());
        synchronized (redceivedTxs) {
            if (!redceivedTxs.containsKey(hash)) {
                redceivedTxs.put(hash, null);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void addWireTransactions(Set<Transaction> transactions) {

        final List<Transaction> newTxs = new ArrayList<>();
        int unknownTx = 0;

        if (transactions.isEmpty()) return;

        for (Transaction tx : transactions) {

            if (addNewTxIfNotExist(tx)) {
                unknownTx++;
                if (isValid(tx)) {
                    newTxs.add(tx);
                } else {
                    logger.info("Non valid TX: " + tx);
                }
            }
        }

        // tight synchronization here since a lot of duplicate transactions can arrive from many peers
        // and isValid(tx) call is very expensive
        synchronized (this) {
            wireTransactions.addAll(newTxs);
        }

        /*
        if (!newTxs.isEmpty()) {
            EventDispatchThread.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.onPendingTransactionsReceived(newTxs);
                    listener.onPendingStateChanged(PendingStateImpl.this);
                }
            });
        }
*/

        logger.info("Wire transaction list added: {} new, {} valid of received {}, #of known txs: {}", unknownTx, newTxs.size(), transactions.size(), redceivedTxs.size());
    }

    /*
     * Validation Of Transaction.
     * Nonce in ETH, Time In TAU.
     * 1. Transaction data structure
     * 2. Check in time
     * 3. Signature
     * 4. Balance
    */
    private boolean isValid(Transaction tx) {

        if(!tx.verify()) {
            if (logger.isWarnEnabled())
                logger.warn("Invalid transaction in structure");
			return false;
        }

        if(!tx.checkTime()) {
            if (logger.isWarnEnabled())
                logger.warn("Invalid transaction in time");
			return false;
        }
        
        TransactionExecutor executor = new TransactionExecutor(tx, pendingState);
        executor.init();

        return true;
    }

    @Override
    public void addPendingTransaction(Transaction tx) {
        pendingStateTransactions.add(tx);
        executeTx(tx);
    }

    @Override
    public List<Transaction> getPendingTransactions() {
        return pendingStateTransactions;
    }

    @Override
    public void processBest(Block block) {

        clearWire(block.getTransactionsList());

        clearOutdated();

        clearPendingState(block.getTransactionsList());

        updateState();
    }

    //Block Number -> Time
    private void clearOutdated() {

        List<Transaction> outdated = new ArrayList<>();

        //clear wired transactions
        synchronized (wireTransactions) {
            for (Transaction tx : wireTransactions)
                if (!tx.checkTime())
                    outdated.add(tx);
        }
		if(!outdated.isEmpty())
            wireTransactions.removeAll(outdated);
        
        outdated.clear();

        //clear pending transactions
        synchronized (pendingStateTransactions) {
            for (Transaction tx : pendingStateTransactions)
                if (!tx.checkTime())
                    outdated.add(tx);
        }
		if(!outdated.isEmpty())
            pendingStateTransactions.removeAll(outdated);

    }

    private void clearWire(List<Transaction> txs) {
        for (Transaction tx : txs) {
            if (logger.isInfoEnabled() && wireTransactions.contains(tx))
                logger.info("Clear wire transaction, hash: [{}]", Hex.toHexString(tx.getHash()));

            wireTransactions.remove(tx);
        }
    }

    private void clearPendingState(List<Transaction> txs) {
        if (logger.isInfoEnabled()) {
            for (Transaction tx : txs)
                if (pendingStateTransactions.contains(tx))
                    logger.info("Clear pending state transaction, hash: [{}]", Hex.toHexString(tx.getHash()));
        }

        pendingStateTransactions.removeAll(txs);
    }

    private void updateState() {

        pendingState = repository.startTracking();

        synchronized (pendingStateTransactions) {
            for (Transaction tx : pendingStateTransactions) executeTx(tx);
        }
    }

    /*
     * Transaction execution, which can be seen in transactionExecutor.java
     * 1. validation of sender's balance
     * 2. update of receiver and miner's balance
    */
    private void executeTx(Transaction tx) {

        logger.info("Apply pending state tx: {}", Hex.toHexString(tx.getHash()));

        TransactionExecutor executor = new TransactionExecutor(tx, getRepository());

        executor.init();

        executor.execute();

    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

}

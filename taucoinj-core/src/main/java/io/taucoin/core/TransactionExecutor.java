package io.taucoin.core;

import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.taucoin.util.BIUtil.*;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger("execute");

    private Transaction tx;
    private Repository track;
    private byte[] coinbase;
    private Blockchain blockchain;
    private static final int MaxHistoryCount = 144;

    long basicTxAmount = 0;
    long basicTxFee = 0;

    //constructor
    public TransactionExecutor(Transaction tx, Repository track,Blockchain blockchain) {
        this.tx= tx;
        this.track= track;
        this.blockchain = blockchain;
    }

    /**
     * Do all the basic validation
     */
    public boolean init() {

        logger.debug("Init entry {}", Hex.toHexString(tx.getHash()));
		// Check In Transaction Amount
        basicTxAmount = toBI(tx.getAmount()).longValue();
        if (basicTxAmount < 0 ) {
            if (logger.isWarnEnabled())
                logger.warn("Transaction amount [{}] is invalid!", basicTxAmount);
            return false;
        }

        // Check In Transaction Fee
        basicTxFee = toBI(tx.transactionCost()).longValue();
        if (basicTxFee < 1 ) {
            if (logger.isWarnEnabled())
                logger.warn("Transaction fee [{}] is invalid!", basicTxFee);
            return false;
        }
        /**
         * node need to check whether this transaction has been recorded in block.
         * a honest node need to avoid transactions duplicated in block chain.
         */
        AccountState accountState = track.getAccountState(tx.getSender());
        if(accountState == null){
            if(logger.isErrorEnabled())
                logger.error("in valid account ,address is: {}", ByteUtil.toHexString(tx.getSender()));
            return false;
        }
        long tranTime = ByteUtil.byteArrayToLong(tx.getTime());
        logger.debug("Init sender {} tx time {}", Hex.toHexString(tx.getSender()), tranTime);
        Set<Long> txHistory = accountState.getTranHistory().keySet();
        for (Long h : txHistory) {
            logger.debug("Init sender {} history time {}", Hex.toHexString(tx.getSender()), h);
        }
        if(!txHistory.isEmpty()) {
            long txTimeCeil = Collections.max(txHistory);
            long txTimeFloor = Collections.min(txHistory);
            /**
             * System should be concurrency high rather than 1 transaction per second.
             */
            if (tranTime <= txTimeCeil) {
                if (accountState.getTranHistory().containsKey(tranTime)) {
                    logger.error("duplicate transaction ,tx is: {}", ByteUtil.toHexString(tx.getHash()));
                    return false;
                }

                if (tranTime < txTimeFloor && blockchain.getSize() > MaxHistoryCount) {
                    logger.error("attacking transaction ,tx is: {}",ByteUtil.toHexString(tx.getHash()));
                    return false;
                }
            }
        }
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        BigInteger senderBalance = track.getBalance(tx.getSender());

        if (!isCovers(senderBalance, totalCost)) {
            if (logger.isWarnEnabled())
                logger.warn("No enough balance: Require: {}, Sender's balance: {}", totalCost, senderBalance);
            return false;
        }

        logger.debug("Init exit {}", Hex.toHexString(tx.getHash()));
        return true;
    }

    /**
     * Do the executation
     * 1. add balance to received address 
     * 2. add transaction fee to actually miner 
     */
    public void executeFinal() {

        logger.debug("execute entry {}", Hex.toHexString(tx.getHash()));
		// Sender subtract balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        logger.info("in executation sender is "+Hex.toHexString(tx.getSender()));
        track.addBalance(tx.getSender(), totalCost.negate());

		// Receiver add balance
        track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()));

        FeeDistributor feeDistributor = new FeeDistributor(ByteUtil.byteArrayToLong(tx.transactionCost()));

        if (feeDistributor.distributeFee()) {
            // Transfer fees to forger
            track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()));
            // Transfer fees to receiver
            //track.addBalance(tx.getReceiveAddress(), toBI(feeDistributor.getReceiveFee()));
            if (track.getAccountState(tx.getSender()).getWitnessAddress() != null) {
                // Transfer fees to last witness
                track.addBalance(track.getAccountState(tx.getSender()).getWitnessAddress(),
                        toBI(feeDistributor.getLastWitFee()));
            }

            if (track.getAccountState(tx.getSender()).getAssociatedAddress() != null) {
                // Transfer fees to last associate
                track.addBalance(track.getAccountState(tx.getSender()).getAssociatedAddress(),
                        toBI(feeDistributor.getLastAssociFee()));
            }

            /**
             * 2 special situation is dealt by distribute associated fee to current forger
             */
            if (track.getAccountState(tx.getSender()).getWitnessAddress() == null) {
                // Transfer fees to last witness
                track.addBalance(coinbase,
                        toBI(feeDistributor.getLastWitFee()));
            }

            if (track.getAccountState(tx.getSender()).getAssociatedAddress() == null) {
                // Transfer fees to last associate
                track.addBalance(coinbase,
                        toBI(feeDistributor.getLastAssociFee()));
            }
        }

        // Increase forge power.
        track.increaseforgePower(tx.getSender());

        logger.info("Pay fees to miner: [{}], feesEarned: [{}]", Hex.toHexString(coinbase), basicTxFee);

        AccountState accountState = track.getAccountState(tx.getSender());
        if(blockchain.getSize() > MaxHistoryCount){

            long txTime = Collections.min(accountState.getTranHistory().keySet());
            // if earliest transaction is beyond expire time
            // it will be removed.
            long freshTime = blockchain.getSize() - MaxHistoryCount;
            if (txTime < ByteUtil.byteArrayToLong(blockchain.getBlockByNumber(freshTime).getTimestamp())) {
                accountState.getTranHistory().remove(txTime);
                logger.debug("{} remove tx time {}", Hex.toHexString(tx.getSender()), txTime);
            } else {
                long txTimeTemp = ByteUtil.byteArrayToLong(tx.getTime());
                accountState.getTranHistory().put(txTimeTemp, tx.getHash());
                logger.debug("{} add tx time {}", Hex.toHexString(tx.getSender()), txTime);
            }

        }else{
            long txTime = ByteUtil.byteArrayToLong(tx.getTime());
            accountState.getTranHistory().put(txTime,tx.getHash());
            logger.debug("{} add tx time {}", Hex.toHexString(tx.getSender()), txTime);
        }

        StakeHolderIdentityUpdate stakeHolderIdentityUpdate =
                new StakeHolderIdentityUpdate(tx, track, coinbase);
        stakeHolderIdentityUpdate.updateStakeHolderIdentity();

        logger.debug("execute exit {}", Hex.toHexString(tx.getHash()));
    }

    public void undoTransaction() {
        // add sender balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        logger.info("Tx sender is "+Hex.toHexString(tx.getSender()));
        track.addBalance(tx.getSender(), totalCost);

        // Subtract receiver balance
        track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()).negate());

        // Transfer fees to miner
        track.addBalance(coinbase, toBI(tx.transactionCost()).negate());

        // undo account transaction history
        AccountState accountState = track.getAccountState(tx.getSender());
        if ( accountState.getTranHistory().keySet().contains( ByteUtil.byteArrayToLong(tx.getTime()) ) ) {
            accountState.getTranHistory().remove( ByteUtil.byteArrayToLong(tx.getTime()) );
        }

        // Increase forge power.
        track.reduceForgePower(tx.getSender());
    }

	/**
	 * Set Miner Address
	 */
	public void setCoinbase(byte [] address){
        this.coinbase= address;
    }
}

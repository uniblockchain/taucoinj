package io.taucoin.core;

import io.taucoin.db.BlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static java.lang.Math.abs;
import static java.lang.Math.log;

public class ProofOfTransaction {
    private static final Logger logger = LoggerFactory.getLogger("proofoftransaction");

    private final static int MAXRATIO = 335;//67;//335;
    private final static int MINRATIO = 265;//53;//265;
    private final static int AVERTIME = 300;//60;//300; //5 min
    private final static BigInteger DiffAdjustNumerator = new BigInteger("010000000000000000",16);
    private final static BigInteger DiffAdjustNumeratorHalf = new BigInteger("0100000000",16);
    private final static BigInteger DiffAdjustNumeratorCoe = new BigInteger("800000000000000",16); //2^59



    /**
     * get required base target
     * @param previousBlock
     * @param blockStore
     * @return
     */
    public static BigInteger calculateRequiredBaseTarget(Block previousBlock, BlockStore blockStore) {
        long blockNumber = previousBlock.getNumber();
        if(blockNumber <= 3) {
            return (new BigInteger("369D0369D036978",16));
        }

        Block ancestor = blockStore.getChainBlockByNumber(blockNumber - 3);
        BigInteger previousBlockBaseTarget = previousBlock.getBaseTarget();
        long pastTimeFromLatestBlock = new BigInteger(previousBlock.getTimestamp()).longValue() -
                new BigInteger(ancestor.getTimestamp()).longValue();

        if (pastTimeFromLatestBlock < 0)
            pastTimeFromLatestBlock = 0;
        long pastTimeAver = pastTimeFromLatestBlock / 3;

        BigInteger newRequiredBaseTarget;
        if( pastTimeAver > AVERTIME ) {
            long min = 0;

            if (pastTimeAver < MAXRATIO){
                min = pastTimeAver;
            }else {
                min = MAXRATIO;
            }

            newRequiredBaseTarget = previousBlockBaseTarget.multiply(BigInteger.valueOf(min).divide(BigInteger.valueOf(AVERTIME)));
        }else{
            long max = 0;

            if (pastTimeAver > MINRATIO){
                max = pastTimeAver;
            }else{
                max = MINRATIO;
            }

            newRequiredBaseTarget = previousBlockBaseTarget.
                    subtract(previousBlockBaseTarget.divide(BigInteger.valueOf(1875)).//375//1875)).
                            multiply(BigInteger.valueOf(AVERTIME-max)).multiply(BigInteger.valueOf(4)));
        }
        return newRequiredBaseTarget;
    }


    /**
     * get next block generation signature
     *     Gn+1 = hash(Gn, pubkey)
     * @param preGenerationSignature
     * @param pubkey
     * @return
     */
    public static byte[] calculateNextBlockGenerationSignature(byte[] preGenerationSignature, byte[] pubkey){
        byte[] data = new byte[preGenerationSignature.length + pubkey.length];
        System.arraycopy(preGenerationSignature, 0, data, 0, preGenerationSignature.length);
        System.arraycopy(pubkey, 0, data, preGenerationSignature.length, pubkey.length);
        byte[] nextGenerationSignature = Sha256Hash.hash(data);
        return nextGenerationSignature;
    }


    /**
     * get miner target value
     * target = base target * mining power * time
     * @param baseTarget
     * @param forgingPower
     * @param time
     * @return
     */
    public static BigInteger calculateMinerTargetValue(BigInteger baseTarget, BigInteger forgingPower, long time){
        BigInteger targetValue = baseTarget.multiply(forgingPower).
                multiply(BigInteger.valueOf(time));
        return targetValue;
    }


    /**
     * calculate hit
     * @param generationSignature
     * @return
     */
    public static BigInteger calculateRandomHit(byte[] generationSignature){
        byte[] headBytes = new byte[8];
        /*
        for (int i = 0; i < 8; i++) {
            headBytes[i] = temp[i];
        }*/
        System.arraycopy(generationSignature,0,headBytes,0,8);
        BigInteger bhit = new BigInteger(1, headBytes);
        logger.info("bhit:{}", bhit);
        BigInteger bhitUzero = bhit.add(BigInteger.ONE);
        logger.info("bhitUzero:{}", bhitUzero);
        double logarithm = abs(log(bhitUzero.doubleValue()) - 2 * log(DiffAdjustNumeratorHalf.doubleValue()));
        logarithm = logarithm * 1000;
        logger.info("logarithm:{}", logarithm);
        long ulogarithm = (new Double(logarithm)).longValue();
        logger.info("ulogarithm:{}", ulogarithm);
        BigInteger adjustHit = DiffAdjustNumeratorCoe.multiply(BigInteger.valueOf(ulogarithm)).divide(BigInteger.valueOf(1000));
        logger.info("adjustHit:{}", adjustHit);
        return adjustHit;
    }

    /**
     * calculate cumulative difficulty
     * @param lastCumulativeDifficulty
     * @param baseTarget
     * @return
     */
    public static BigInteger calculateCumulativeDifficulty(BigInteger lastCumulativeDifficulty, BigInteger baseTarget){
        BigInteger delta = DiffAdjustNumerator.divide(baseTarget);
        BigInteger cumulativeDifficulty = lastCumulativeDifficulty.add(delta);
        return cumulativeDifficulty;
    }

    /**
     * calculate forging time interval
     * @param hit
     * @param baseTarget
     * @param forgingPower
     * @return
     */
    public static long calculateForgingTimeInterval(BigInteger hit, BigInteger baseTarget, BigInteger forgingPower) {
        long timeInterval =
                hit.divide(baseTarget).divide(forgingPower).add(BigInteger.ONE).longValue();
        return timeInterval;
    }

}

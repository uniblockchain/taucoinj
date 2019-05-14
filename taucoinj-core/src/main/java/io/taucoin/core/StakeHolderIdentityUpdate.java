package io.taucoin.core;

import io.taucoin.crypto.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class StakeHolderIdentityUpdate {
    private static final Logger logger = LoggerFactory.getLogger("stakeholder");
    private Repository track;
    private Transaction tx;
    private byte[] forgeAddress;
    private long blockNumber;

    public StakeHolderIdentityUpdate(Transaction tx, Repository track,byte[] forgeAddress,long blockNumber){
       this.tx = tx;
       this.track = track;
       this.forgeAddress = forgeAddress;
       this.blockNumber = blockNumber;
    }

    public AssociatedAccount updateStakeHolderIdentity(){
        byte[] senderAddress = tx.getSender();
        byte[] receiverAddress = tx.getReceiveAddress();

        AccountState senderAccount = track.getAccountState(senderAddress);
        AccountState receiverAccount = track.getAccountState(receiverAddress);

        senderAccount.updateAssociatedAddress(receiverAddress, blockNumber);
        senderAccount.setWitnessAddress(this.forgeAddress);
        receiverAccount.updateAssociatedAddress(senderAddress, blockNumber);
        receiverAccount.setWitnessAddress(this.forgeAddress);
        return new AssociatedAccount(senderAddress,senderAccount,receiverAddress,receiverAccount);
    }

    public void rollbackStakeHolderIdentity(){
        tx.setIsCompositeTx(true);
        byte[] senderAddress = tx.getSender();
        byte[] receiverAddress = tx.getReceiveAddress();

        AccountState senderAccount = track.getAccountState(senderAddress);
        AccountState receiverAccount = track.getAccountState(receiverAddress);

        senderAccount.updateAssociatedAddress(tx.getSenderAssociatedAddress(), blockNumber);
        senderAccount.setWitnessAddress(tx.getSenderWitnessAddress());
        receiverAccount.updateAssociatedAddress(tx.getReceiverAssociatedAddress(), blockNumber);
        receiverAccount.setWitnessAddress(tx.getReceiverWitnessAddress());
    }
}

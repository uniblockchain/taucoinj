package io.taucoin.net.eth.message;

import io.taucoin.net.message.Message;

public abstract class EthMessage extends Message {

    public EthMessage() {
    }

    public EthMessage(byte[] encoded) {
        super(encoded);
    }

    abstract public EthMessageCodes getCommand();
}

package io.taucoin.net.shh;

import io.taucoin.net.message.Message;

public abstract class ShhMessage extends Message {

    public ShhMessage() {
    }

    public ShhMessage(byte[] encoded) {
        super(encoded);
    }

    public ShhMessageCodes getCommand() {
        return ShhMessageCodes.fromByte(code);
    }
}

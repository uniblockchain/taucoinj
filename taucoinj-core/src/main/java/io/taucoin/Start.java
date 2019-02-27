package io.taucoin;

import io.taucoin.cli.CLIInterface;
import io.taucoin.facade.Taucoin;
import io.taucoin.facade.TaucoinFactory;
import io.taucoin.net.rlpx.Node;
import io.taucoin.rpc.server.JsonRpcServerFactory;
import io.taucoin.rpc.server.JsonRpcServer;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 *  * @author Roman Mandeleil
 *   * @since 14.11.2014
 *    */
public class Start {
    private static final Logger logger = LoggerFactory.getLogger("main");
    private static Taucoin taucoin;
    private static class TauSignalHandler implements SignalHandler {
    
        @Override
        public void handle(Signal signal) {
            logger.info("Signal handler called for signal {}", signal);
            try {
                taucoin.close();
                System.exit(0);
                logger.info("Handling {}", signal.getName());
            } catch (Exception e) {
                logger.error("handle|Signal handler" + "failed, reason "
                + e.getMessage());
                e.printStackTrace();
            }
        }   
    }

    public static void main(String args[]) {

        SignalHandler handler = new TauSignalHandler();
        // kill (-15)
        Signal termSignal = new Signal("TERM");
        Signal.handle(termSignal, handler);
        // kill (-2) and ctrl+c
        Signal intSignal = new Signal("INT");
        Signal.handle(intSignal, handler);

        try{

            CLIInterface.call(args);

            if (!CONFIG.blocksLoader().equals("")) {
                CONFIG.setSyncEnabled(false);
                CONFIG.setDiscoveryEnabled(false);
            }

            taucoin = TaucoinFactory.createTaucoin();

            if (!CONFIG.blocksLoader().equals(""))
                taucoin.getBlockLoader().loadBlocks();

            // Start rpc server
            if (CONFIG.isRpcEnabled()) {
                JsonRpcServer rpcServer = JsonRpcServerFactory.createJsonRpcServer(taucoin);
                try {
                    rpcServer.start(CONFIG.rpcListenPort());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.error("Interrupted: " + e.getMessage());
        }

    }
}

package io.taucoin;

import org.ethereum.cli.CLIInterface;
import io.taucoin.facade.Taucoin;
import io.taucoin.facade.TaucoinFactory;
import io.taucoin.net.rlpx.Node;
import org.ethereum.rpc.server.JsonRpcServerFactory;
import org.ethereum.rpc.server.JsonRpcServer;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.net.URISyntaxException;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * @author Roman Mandeleil
 * @since 14.11.2014
 */
public class Start {

    public static void main(String args[]) throws IOException, URISyntaxException {
        CLIInterface.call(args);

        if (!CONFIG.blocksLoader().equals("")) {
            CONFIG.setSyncEnabled(false);
            CONFIG.setDiscoveryEnabled(false);
        }

        Taucoin taucoin = TaucoinFactory.createTaucoin();

        if (!CONFIG.blocksLoader().equals(""))
            taucoin.getBlockLoader().loadBlocks();

        // Start rpc server
//        if (CONFIG.isRpcEnabled()) {
//            JsonRpcServer rpcServer = JsonRpcServerFactory.createJsonRpcServer(taucoin);
//            try {
//                rpcServer.start(CONFIG.rpcListenPort());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }

}

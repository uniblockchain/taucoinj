package io.taucoin.rpc.server.full;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.taucoin.rpc.server.full.filter.FilterManager;
import io.taucoin.facade.Taucoin;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.method.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import io.taucoin.rpc.server.*;


public final class JsonRpcServer extends io.taucoin.rpc.server.JsonRpcServer{

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    private Taucoin taucoin;
    private Dispatcher dispatcher;
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    private int port;

    public JsonRpcServer(Taucoin taucoin) {
        super(taucoin);
        this.taucoin = taucoin;

        this.dispatcher = new Dispatcher();

        // Custom methods to receive Address Transaction History
        this.dispatcher.register(new tau_getTransactions(this.taucoin));

        //this.dispatcher.register(new web3_clientVersion(this.taucoin));
        //this.dispatcher.register(new web3_sha3(this.taucoin));

        this.dispatcher.register(new net_version(this.taucoin));
        this.dispatcher.register(new net_listening(this.taucoin));
        this.dispatcher.register(new net_peerCount(this.taucoin));

        this.dispatcher.register(new tau_protocolVersion(this.taucoin));
        //this.dispatcher.register(new tau_coinbase(this.taucoin));
        this.dispatcher.register(new tau_mining(this.taucoin));
        //this.dispatcher.register(new tau_hashrate(this.taucoin));
        //this.dispatcher.register(new tau_gasPrice(this.taucoin));
        this.dispatcher.register(new tau_accounts(this.taucoin));
        this.dispatcher.register(new tau_newaccount(this.taucoin));
        this.dispatcher.register(new tau_importprikey(this.taucoin));
        this.dispatcher.register(new tau_blockNumber(this.taucoin));
        this.dispatcher.register(new tau_getBalance(this.taucoin));
        //this.dispatcher.register(new tau_getStorageAt(this.taucoin));
        this.dispatcher.register(new tau_getTransactionCount(this.taucoin));
        this.dispatcher.register(new tau_getBlockTransactionCountByHash(this.taucoin));
        this.dispatcher.register(new tau_getBlockTransactionCountByNumber(this.taucoin));
        //this.dispatcher.register(new tau_getUncleCountByBlockHash(this.taucoin));
        //this.dispatcher.register(new tau_getUncleCountByBlockNumber(this.taucoin));
        //this.dispatcher.register(new tau_getCode(this.taucoin));
        this.dispatcher.register(new tau_sign(this.taucoin));
        this.dispatcher.register(new tau_sendTransaction(this.taucoin));
        //this.dispatcher.register(new tau_call(this.taucoin));
        //this.dispatcher.register(new tau_estimateGas(this.taucoin));
        this.dispatcher.register(new tau_getBlockByHash(this.taucoin));
        this.dispatcher.register(new tau_getBlockByNumber(this.taucoin));
        this.dispatcher.register(new tau_getTransactionByHash(this.taucoin));
        //this.dispatcher.register(new tau_getTransactionByBlockHashAndIndex(this.taucoin));
        //this.dispatcher.register(new tau_getTransactionByBlockNumberAndIndex(this.taucoin));
        //this.dispatcher.register(new tau_getUncleByBlockHashAndIndex(this.taucoin));
        //this.dispatcher.register(new tau_getUncleByBlockNumberAndIndex(this.taucoin));
        //this.dispatcher.register(new tau_getTransactionReceipt(this.taucoin));
        //this.dispatcher.register(new tau_getCompilers(this.taucoin));
        //this.dispatcher.register(new tau_compileSolidity(this.taucoin));
        //this.dispatcher.register(new tau_compileLLL(this.taucoin));
        //this.dispatcher.register(new tau_compileSerpent(this.taucoin));
        //this.dispatcher.register(new tau_newFilter(this.taucoin));
        //this.dispatcher.register(new tau_newBlockFilter(this.taucoin));
        //this.dispatcher.register(new tau_newPendingTransactionFilter(this.taucoin));
        //this.dispatcher.register(new tau_uninstallFilter(this.taucoin));
        //this.dispatcher.register(new tau_getFilterChanges(this.taucoin));
        //this.dispatcher.register(new tau_getFilterLogs(this.taucoin));
        //this.dispatcher.register(new tau_getLogs(this.taucoin));
        this.dispatcher.register(new tau_getWork(this.taucoin));
        this.dispatcher.register(new tau_submitWork(this.taucoin));

        this.dispatcher.register(new db_putString(this.taucoin));
        this.dispatcher.register(new db_getString(this.taucoin));
        this.dispatcher.register(new db_putHex(this.taucoin));
        this.dispatcher.register(new db_getHex(this.taucoin));

        //this.dispatcher.register(new shh_version(this.taucoin));
        //this.dispatcher.register(new shh_post(this.taucoin));
        //this.dispatcher.register(new shh_newIdentity(this.taucoin));
        //this.dispatcher.register(new shh_hasIdentity(this.taucoin));
        //this.dispatcher.register(new shh_newGroup(this.taucoin));
        //this.dispatcher.register(new shh_addToGroup(this.taucoin));
        //this.dispatcher.register(new shh_newFilter(this.taucoin));
        //this.dispatcher.register(new shh_uninstallFilter(this.taucoin));
        //this.dispatcher.register(new shh_getFilterChanges(this.taucoin));
        //this.dispatcher.register(new shh_getMessages(this.taucoin));

        taucoin.addListener(FilterManager.getInstance());
        //io.taucoin.rpc.server.full.whisper.FilterManager.getInstance();
    }

    public void start(int port) throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        this.port = port;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            //b.localAddress(InetAddress.getLocalHost(), port);
            b.localAddress(port);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new JsonRpcServerInitializer());

            Channel ch = b.bind().sync().channel();

            logger.info("Full json rpc server is starting, listen port: {}", this.port);

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    class JsonRpcServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new HttpServerCodec());
            p.addLast(new JsonRpcServerHandler(dispatcher));
        }
    }
}

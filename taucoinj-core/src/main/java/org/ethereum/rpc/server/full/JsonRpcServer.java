package org.ethereum.rpc.server.full;

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
import org.ethereum.rpc.server.full.filter.FilterManager;
import org.ethereum.facade.Ethereum;
import com.thetransactioncompany.jsonrpc2.server.*;
import org.ethereum.rpc.server.full.method.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import org.ethereum.rpc.server.*;


public final class JsonRpcServer extends org.ethereum.rpc.server.JsonRpcServer{

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    private Ethereum ethereum;
    private Dispatcher dispatcher;
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    private int port;

    public JsonRpcServer(Ethereum ethereum) {
        super(ethereum);
        this.ethereum = ethereum;

        this.dispatcher = new Dispatcher();

        // Custom methods to receive Address Transaction History
        this.dispatcher.register(new eth_getTransactionHistory(this.ethereum));

        this.dispatcher.register(new web3_clientVersion(this.ethereum));
        this.dispatcher.register(new web3_sha3(this.ethereum));

        this.dispatcher.register(new net_version(this.ethereum));
        this.dispatcher.register(new net_listening(this.ethereum));
        this.dispatcher.register(new net_peerCount(this.ethereum));

        this.dispatcher.register(new eth_protocolVersion(this.ethereum));
        this.dispatcher.register(new eth_coinbase(this.ethereum));
        this.dispatcher.register(new eth_mining(this.ethereum));
        this.dispatcher.register(new eth_hashrate(this.ethereum));
        this.dispatcher.register(new eth_gasPrice(this.ethereum));
        this.dispatcher.register(new eth_accounts(this.ethereum));
        this.dispatcher.register(new eth_blockNumber(this.ethereum));
        this.dispatcher.register(new eth_getBalance(this.ethereum));
        this.dispatcher.register(new eth_getStorageAt(this.ethereum));
        this.dispatcher.register(new eth_getTransactionCount(this.ethereum));
        this.dispatcher.register(new eth_getBlockTransactionCountByHash(this.ethereum));
        this.dispatcher.register(new eth_getBlockTransactionCountByNumber(this.ethereum));
        this.dispatcher.register(new eth_getUncleCountByBlockHash(this.ethereum));
        this.dispatcher.register(new eth_getUncleCountByBlockNumber(this.ethereum));
        this.dispatcher.register(new eth_getCode(this.ethereum));
        this.dispatcher.register(new eth_sign(this.ethereum));
        this.dispatcher.register(new eth_sendTransaction(this.ethereum));
        this.dispatcher.register(new eth_call(this.ethereum));
        this.dispatcher.register(new eth_estimateGas(this.ethereum));
        this.dispatcher.register(new eth_getBlockByHash(this.ethereum));
        this.dispatcher.register(new eth_getBlockByNumber(this.ethereum));
        this.dispatcher.register(new eth_getTransactionByHash(this.ethereum));
        this.dispatcher.register(new eth_getTransactionByBlockHashAndIndex(this.ethereum));
        this.dispatcher.register(new eth_getTransactionByBlockNumberAndIndex(this.ethereum));
        this.dispatcher.register(new eth_getUncleByBlockHashAndIndex(this.ethereum));
        this.dispatcher.register(new eth_getUncleByBlockNumberAndIndex(this.ethereum));
        this.dispatcher.register(new eth_getTransactionReceipt(this.ethereum));
        this.dispatcher.register(new eth_getCompilers(this.ethereum));
        this.dispatcher.register(new eth_compileSolidity(this.ethereum));
        this.dispatcher.register(new eth_compileLLL(this.ethereum));
        this.dispatcher.register(new eth_compileSerpent(this.ethereum));
        this.dispatcher.register(new eth_newFilter(this.ethereum));
        this.dispatcher.register(new eth_newBlockFilter(this.ethereum));
        this.dispatcher.register(new eth_newPendingTransactionFilter(this.ethereum));
        this.dispatcher.register(new eth_uninstallFilter(this.ethereum));
        this.dispatcher.register(new eth_getFilterChanges(this.ethereum));
        this.dispatcher.register(new eth_getFilterLogs(this.ethereum));
        this.dispatcher.register(new eth_getLogs(this.ethereum));
        this.dispatcher.register(new eth_getWork(this.ethereum));
        this.dispatcher.register(new eth_submitWork(this.ethereum));

        this.dispatcher.register(new db_putString(this.ethereum));
        this.dispatcher.register(new db_getString(this.ethereum));
        this.dispatcher.register(new db_putHex(this.ethereum));
        this.dispatcher.register(new db_getHex(this.ethereum));

        this.dispatcher.register(new shh_version(this.ethereum));
        this.dispatcher.register(new shh_post(this.ethereum));
        this.dispatcher.register(new shh_newIdentity(this.ethereum));
        this.dispatcher.register(new shh_hasIdentity(this.ethereum));
        this.dispatcher.register(new shh_newGroup(this.ethereum));
        this.dispatcher.register(new shh_addToGroup(this.ethereum));
        this.dispatcher.register(new shh_newFilter(this.ethereum));
        this.dispatcher.register(new shh_uninstallFilter(this.ethereum));
        this.dispatcher.register(new shh_getFilterChanges(this.ethereum));
        this.dispatcher.register(new shh_getMessages(this.ethereum));

        ethereum.addListener(FilterManager.getInstance());
        org.ethereum.rpc.server.full.whisper.FilterManager.getInstance();
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
package io.taucoin.rpc.server.light;

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
import io.taucoin.facade.Taucoin;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.light.method.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;

import io.taucoin.rpc.server.*;


public final class JsonRpcServer extends io.taucoin.rpc.server.JsonRpcServer{

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    static private ArrayList<URL> RemoteServer = new ArrayList<>();
    static private int currentRemoteServer = 0;
    static public boolean IsRemoteServerRecuring = false;

    private Taucoin taucoin;
    private Dispatcher dispatcher;
    private int port;

    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;

    public JsonRpcServer(Taucoin taucoin) {
        super(taucoin);
        this.taucoin = taucoin;

        this.dispatcher = new Dispatcher();

        // Custom methods to receive Address Transaction History
        this.dispatcher.register(new eth_getTransactionHistory(this.taucoin));

        this.dispatcher.register(new eth_coinbase(this.taucoin));
        this.dispatcher.register(new eth_accounts(this.taucoin));
        this.dispatcher.register(new eth_sign(this.taucoin));
        this.dispatcher.register(new eth_sendTransaction(this.taucoin));

        this.dispatcher.register(new proxy(this.taucoin));

        //addRemoteServer("http://rpc0.syng.io:8545/", true);
    }

    public static void addRemoteServer(String serverUrl) {
        try {
            RemoteServer.add(new URL(serverUrl));
        } catch (Exception e) {
            System.out.println("Exception adding remote server: " + e.getMessage());

        }
    }

    public void addRemoteServer(String serverUrl, boolean clearList) {
        try {
            if (clearList) {
                RemoteServer.clear();
            }
            RemoteServer.add(new URL(serverUrl));
            System.out.println("Changed rpc remote server to: " + serverUrl);
            //this.taucoin.getListener().trace("Slaving to <" + serverUrl + ">");
        } catch (Exception e) {
            System.out.println("Exception adding remote server: " + e.getMessage());
            //this.taucoin.getListener().trace("Exception adding remote server: " + e.getMessage());
        }
    }

    public static URL getRemoteServer() {
        if (currentRemoteServer >= RemoteServer.size()){
            currentRemoteServer = 0;
            IsRemoteServerRecuring = true;
        }
        URL res = RemoteServer.get(currentRemoteServer);
        currentRemoteServer++;
        return res;
    }

    public void start(int port) throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        this.port = port;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
//            b.localAddress(InetAddress.getLocalHost(), port);
            b.localAddress(port);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new JsonRpcServerInitializer());

            Channel ch = b.bind().sync().channel();

            logger.info("Light json rpc server is starting, listen port: {}", this.port);

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
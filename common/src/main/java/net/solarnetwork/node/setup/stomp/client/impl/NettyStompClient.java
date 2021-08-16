/* ==================================================================
 * NettyStompClient.java - 9/08/2021 9:50:05 AM
 * 
 * Copyright 2021 SolarNetwork Foundation
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.setup.stomp.client.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;
import io.netty.handler.codec.stomp.StompSubframeEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.solarnetwork.node.setup.stomp.client.domain.StompMessage;
import net.solarnetwork.node.setup.stomp.client.service.StompSetupClient;

/**
 * Netty implementation of the client.
 * 
 * @author matt
 * @version 1.0
 */
public class NettyStompClient implements StompSetupClient {

  /** The UTF-8 character set. */
  public static final Charset UTF8 = Charset.forName("UTF-8");

  private static final Logger log = LoggerFactory.getLogger(NettyStompClient.class);

  private final String host;
  private final int port;
  private final Set<Consumer<StompMessage<String>>> consumers = new CopyOnWriteArraySet<>();

  private EventLoopGroup workerGroup;
  private Channel channel;

  /**
   * Constructor.
   */
  public NettyStompClient(String host, int port) {
    super();
    this.host = host;
    this.port = port;
  }

  /**
   * Shut the client down.
   */
  public synchronized void shutdown() {
    if (workerGroup != null && !workerGroup.isShuttingDown()) {
      workerGroup.shutdownGracefully();
      workerGroup = null;
    }
    if (channel != null) {
      channel = null;
    }
  }

  @Override
  public Future<?> connect() {
    shutdown();
    final ThreadFactory tf = new DefaultThreadFactory("STOMP-Setup-Client:" + port, true);
    workerGroup = new NioEventLoopGroup(tf);
    try {
      Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioSocketChannel.class);
      b.option(ChannelOption.SO_KEEPALIVE, true);
      b.handler(new StompChannelInitializer());

      ChannelFuture future = b.connect(host, port);
      future.addListener((ChannelFutureListener) f -> {
        if (f.isSuccess()) {
          NettyStompClient.this.channel = f.channel();
        } else {
          Throwable t = f.cause();
          log.error("Error connecting to STOMP setup server {}:{}: {}", this, host, port,
              (t != null ? t.toString() : "?"));
        }
      });
      return future;
    } catch (RuntimeException e) {
      shutdown();
      log.error("Error connecting to STOMP setup server {}:{}: {}", this, host, port, e.toString());
      String msg = "Error connecting to STOMP setup server " + host + ":" + port + ": "
          + e.getMessage();
      throw new RuntimeException(msg, e);
    }
  }

  private class StompChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
      if (true) { // FIXME add toggle
        ch.pipeline().addLast(
            new LoggingHandler("net.solarnetwork.node.setup.stomp.client.WIRE", LogLevel.TRACE));
      }
      // @formatter:off
      ch.pipeline().addLast(
          new StompSubframeDecoder(),
          new StompSubframeAggregator(4096),
          new StompSubframeEncoder(),
          new StompSetupClientHandler(consumers));
      // @formatter:on
    }
  }

  @Override
  public boolean isConnected() {
    Channel ch = this.channel;
    return (ch != null && ch.isActive());
  }

  @Override
  public void disconnect() {
    Channel ch = this.channel;
    if (ch == null || !ch.isOpen()) {
      return;
    }
    DefaultStompFrame msg = new DefaultStompFrame(StompCommand.DISCONNECT);
    try {
      ChannelFuture f = sendAndFlush(msg);
      try {
        f.await(10_000);
      } catch (InterruptedException e) {
        // ignore
      }
      ch.close();
    } finally {
      this.channel = null;
      shutdown();
    }
  }

  @Override
  public void addMessageConsumer(Consumer<StompMessage<String>> consumer) {
    consumers.add(consumer);
  }

  @Override
  public void removeMessageConsumer(Consumer<StompMessage<String>> consumer) {
    consumers.remove(consumer);
  }

  @Override
  public Future<?> post(StompMessage<?> message) {
    Channel ch = this.channel;
    if (ch == null || !ch.isOpen()) {
      CompletableFuture<Void> f = new CompletableFuture<>();
      f.completeExceptionally(new RuntimeException("Not connected."));
      return f;
    }
    DefaultStompFrame msg = new DefaultStompFrame(StompCommand.valueOf(message.getCommand()));
    if (message.getHeaders() != null) {
      for (java.util.Map.Entry<String, List<String>> e : message.getHeaders().entrySet()) {
        List<String> values = e.getValue();
        if (values != null) {
          for (String v : values) {
            msg.headers().add(e.getKey(), encodeHeaderValue(v));
          }
        }
      }
    }
    if (message.getBody() != null) {
      msg.content().writeBytes(message.getContent());
    }
    return sendAndFlush(msg);
  }

  private static String encodeHeaderValue(String v) {
    // @formatter:off
    return v.replaceAll("\\\\", "\\\\\\\\")
        .replaceAll("\r", "\\\\r")
        .replaceAll("\n", "\\\\n")
        .replaceAll(":", "\\\\c");
    // @formatter:on
  }

  private ChannelFuture sendAndFlush(Object message) {
    if (this.channel == null) {
      return null;
    }
    if (this.channel.isActive()) {
      return this.channel.writeAndFlush(message);
    }
    return this.channel.newFailedFuture(new IOException("Channel is closed!"));
  }

}

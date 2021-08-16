/* ==================================================================
 * StompSetupClientHandler.java - 9/08/2021 11:02:21 AM
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

import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.util.ReferenceCountUtil;
import net.solarnetwork.node.setup.stomp.client.domain.StompMessage;

/**
 * Client handler for STOMP setup.
 * 
 * @author matt
 * @version 1.0
 */
public class StompSetupClientHandler extends ChannelInboundHandlerAdapter {

  private static final Logger log = LoggerFactory.getLogger(StompSetupClientHandler.class);

  private final Set<Consumer<StompMessage<String>>> consumers;

  /**
   * Constructor.
   * 
   * @param consumers
   *          the consumers set
   * @throws IllegalArgumentException
   *           if any argument is {@literal null}
   */
  public StompSetupClientHandler(Set<Consumer<StompMessage<String>>> consumers) {
    super();
    if (consumers == null) {
      throw new IllegalArgumentException("The consumers argument must not be null.");
    }
    this.consumers = consumers;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    try {
      StompFrame frame = (msg instanceof StompFrame ? (StompFrame) msg : null);
      if (frame == null) {
        return;
      }
      log.debug("Got stomp message: {}", frame);
      NettyStompMessage<String> message = NettyStompMessage.stringMessage(frame);
      for (Consumer<StompMessage<String>> c : consumers) {
        c.accept(message);
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

}

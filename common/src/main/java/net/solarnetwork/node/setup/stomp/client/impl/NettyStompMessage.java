/* ==================================================================
 * NettyStompMessage.java - 16/08/2021 3:31:33 PM
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

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map.Entry;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;
import net.solarnetwork.node.setup.stomp.client.domain.StompMessage;

/**
 * Netty implementation of {@link StompMessage}.
 * 
 * @param <T>
 *          message type
 * @author matt
 * @version 1.0
 */
public abstract class NettyStompMessage<T> implements StompMessage<T> {

  protected final StompFrame frame;

  /**
   * Constructor.
   * 
   * @param frame
   *          the frame
   * @throws IllegalArgumentException
   *           if any argument is {@literal null}
   */
  protected NettyStompMessage(StompFrame frame) {
    super();
    if (frame == null) {
      throw new IllegalArgumentException("The frame argument must not be null.");
    }
    this.frame = frame;
  }

  @Override
  public String getCommand() {
    return frame.command().toString();
  }

  @Override
  public MultiValueMap<String, String> getHeaders() {
    StompHeaders h = frame.headers();
    if (h == null || h.isEmpty()) {
      return null;
    }
    LinkedMultiValueMap<String, String> m = new LinkedMultiValueMap<>(h.size());
    for (Iterator<Entry<String, String>> itr = h.iteratorAsString(); itr.hasNext();) {
      Entry<String, String> e = itr.next();
      m.add(e.getKey(), e.getValue());
    }
    return m;
  }

  @Override
  public byte[] getContent() {
    ByteBuf b = frame.content();
    if (b == null) {
      return null;
    }
    if (!b.isReadable()) {
      return null;
    }
    byte[] data = new byte[b.readableBytes()];
    b.getBytes(0, data);
    return data;
  }

  @Override
  public String toString() {
    return frame.toString();
  }

  /**
   * Get a UTF-8 encoded string message.
   * 
   * @param frame
   *          the STOMP frame
   * @return the message instance
   */
  public static NettyStompMessage<String> stringMessage(StompFrame frame) {
    return new NettyStringStompMessage(frame, NettyStompClient.UTF8);
  }

  /**
   * String STOMP message.
   */
  public static final class NettyStringStompMessage extends NettyStompMessage<String> {

    private final Charset charset;

    /**
     * Constructor.
     * 
     * @param frame
     *          the frame
     * @param charset
     *          the charset
     * @throws IllegalArgumentException
     *           if any argument is {@literal null}
     */
    public NettyStringStompMessage(StompFrame frame, Charset charset) {
      super(frame);
      if (charset == null) {
        throw new IllegalArgumentException("The charset argument must not be null.");
      }
      this.charset = charset;
    }

    @Override
    public String getBody() {
      ByteBuf b = frame.content();
      if (b == null) {
        return null;
      }
      return b.toString(charset);
    }

  }

}

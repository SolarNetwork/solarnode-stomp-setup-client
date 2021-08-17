/* ==================================================================
 * BasicStompMessage.java - 16/08/2021 4:17:17 PM
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

package net.solarnetwork.node.setup.stomp.client.domain;

import java.nio.charset.Charset;

import org.springframework.util.MultiValueMap;

import net.solarnetwork.node.setup.stomp.StompCommand;

/**
 * Basic STOMP message.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BasicStompMessage<T> implements StompMessage<T> {

  private final StompCommand command;
  private final MultiValueMap<String, String> headers;

  /**
   * Constructor.
   * 
   * @param command
   *          the command
   * @param headers
   *          the headers
   * @throws IllegalArgumentException
   *           if {@code command} is {@literal null}
   */
  protected BasicStompMessage(StompCommand command, MultiValueMap<String, String> headers) {
    super();
    if (command == null) {
      throw new IllegalArgumentException("The command argument must not be null.");
    }
    this.command = command;
    this.headers = headers;
  }

  @Override
  public StompCommand getCommand() {
    return command;
  }

  @Override
  public MultiValueMap<String, String> getHeaders() {
    return headers;
  }

  /**
   * Create a new string STOMP message.
   * 
   * @param command
   *          the command
   * @param headers
   *          the headers
   * @param body
   *          the body
   * @return the new message
   * @throws IllegalArgumentException
   *           if {@code command} is {@literal null}
   */
  public static StompMessage<String> stringMessage(StompCommand command,
      MultiValueMap<String, String> headers, String body) {
    return new StringStompMessage(command, headers, body);
  }

  /**
   * Create a new string STOMP message without a body.
   * 
   * @param command
   *          the command
   * @param headers
   *          the headers
   * @return the new message
   * @throws IllegalArgumentException
   *           if {@code command} is {@literal null}
   */
  public static StompMessage<String> stringMessage(StompCommand command,
      MultiValueMap<String, String> headers) {
    return stringMessage(command, headers, null);
  }

  /**
   * A string STOMP message.
   */
  public static final class StringStompMessage extends BasicStompMessage<String> {

    private String body;

    /**
     * Constructor.
     * 
     * @param command
     *          the command
     * @param headers
     *          the headers
     * @param body
     *          the body
     * @throws IllegalArgumentException
     *           if {@code command} is {@literal null}
     */
    public StringStompMessage(StompCommand command, MultiValueMap<String, String> headers,
        String body) {
      super(command, headers);
      this.body = body;
    }

    @Override
    public String getBody() {
      return body;
    }

    @Override
    public byte[] getContent() {
      return body.getBytes(Charset.forName("UTF-8"));
    }

  }

}

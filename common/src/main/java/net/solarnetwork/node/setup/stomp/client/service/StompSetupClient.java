/* ==================================================================
 * StompSetupClient.java - 9/08/2021 11:06:46 AM
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

package net.solarnetwork.node.setup.stomp.client.service;

import java.util.concurrent.Future;
import java.util.function.Consumer;

import net.solarnetwork.node.setup.stomp.client.domain.StompMessage;

/**
 * API for a STOMP setup client.
 * 
 * @author matt
 * @version 1.0
 */
public interface StompSetupClient {

  /**
   * Connect.
   * 
   * @return a future with the connection result
   */
  Future<?> connect();

  /**
   * Test if the client is connected.
   * 
   * @return {@literal true} if the client is connected to a STOMP setup server
   */
  boolean isConnected();

  /**
   * Disconnect from the currently connected SolarNode Setup server.
   */
  void disconnect();

  /**
   * Add a message consumer to receive messages with.
   * 
   * @param consumer
   *          the consumer
   */
  void addMessageConsumer(Consumer<StompMessage<String>> consumer);

  /**
   * Remove a previously added message consumer.
   * 
   * @param consumer
   *          the consumer to remove
   */
  void removeMessageConsumer(Consumer<StompMessage<String>> consumer);

  /**
   * Post a message.
   * 
   * @param message
   *          the message to post
   * @return a future with the post result
   */
  Future<?> post(StompMessage<?> message);

}

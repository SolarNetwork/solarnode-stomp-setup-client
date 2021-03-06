/* ==================================================================
 * SetupClientService.java - 9/08/2021 11:09:31 AM
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

import java.util.Collection;
import java.util.Set;

import org.springframework.util.MultiValueMap;

import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.node.setup.stomp.client.domain.Message;

/**
 * High-level API for a SolarNode setup client.
 * 
 * @author matt
 * @version 1.0
 */
public interface SetupClientService {

  /**
   * Connect to a SolarNode Setup server.
   * 
   * @param host
   *          the server host name or IP address
   * @param port
   *          the server port
   * @param username
   *          the username to authenticate as
   * @param password
   *          the password to authenticate with
   */
  void connect(String host, int port, String username, String password);

  /**
   * Disconnect from the currently connected SolarNode Setup server.
   */
  void disconnect();

  /**
   * Test if the client is connected.
   * 
   * @return {@literal true} if the client is connected to a setup server
   */
  boolean isConnected();

  /**
   * Get the latest available datum.
   * 
   * @param sourceIdFilter
   *          optional source ID filter or {@literal null} for all available datum; can use
   *          Ant-style patterns
   * @return the latest available datum, never {@literal null}
   */
  Collection<Datum> latestDatum(Set<String> sourceIdFilter);

  /**
   * Execute an arbitrary command.
   * 
   * @param service
   *          the setup service to execute
   * @param headers
   *          the headers
   * @param body
   *          the body content
   * @return the result message
   */
  Message<String> executeCommand(String service, MultiValueMap<String, String> headers,
      Object body);

}

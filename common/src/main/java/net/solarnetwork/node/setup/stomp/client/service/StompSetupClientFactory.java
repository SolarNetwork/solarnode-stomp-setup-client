/* ==================================================================
 * StompSetupClientFactory.java - 17/08/2021 10:04:54 AM
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

/**
 * Factory API for STOMP setup clients.
 * 
 * @author matt
 * @version 1.0
 */
@FunctionalInterface
public interface StompSetupClientFactory {

  /**
   * Create the client.
   * 
   * @param host
   *          the host to connect to
   * @param port
   *          the port to connect to
   * @return the client, never {@literal null}
   */
  StompSetupClient createClient(String host, int port);

}

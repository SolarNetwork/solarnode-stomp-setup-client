/* ==================================================================
 * StompMessage.java - 16/08/2021 3:20:35 PM
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

import net.solarnetwork.node.setup.stomp.StompCommand;

/**
 * API for a STOMP message.
 * 
 * @param <T>
 *          the body content type
 * @author matt
 * @version 1.0
 */
public interface StompMessage<T> extends Message<T> {

  /**
   * Get the STOMP command.
   * 
   * @return the command
   */
  StompCommand getCommand();

}

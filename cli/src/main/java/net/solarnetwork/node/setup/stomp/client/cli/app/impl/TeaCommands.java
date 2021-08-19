/* ==================================================================
 * TeaCommands.java - 20/08/2021 10:00:15 AM
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

package net.solarnetwork.node.setup.stomp.client.cli.app.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import net.solarnetwork.node.setup.stomp.client.domain.Message;
import net.solarnetwork.node.setup.stomp.client.service.SetupClientService;

/**
 * Tea setup tasks.
 * 
 * @author matt
 * @version 1.0
 */
@ShellComponent
@ShellCommandGroup("Tea")
public class TeaCommands {

  private final SetupClientService setupService;

  /**
   * Constructor.
   * 
   * @param setupService
   *          the setup service
   */
  @Autowired
  public TeaCommands(SetupClientService setupService) {
    super();
    this.setupService = setupService;
  }

  /**
   * Test if the all commands are available (from being connected).
   * 
   * @return the availability
   */
  @ShellMethodAvailability(value = "*")
  public Availability availability() {
    return (setupService.isConnected() ? Availability.available()
        : Availability.unavailable("Must connect to the server first."));
  }

  /**
   * Order some coffee.
   * 
   * @return the order result
   */
  @ShellMethod(key = "coffee-order", value = "Order a cup of coffee.")
  public String coffeeOrder() {
    Message<String> response = setupService.executeCommand("/coffee/order", null, null);
    return ShellUtils.renderResponseMessage(response);
  }

  /**
   * Order some tea.
   * 
   * @return the order result
   */
  @ShellMethod(key = "tea-order", value = "Order a cup of tea.")
  public String teaOrder() {
    Message<String> response = setupService.executeCommand("/tea/order", null, null);
    return ShellUtils.renderResponseMessage(response);
  }

}

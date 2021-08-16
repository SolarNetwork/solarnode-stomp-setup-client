/* ==================================================================
 * SessionCommands.java - 9/08/2021 9:25:22 AM
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
import org.springframework.shell.standard.ShellOption;

import net.solarnetwork.node.setup.stomp.client.service.SetupClientService;

/**
 * Commands for starting/ending a setup session.
 * 
 * @author matt
 * @version 1.0
 */
@ShellComponent
@ShellCommandGroup("Session")
public class SessionCommands {

  private final SetupClientService setupService;

  /**
   * Constructor.
   * 
   * @param setupService
   *          the setup service
   */
  @Autowired
  public SessionCommands(SetupClientService setupService) {
    super();
    this.setupService = setupService;
  }

  /**
   * Connect to a setup server.
   * 
   * @param host
   *          the host name or IP address and port number, separated by a {@literal :}
   * @param username
   *          the username to authenticate as
   * @param password
   *          the password to authenticate with
   * @return the connection result
   */
  @ShellMethod(key = "connect", value = "Connect to a SolarNode Setup server.")
  public String connect(@ShellOption(value = "host",
      help = "The host:port to connect to (port defaults to 8780 if not provided).") String host,
      @ShellOption(value = "username", help = "The username to login as.") String username,
      @ShellOption(value = "password", help = "The password to login with.") String password) {

    String hostname = host;
    int port = 8780;

    int idx = host.indexOf(':');
    if (idx > 0) {
      hostname = host.substring(0, idx);
      port = Integer.parseInt(host.substring(idx + 1));
    }
    setupService.connect(hostname, port, username, password);
    return "OK";
  }

  /**
   * Test if the {@literal connect} command is available.
   * 
   * @return the availability
   */
  @ShellMethodAvailability(value = "connect")
  public Availability connectAvailability() {
    return (!setupService.isConnected() ? Availability.available()
        : Availability.unavailable("Must disconnect from current server first."));
  }

  /**
   * Disconnect from a setup server.
   * 
   * @return the disconnection result
   */
  @ShellMethod(key = "disconnect", value = "Disconnect from the active SolarNode Setup server.")
  public String disconnect() {
    if (setupService.isConnected()) {
      setupService.disconnect();
      return "OK";
    }
    return "ERROR: not connected.";
  }

  /**
   * Test if the {@literal disconnect} command is available.
   * 
   * @return the availability
   */
  @ShellMethodAvailability(value = "disconnect")
  public Availability disconnectAvailability() {
    return (setupService.isConnected() ? Availability.available()
        : Availability.unavailable("Must connect to a server first."));
  }

}

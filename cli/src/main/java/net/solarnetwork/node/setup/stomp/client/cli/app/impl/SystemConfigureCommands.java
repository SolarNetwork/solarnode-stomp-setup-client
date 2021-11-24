/* ==================================================================
 * DatumCommands.java - 18/08/2021 3:57:33 PM
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import net.solarnetwork.node.setup.stomp.client.domain.Message;
import net.solarnetwork.node.setup.stomp.client.service.SetupClientService;

/**
 * System configure related commands.
 * 
 * @author matt
 * @version 1.0
 */
@ShellComponent
@ShellCommandGroup("System Configure")
public class SystemConfigureCommands {

  private static final Logger log = LoggerFactory.getLogger(SystemConfigureCommands.class);

  private final SetupClientService setupService;

  /**
   * Constructor.
   * 
   * @param setupService
   *          the setup service
   */
  @Autowired
  public SystemConfigureCommands(SetupClientService setupService) {
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
   * Execute the "identity" setup task.
   * 
   * @return the ping result
   */
  @ShellMethod(key = "sysconf-ident", value = "View the node identity information.")
  public String sysconfIdentity() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    Message<String> result = setupService.executeCommand("/setup/identity", headers, null);
    return ShellUtils.renderResponseMessage(result);
  }

  /**
   * Execute the "ping" setup task.
   * 
   * @return the ping result
   */
  @ShellMethod(key = "sysconf-ping", value = "Test the network connection.")
  public String sysconfPing() {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    Message<String> result = setupService.executeCommand("/setup/network/ping", headers, null);
    return ShellUtils.renderResponseMessage(result);
  }

  /**
   * Connect to a setup server.
   * 
   * @param sourceIdFilter
   *          an optional source ID filter to use
   * @return the connection result
   */
  @ShellMethod(key = "sysconf-wifi", value = "Configure WiFi settings.")
  public String sysconfWifi(
  // @formatter:off
      @ShellOption(value = "country", help = "A 2-character country code.")
      String country,
      
      @ShellOption(value = "ssid", help = "The WiFi network name.")
      String ssid,
      
      @ShellOption(value = "password", help = "The WiFi network password.")
      String password
      // @formatter:on
  ) {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("country", country);
    headers.add("ssid", ssid);
    headers.add("password", password);
    Message<String> result = setupService.executeCommand("/setup/network/wifi", headers, null);
    log.debug("WiFi response: {}", result);
    return ShellUtils.renderResponseMessage(result);
  }

}

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

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.setup.stomp.client.service.SetupClientService;
import net.solarnetwork.util.StringUtils;

/**
 * Datum related commands.
 * 
 * @author matt
 * @version 1.0
 */
@ShellComponent
@ShellCommandGroup("Datum")
public class DatumCommands {

  private final SetupClientService setupService;

  /**
   * Constructor.
   * 
   * @param setupService
   *          the setup service
   */
  @Autowired
  public DatumCommands(SetupClientService setupService) {
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
   * Connect to a setup server.
   * 
   * @param sourceIdFilter
   *          an optional source ID filter to use
   * @return the connection result
   */
  @ShellMethod(key = "latest", value = "Get the latest datum.")
  public String latestDatum(@ShellOption(value = "filter", defaultValue = "",
      help = "A source ID filter (Ant-style wild cards allowd).") String sourceIdFilter) {
    Set<String> sourceIdsFilter = StringUtils.commaDelimitedStringToSet(sourceIdFilter);
    Collection<GeneralDatum> datum = setupService.latestDatum(sourceIdsFilter);
    return ShellUtils.renderDatumTable(datum);
  }

}

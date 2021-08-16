/* ==================================================================
 * SetupTool.java - 3/08/2020 11:47:48 AM
 * 
 * Copyright 2020 SolarNetwork Foundation
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

package net.solarnetwork.node.setup.stomp.client.cli.app;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import net.solarnetwork.node.setup.stomp.client.cli.app.config.AppConfiguration;
import net.solarnetwork.node.setup.stomp.client.cli.app.impl.AppServices;

/**
 * Main entry point for SolarNode STOMP Setup Client CLI application.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootApplication(scanBasePackageClasses = { AppConfiguration.class, AppServices.class })
public class SetupTool {

  /**
   * Command-line entry point.
   * 
   * @param args
   *          the command-line arguments
   */
  public static void main(String[] args) throws InterruptedException {
    new SpringApplicationBuilder().sources(SetupTool.class).web(WebApplicationType.NONE)
        .logStartupInfo(false).build().run(args);
  }

}

/* ==================================================================
 * SetupServiceConfig.java - 9/08/2021 11:37:36 AM
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

package net.solarnetwork.node.setup.stomp.client.cli.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.solarnetwork.node.setup.stomp.client.impl.StompSetupClientService;
import net.solarnetwork.node.setup.stomp.client.service.SetupClientService;

/**
 * Configuration for the STOMP setup client.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SetupServiceConfig {

  @Bean
  public SetupClientService setupClientService() {
    return new StompSetupClientService();
  }

}

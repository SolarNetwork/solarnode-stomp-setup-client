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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import net.solarnetwork.codec.BasicGeneralDatumDeserializer;
import net.solarnetwork.codec.BasicGeneralDatumSerializer;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.setup.stomp.client.impl.NettyStompSetupClientFactory;
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

  private ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule mod = new SimpleModule("Test");
    mod.addSerializer(GeneralDatum.class, BasicGeneralDatumSerializer.INSTANCE);
    mod.addDeserializer(GeneralDatum.class, BasicGeneralDatumDeserializer.INSTANCE);
    mapper.registerModule(mod);
    return mapper;
  }

  /**
   * Create the setup client service.
   * 
   * @return the service
   */
  @Bean
  public SetupClientService setupClientService() {
    StompSetupClientService s = new StompSetupClientService(new NettyStompSetupClientFactory());
    s.setObjectMapper(objectMapper());
    return s;
  }

}

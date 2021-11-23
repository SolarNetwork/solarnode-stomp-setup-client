/* ==================================================================
 * StompSetupClientServiceTests.java - 17/08/2021 10:01:30 AM
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

package net.solarnetwork.node.setup.stomp.client.util.test;

import static net.solarnetwork.node.setup.stomp.client.domain.BasicStompMessage.stringMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.setup.stomp.SetupHeader;
import net.solarnetwork.node.setup.stomp.SetupTopic;
import net.solarnetwork.node.setup.stomp.StompCommand;
import net.solarnetwork.node.setup.stomp.StompHeader;
import net.solarnetwork.node.setup.stomp.client.domain.StompMessage;
import net.solarnetwork.node.setup.stomp.client.impl.StompSetupClientService;
import net.solarnetwork.node.setup.stomp.client.service.StompSetupClient;
import net.solarnetwork.node.setup.stomp.client.service.StompSetupClientFactory;

/**
 * Test cases for the {@link StompSetupClientService} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class StompSetupClientServiceTests {

  @Mock
  private StompSetupClientFactory clientFactory;

  @Mock
  private StompSetupClient client;

  @Captor
  private ArgumentCaptor<Consumer<StompMessage<String>>> consumerCaptor;

  @Captor
  private ArgumentCaptor<StompMessage<String>> postCaptor;

  private ObjectMapper mapper;
  private StompSetupClientService service;

  /**
   * Setup for test.
   */
  @BeforeEach
  public void setup() {
    service = new StompSetupClientService(clientFactory);
    mapper = JsonUtils.newDatumObjectMapper();
    service.setObjectMapper(mapper);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void connect() {
    // GIVEN
    String host = "localhost";
    int port = 1234;
    given(clientFactory.createClient(host, port)).willReturn(client);

    client.addMessageConsumer(service);

    Future doneFuture = CompletableFuture.completedFuture(null);
    given(client.connect()).willReturn(doneFuture);

    // the service is a consumer of messages from the client
    verify(client).addMessageConsumer(consumerCaptor.capture());

    // post CONNECT message followed by SEND:/setup/authenticate message
    // followed by SUBSCRIBE:/setup/**
    String salt = BCrypt.gensalt();
    given(client.post(postCaptor.capture())).willAnswer(new Answer<Future<?>>() {

      @Override
      public Future<?> answer(InvocationOnMock invocation) throws Throwable {
        // "async" CONNECTED message reply
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(SetupHeader.AuthHash.getValue(), "bcrypt");
        headers.add("auth-hash-param-salt", salt);
        StompMessage<String> connected = stringMessage(StompCommand.CONNECTED, headers);
        service.accept(connected);
        return doneFuture;
      }
    }).willReturn(doneFuture).willReturn(doneFuture);

    // WHEN
    String username = "foo";
    service.connect("localhost", 1234, username, "bar");

    // THEN
    verify(client).connect();
    assertThat("Service is message consumer for client", consumerCaptor.getValue(),
        is(sameInstance(service)));

    assertThat("3 messages posted (CONNECT, SEND, SUBSCRIBE)", postCaptor.getAllValues(),
        hasSize(3));

    StompMessage<String> connectMessage = postCaptor.getAllValues().get(0);
    assertThat("Posted CONNECT message", connectMessage.getCommand(), is(StompCommand.CONNECT));
    assertThat("Posted login header",
        connectMessage.getHeaders().getFirst(StompHeader.Login.getValue()), is(username));

    StompMessage<String> authMessage = postCaptor.getAllValues().get(1);
    assertThat("Posted SEND message", authMessage.getCommand(), is(StompCommand.SEND));
    assertThat("Destination header",
        authMessage.getHeaders().getFirst(StompHeader.Destination.getValue()),
        is(SetupTopic.Authenticate.getValue()));
    assertThat("Date header provided",
        authMessage.getHeaders().getFirst(StompHeader.Destination.getValue()), is(notNullValue()));
    assertThat("SNS authorization header provided",
        authMessage.getHeaders().getFirst(SetupHeader.Authorization.getValue()),
        matchesPattern("^SNS Credential=" + username + ",SignedHeaders=date,Signature=.+$"));

    StompMessage<String> subMessage = postCaptor.getAllValues().get(2);
    assertThat("Posted SUBSCRIBE message", subMessage.getCommand(), is(StompCommand.SUBSCRIBE));
    assertThat("Destination header",
        subMessage.getHeaders().getFirst(StompHeader.Destination.getValue()),
        is(StompSetupClientService.SETUP_SUBSCRIBE_TOPIC));
    assertThat("ID header", subMessage.getHeaders().getFirst(StompHeader.Id.getValue()), is("0"));
  }

  @SuppressWarnings({ "rawtypes" })
  @Test
  public void latestDatum() {
    // GIVEN
    connect(); // get service into connected state
    reset(client, clientFactory);

    given(client.isConnected()).willReturn(true);

    // post SEND:/setup/datum/latest message
    List<GeneralDatum> datum = new ArrayList<>();
    LocalDateTime date = LocalDateTime.of(2021, 8, 17, 14, 28, 12,
        (int) TimeUnit.MILLISECONDS.toNanos(345));
    DatumSamples s = new DatumSamples();
    s.putInstantaneousSampleValue("a", 1);
    s.putAccumulatingSampleValue("b", 2);
    datum.add(
        new net.solarnetwork.domain.datum.GeneralDatum("s1", date.toInstant(ZoneOffset.UTC), s));
    s = new DatumSamples();
    s.putInstantaneousSampleValue("A", 3);
    s.putAccumulatingSampleValue("B", 4);
    datum.add(
        new net.solarnetwork.domain.datum.GeneralDatum("s2", date.toInstant(ZoneOffset.UTC), s));

    Future doneFuture = CompletableFuture.completedFuture(null);
    given(client.post(postCaptor.capture())).willAnswer(new Answer<Future<?>>() {

      @Override
      public Future<?> answer(InvocationOnMock invocation) throws Throwable {
        final String json = mapper.writeValueAsString(datum);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(StompHeader.Destination.getValue(), SetupTopic.DatumLatest.getValue());
        headers.add(StompHeader.ContentType.getValue(),
            StompSetupClientService.JSON_UTF8_CONTENT_TYPE);
        headers.add(StompHeader.ContentLength.getValue(), String.valueOf(json.length()));

        StompMessage<String> reply = stringMessage(StompCommand.MESSAGE, headers, json);
        service.accept(reply);
        return doneFuture;
      }
    });

    // WHEN
    Collection<Datum> result = service.latestDatum(Collections.singleton("/**"));

    // THEN
    assertThat("Two datum returned", result, hasSize(2));
  }

}

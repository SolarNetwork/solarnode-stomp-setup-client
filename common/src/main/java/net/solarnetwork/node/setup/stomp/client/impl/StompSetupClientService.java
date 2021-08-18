/* ==================================================================
 * StompSetupClientService.java - 9/08/2021 11:15:38 AM
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

package net.solarnetwork.node.setup.stomp.client.impl;

import static net.solarnetwork.node.setup.stomp.client.domain.BasicStompMessage.stringMessage;
import static net.solarnetwork.util.NumberUtils.getAndIncrementWithWrap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.setup.stomp.SetupHeader;
import net.solarnetwork.node.setup.stomp.SetupTopic;
import net.solarnetwork.node.setup.stomp.StompCommand;
import net.solarnetwork.node.setup.stomp.StompHeader;
import net.solarnetwork.node.setup.stomp.client.domain.StompMessage;
import net.solarnetwork.node.setup.stomp.client.service.SetupClientService;
import net.solarnetwork.node.setup.stomp.client.service.StompSetupClient;
import net.solarnetwork.node.setup.stomp.client.service.StompSetupClientFactory;
import net.solarnetwork.security.SnsAuthorizationBuilder;

/**
 * Implementation of {@link SetupClientService} using the STOMP protocol.
 * 
 * @author matt
 * @version 1.0
 */
@Service
public class StompSetupClientService implements SetupClientService, Consumer<StompMessage<String>> {

  /**
   * The {@code timeoutSeconds} property default value.
   */
  public static final int DEFAULT_TIMEOUT_SECONDS = 600; // FIXME: shorten

  /** The topic subscribed to when connecting. */
  public static final String SETUP_SUBSCRIBE_TOPIC = "/setup/**";

  /** The JSON UTF-8 content type. */
  public static final String JSON_UTF8_CONTENT_TYPE = "application/json;charset=utf-8";

  private static final Logger log = LoggerFactory.getLogger(StompSetupClientService.class);

  private final AtomicInteger ids = new AtomicInteger(0);
  private final StompSetupClientFactory clientFactory;
  private StompSetupClient stompClient;
  private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
  private ObjectMapper objectMapper = new ObjectMapper();

  private Predicate<StompMessage<String>> actionFilter;
  private CompletableFuture<StompMessage<String>> actionFuture;

  /**
   * Constructor.
   * 
   * @param clientFactory
   *          the client factory to use
   */
  public StompSetupClientService(StompSetupClientFactory clientFactory) {
    super();
    if (clientFactory == null) {
      throw new IllegalArgumentException("The clientFactory argument must not be null.");
    }
    this.clientFactory = clientFactory;
  }

  private static Predicate<StompMessage<String>> commandFilter(StompCommand command) {
    return (msg) -> {
      return (msg != null && command == msg.getCommand());
    };
  }

  private static Predicate<StompMessage<String>> messageTopicFilter(String topic) {
    return (msg) -> {
      return (msg != null && StompCommand.MESSAGE == msg.getCommand() && msg.getHeaders() != null
          && topic.equals(msg.getHeaders().getFirst(StompHeader.Destination.getValue())));
    };
  }

  @Override
  public void connect(String host, int port, String username, String password) {
    StompSetupClient c = null;
    try {
      final CompletableFuture<Void> authFuture;
      synchronized (this) {
        if (stompClient != null) {
          stompClient.disconnect();
          stompClient = null;
        }
        if (actionFuture != null) {
          throw new RuntimeException("Multiplexed messages not supported.");
        }
        c = clientFactory.createClient(host, port);
        c.addMessageConsumer(this);
        c.connect().get(timeoutSeconds, TimeUnit.SECONDS);
        stompClient = c;

        final StompSetupClient client = c;
        final CompletableFuture<StompMessage<String>> connFuture = new CompletableFuture<>();
        authFuture = connFuture.thenCompose(msg -> {
          CompletableFuture<Void> f = new CompletableFuture<>();
          MultiValueMap<String, String> headers = msg.getHeaders();
          if (headers == null) {
            f.completeExceptionally(new RuntimeException("No CONNECTED headers."));
          } else if (!"bcrypt".equals(headers.getFirst(SetupHeader.AuthHash.getValue()))) {
            f.completeExceptionally(new RuntimeException("Unsupported auth-hash value: "
                + headers.getFirst(SetupHeader.AuthHash.getValue())));
          } else if (headers.getFirst("auth-hash-param-salt") == null) {
            f.completeExceptionally(new RuntimeException("Missing auth-hash-param-salt header."));
          } else {
            String salt = headers.getFirst("auth-hash-param-salt");
            String secret = DigestUtils.sha256Hex(BCrypt.hashpw(password, salt));
            // @formatter:off
            SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder(username)
                .verb(StompCommand.SEND.getValue())
                .path(SetupTopic.Authenticate.getValue());
            // @formatter:on
            String authHeader = authBuilder.build(secret);
            String dateHeader = authBuilder.headerValue("date");
            MultiValueMap<String, String> authHeaders = new LinkedMultiValueMap<>(2);
            authHeaders.set(SetupHeader.Authorization.getValue(), authHeader);
            authHeaders.set(SetupHeader.Date.getValue(), dateHeader);
            authHeaders.set(StompHeader.Destination.getValue(), "/setup/authenticate");
            postAndWait(client, StompCommand.SEND, authHeaders, null, f);
          }
          return f;
        }).thenCompose(Void -> {
          CompletableFuture<Void> f = new CompletableFuture<>();
          MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(2);
          headers.set(StompHeader.Destination.getValue(), SETUP_SUBSCRIBE_TOPIC);
          headers.set(StompHeader.Id.getValue(), String.valueOf(getAndIncrementWithWrap(ids, 0)));
          postAndWait(client, StompCommand.SUBSCRIBE, headers, null, f);
          return f;
        });
        this.actionFuture = connFuture;
        this.actionFilter = commandFilter(StompCommand.CONNECTED);
      }

      // CONNECT
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(2);
      headers.set(StompHeader.AcceptVersion.getValue(), "1.2");
      headers.set(StompHeader.Host.getValue(), host);
      headers.set(StompHeader.Login.getValue(), username);
      c.post(stringMessage(StompCommand.CONNECT, headers, null)).get(timeoutSeconds,
          TimeUnit.SECONDS);

      authFuture.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      if (c != null) {
        try {
          c.disconnect();
        } catch (Exception e2) {
          // ignore
        }
      }
      String msg = "Error connecting to " + host + ":" + port + ": " + e.getMessage();
      throw new RuntimeException(msg, e);
    }
  }

  @Override
  public synchronized void disconnect() {
    StompSetupClient c = this.stompClient;
    if (c != null) {
      c.disconnect();
      this.stompClient = null;
    }
  }

  @Override
  public boolean isConnected() {
    StompSetupClient c = this.stompClient;
    return (c != null && c.isConnected());
  }

  @Override
  public void accept(StompMessage<String> message) {
    final CompletableFuture<StompMessage<String>> f;
    final Predicate<StompMessage<String>> p;
    synchronized (this) {
      f = this.actionFuture;
      p = this.actionFilter;
    }
    if (f != null && p != null && p.test(message)) {
      synchronized (this) {
        this.actionFuture = null;
        this.actionFilter = null;
      }
      f.complete(message);
    }
  }

  @Override
  public Collection<GeneralDatum> latestDatum(Set<String> sourceIdFilter) {
    String body = null;
    if (sourceIdFilter != null && !sourceIdFilter.isEmpty()) {
      try {
        body = objectMapper.writeValueAsString(sourceIdFilter);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    final List<GeneralDatum> result = new ArrayList<>();
    final CompletableFuture<StompMessage<String>> future = new CompletableFuture<>();
    try {
      sendForMessage(SetupTopic.DatumLatest.getValue(), new LinkedMultiValueMap<>(2), body,
          JSON_UTF8_CONTENT_TYPE, future).get(timeoutSeconds, TimeUnit.SECONDS);
      StompMessage<String> response = future.get(timeoutSeconds, TimeUnit.SECONDS);

      // response body should be JSON array of objects, objects being GeneralDatum
      JsonNode json = objectMapper.readTree(response.getBody());
      for (JsonNode n : json) {
        if (!n.isObject()) {
          continue;
        }
        GeneralDatum datum = objectMapper.treeToValue(n, GeneralDatum.class);
        if (datum != null) {
          result.add(datum);
        }
      }
      return result;
    } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Future<?> sendForMessage(String topic, MultiValueMap<String, String> headers, String body,
      String contentType, CompletableFuture<StompMessage<String>> actionFuture) {
    final StompSetupClient c = this.stompClient;
    if (c == null || !c.isConnected()) {
      throw new RuntimeException("Not connected.");
    }
    headers.set(StompHeader.Destination.getValue(), topic);
    if (body != null) {
      headers.set(StompHeader.ContentType.getValue(), contentType);
      headers.set(StompHeader.ContentLength.getValue(), String.valueOf(body.length()));
    }
    synchronized (this) {
      this.actionFuture = actionFuture;
      this.actionFilter = messageTopicFilter(topic);
      return c.post(stringMessage(StompCommand.SEND, headers, body));
    }
  }

  private void postAndWait(StompCommand command, MultiValueMap<String, String> headers, String body,
      CompletableFuture<Void> postFuture) {
    final StompSetupClient c = this.stompClient;
    if (c == null || !c.isConnected()) {
      throw new RuntimeException("Not connected.");
    }
    postAndWait(c, command, headers, body, postFuture);
  }

  private void postAndWait(StompSetupClient c, StompCommand command,
      MultiValueMap<String, String> headers, String body, CompletableFuture<Void> postFuture) {
    try {
      c.post(stringMessage(command, headers, body)).get(timeoutSeconds, TimeUnit.SECONDS);
      postFuture.complete(null);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      log.error("Error posting {} message", command, e);
      postFuture.completeExceptionally(e);
    }
  }

  /**
   * Get the message timeout value.
   * 
   * @return the timeout seconds; defaults to {@link #DEFAULT_TIMEOUT_SECONDS}
   */
  public long getTimeoutSeconds() {
    return timeoutSeconds;
  }

  /**
   * Set the message timeout value.
   * 
   * @param timeoutSeconds
   *          the timeout seconds to set
   */
  public void setTimeoutSeconds(long timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  /**
   * Get the object mapper.
   * 
   * @return the object mapper, never {@literal null}
   */
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * Set the object mapper.
   * 
   * @param objectMapper
   *          the object mapper to set
   * @throws IllegalArgumentException
   *           if {@code objectMapper} is {@literal null}
   */
  public void setObjectMapper(ObjectMapper objectMapper) {
    if (objectMapper == null) {
      throw new IllegalArgumentException("The objectMapper argument must not be null.");
    }
    this.objectMapper = objectMapper;
  }

}

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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import net.solarnetwork.node.setup.stomp.client.domain.StompMessage;
import net.solarnetwork.node.setup.stomp.client.service.SetupClientService;
import net.solarnetwork.node.setup.stomp.client.service.StompSetupClient;
import net.solarnetwork.node.setup.stomp.client.util.SnsAuthorizationBuilder;

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

  private static final Logger log = LoggerFactory.getLogger(StompSetupClientService.class);

  private StompSetupClient stompClient;
  private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

  private final BlockingQueue<StompMessage<String>> queue = new ArrayBlockingQueue<>(1);

  private Predicate<StompMessage<String>> actionFilter;
  private CompletableFuture<StompMessage<String>> actionFuture;

  private static Predicate<StompMessage<String>> commandFilter(String command) {
    return (msg) -> {
      return (msg != null && command.equals(msg.getCommand()));
    };
  }

  private static Predicate<StompMessage<String>> sendTopicFilter(String topic) {
    return (msg) -> {
      return (msg != null && "SEND".equals(msg.getCommand()) && msg.getHeaders() != null
          && topic.equals(msg.getHeaders().getFirst("destination")));
    };
  }

  @Override
  public void connect(String host, int port, String username, String password) {
    NettyStompClient c = null;
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
        c = new NettyStompClient(host, port);
        c.addMessageConsumer(this);
        c.connect().get(timeoutSeconds, TimeUnit.SECONDS);
        stompClient = c;

        final CompletableFuture<StompMessage<String>> connFuture = new CompletableFuture<>();
        authFuture = connFuture.thenCompose(msg -> {
          CompletableFuture<Void> f = new CompletableFuture<>();
          MultiValueMap<String, String> headers = msg.getHeaders();
          if (headers == null) {
            f.completeExceptionally(new RuntimeException("No CONNECTED headers."));
          } else if (!"bcrypt".equals(headers.getFirst("auth-hash"))) {
            f.completeExceptionally(new RuntimeException(
                "Unsupported auth-hash value: " + headers.getFirst("auth-hash")));
          } else if (headers.getFirst("auth-hash-param-salt") == null) {
            f.completeExceptionally(new RuntimeException("Missing auth-hash-param-salt header."));
          } else {
            String salt = headers.getFirst("auth-hash-param-salt");
            String secret = DigestUtils.sha256Hex(BCrypt.hashpw(password, salt));
            // @formatter:off
            SnsAuthorizationBuilder authBuilder = new SnsAuthorizationBuilder(username)
                .verb("SEND")
                .path("/setup/authenticate");
            // @formatter:on
            String authHeader = authBuilder.build(secret);
            String dateHeader = authBuilder.headerValue("date");
            MultiValueMap<String, String> authHeaders = new LinkedMultiValueMap<>(2);
            authHeaders.set("authorization", authHeader);
            authHeaders.set("date", dateHeader);
            authHeaders.set("destination", "/setup/authenticate");

            final StompSetupClient client = this.stompClient;
            if (client == null) {
              f.completeExceptionally(new RuntimeException("Connection closed."));
            } else {
              try {
                client.post(stringMessage("SEND", authHeaders, null)).get(timeoutSeconds,
                    TimeUnit.SECONDS);
                f.complete(null);
              } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Error posting authentication message", e);
                f.completeExceptionally(e);
              }
            }
          }
          return f;
        });
        this.actionFuture = connFuture;
        this.actionFilter = commandFilter("CONNECTED");
      }

      // CONNECT
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>(2);
      headers.set("accept-version", "1.2");
      headers.set("host", host);
      headers.set("login", username);
      c.post(stringMessage("CONNECT", headers, null)).get(timeoutSeconds, TimeUnit.SECONDS);

      authFuture.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      if (c != null) {
        try {
          c.shutdown();
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

}

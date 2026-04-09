package com.agentgierka.mmo.agent.web;

import com.agentgierka.mmo.agent.event.AgentStateUpdatedEvent;
import com.agentgierka.mmo.agent.model.AgentWorldState;
import com.agentgierka.mmo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JwtService jwtService;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @Test
    void shouldReceiveAgentUpdateOverWebSocket() throws Exception {
        // Given
        UUID agentId = UUID.randomUUID();
        String token = generateTestToken("player1");
        
        BlockingQueue<AgentWorldState> blockingQueue = new LinkedBlockingDeque<>();
        
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        // When
        StompSession session = stompClient
                .connectAsync(String.format("ws://localhost:%d/ws", port), (org.springframework.web.socket.WebSocketHttpHeaders) null, connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/agents/" + agentId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AgentWorldState.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.add((AgentWorldState) payload);
            }
        });

        // Simulae Game Engine event
        AgentWorldState state = AgentWorldState.builder()
                .agentId(agentId)
                .x(100).y(200)
                .build();
        
        Thread.sleep(500); // Give time for subscription to settle
        eventPublisher.publishEvent(new AgentStateUpdatedEvent(state));

        // Then
        AgentWorldState receivedState = blockingQueue.poll(5, TimeUnit.SECONDS);
        assertThat(receivedState).isNotNull();
        assertThat(receivedState.getAgentId()).isEqualTo(agentId);
        assertThat(receivedState.getX()).isEqualTo(100);
        assertThat(receivedState.getY()).isEqualTo(200);
    }

    private String generateTestToken(String username) {
        UserDetails userDetails = User.withUsername(username)
                .password("password")
                .authorities("USER")
                .build();
        return jwtService.generateToken(userDetails);
    }
}

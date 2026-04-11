package com.agentgierka.mmo.config;

import com.agentgierka.mmo.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configure standard STOMP WebSocket message brokering with JWT security.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    @Bean(name = "applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new VirtualThreadTaskExecutor("mmo-task-");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:8081", "http://localhost:19006", "http://127.0.0.1:8081");
        
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:8081", "http://localhost:19006", "http://127.0.0.1:8081")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (isConnectCommand(accessor)) {
                    authenticate(accessor);
                }
                
                return message;
            }
        });
    }

    private boolean isConnectCommand(StompHeaderAccessor accessor) {
        return accessor != null && StompCommand.CONNECT.equals(accessor.getCommand());
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null) return;

        try {
            String username = jwtService.extractUsername(token);
            if (username != null && isNotAuthenticated()) {
                setupSpringSecurityContext(accessor, username);
            }
        } catch (Exception e) {
            log.error("WebSocket auth failed: {}", e.getMessage());
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        return (authHeader != null && authHeader.startsWith("Bearer ")) 
                ? authHeader.substring(7) 
                : null;
    }

    private boolean isNotAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() == null;
    }

    private void setupSpringSecurityContext(StompHeaderAccessor accessor, String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        accessor.setUser(authToken);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}

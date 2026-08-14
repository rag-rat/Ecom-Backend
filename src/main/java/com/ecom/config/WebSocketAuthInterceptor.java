package com.ecom.config;

import com.ecom.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;

import java.util.List;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);


                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

                    List<String> authHeaders = accessor.getNativeHeader("Authorization");

                    if (authHeaders != null && !authHeaders.isEmpty()) {

                        String bearerToken = authHeaders.get(0);

                        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {

                            String token = bearerToken.substring(7);

                            try {
                                String userId = jwtService.extractUserId(token);

                                /*
                                 * We set a minimal Authentication here — just enough for accessor.setUser() to attach an identity to this connection.

                                 * We deliberately don't reload the full CustomUserDetails/User entity here (no DB roundtrip needed just to identify WHO is connecting) —

                                 * ChatService looks up full user details separately when actually processing messages, the same lazy-loading philosophy used throughout this project.
                                 */
                                var authentication = new UsernamePasswordAuthenticationToken(
                                        userId, null, List.of());
                                accessor.setUser(authentication);

                                log.info("WebSocket authenticated for user: {}", userId);

                            } catch (Exception e) {
                                log.warn("WebSocket authentication failed: {}", e.getMessage());
                                // Connection proceeds unauthenticated — ChatController's
                                // methods will reject unauthenticated senders explicitly
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
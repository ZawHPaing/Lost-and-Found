package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// Register the SockJS endpoint at /ws.
		registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:8080").withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// Enable a simple broker with the /topic prefix.
		registry.enableSimpleBroker("/topic");
		// Set /app as the prefix for messages bound for @MessageMapping methods.
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		// Set up a Jackson message converter for JSON.
		DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
		resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(new ObjectMapper());
		converter.setContentTypeResolver(resolver);

		messageConverters.add(converter);
		return false; // Return false to prevent using default converters in addition.
	}

	public void addResourceHandlers(ResourceHandlerRegistry registry) {
	    registry.addResourceHandler("/uploads/**")
	            .addResourceLocations("file:D:/LostAndFoundWithoutSecurity/uploads/chats");
	}

	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// Map the "/notifications" endpoint to the handler
		registry.addHandler(new NotificationWebSocketHandler(), "/notifications").setAllowedOrigins("*"); 
	}
}

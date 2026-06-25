package com.helpdesk.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String TICKET_EXCHANGE = "helpdesk.ticket.exchange";
    public static final String TICKET_EVENT_QUEUE = "helpdesk.ticket.event.queue";
    
    // Routing key pattern to catch all ticket events (e.g., ticket.event.created, ticket.event.closed)
    public static final String TICKET_ROUTING_KEY = "ticket.event.#";

    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(TICKET_EXCHANGE);
    }

    @Bean
    public Queue ticketEventQueue() {
        return QueueBuilder.durable(TICKET_EVENT_QUEUE).build();
    }

    @Bean
    public Binding binding(Queue ticketEventQueue, TopicExchange ticketExchange) {
        return BindingBuilder.bind(ticketEventQueue).to(ticketExchange).with(TICKET_ROUTING_KEY);
    }
}
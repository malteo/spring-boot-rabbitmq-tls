package me.matteogiordano.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);
    private static final String EXCHANGE = "messages.exchange";
    private static final String OUTPUT_ROUTING_KEY = "output";

    private final RabbitTemplate rabbitTemplate;

    public MessageListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "input.queue")
    public void processMessage(Message message) {
        log.info("Received message from input queue: {}", message);

        Message enrichedMessage = new Message(
            message.id(),
            message.content(),
            message.timestamp(),
            "consumer"
        );

        log.info("Sending enriched message to output queue: {}", enrichedMessage);
        rabbitTemplate.convertAndSend(EXCHANGE, OUTPUT_ROUTING_KEY, enrichedMessage);
    }

}


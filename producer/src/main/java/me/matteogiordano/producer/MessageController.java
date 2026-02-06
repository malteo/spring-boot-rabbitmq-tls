package me.matteogiordano.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    private static final String EXCHANGE = "messages.exchange";
    private static final String INPUT_ROUTING_KEY = "input";

    private final RabbitTemplate rabbitTemplate;

    public MessageController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/messages")
    public Message sendMessage(@RequestBody String content) {
        Message message = new Message(
            UUID.randomUUID().toString(),
            content,
            LocalDateTime.now(),
            null
        );

        log.info("Sending message to input queue: {}", message);
        rabbitTemplate.convertAndSend(EXCHANGE, INPUT_ROUTING_KEY, message);

        return message;
    }

}


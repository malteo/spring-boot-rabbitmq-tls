package me.matteogiordano.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OutputListener {

    private static final Logger log = LoggerFactory.getLogger(OutputListener.class);

    @RabbitListener(queues = "output.queue")
    public void receiveEnrichedMessage(Message message) {
        log.info("Received enriched message from output queue: {}", message);
    }

}


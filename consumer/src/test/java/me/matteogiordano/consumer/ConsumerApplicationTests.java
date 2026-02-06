package me.matteogiordano.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
class ConsumerApplicationTests {

    @Test
    void contextLoads() {
    }

}



package me.matteogiordano.producer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
class ProducerApplicationTests {

    @Test
    void contextLoads() {
    }

}



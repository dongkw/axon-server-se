package io.axoniq.axonserver;

import io.axoniq.axonserver.grpc.GrpcFlowControlledDispatcherListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;

/**
 * Main class for AxonServer.
 *
 * @@author Marc Gathier
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AxonServer {
    public static void main(String[] args) {
        System.setProperty("spring.config.name", "axonserver");
        SpringApplication.run(AxonServer.class, args);
    }

    @PreDestroy
    public void clean() {
        GrpcFlowControlledDispatcherListener.shutdown();

    }

}

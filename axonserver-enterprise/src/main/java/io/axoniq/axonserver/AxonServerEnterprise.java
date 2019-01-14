package io.axoniq.axonserver;

import io.axoniq.axonserver.grpc.GrpcFlowControlledDispatcherListener;
import io.axoniq.axonserver.licensing.LicenseConfiguration;
import io.axoniq.axonserver.licensing.LicenseException;
import io.axoniq.axonserver.rest.PluginImportSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PreDestroy;

/**
 * @author Marc Gathier
 */
@SpringBootApplication(scanBasePackages = "io.axoniq")
@EnableAsync
@EnableScheduling
@EnableJpaRepositories("io.axoniq")
@EntityScan("io.axoniq")
@Import(PluginImportSelector.class)
public class AxonServerEnterprise {
    private static final Logger log = LoggerFactory.getLogger(AxonServerEnterprise.class);

    public static void main(String[] args) {
        try {
            LicenseConfiguration.getInstance();
        } catch(LicenseException ex) {
            log.error(ex.getMessage());
            System.exit(-1);
        }
        System.setProperty("spring.config.name", "axonserver");
        SpringApplication.run(AxonServerEnterprise.class, args);
    }


    @PreDestroy
    public void clean() {
        GrpcFlowControlledDispatcherListener.shutdown();

    }
}

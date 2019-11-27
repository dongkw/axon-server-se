package io.axoniq.axonserver.enterprise.storage.advancedstorage;

import io.axoniq.axonserver.localstorage.EventStoreFactory;
import io.axoniq.axonserver.localstorage.file.EmbeddedDBProperties;
import io.axoniq.axonserver.localstorage.transformation.EventTransformerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marc Gathier
 */
@Configuration
public class AdvancedStorageAutoConfiguration {
    @Bean
    @Conditional(MultitierStorageCondition.class)
    public EventStoreFactory eventStoreFactory(EmbeddedDBProperties embeddedDBProperties, EventTransformerFactory eventTransformerFactory,
                                               AdvancedStorageProperties advancedStorageProperties) {
        return new MultitierDatafileEventStoreFactory(embeddedDBProperties,
                                                      eventTransformerFactory,
                                                      advancedStorageProperties);
    }


}

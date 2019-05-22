package io.axoniq.axonserver.migration.jpa;


import io.axoniq.axonserver.migration.DomainEvent;
import io.axoniq.axonserver.migration.SnapshotEvent;
import io.axoniq.axonserver.migration.EventProducer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * @author Marc Gathier
 */
@Component
@Profile({"migrate-from-jpa"})
public class JpaEventProcessor implements EventProducer {
    @PersistenceContext(name = "eventstore")
    private EntityManager entityManager;


    public JpaEventProcessor() {

    }


    @Override
    public List<? extends DomainEvent> findEvents(long lastProcessedToken, int batchSize) {
        return entityManager.createNamedQuery("DomainEventEntry.findByGlobalIndex", DomainEventEntry.class)
                .setParameter("lastToken", lastProcessedToken)
                .setMaxResults(batchSize)
                .getResultList();
    }

    @Override
    public List<? extends SnapshotEvent> findSnapshots(String lastProcessedTimestamp, int batchSize) {
        return entityManager.createNamedQuery("SnapshotEventEntry.findByTimestamp", SnapshotEventEntry.class)
                .setParameter("lastTimeStamp", lastProcessedTimestamp)
                .setMaxResults(batchSize)
                .getResultList();
    }
}
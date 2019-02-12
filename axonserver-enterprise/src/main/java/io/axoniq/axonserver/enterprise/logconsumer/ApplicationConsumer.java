package io.axoniq.axonserver.enterprise.logconsumer;

import io.axoniq.axonserver.access.application.ApplicationController;
import io.axoniq.axonserver.enterprise.cluster.GrpcRaftController;
import io.axoniq.axonserver.grpc.ApplicationProtoConverter;
import io.axoniq.axonserver.grpc.cluster.Entry;
import io.axoniq.axonserver.grpc.internal.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Author: marc
 */
@Component
public class ApplicationConsumer implements LogEntryConsumer {
    private final Logger logger = LoggerFactory.getLogger(ApplicationConsumer.class);
    private final ApplicationController applicationController;

    public ApplicationConsumer(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    @Override
    public void consumeLogEntry(String groupId, Entry entry) {
        if( groupId.equals(GrpcRaftController.ADMIN_GROUP)) return;

        if( entryType(entry, Application.class.getName())) {
            try {
                Application application = Application.parseFrom(entry.getSerializedObject().getData());
                applicationController.mergeContext(ApplicationProtoConverter.createJpaApplication(application), groupId);
            } catch (Exception e) {
                logger.warn("{}: Failed to process application", groupId, e);
            }
        }

    }
}
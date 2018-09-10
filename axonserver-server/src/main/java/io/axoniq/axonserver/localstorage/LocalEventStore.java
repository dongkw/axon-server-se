package io.axoniq.axonserver.localstorage;

import io.axoniq.axondb.Event;
import io.axoniq.axondb.grpc.Confirmation;
import io.axoniq.axondb.grpc.EventWithToken;
import io.axoniq.axondb.grpc.GetAggregateEventsRequest;
import io.axoniq.axondb.grpc.GetEventsRequest;
import io.axoniq.axondb.grpc.GetFirstTokenRequest;
import io.axoniq.axondb.grpc.GetLastTokenRequest;
import io.axoniq.axondb.grpc.GetTokenAtRequest;
import io.axoniq.axondb.grpc.QueryEventsRequest;
import io.axoniq.axondb.grpc.QueryEventsResponse;
import io.axoniq.axondb.grpc.ReadHighestSequenceNrRequest;
import io.axoniq.axondb.grpc.ReadHighestSequenceNrResponse;
import io.axoniq.axondb.grpc.TrackingToken;
import io.axoniq.axonserver.ClusterEvents;
import io.axoniq.axonhub.internal.grpc.TransactionWithToken;
import io.axoniq.axonserver.exception.MessagingPlatformException;
import io.axoniq.axonserver.localstorage.query.QueryEventsRequestStreamObserver;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Author: marc
 */
@Component
public class LocalEventStore implements io.axoniq.axonserver.message.event.EventStore, SmartLifecycle {
    private static final Confirmation CONFIRMATION = Confirmation.newBuilder().setSuccess(true).build();
    private final Logger logger = LoggerFactory.getLogger(LocalEventStore.class);
    private final Map<String, Workers> workersMap = new ConcurrentHashMap<>();
    private final EventStoreFactory eventStoreFactory;
    private volatile boolean running;
    @Value("${axoniq.axondb.query.limit:200}")
    private long defaultLimit = 200;

    public LocalEventStore(EventStoreFactory eventStoreFactory) {
        this.eventStoreFactory = eventStoreFactory;
    }

    public void initContext(String context, boolean validating) {
        if( workersMap.containsKey(context)) return;
        Workers workers = new Workers(context);
        workers.init(validating);
        workersMap.put(context, workers);
    }

    public void cleanupContext(String context) {
        Workers workers = workersMap.remove(context);
        if( workers == null) return;
        workers.cleanup();
    }

    @EventListener
    public void on(ClusterEvents.MasterStepDown stepDown) {
        if( stepDown.isForwarded()) return;

        Workers workers = workersMap.get(stepDown.getContextName());
        if( workers == null) return;

        workers.eventWriteStorage.cancelPendingTransactions();

    }

    @Override
    public CompletableFuture<Confirmation> appendSnapshot(String context, Event eventMessage) {
        return workersMap.get(context).snapshotWriteStorage.store(eventMessage);
    }

    @Override
    public StreamObserver<Event> createAppendEventConnection(String context,
                                                                   StreamObserver<Confirmation> responseObserver) {
        return new StreamObserver<Event>() {
            private final List<Event> eventList = new ArrayList<>();
            @Override
            public void onNext(Event event) {
                eventList.add(event);
            }

            @Override
            public void onError(Throwable cause) {
                logger.warn("Error on connection to client while storing events", cause);
            }

            @Override
            public void onCompleted() {
                workersMap.get(context).eventWriteStorage.store(eventList).whenComplete((result, exception) -> {
                    if( exception != null) {
                        if( isClientException(exception)) {
                            logger.warn("Error while storing events: {}", exception.getMessage());
                        } else {
                            logger.warn("Error while storing events", exception);
                        }
                        logger.warn("Error while storing events", exception);
                        responseObserver.onError(exception);
                    } else {
                        responseObserver.onNext(CONFIRMATION);
                        responseObserver.onCompleted();
                    }
                });
            }
        };
    }

    @Override
    public void listAggregateEvents(String context, GetAggregateEventsRequest request,
                                    StreamObserver<InputStream> responseStreamObserver) {
        MethodDescriptor.Marshaller<Event> marshaller = ProtoUtils
                .marshaller(Event.getDefaultInstance());
        workersMap.get(context).aggregateReader.readEvents( request.getAggregateId(),
                                                            request.getAllowSnapshots(),
                                                            request.getInitialSequence(),
                                                            event -> responseStreamObserver.onNext(marshaller.stream(event)));
        responseStreamObserver.onCompleted();
    }

    @Override
    public StreamObserver<GetEventsRequest> listEvents(String context,
                                                       StreamObserver<InputStream> responseStreamObserver) {
        MethodDescriptor.Marshaller<EventWithToken> marshaller = ProtoUtils
                .marshaller(EventWithToken.getDefaultInstance());
        EventStreamController controller = workersMap.get(context).eventStreamReader.createController(
                eventWithToken -> responseStreamObserver.onNext(marshaller.stream(eventWithToken)),responseStreamObserver::onError);
        return new StreamObserver<GetEventsRequest>() {
            @Override
            public void onNext(GetEventsRequest getEventsRequest) {
                controller.update(getEventsRequest.getTrackingToken(), getEventsRequest.getNumberOfPermits());
            }

            @Override
            public void onError(Throwable throwable) {
                controller.stop();

            }

            @Override
            public void onCompleted() {
                controller.stop();
            }
        };
    }

    @Override
    public void getFirstToken(String context, GetFirstTokenRequest request,
                              StreamObserver<TrackingToken> responseObserver) {
        long token = workersMap.get(context).eventStreamReader.getFirstToken();
        responseObserver.onNext(TrackingToken.newBuilder().setToken(token).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLastToken(String context, GetLastTokenRequest request,
                             StreamObserver<TrackingToken> responseObserver) {
        try {
            responseObserver.onNext(TrackingToken.newBuilder().setToken(workersMap.get(context).eventWriteStorage.getLastToken()).build());
            responseObserver.onCompleted();
        } catch (RuntimeException ex) {
            responseObserver.onError(ex);
        }
    }

    @Override
    public void getTokenAt(String context, GetTokenAtRequest request, StreamObserver<TrackingToken> responseObserver) {
        long token = workersMap.get(context).eventStreamReader.getTokenAt(request.getInstant());
        responseObserver.onNext(TrackingToken.newBuilder().setToken(token).build());
        responseObserver.onCompleted();
    }

    @Override
    public void readHighestSequenceNr(String context, ReadHighestSequenceNrRequest request,
                                      StreamObserver<ReadHighestSequenceNrResponse> responseObserver) {
        try {
            long sequenceNumber = workersMap.get(context).aggregateReader.readHighestSequenceNr(request.getAggregateId());
            responseObserver.onNext(ReadHighestSequenceNrResponse.newBuilder().setToSequenceNr(sequenceNumber).build());
            responseObserver.onCompleted();
        } catch (RuntimeException ex) {
            responseObserver.onError(ex);
        }
    }


    @Override
    public StreamObserver<QueryEventsRequest> queryEvents(String context,
                                                          StreamObserver<QueryEventsResponse> responseObserver) {
        Workers workers = workersMap.get(context);
        return new QueryEventsRequestStreamObserver(workers.eventWriteStorage, workers.eventStreamReader, defaultLimit, responseObserver);
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable runnable) {
        running = false;
        runnable.run();
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        stop(() -> {});
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    public long getLastToken(String context) {
        return workersMap.get(context).eventWriteStorage.getLastToken();
    }

    public long getLastSnapshot(String context) {
        return workersMap.get(context).snapshotWriteStorage.getLastToken();
    }

    public void streamEventTransactions(String context, long firstToken, Predicate<TransactionWithToken> transactionConsumer, StorageCallback onCompleted) {
        workersMap.get(context).eventStreamReader.streamTransactions( firstToken, onCompleted, transactionConsumer);
    }

    public void streamSnapshotTransactions(String context, long firstToken,
                                           Predicate<TransactionWithToken> transactionConsumer, StorageCallback onCompleted) {
        workersMap.get(context).snapshotStreamReader.streamTransactions(firstToken, onCompleted, transactionConsumer);
    }

    public long syncEvents(String context, TransactionWithToken value) {
        SyncStorage writeStorage = workersMap.get(context).eventSyncStorage;
        writeStorage.sync(value.getEventsList());
        return value.getToken() + value.getEventsCount();
    }

    public long syncSnapshots(String context, TransactionWithToken value) {
        SyncStorage writeStorage = workersMap.get(context).snapshotSyncStorage;
        writeStorage.sync(value.getEventsList());
        return value.getToken() + value.getEventsCount();
    }

    public long getWaitingEventTransactions(String context) {
        return workersMap.get(context).eventWriteStorage.waitingTransactions();
    }
    public long getWaitingSnapshotTransactions(String context) {
        return workersMap.get(context).snapshotWriteStorage.waitingTransactions();
    }

    public long getLastCommittedToken(String context) {
        return workersMap.get(context).eventWriteStorage.getLastCommittedToken();
    }
    public long getLastCommittedSnapshot(String context) {
        return workersMap.get(context).snapshotWriteStorage.getLastCommittedToken();
    }

    public void rollbackEvents(String context, long token) {
        Workers workers = workersMap.get(context);
        if( workers != null) {
            logger.debug("{}: Rollback events to {}, last token {}", context, token, getLastToken(context));
            workers.eventWriteStorage.rollback(token);
        }
    }
    public void rollbackSnapshots(String context, long token) {
        Workers workers = workersMap.get(context);
        if( workers != null) {
            logger.debug("{}: Rollback snapshots to {}, last token {}", context, token, getLastSnapshot(context));
            workers.snapshotWriteStorage.rollback(token);
        }
    }

    public Stream<String> getBackupFilenames(String context, EventType eventType, long lastSegmentBackedUp) {
        Workers workers = workersMap.get(context);
        if( workers != null) {
            if (eventType == EventType.SNAPSHOT) {
                return workers.snapshotDatafileManagerChain.getBackupFilenames(lastSegmentBackedUp);
            } else if (eventType == EventType.EVENT) {
                return workers.eventDatafileManagerChain.getBackupFilenames(lastSegmentBackedUp);
            }
        }
        return Stream.empty();
    }

    public void health(Health.Builder builder) {
        workersMap.values().forEach(worker -> worker.eventStreamReader.health(builder));
    }

    private boolean isClientException(Throwable exception) {
        return exception instanceof MessagingPlatformException
                && ((MessagingPlatformException) exception).getErrorCode().isClientException();
    }


    private class Workers {
        private final EventWriteStorage eventWriteStorage;
        private final SnapshotWriteStorage snapshotWriteStorage;
        private final AggregateReader aggregateReader;
        private final EventStreamReader eventStreamReader;
        private final EventStreamReader snapshotStreamReader;
        private final EventStore eventDatafileManagerChain;
        private final EventStore snapshotDatafileManagerChain;
        private final SyncStorage eventSyncStorage;
        private final SyncStorage snapshotSyncStorage;

        public Workers(String context) {
            this.eventDatafileManagerChain = eventStoreFactory.createEventManagerChain(context);
            this.snapshotDatafileManagerChain = eventStoreFactory.createSnapshotManagerChain(context);
            this.eventWriteStorage = new EventWriteStorage(eventStoreFactory.createTransactionManager(this.eventDatafileManagerChain));
            this.snapshotWriteStorage = new SnapshotWriteStorage(eventStoreFactory.createTransactionManager(this.snapshotDatafileManagerChain));
            this.aggregateReader = new AggregateReader(eventDatafileManagerChain, new SnapshotReader(snapshotDatafileManagerChain));
            this.eventStreamReader = new EventStreamReader(eventDatafileManagerChain, eventWriteStorage);
            this.snapshotStreamReader = new EventStreamReader(snapshotDatafileManagerChain, null);
            this.snapshotSyncStorage = new SyncStorage(snapshotDatafileManagerChain);
            this.eventSyncStorage = new SyncStorage(eventDatafileManagerChain);
        }

        public void init(boolean validate) {
            eventDatafileManagerChain.init(validate);
            snapshotDatafileManagerChain.init(validate);
        }

        public void cleanup() {
            eventDatafileManagerChain.cleanup();
            snapshotDatafileManagerChain.cleanup();


        }
    }
}


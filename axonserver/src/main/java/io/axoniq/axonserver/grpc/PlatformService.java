package io.axoniq.axonserver.grpc;

import io.axoniq.axonserver.EventProcessorEvents.PauseEventProcessorRequest;
import io.axoniq.axonserver.EventProcessorEvents.ProcessorStatusRequest;
import io.axoniq.axonserver.EventProcessorEvents.ReleaseSegmentRequest;
import io.axoniq.axonserver.EventProcessorEvents.StartEventProcessorRequest;
import io.axoniq.axonserver.TopologyEvents;
import io.axoniq.axonserver.topology.AxonServerNode;
import io.axoniq.axonserver.topology.Topology;
import io.axoniq.axonserver.grpc.control.ClientIdentification;
import io.axoniq.axonserver.grpc.control.NodeInfo;
import io.axoniq.axonserver.grpc.control.PauseEventProcessor;
import io.axoniq.axonserver.grpc.control.PlatformInboundInstruction;
import io.axoniq.axonserver.grpc.control.PlatformInboundInstruction.RequestCase;
import io.axoniq.axonserver.grpc.control.PlatformInfo;
import io.axoniq.axonserver.grpc.control.PlatformOutboundInstruction;
import io.axoniq.axonserver.grpc.control.PlatformServiceGrpc;
import io.axoniq.axonserver.grpc.control.ReleaseEventProcessorSegment;
import io.axoniq.axonserver.grpc.control.RequestEventProcessorInfo;
import io.axoniq.axonserver.grpc.control.RequestReconnect;
import io.axoniq.axonserver.grpc.control.StartEventProcessor;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Author: marc
 */
@Service("PlatformService")
public class PlatformService extends PlatformServiceGrpc.PlatformServiceImplBase implements AxonServerClientService {
    private final Map<ClientComponent, SendingStreamObserver<PlatformOutboundInstruction>> connectionMap = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(PlatformService.class);

    private final Topology clusterController;
    private final ContextProvider contextProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<RequestCase, Deque<InstructionConsumer>> handlers = new EnumMap<>(RequestCase.class);


    @FunctionalInterface
    public interface InstructionConsumer {
        void accept(String client, String context, PlatformInboundInstruction instruction);
    }

    public PlatformService(Topology clusterController,
                           ContextProvider contextProvider,
                           ApplicationEventPublisher eventPublisher) {
        this.clusterController = clusterController;
        this.contextProvider = contextProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void getPlatformServer(ClientIdentification request, StreamObserver<PlatformInfo> responseObserver) {
        String context = contextProvider.getContext();
        try {
            AxonServerNode connectTo = clusterController.findNodeForClient(request.getClientName(),
                                                                           request.getComponentName(),
                                                                           context);
            responseObserver.onNext(PlatformInfo.newBuilder()
                                                .setPrimary(NodeInfo.newBuilder().setNodeName(connectTo.getName())
                                                                    .setHostName(connectTo.getHostName())
                                                                    .setGrpcPort(connectTo.getGrpcPort())
                                                                    .setHttpPort(connectTo.getHttpPort())
                                                ).build());
            responseObserver.onCompleted();
        } catch( RuntimeException cause) {
            logger.warn("Error processing client request {}", request, cause);
            responseObserver.onError(GrpcExceptionBuilder.build(cause));
        }
    }

    @Override
    public StreamObserver<PlatformInboundInstruction> openStream(StreamObserver<PlatformOutboundInstruction> responseObserver) {
        String context = contextProvider.getContext();
        SendingStreamObserver<PlatformOutboundInstruction> sendingStreamObserver = new SendingStreamObserver<>(responseObserver);
        return new ReceivingStreamObserver<PlatformInboundInstruction>(logger) {
            private ClientComponent clientComponent;
            private volatile ScheduledFuture<?> checkConnectionTask;
            @Override
            protected void consume(PlatformInboundInstruction instruction) {
                RequestCase requestCase = instruction.getRequestCase();
                handlers.getOrDefault(requestCase, new ArrayDeque<>())
                        .forEach(consumer -> consumer.accept(clientComponent.client, context, instruction));
                switch (requestCase) {
                    case REGISTER:
                        ClientIdentification client = instruction.getRegister();
                        clientComponent = new ClientComponent(client.getClientName(), client.getComponentName(), context);
                        registerClient(clientComponent, sendingStreamObserver);
                        break;
                    case REQUEST_NOT_SET:
                        break;
                }
            }

            @Override
            protected String sender() {
                return clientComponent == null ? null : clientComponent.client;
            }

            @Override
            public void onError(Throwable throwable) {
                stopConnectionCheck();
                deregisterClient(clientComponent);
            }

            private void stopConnectionCheck() {
                if( checkConnectionTask != null) {
                    checkConnectionTask.cancel(true);
                    checkConnectionTask = null;
                }
            }

            @Override
            public void onCompleted() {
                stopConnectionCheck();
                deregisterClient(clientComponent);
            }
        };
    }

    public boolean requestReconnect(ClientComponent clientName) {
        logger.debug("Request reconnect: {}", clientName);
        StreamObserver<PlatformOutboundInstruction> stream = connectionMap.get(clientName);
        if( stream != null) {
            stream.onNext(PlatformOutboundInstruction.newBuilder().setRequestReconnect(RequestReconnect.newBuilder()).build());
            return true;
        }
        return false;
    }

    public boolean requestReconnect(String client) {
        logger.debug("Request reconnect: {}", client);
        return connectionMap.entrySet().stream()
                .filter(e -> e.getKey().client.equals(client))
                .map(e -> requestReconnect(e.getKey()))
                .findFirst().orElse(false);
    }


    public void sendToClient(String clientName, PlatformOutboundInstruction instruction) {
        connectionMap.entrySet().stream()
                     .filter(e -> e.getKey().client.equals(clientName))
                     .map(Map.Entry::getValue)
                     .forEach(stream -> stream.onNext(instruction));
    }


    public void requestReconnectForContext(String context) {
        connectionMap.entrySet().stream()
                            .filter(e -> e.getKey().context.equals(context))
                            .forEach(e -> requestReconnect(e.getKey()));

    }

    @EventListener
    public void onPauseEventProcessorRequest(PauseEventProcessorRequest evt){
        PlatformOutboundInstruction instruction = PlatformOutboundInstruction
                .newBuilder()
                .setPauseEventProcessor(PauseEventProcessor.newBuilder()
                                                           .setProcessorName(evt.processorName()))
                .build();
        this.sendToClient(evt.clientName(), instruction);
    }

    @EventListener
    public void onStartEventProcessorRequest(StartEventProcessorRequest evt){
        PlatformOutboundInstruction instruction = PlatformOutboundInstruction
                .newBuilder()
                .setStartEventProcessor(StartEventProcessor.newBuilder().setProcessorName(evt.processorName()))
                .build();
        this.sendToClient(evt.clientName(), instruction);
    }

    @EventListener
    public void on(ReleaseSegmentRequest evt){
        PlatformOutboundInstruction instruction = PlatformOutboundInstruction
                .newBuilder()
                .setReleaseSegment(ReleaseEventProcessorSegment.newBuilder()
                                                               .setProcessorName(evt.processorName())
                                                               .setSegmentIdentifier(evt.segmentId()))
                .build();
        this.sendToClient(evt.clientName(), instruction);
    }

    @EventListener
    public void on(ProcessorStatusRequest evt){
        PlatformOutboundInstruction instruction = PlatformOutboundInstruction
                .newBuilder()
                .setRequestEventProcessorInfo(RequestEventProcessorInfo.newBuilder()
                                                                       .setProcessorName(evt.processorName()))
                .build();
        this.sendToClient(evt.clientName(), instruction);
    }

    @EventListener
    public void on(TopologyEvents.ApplicationDisconnected event) {
        StreamObserver<PlatformOutboundInstruction> connection = connectionMap.remove(new ClientComponent(event.getClient(), event.getComponentName(), event.getContext()));
        logger.debug("application disconnected: {}, connection: {}", event.getClient(), connection);
        if( connection != null) {
            try {
                connection.onCompleted();
            } catch (Exception ex ) {
                logger.debug("Error while closing tracking event processor connection from {} - {}", event.getClient(), ex.getMessage());
            }
        }
    }

    public void onInboundInstruction(RequestCase requestCase, InstructionConsumer consumer) {
        Deque<InstructionConsumer> consumers = handlers.computeIfAbsent(requestCase,rc -> new ArrayDeque<>());
        consumers.add(consumer);
    }


    private void registerClient(ClientComponent clientComponent,
                                SendingStreamObserver<PlatformOutboundInstruction> responseObserver){
        connectionMap.put(clientComponent, responseObserver);
        logger.debug("Registered client : {}", clientComponent);
        eventPublisher.publishEvent(new TopologyEvents.ApplicationConnected(clientComponent.context,
                                                                            clientComponent.component, clientComponent.client));
    }

    private void deregisterClient(ClientComponent cc){
        logger.debug("De-registered client : {}", cc);
        if( cc != null)  {
            connectionMap.remove(cc);
            eventPublisher.publishEvent(new TopologyEvents.ApplicationDisconnected(cc.context,cc.component, cc.client, null));
        }

    }

    public Set<ClientComponent> getConnectedClients() {
        return connectionMap.keySet();
    }

    public static class ClientComponent {

        private final String client;
        private final String component;
        private final String context;

        private ClientComponent(String client, String component, String context) {
            this.client = client;
            this.component = component;
            this.context = context;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientComponent that = (ClientComponent) o;
            return Objects.equals(client, that.client);
        }

        public String getClient() {
            return client;
        }

        public String getComponent() {
            return component;
        }

        public String getContext() {
            return context;
        }

        @Override
        public int hashCode() {
            return Objects.hash(client);
        }

        @Override
        public String toString() {
            return "ClientComponent{" +
                    "client='" + client + '\'' +
                    '}';
        }
    }
}
package io.axoniq.axonserver.cluster;

import io.axoniq.axonserver.ClusterEvents;
import io.axoniq.axonserver.LoadBalancingSynchronizationEvents;
import io.axoniq.axonserver.component.processor.balancing.jpa.LoadBalanceStrategyRepository;
import io.axoniq.axonserver.component.processor.balancing.jpa.LoadBalancingStrategy;
import io.axoniq.axonserver.grpc.Converter;
import io.axoniq.axonserver.grpc.LoadBalancingStrategyProtoConverter;
import io.axoniq.axonserver.grpc.Publisher;
import io.axoniq.axonserver.grpc.internal.MessagingClusterService;
import io.axoniq.axonhub.internal.grpc.ConnectorCommand;
import io.axoniq.axonhub.internal.grpc.ConnectorResponse;
import io.axoniq.axonhub.internal.grpc.GetLBStrategiesRequest;
import io.axoniq.axonhub.internal.grpc.LoadBalancingStrategies;
import io.axoniq.platform.application.ApplicationController;
import io.axoniq.platform.grpc.Action;
import io.axoniq.platform.grpc.LoadBalanceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import static io.axoniq.axonhub.internal.grpc.ConnectorCommand.RequestCase.REQUEST_LOAD_BALANCING_STRATEGIES;

/**
 * Created by Sara Pellegrini on 16/08/2018.
 * sara.pellegrini@gmail.com
 */
@Controller @Transactional
public class LoadBalancingStrategySynchronizer {

    private final ApplicationController applicationController;
    private final LoadBalanceStrategyRepository repository;
    private final Converter<LoadBalanceStrategy, LoadBalancingStrategy> mapping;
    private final Publisher<ConnectorResponse> publisher;

    @Autowired
    public LoadBalancingStrategySynchronizer(ApplicationController applicationController,
                                             LoadBalanceStrategyRepository repository,
                                             MessagingClusterService clusterService) {
        this(applicationController, repository,
             message -> clusterService.sendToAll(message, name -> "Error sending load balancing strategy to " + name),
             new LoadBalancingStrategyProtoConverter());

        clusterService.onConnectorCommand(REQUEST_LOAD_BALANCING_STRATEGIES, this::onRequestLBStrategies);
    }

    LoadBalancingStrategySynchronizer(ApplicationController applicationController,
                                      LoadBalanceStrategyRepository repository,
                                      Publisher<ConnectorResponse> publisher,
                                      Converter<LoadBalanceStrategy, LoadBalancingStrategy> mapping) {
        this.applicationController = applicationController;
        this.repository = repository;
        this.mapping = mapping;
        this.publisher = publisher;
    }

    @EventListener
    public void on(LoadBalancingSynchronizationEvents.LoadBalancingStrategyReceived event) {
        if( event.isProxied()) {
            Action action = event.strategy().getAction();
            switch (action){
                case DELETE:
                    repository.deleteByName(event.strategy().getName());
                    break;
                case MERGE:
                    repository.save(mapping.map(event.strategy()));
                    break;
            }
        } else {
            publisher.publish(ConnectorResponse.newBuilder().setLoadBalancingStrategy(event.strategy()).build());
        }
    }

    @EventListener
    public void on(LoadBalancingSynchronizationEvents.LoadBalancingStrategiesReceived event) {
        repository.deleteAll();
        repository.flush();
        event.strategies().getStrategyList().forEach(processor -> repository.save(mapping.map(processor)));
        applicationController.updateModelVersion(event.strategies().getVersion());
    }

    @EventListener
    public void on(ClusterEvents.AxonHubInstanceConnected event) {
        if (applicationController.getModelVersion() < event.getModelVersion()) {
            ConnectorCommand command = ConnectorCommand
                    .newBuilder()
                    .setRequestLoadBalancingStrategies(GetLBStrategiesRequest.newBuilder())
                    .build();
            event.getRemoteConnection().publish(command);
        }
    }

    public void onRequestLBStrategies(ConnectorCommand requestStrategy,
                                      Publisher<ConnectorResponse> responsePublisher){
        LoadBalancingStrategies.Builder strategies = LoadBalancingStrategies
                .newBuilder().setVersion(applicationController.getModelVersion());
        repository.findAll().forEach(processor -> strategies.addStrategy(mapping.unmap(processor)));
        responsePublisher.publish(ConnectorResponse.newBuilder().setLoadBalancingStrategies(strategies).build());
    }

}
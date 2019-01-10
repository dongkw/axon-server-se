package io.axoniq.axonserver.message.command;

import io.axoniq.axonserver.DispatchEvents;
import io.axoniq.axonserver.ProcessingInstructionHelper;
import io.axoniq.axonserver.SubscriptionEvents;
import io.axoniq.axonserver.TopologyEvents;
import io.axoniq.axonserver.exception.ErrorCode;
import io.axoniq.axonserver.exception.ErrorMessageFactory;
import io.axoniq.axonserver.grpc.SerializedCommand;
import io.axoniq.axonserver.grpc.SerializedCommandResponse;
import io.axoniq.axonserver.grpc.command.Command;
import io.axoniq.axonserver.grpc.command.CommandResponse;
import io.axoniq.axonserver.grpc.command.CommandSubscription;
import io.axoniq.axonserver.message.ClientIdentification;
import io.axoniq.axonserver.message.FlowControlQueues;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Responsible for managing command subscriptions and processing commands.
 * Subscriptions are stored in the {@link CommandRegistrationCache}.
 * Running commands are stored in the {@link CommandCache}.
 *
 * Author: marc
 */
@Component("CommandDispatcher")
public class CommandDispatcher {

    private static final String COMMAND_COUNTER_NAME = "axon.commands.count";
    private static final String ACTIVE_COMMANDS_GAUGE = "axon.commands.active";
    private final CommandRegistrationCache registrations;
    private final CommandCache commandCache;
    private final CommandMetricsRegistry metricRegistry;
    private final Logger logger = LoggerFactory.getLogger(CommandDispatcher.class);
    private final FlowControlQueues<WrappedCommand> commandQueues = new FlowControlQueues<>(Comparator.comparing(WrappedCommand::priority).reversed());
    private final Counter commandCounter;

    public CommandDispatcher(CommandRegistrationCache registrations, CommandCache commandCache, CommandMetricsRegistry metricRegistry) {
        this.registrations = registrations;
        this.commandCache = commandCache;
        this.metricRegistry = metricRegistry;
        this.commandCounter = metricRegistry.counter(COMMAND_COUNTER_NAME);
        metricRegistry.gauge(ACTIVE_COMMANDS_GAUGE, commandCache, ConcurrentHashMap::size);
    }

    @EventListener
    public void on(SubscriptionEvents.SubscribeCommand event) {
        CommandSubscription request = event.getRequest();
        registrations.add( request.getCommand(), event.getHandler());
    }

    @EventListener
    public void on(SubscriptionEvents.UnsubscribeCommand event) {
        CommandSubscription request = event.getRequest();
        registrations.remove(event.clientIdentification(),request.getCommand());
    }

    @EventListener
    public void on(DispatchEvents.DispatchCommand dispatchCommand) {
        dispatch(dispatchCommand.getContext(), dispatchCommand.getRequest(), dispatchCommand.getResponseObserver(), dispatchCommand.isProxied());
    }

    public void dispatch(String context, SerializedCommand request, Consumer<SerializedCommandResponse> responseObserver, boolean proxied) {
        if( proxied) {
            CommandHandler handler = registrations.findByClientAndCommand(new ClientIdentification(context,request.getClient()), request.getCommand());
            dispatchToCommandHandler(context, request, handler, responseObserver);
        } else {
            commandCounter.increment();
            CommandHandler commandHandler = registrations.getHandlerForCommand(context, request.wrapped(), request.getRoutingKey());
            dispatchToCommandHandler(context, request, commandHandler, responseObserver);
        }
    }

    @EventListener
    public void on(TopologyEvents.ApplicationDisconnected event) {
        handleDisconnection(event.clientIdentification(), event.isProxied());
    }

    @EventListener
    public void on(TopologyEvents.CommandHandlerDisconnected event){
        handleDisconnection(event.clientIdentification(), event.isProxied());
    }

    private void handleDisconnection(ClientIdentification client, boolean proxied){
        cleanupRegistrations(client);
        if(!proxied) {
            getCommandQueues().move(client.toString(), this::redispatch);
        }
        handlePendingCommands(client);
    }

    private void dispatchToCommandHandler(String context, SerializedCommand command, CommandHandler commandHandler,
                                          Consumer<SerializedCommandResponse> responseObserver) {
        if (commandHandler == null) {
            responseObserver.accept(new SerializedCommandResponse(CommandResponse.newBuilder()
                                                   .setMessageIdentifier(command.getMessageIdentifier())
                                                   .setRequestIdentifier(command.getMessageIdentifier())
                                                   .setErrorCode(ErrorCode.NO_HANDLER_FOR_COMMAND.getCode())
                                                   .setErrorMessage(ErrorMessageFactory.build("No Handler for command: " + command.getName()))
                                                   .build()));
            return;
        }

        logger.debug("Dispatch {} to: {}", command.getName(), commandHandler.getClient());
        commandCache.put(command.getMessageIdentifier(), new CommandInformation(command.getName(), responseObserver, commandHandler.getClient(), commandHandler.getComponentName()));
        commandQueues.put(commandHandler.queueName(), new WrappedCommand( commandHandler.getClient(), command));
    }


    public void handleResponse(SerializedCommandResponse commandResponse, boolean proxied) {
        CommandInformation toPublisher = commandCache.remove(commandResponse.getRequestIdentifier());
        if (toPublisher != null) {
            logger.debug("Sending response to: {}", toPublisher);
            if (!proxied) {
                metricRegistry.add(toPublisher.getCommand(), toPublisher.getClientId(), System.currentTimeMillis() - toPublisher.getTimestamp());
            }
            toPublisher.getResponseConsumer().accept(commandResponse);
        } else {
            logger.info("Could not find command request: {}", commandResponse.getRequestIdentifier());
        }

    }

    private void cleanupRegistrations(ClientIdentification client) {
        registrations.remove(client);
    }

    public FlowControlQueues<WrappedCommand> getCommandQueues() {
        return commandQueues;
    }

    private String redispatch(WrappedCommand command) {
        SerializedCommand request = command.command();
        CommandInformation commandInformation = commandCache.remove(request.getMessageIdentifier());
        if( commandInformation == null) return null;

        CommandHandler client = registrations.getHandlerForCommand(command.client().getContext(), request.wrapped(),
                                                                   request.getRoutingKey());
        if (client == null) {
            commandInformation.getResponseConsumer().accept(new SerializedCommandResponse(CommandResponse.newBuilder()
                    .setMessageIdentifier(request.getMessageIdentifier()).setRequestIdentifier(request.getMessageIdentifier())
                    .setErrorCode(ErrorCode.NO_HANDLER_FOR_COMMAND.getCode())
                    .setErrorMessage(ErrorMessageFactory.build("No Handler for command: " + request.getName()))
                    .build()));
            return null;
        }

        logger.debug("Dispatch {} to: {}", request.getName(), client.getClient());

        commandCache.put(request.getMessageIdentifier(), new CommandInformation(request.getName(), commandInformation.getResponseConsumer(),
                client.getClient(), client.getComponentName()));
        return client.queueName();
    }

    private void handlePendingCommands(ClientIdentification client) {
        List<String> messageIds = commandCache.entrySet().stream().filter(e -> e.getValue().checkClient(client)).map(Map.Entry::getKey).collect(Collectors.toList());

        messageIds.forEach(m -> {
            CommandInformation ci = commandCache.remove(m);
            if( ci != null) {
                ci.getResponseConsumer().accept(new SerializedCommandResponse(CommandResponse.newBuilder()
                        .setMessageIdentifier(m)
                                                               .setRequestIdentifier(m)
                        .setErrorMessage(ErrorMessageFactory.build("Connection lost while executing command on: " + ci.getClientId()))
                        .setErrorCode(ErrorCode.CONNECTION_TO_HANDLER_LOST.getCode())
                        .build()));
            }
        });
    }

    public long getNrOfCommands() {
        return (long)commandCounter.count();
    }

    public int commandCount() {
        return commandCache.size();
    }


}

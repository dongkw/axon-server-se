package io.axoniq.axonserver.enterprise.cluster;

import io.axoniq.axonserver.enterprise.cluster.events.ClusterEvents;
import io.axoniq.axonserver.enterprise.cluster.internal.RemoteConnection;
import io.axoniq.axonserver.enterprise.jpa.AdminReplicationGroup;
import io.axoniq.axonserver.enterprise.jpa.ClusterNode;
import io.axoniq.axonserver.enterprise.taskscheduler.TaskPublisher;
import io.axoniq.axonserver.enterprise.taskscheduler.task.PrepareUpdateLicenseTask;
import io.axoniq.axonserver.enterprise.taskscheduler.task.UpdateLicenseTask;
import io.axoniq.axonserver.enterprise.taskscheduler.task.UpdateLicenseTaskPayload;
import io.axoniq.axonserver.grpc.cluster.Role;
import io.axoniq.axonserver.grpc.internal.ConnectorCommand;
import io.axoniq.axonserver.licensing.LicenseManager;
import io.axoniq.axonserver.taskscheduler.TransientException;
import junit.framework.TestCase;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.junit.*;
import org.mockito.stubbing.*;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

/**
 * @author Stefan Dragisic
 */
@RunWith(MockitoJUnitRunner.class)
public class DistributeLicenseServiceTest {
    private DistributeLicenseService testSubject;
    private AdminDB adminDB;

    private TaskPublisher taskPublisher;
    private final Map<String, Set<Object>> scheduledPayloadsTasks = new ConcurrentHashMap<>();
    private ClusterController clusterController;
    private ApplicationEventPublisher applicationEventPublisher;

    private ClusterNode createNode(String name) {
        return new ClusterNode(name, name, name, 1, 2, 3);
    }

    private class AdminDB {

        private Map<String, AdminReplicationGroup> contextMap = new ConcurrentHashMap<>();
        private Map<String, ClusterNode> nodeMap = new ConcurrentHashMap<>();

        public void addContext(String name, String... nodes) {
            AdminReplicationGroup context = contextMap.computeIfAbsent(name, AdminReplicationGroup::new);
            for (String node : nodes) {
                nodeMap.computeIfAbsent(node, DistributeLicenseServiceTest.this::createNode)
                       .addReplicationGroup(context,
                                            node + "/"
                                                    + context,
                                            Role.PRIMARY);
            }
        }
    }

    @Before
    public void setUp() {
        adminDB = new AdminDB();
        adminDB.addContext("sample", "node1");
        adminDB.addContext("_admin", "node1", "node2");

        clusterController = mock(ClusterController.class, Mockito.RETURNS_DEEP_STUBS);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);

        when(clusterController.nodes()).then((Answer<Stream<ClusterNode>>) invocationOnMock -> adminDB.nodeMap.values().stream());

        when(clusterController.getNode(anyString())).then((Answer<ClusterNode>) invocationOnMock -> {
            String name = invocationOnMock.getArgument(0);
            return adminDB.nodeMap.get(name);
        });


        taskPublisher = new TaskPublisher(null, null, null) {
            @Override
            public CompletableFuture<String> publishScheduledTask(String context, String taskHandler, Object payload,
                                                                Duration delay) {
                scheduledPayloadsTasks.computeIfAbsent(taskHandler, c -> new CopyOnWriteArraySet<>()).add(payload);
                return CompletableFuture.completedFuture(null);
            }
        };
        testSubject = new DistributeLicenseService(
                taskPublisher, mock(LicenseManager.class));
    }


    @Test
    public void distributeLicense() {
        testSubject.distributeLicense("myLicense".getBytes());
        assertEquals(1, scheduledPayloadsTasks.size());
        assertTrue(scheduledPayloadsTasks.containsKey(PrepareUpdateLicenseTask.class.getName()));
    }

    @Test
    public void prepareUpdateLicenseTask() {

        PrepareUpdateLicenseTask prepareUpdateLicenseTask = new PrepareUpdateLicenseTask(taskPublisher, clusterController);
        prepareUpdateLicenseTask.execute("","myLicense".getBytes());


        Set<UpdateLicenseTaskPayload> scheduledPayloads = scheduledPayloadsTasks.getOrDefault(UpdateLicenseTask.class.getName(), Collections.emptySet())
                .stream()
                .map(it -> (UpdateLicenseTaskPayload) it)
                .collect(Collectors.toSet());

        TestCase.assertEquals(2, scheduledPayloads.size());
        TestCase.assertEquals(1, scheduledPayloads.stream().filter(it -> it.getNodeName().equals("node1")).count());
        TestCase.assertEquals(1, scheduledPayloads.stream().filter(it -> it.getNodeName().equals("node2")).count());
    }

    @Test
    public void updateLicenseTaskAdminNode() {
        when(clusterController.getMe().getName()).thenReturn("adminNode");

        UpdateLicenseTaskPayload updateLicenseTaskPayload = new UpdateLicenseTaskPayload("adminNode", "myLicense".getBytes());

        UpdateLicenseTask updateLicenseTask = new UpdateLicenseTask(clusterController, applicationEventPublisher);
        updateLicenseTask.execute("",updateLicenseTaskPayload);

        verify(applicationEventPublisher, times(1)).publishEvent(any(ClusterEvents.LicenseUpdated.class));
    }

    @Test
    public void updateLicenseTaskRemoteConnectedNode() {
        when(clusterController.getMe().getName()).thenReturn("adminNode");

        RemoteConnection remoteConnection = mock(RemoteConnection.class, RETURNS_DEEP_STUBS);
        when(remoteConnection.isConnected()).thenReturn(true);
        when(remoteConnection.getClusterNode().getName()).thenReturn("remoteNode");

        when(clusterController.getRemoteConnection(eq("remoteNode"))).thenReturn(Optional.of(remoteConnection));

        UpdateLicenseTaskPayload updateLicenseTaskPayload = new UpdateLicenseTaskPayload("remoteNode", "myLicense".getBytes());

        UpdateLicenseTask updateLicenseTask = new UpdateLicenseTask(clusterController, applicationEventPublisher);
        updateLicenseTask.execute("",updateLicenseTaskPayload);

        verify(remoteConnection, times(1)).publish(any(ConnectorCommand.class));

        ArgumentCaptor<ConnectorCommand> captor = ArgumentCaptor.forClass(ConnectorCommand.class);

        verify(remoteConnection, times(1)).publish(captor.capture());

        String licenseContent = new String(captor.getValue().getUpdateLicense().getLicense().toByteArray());
        assertEquals(licenseContent, "myLicense");
    }

    @Test(expected = TransientException.class)
    public void updateLicenseTaskRemoteDisconnectedNode() {
        when(clusterController.getMe().getName()).thenReturn("adminNode");

        RemoteConnection remoteConnection = mock(RemoteConnection.class, RETURNS_DEEP_STUBS);

        when(remoteConnection.isConnected()).thenReturn(false);

        when(clusterController.getRemoteConnection(any())).thenReturn(Optional.of(remoteConnection));

        UpdateLicenseTaskPayload updateLicenseTaskPayload = new UpdateLicenseTaskPayload("remoteNode", "myLicense".getBytes());

        UpdateLicenseTask updateLicenseTask = new UpdateLicenseTask(clusterController, applicationEventPublisher);


        updateLicenseTask.execute("",updateLicenseTaskPayload);
    }


}
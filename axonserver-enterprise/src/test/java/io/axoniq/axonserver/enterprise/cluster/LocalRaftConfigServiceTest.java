package io.axoniq.axonserver.enterprise.cluster;

import com.google.protobuf.InvalidProtocolBufferException;
import io.axoniq.axonserver.access.application.ApplicationController;
import io.axoniq.axonserver.cluster.RaftGroup;
import io.axoniq.axonserver.cluster.RaftNode;
import io.axoniq.axonserver.config.MessagingPlatformConfiguration;
import io.axoniq.axonserver.config.SystemInfoProvider;
import io.axoniq.axonserver.enterprise.context.ContextController;
import io.axoniq.axonserver.enterprise.jpa.ClusterNode;
import io.axoniq.axonserver.exception.ErrorCode;
import io.axoniq.axonserver.exception.MessagingPlatformException;
import io.axoniq.axonserver.grpc.cluster.Node;
import io.axoniq.axonserver.grpc.internal.Context;
import io.axoniq.axonserver.grpc.internal.ContextApplication;
import io.axoniq.axonserver.grpc.internal.ContextConfiguration;
import io.axoniq.axonserver.grpc.internal.ContextMember;
import io.axoniq.axonserver.grpc.internal.ContextRole;
import io.axoniq.axonserver.grpc.internal.ContextUpdateConfirmation;
import io.axoniq.axonserver.grpc.internal.LoadBalanceStrategy;
import io.axoniq.axonserver.grpc.internal.NodeInfo;
import io.axoniq.axonserver.grpc.internal.NodeInfoWithLabel;
import io.axoniq.axonserver.grpc.internal.ProcessorLBStrategy;
import org.junit.*;
import org.mockito.stubbing.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * @author Marc Gathier
 */
public class LocalRaftConfigServiceTest {
    private LocalRaftConfigService testSubject;
    private ContextController contextcontroller;
    private ApplicationController applicationController;
    private AdminDB adminDB;
    private RaftNode adminNode;
    private FakeRaftGroupService fakeRaftGroupService = new FakeRaftGroupService();

    private ClusterNode createNode(String name) {
        return new ClusterNode(name, name, name, 1, 2,3);
    }

    private class AdminDB {
        private Map<String,io.axoniq.axonserver.enterprise.jpa.Context> contextMap = new ConcurrentHashMap<>();
        private Map<String,ClusterNode> nodeMap = new ConcurrentHashMap<>();

        public void addContext(String name, String... nodes) {
            io.axoniq.axonserver.enterprise.jpa.Context context = contextMap.computeIfAbsent(name, io.axoniq.axonserver.enterprise.jpa.Context::new);
            for (String node : nodes) {
                nodeMap.computeIfAbsent(node, n -> createNode(n)).addContext(context, node + "/" + context);
            }
        }


        public void applyEntry(String entryType, byte[] value) {
            if( entryType.equals(ContextConfiguration.class.getName())) {
                try {
                    ContextConfiguration contextConfiguration = ContextConfiguration.parseFrom(value);
                    io.axoniq.axonserver.enterprise.jpa.Context context = contextMap.get(contextConfiguration.getContext());
                    if( contextConfiguration.getNodesCount() == 0) {
                        contextMap.remove(context.getName());
                        return;
                    }

                    if( context == null) {
                        context = new io.axoniq.axonserver.enterprise.jpa.Context(contextConfiguration.getContext());
                        contextMap.put(context.getName(), context);
                    }
                    Map<String, ClusterNode> currentNodes = new HashMap<>();
                    context.getAllNodes().forEach(n -> currentNodes.put(n.getClusterNode().getName(), n.getClusterNode()) );
                    Map<String, NodeInfoWithLabel> newNodes = new HashMap<>();
                    contextConfiguration.getNodesList().forEach(n -> newNodes.put(n.getNode().getNodeName(), n));

                    Map<String, ClusterNode> clusterInfoMap = new HashMap<>();
                    for (NodeInfoWithLabel nodeInfo : contextConfiguration.getNodesList()) {
                        String nodeName = nodeInfo.getNode().getNodeName();
                        ClusterNode clusterNode = nodeMap.get(nodeName);
                        if( clusterNode == null) clusterNode = createNode(nodeName);
                        clusterInfoMap.put(nodeName, clusterNode);
                    }

                    io.axoniq.axonserver.enterprise.jpa.Context finalContext = context;
                    currentNodes.forEach((node, clusterNode) -> {
                        if( !newNodes.containsKey(node)) {
                            clusterNode.removeContext(finalContext.getName());
                        }
                    });
                    newNodes.forEach((node, nodeInfo) -> {
                        if( !currentNodes.containsKey(node)) {
                            clusterInfoMap.get(node).addContext(finalContext, nodeInfo.getLabel());
                        }
                    });

                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class GroupDB {
        private Map<String, String> nodes = new ConcurrentHashMap<>();

        private GroupDB(io.axoniq.axonserver.enterprise.jpa.Context context) {
            context.getAllNodes().forEach(ccn -> nodes.put(ccn.getClusterNodeLabel(), ccn.getClusterNode().getName()));
        }

        public GroupDB() {

        }
    }

    private class FakeRaftGroupService implements RaftGroupService {
        private Map<String, GroupDB> groupDBs = new ConcurrentHashMap<>();

        void add(io.axoniq.axonserver.enterprise.jpa.Context context) {
            groupDBs.put(context.getName(),new GroupDB(context));
        }

        @Override
        public CompletableFuture<ContextUpdateConfirmation> addNodeToContext(String context, Node node) {
            groupDBs.get(context).nodes.put(node.getNodeId(), node.getNodeName());
            ContextUpdateConfirmation.Builder contextUpdateConfirmation = ContextUpdateConfirmation.newBuilder()
                    .setSuccess(true);
            groupDBs.get(context).nodes.forEach((id,name) ->
                                                        contextUpdateConfirmation.addMembers(ContextMember.newBuilder().setNodeName(name).setNodeId(id)));

            return CompletableFuture.completedFuture(contextUpdateConfirmation.build());
        }

        @Override
        public CompletableFuture<Void> getStatus(Consumer<Context> contextConsumer) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<ContextConfiguration> configuration(String context) {
            return null;
        }

        @Override
        public CompletableFuture<Void> initContext(String context, List<Node> nodes) {
            GroupDB groupDB = new GroupDB();
            nodes.forEach(n -> groupDB.nodes.put(n.getNodeId(), n.getNodeName()));
            groupDBs.put(context, groupDB);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<ContextUpdateConfirmation> deleteNode(String context, String node) {
            groupDBs.get(context).nodes.remove(node);
            ContextUpdateConfirmation.Builder contextUpdateConfirmation = ContextUpdateConfirmation.newBuilder()
                                                                                                   .setSuccess(true);
            groupDBs.get(context).nodes.forEach((id,name) ->
                                                        contextUpdateConfirmation.addMembers(ContextMember.newBuilder().setNodeName(name).setNodeId(id)));

            return CompletableFuture.completedFuture(contextUpdateConfirmation.build());
        }

        @Override
        public CompletableFuture<Void> updateApplication(ContextApplication application) {
            return null;
        }

        @Override
        public CompletableFuture<Void> updateLoadBalancingStrategy(String context,
                                                                   LoadBalanceStrategy loadBalancingStrategy) {
            return null;
        }

        @Override
        public CompletableFuture<Void> updateProcessorLoadBalancing(String context,
                                                                    ProcessorLBStrategy processorLBStrategy) {
            return null;
        }

        @Override
        public CompletableFuture<Void> deleteLoadBalancingStrategy(String context,
                                                                   LoadBalanceStrategy loadBalancingStrategy) {
            return null;
        }

        @Override
        public CompletableFuture<Void> deleteContext(String context) {
            groupDBs.remove(context);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> appendEntry(String context, String name, byte[] toByteArray) {
            if( context.equals("_admin")) {
                adminDB.applyEntry(name, toByteArray);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Before
    public void setUp() {
        GrpcRaftController grpcRaftController = mock(GrpcRaftController.class);
        adminNode = mock(RaftNode.class);

        adminDB = new AdminDB();
        adminDB.addContext("sample", "node1");
        adminDB.addContext("_admin", "node1", "node2");
        adminDB.contextMap.forEach((key,context) -> fakeRaftGroupService.add(context));

        when(adminNode.appendEntry(any(), any())).then((Answer<CompletableFuture<Void>>) invocationOnMock -> {
            adminDB.applyEntry(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1));
                return CompletableFuture.completedFuture(null);
        });

        when(grpcRaftController.getRaftNode("_admin")).thenReturn(adminNode);
        contextcontroller = mock(ContextController.class);
        RaftGroupServiceFactory raftGroupServiceFactory = mock(RaftGroupServiceFactory.class);

        when(raftGroupServiceFactory.getRaftGroupService(anyString())).thenReturn(fakeRaftGroupService);
        when(raftGroupServiceFactory.getRaftGroupServiceForNode(anyString())).thenReturn(fakeRaftGroupService);
        when(raftGroupServiceFactory.getRaftGroupServiceForNode(any(ClusterNode.class))).thenReturn(fakeRaftGroupService);
        applicationController = mock(ApplicationController.class);
        MessagingPlatformConfiguration messagingPlatformConfiguration = new MessagingPlatformConfiguration(new SystemInfoProvider() {
            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public String getHostName()  {
                return "localhost";
            }
        });
        when(contextcontroller.getNode(anyString())).then((Answer<ClusterNode>) invocationOnMock -> {
            String name = invocationOnMock.getArgument(0);
            return adminDB.nodeMap.get(name);
        });
        when(contextcontroller.getContext(anyString())).then((Answer<io.axoniq.axonserver.enterprise.jpa.Context>) invocationOnMock -> {
            String name = invocationOnMock.getArgument(0);
            return adminDB.contextMap.get(name);
        });
        RaftGroup fakeRaftGroup = mock(RaftGroup.class);
        when(fakeRaftGroup.localNode()).thenReturn(adminNode);
        when(grpcRaftController.initRaftGroup(anyString(), anyString(), anyString())).thenAnswer((Answer<RaftGroup>)invocationOnMock -> {
            GroupDB groupDB = new GroupDB();
            groupDB.nodes.put(invocationOnMock.getArgument(1), invocationOnMock.getArgument(2));
            fakeRaftGroupService.groupDBs.put(invocationOnMock.getArgument(0), groupDB);
            return fakeRaftGroup;
        });
        when(grpcRaftController.waitForLeader(any())).thenReturn(adminNode);
        when(adminNode.addNode(any())).thenReturn(CompletableFuture.completedFuture(null));
        testSubject = new LocalRaftConfigService(grpcRaftController, contextcontroller, raftGroupServiceFactory, applicationController, messagingPlatformConfiguration);
    }

    @Test
    public void addNodeToContext() {
        testSubject.addNodeToContext("sample", "node2");
        assertTrue(adminDB.contextMap.get("sample").getNodeNames().contains("node2"));
        assertTrue(fakeRaftGroupService.groupDBs.get("sample").nodes.containsValue("node2"));
    }

    @Test
    public void addNodeToNonExistingContext() {
        try {
            testSubject.addNodeToContext("myContext", "node1");
            fail("Expect exception");
        } catch( MessagingPlatformException mpe) {
            assertEquals(ErrorCode.CONTEXT_NOT_FOUND, mpe.getErrorCode());
        }
    }

    @Test
    public void addNonExistingNodeToContext() {
        try {
            testSubject.addNodeToContext("sample", "node3");
            fail("Expect exception");
        } catch( MessagingPlatformException mpe) {
            assertEquals(ErrorCode.NO_SUCH_NODE, mpe.getErrorCode());
        }
    }

    @Test
    public void addExistingMemberToContext() {
        testSubject.addNodeToContext("sample", "node1");
    }

    @Test
    public void deleteContext() {
        testSubject.deleteContext("sample");
        assertNull( adminDB.contextMap.get("sample"));
    }

    @Test
    public void deleteAdminContext() {
        try {
            testSubject.deleteContext("_admin");
            fail("Expect exception");
        } catch(MessagingPlatformException mpe) {
            assertEquals(ErrorCode.CANNOT_DELETE_INTERNAL_CONTEXT, mpe.getErrorCode());
        }
    }

    @Test
    public void deleteNonExistingContext() {
        try {
            testSubject.deleteContext("demo");
            fail("Expect exception");
        } catch(MessagingPlatformException mpe) {
            assertEquals(ErrorCode.CONTEXT_NOT_FOUND, mpe.getErrorCode());
        }
    }

    @Test
    public void deleteNodeFromContext() {
        testSubject.deleteNodeFromContext("_admin", "node2");
        assertEquals(1, adminDB.contextMap.get("_admin").getNodeNames().size());
        try {
            testSubject.deleteNodeFromContext("_admin", "node1");
            fail("Expect exception");
        } catch(MessagingPlatformException mpe) {
            assertEquals(ErrorCode.OTHER, mpe.getErrorCode());
        }
    }

    @Test
    public void deleteNode() {
        testSubject.deleteNode("node2");
        assertEquals(1, fakeRaftGroupService.groupDBs.get("_admin").nodes.size());
    }

    @Test
    public void addContext() {
        testSubject.addContext("second", Arrays.asList("node1", "node2"));
    }

    @Test
    public void join() {
        when(adminNode.isLeader()).thenReturn(true);
        testSubject.join(NodeInfo.newBuilder().setNodeName("node3").setInternalHostName("node3").setHostName("node3")
                                 .addContexts(ContextRole.newBuilder().setName("sample").setNodeLabel("sample/node3")).build());

    }

    @Test
    public void joinNewContext() {
        when(adminNode.isLeader()).thenReturn(true);
        testSubject.join(NodeInfo.newBuilder().setNodeName("node3").setInternalHostName("node3").setHostName("node3")
                                 .addContexts(ContextRole.newBuilder().setName("sample2").setNodeLabel("sample2/node3")).build());

    }

    @Test
    public void joinAllContexts() {
        when(adminNode.isLeader()).thenReturn(true);
        testSubject.join(NodeInfo.newBuilder().setNodeName("node3").setInternalHostName("node3").setHostName("node3").build());

    }

    @Test
    public void init() {
        adminDB.contextMap.clear();
        adminDB.nodeMap.clear();
        adminDB.nodeMap.put("localhost", createNode("localhost"));
        fakeRaftGroupService.groupDBs.clear();

        testSubject.init(Arrays.asList("default"));
        assertEquals(2, adminDB.contextMap.size());
        assertEquals(2,fakeRaftGroupService.groupDBs.size());

    }

}
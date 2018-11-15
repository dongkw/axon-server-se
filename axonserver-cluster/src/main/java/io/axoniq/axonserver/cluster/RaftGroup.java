package io.axoniq.axonserver.cluster;

import io.axoniq.axonserver.cluster.election.ElectionStore;
import io.axoniq.axonserver.cluster.replication.LogEntryStore;
import io.axoniq.axonserver.grpc.cluster.*;

import java.util.function.Function;

public interface RaftGroup {

    Registration onAppendEntries(Function<AppendEntriesRequest, AppendEntriesResponse> handler);

    Registration onInstallSnapshot(Function<InstallSnapshotRequest, InstallSnapshotResponse> handler);

    Registration onRequestVote(Function<RequestVoteRequest, RequestVoteResponse> handler);

    LogEntryStore localLogEntryStore();

    ElectionStore localElectionStore();

    RaftConfiguration raftConfiguration();

    RaftPeer peer(String hostName, int port);

    RaftNode localNode();

    default void connect() {
        localNode().start();
    }
}

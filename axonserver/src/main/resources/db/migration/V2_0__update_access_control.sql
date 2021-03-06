create table PATHS_TO_FUNCTIONS
(
    path     varchar(255) not null primary key,
    function varchar(255) not null
);

create table ROLES
(
    role        varchar(255) not null primary key,
    description varchar(244)
);

create table FUNCTION_ROLES
(
    id       BIGINT generated by default as identity,
    function varchar(255) not null,
    role     varchar(255) not null references roles (role),
);

alter table USER_ROLES
    add (
        CONTEXT varchar(255)
        );

alter table USER_ROLES
    alter column
        username varchar(45) null
;

insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/ListEvents', 'LIST_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/AppendEvent', 'APPEND_EVENT');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/AppendSnapshot', 'APPEND_SNAPSHOT');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/ListAggregateEvents', 'LIST_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/ListAggregateSnapshots', 'LIST_SNAPSHOTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/ReadHighestSequenceNr', 'READ_HIGHEST_SEQNR');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/GetFirstToken', 'GET_FIRST_TOKEN');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/GetLastToken', 'GET_LAST_TOKEN');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/GetTokenAt', 'GET_TOKEN_AT');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.event.EventStore/QueryEvents', 'SEARCH_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.command.CommandService/OpenStream', 'HANDLE_COMMANDS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.command.CommandService/Dispatch', 'DISPATCH_COMMAND');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.query.QueryService/OpenStream', 'HANDLE_QUERIES');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.query.QueryService/Query', 'DISPATCH_QUERY');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonserver.grpc.query.QueryService/Subscription', 'DISPATCH_SUBSCRIPTION_QUERY');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/ListEvents', 'LIST_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/AppendEvent', 'APPEND_EVENT');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/AppendSnapshot', 'APPEND_SNAPSHOT');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/ListAggregateEvents', 'LIST_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/ListAggregateSnapshots', 'LIST_SNAPSHOTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/ReadHighestSequenceNr', 'READ_HIGHEST_SEQNR');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/GetFirstToken', 'GET_FIRST_TOKEN');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/GetLastToken', 'GET_LAST_TOKEN');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/GetTokenAt', 'GET_TOKEN_AT');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axondb.grpc.EventStore/QueryEvents', 'SEARCH_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonhub.grpc.CommandService/OpenStream', 'HANDLE_COMMANDS');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonhub.grpc.CommandService/Dispatch', 'DISPATCH_COMMAND');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonhub.grpc.QueryService/OpenStream', 'HANDLE_QUERIES');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonhub.grpc.QueryService/Query', 'DISPATCH_QUERY');
insert into PATHS_TO_FUNCTIONS
values ('io.axoniq.axonhub.grpc.QueryService/Subscription', 'DISPATCH_SUBSCRIPTION_QUERY');
insert into PATHS_TO_FUNCTIONS
values ('GET:/internal/context/[^/]*/stepdown', 'RAFT_STEPDOWN');
insert into PATHS_TO_FUNCTIONS
values ('GET:/internal/raft/applications', 'RAFT_LIST_APPLICATIONS');
insert into PATHS_TO_FUNCTIONS
values ('POST:/internal/raft/context/[^/]*/cleanLogEntries/.*', 'RAFT_CLEAN_LOG');
insert into PATHS_TO_FUNCTIONS
values ('POST:/internal/raft/context/[^/]*/start', 'RAFT_START_CONTEXT');
insert into PATHS_TO_FUNCTIONS
values ('POST:/internal/raft/context/[^/]*/stop', 'RAFT_STOP_CONTEXT');
insert into PATHS_TO_FUNCTIONS
values ('GET:/internal/raft/groups', 'RAFT_LIST_CONTEXTS');
insert into PATHS_TO_FUNCTIONS
values ('GET:/internal/raft/members/.*', 'RAFT_LIST_CONTEXT_MEMBERS');
insert into PATHS_TO_FUNCTIONS
values ('GET:/internal/raft/status', 'RAFT_GET_STATUS');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/applications', 'CREATE_APP');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/applications/.*', 'GET_APP_DETAILS');
insert into PATHS_TO_FUNCTIONS
values ('DELETE:/v1/applications/.*', 'DELETE_APP');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/applications/.*', 'RENEW_APP_TOKEN');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/backup/createControlDbBackup', 'CREATE_CONTROLDB_BACKUP');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/backup/log/filenames', 'LIST_BACKUP_LOGFILES');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/backup/filenames', 'LIST_BACKUP_FILENAMES');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/cluster', 'LIST_NODES');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/cluster', 'ADD_NODE_TO_CLUSTER');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/cluster/.*','GET_NODE');
insert into PATHS_TO_FUNCTIONS
values ('DELETE:/v1/cluster/.*', 'REMOVE_NODE_FROM_CLUSTER');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/commands/count', 'GET_COMMANDS_COUNT');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/commands/queues', 'GET_COMMANDS_QUEUE');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/commands/run', 'DISPATCH_COMMAND');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/components/clients','GET_CLIENTS');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/components/[^/]*/subscription-query-metric','GET_SUBSCRIPTION_METRIC');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/components/[^/]*/subscription-query-metric/query/.*','GET_SUBSCRIPTION_METRIC');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/components/[^/]*/commands','GET_CLIENT_APP_COMMANDS');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/components/[^/]*/instances','GET_CLIENT_APP_INSTANCES');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/components/[^/]*/processors','GET_EVENT_PROCESSORS');
-- insert into PATHS_TO_FUNCTIONS
-- values ('GET:/v1/components/[^/]*/processors/loadbalance/strategies', 'GET_EVENT_PROCESSORS_STRATEGIES');
-- insert into PATHS_TO_FUNCTIONS
-- values ('GET:/v1/components/[^/]*/processors/[^/]*/loadbalance', 'GET_EVENT_PROCESSOR_STRATEGY');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/components/[^/]*/processors/[^/]*/loadbalance', 'REBALANCE_PROCESSOR');
insert into PATHS_TO_FUNCTIONS
values ('PUT:/v1/components/[^/]*/processors/[^/]*/loadbalance', 'SET_EVENT_PROCESSOR_STRATEGY');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/components/[^/]*/processors/[^/]*/pause', 'PAUSE_EVENT_PROCESSOR');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/components/[^/]*/processors/[^/]*/segments/merge', 'MERGE_EVENT_PROCESSOR_SEGMENTS');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/components/[^/]*/processors/[^/]*/segments/split', 'SPLIT_EVENT_PROCESSOR_SEGMENTS');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/components/[^/]*/processors/[^/]*/segments/[^/]*/move', 'MOVE_EVENT_PROCESSOR_SEGMENT');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/components/[^/]*/processors/[^/]*/start', 'START_EVENT_PROCESSOR');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/components/[^/]*/queries','GET_CLIENT_APP_QUERIES');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/context', 'CREATE_CONTEXT');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/context/init', 'INIT_CLUSTER');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/context/[^/]*/.*', 'ADD_NODE_TO_CONTEXT');
insert into PATHS_TO_FUNCTIONS
values ('DELETE:/v1/context/[^/]*/.*', 'DELETE_NODE_FROM_CONTEXT');
insert into PATHS_TO_FUNCTIONS
values ('DELETE:/v1/context/.*', 'DELETE_CONTEXT');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/events', 'LIST_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/events', 'APPEND_EVENT');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/instructions', 'RECONNECT_CLIENT');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/local/events', 'LOCAL_GET_LAST_EVENT');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/local/snapshots', 'LOCAL_GET_LAST_SNAPSHOT');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/processors/loadbalance/strategies','LIST_LOADBALANCE_STRATEGIES');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/processors/loadbalance/strategies', '-');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/processors/loadbalance/strategies/factories', '-');
insert into PATHS_TO_FUNCTIONS
values ('DELETE:/v1/processors/loadbalance/strategies/.*', '-');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/processors/loadbalance/strategies/[^/]*/factoryBean/.*', '-');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/processors/loadbalance/strategies/[^/]*/label/.*', '-');
insert into PATHS_TO_FUNCTIONS
values ('PATCH:/v1/processors/[^/]*/loadbalance', 'REBALANCE_PROCESSOR');
insert into PATHS_TO_FUNCTIONS
values ('PUT:/v1/processors/[^/]*/autoloadbalance', 'SET_EVENT_PROCESSOR_STRATEGY');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public','LIST_NODES');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/public/applications', 'LIST_APPS');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/command-metrics','GET_COMMAND_METRICS');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/public/context', 'LIST_CONTEXTS');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/license','GET_LICENSE');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/me','GET_CURRENT_NODE');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/mycontexts','GET_MY_CONTEXTS');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/overview','GET_OVERVIEW');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/pages','GET_PLUGIN_PAGES');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/query-metrics','GET_QUERY_METRICS');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/status','GET_NODE_STATUS');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/public/user','GET_CURRENT_USER');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/public/users', 'LIST_USERS');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/queries', 'LIST_QUERIES');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/queries/run', 'DISPATCH_QUERY');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/roles/application','LIST_APP_ROLES');
-- insert into PATHS_TO_FUNCTIONS values ('GET:/v1/roles/user','LIST_USER_ROLES');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/search', 'SEARCH_EVENTS');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/snapshot', '-');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/snapshots', 'APPEND_SNAPSHOT');
insert into PATHS_TO_FUNCTIONS
values ('GET:/v1/snapshots', 'LIST_SNAPSHOTS');
insert into PATHS_TO_FUNCTIONS
values ('POST:/v1/users', 'MERGE_USER');
insert into PATHS_TO_FUNCTIONS
values ('DELETE:/v1/users/.*', 'DELETE_USER');

insert into ROLES
values ('READ_EVENTS', 'Read events/snapshot from Event Store');
insert into ROLES
values ('PUBLISH_EVENTS', 'Store events/snapshots in Event Store');
insert into ROLES
values ('DISPATCH_COMMANDS', 'Send commands');
insert into ROLES
values ('SUBSCRIBE_COMMAND_HANDLER', 'Handle commands');
insert into ROLES
values ('DISPATCH_QUERY', 'Send queries and subscription queries');
insert into ROLES
values ('SUBSCRIBE_QUERY_HANDLER', 'Handle queries and emit updates');
insert into ROLES
values ('ADMIN', 'Global administrator');
insert into ROLES
values ('CONTEXT_ADMIN', 'Context administrator');
insert into ROLES
values ('MONITOR', 'Monitor status');
-- Legacy roles
insert into ROLES
values ('READ', 'Deprecated - read events and perform/handle queries');
insert into ROLES
values ('WRITE', 'Deprecated - store events and perform/handle commands');

insert into FUNCTION_ROLES(function, role)
values ('ADD_NODE_TO_CONTEXT', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('APPEND_EVENT', 'PUBLISH_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('APPEND_EVENT', 'WRITE');
insert into FUNCTION_ROLES(function, role)
values ('APPEND_SNAPSHOT', 'PUBLISH_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('APPEND_SNAPSHOT', 'WRITE');
insert into FUNCTION_ROLES(function, role)
values ('CREATE_APP', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('CREATE_CONTEXT', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('CREATE_CONTROLDB_BACKUP', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('DELETE_APP', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('DELETE_CONTEXT', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('DELETE_NODE_FROM_CONTEXT', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('DELETE_USER', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('DISPATCH_COMMAND', 'DISPATCH_COMMANDS');
insert into FUNCTION_ROLES(function, role)
values ('DISPATCH_COMMAND', 'WRITE');
insert into FUNCTION_ROLES(function, role)
values ('DISPATCH_QUERY', 'DISPATCH_QUERY');
insert into FUNCTION_ROLES(function, role)
values ('DISPATCH_QUERY', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('DISPATCH_SUBSCRIPTION_QUERY', 'DISPATCH_QUERY');
insert into FUNCTION_ROLES(function, role)
values ('DISPATCH_SUBSCRIPTION_QUERY', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('GET_APP_DETAILS', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('GET_COMMANDS_COUNT', 'MONITOR');
insert into FUNCTION_ROLES(function, role)
values ('GET_COMMANDS_QUEUE', 'MONITOR');
-- insert into FUNCTION_ROLES(function, role)
-- values ('GET_EVENT_PROCESSORS', 'CONTEXT_ADMIN');
-- insert into FUNCTION_ROLES(function, role)
-- values ('GET_EVENT_PROCESSORS_STRATEGIES', 'CONTEXT_ADMIN');
-- insert into FUNCTION_ROLES(function, role)
-- values ('GET_EVENT_PROCESSOR_STRATEGY', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('GET_FIRST_TOKEN', 'READ_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('GET_FIRST_TOKEN', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('GET_LAST_TOKEN', 'READ_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('GET_LAST_TOKEN', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('GET_TOKEN_AT', 'READ_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('GET_TOKEN_AT', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('HANDLE_COMMANDS', 'SUBSCRIBE_COMMAND_HANDLER');
insert into FUNCTION_ROLES(function, role)
values ('HANDLE_COMMANDS', 'WRITE');
insert into FUNCTION_ROLES(function, role)
values ('HANDLE_QUERIES', 'SUBSCRIBE_QUERY_HANDLER');
insert into FUNCTION_ROLES(function, role)
values ('HANDLE_QUERIES', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('LIST_APPS', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_BACKUP_FILENAMES', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_BACKUP_FILENAMES', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_BACKUP_LOGFILES', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_BACKUP_LOGFILES', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_CONTEXTS', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_EVENTS', 'READ_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('LIST_EVENTS', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('LIST_NODES', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_QUERIES', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LIST_SNAPSHOTS', 'READ_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('LIST_SNAPSHOTS', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('LIST_USERS', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LOCAL_GET_LAST_EVENT', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('LOCAL_GET_LAST_SNAPSHOT', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('MERGE_EVENT_PROCESSOR_SEGMENTS', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('MERGE_USER', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('MOVE_EVENT_PROCESSOR_SEGMENT', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('PAUSE_EVENT_PROCESSOR', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_CLEAN_LOG', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_GET_STATUS', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_LIST_APPLICATIONS', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_LIST_CONTEXTS', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_LIST_CONTEXT_MEMBERS', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_START_CONTEXT', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_STEPDOWN', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RAFT_STOP_CONTEXT', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('READ_HIGHEST_SEQNR', 'READ_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('RECONNECT_CLIENT', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('REMOVE_NODE_FROM_CLUSTER', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('RENEW_APP_TOKEN', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('SEARCH_EVENTS', 'READ_EVENTS');
insert into FUNCTION_ROLES(function, role)
values ('SEARCH_EVENTS', 'READ');
insert into FUNCTION_ROLES(function, role)
values ('SET_EVENT_PROCESSOR_STRATEGY', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('SPLIT_EVENT_PROCESSOR_SEGMENTS', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('START_EVENT_PROCESSOR', 'CONTEXT_ADMIN');

insert into FUNCTION_ROLES(function, role)
values ('INIT_CLUSTER', 'ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('ADD_NODE_TO_CLUSTER', 'ADMIN');

insert into FUNCTION_ROLES(function, role)
values ('REBALANCE_PROCESSOR', 'CONTEXT_ADMIN');
insert into FUNCTION_ROLES(function, role)
values ('AUTO_REBALANCE_PROCESSOR', 'CONTEXT_ADMIN');
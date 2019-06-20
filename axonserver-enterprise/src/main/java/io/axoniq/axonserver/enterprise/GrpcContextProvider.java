/*
 * Copyright (c) 2017-2019 AxonIQ B.V. and/or licensed to AxonIQ B.V.
 * under one or more contributor license agreements.
 *
 *  Licensed under the AxonIQ Open Source License Agreement v1.0;
 *  you may not use this file except in compliance with the license.
 *
 */

package io.axoniq.axonserver.enterprise;

import io.axoniq.axonserver.grpc.ContextProvider;
import io.axoniq.axonserver.grpc.GrpcMetadataKeys;
import io.axoniq.axonserver.topology.Topology;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Controller;

/**
 * Implementation of {@link ContextProvider} that retrieves the context from gRPC threadlocal.
 * @author Marc Gathier
 * @since 4.0
 */
@Primary
@Controller
public class GrpcContextProvider implements ContextProvider {

    @Override
    public String getContext() {
        String context = GrpcMetadataKeys.CONTEXT_KEY.get();

        return context == null ? Topology.DEFAULT_CONTEXT : context;
    }
}
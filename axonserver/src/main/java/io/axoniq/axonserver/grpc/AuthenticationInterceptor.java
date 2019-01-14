package io.axoniq.axonserver.grpc;

import io.axoniq.axonserver.AxonServerAccessController;
import io.axoniq.axonserver.exception.ErrorCode;
import io.grpc.*;

/**
 * @author Marc Gathier
 */
public class AuthenticationInterceptor implements ServerInterceptor{
    private final AxonServerAccessController axonHubAccessController;

    public AuthenticationInterceptor(AxonServerAccessController axonHubAccessController) {
        this.axonHubAccessController = axonHubAccessController;
    }

    @Override
    public <T, R> ServerCall.Listener<T> interceptCall(ServerCall<T, R> serverCall, Metadata metadata, ServerCallHandler<T, R> serverCallHandler) {
        String token = metadata.get(GrpcMetadataKeys.TOKEN_KEY);
        StatusRuntimeException sre = null;
        String context = GrpcMetadataKeys.CONTEXT_KEY.get();

        if( token == null) {
            sre = GrpcExceptionBuilder.build(ErrorCode.AUTHENTICATION_TOKEN_MISSING, "Token missing");
        } else if( ! axonHubAccessController.allowed( serverCall.getMethodDescriptor().getFullMethodName(), context, token)) {
            sre = GrpcExceptionBuilder.build(ErrorCode.AUTHENTICATION_INVALID_TOKEN, "Invalid token for " + serverCall.getMethodDescriptor().getFullMethodName());
        }

        if( sre != null) {
            serverCall.close(sre.getStatus(), sre.getTrailers());
            return new ServerCall.Listener<T>() {};
        }

        Context updatedGrpcContext = Context.current().withValue(GrpcMetadataKeys.TOKEN_CONTEXT_KEY, token);
        return Contexts.interceptCall(updatedGrpcContext, serverCall, metadata, serverCallHandler);
    }
}

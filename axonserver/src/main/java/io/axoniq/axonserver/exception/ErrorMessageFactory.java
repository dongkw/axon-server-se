package io.axoniq.axonserver.exception;

import io.axoniq.axonserver.grpc.ErrorMessage;

/**
 * @author Marc Gathier
 */
public class ErrorMessageFactory {
    public static ErrorMessage build(String message) {
        return build(message, "AxonHub");
    }

    public static ErrorMessage build(String message, String location) {
        return ErrorMessage.newBuilder().setLocation(location).setMessage(message).build();
    }
}

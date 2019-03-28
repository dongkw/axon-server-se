package io.axoniq.axonserver.rest.json;

import com.google.protobuf.ByteString;
import io.axoniq.axonserver.grpc.SerializedObject;
import io.axoniq.axonserver.util.StringUtils;

/**
 * @author Marc Gathier
 */
public class SerializedObjectJson {
    private String type;
    private String data;
    private String revision;

    public SerializedObjectJson() {

    }
    public SerializedObjectJson(SerializedObject payload) {
        type = payload.getType();
        data = payload.getData().toStringUtf8();
        revision = payload.getRevision();
    }

    public SerializedObject asSerializedObject() {
        return SerializedObject.newBuilder()
                               .setData(ByteString.copyFromUtf8(data))
                               .setType(type)
                               .setRevision(StringUtils.getOrDefault(revision, ""))
                               .build();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}

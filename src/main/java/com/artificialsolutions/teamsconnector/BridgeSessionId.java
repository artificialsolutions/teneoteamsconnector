package com.artificialsolutions.teamsconnector;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import com.artificialsolutions.common.JsonUtils;

public class BridgeSessionId implements Serializable {

    private static final long serialVersionUID = -2040586485067270877L;

    private final String objectId;
    private final String channelId;
    private final int hash;

    public BridgeSessionId(final String objectId, final String channelId) {
        this.objectId = objectId;
        this.channelId = channelId;
        this.hash = (objectId == null ? 0 : objectId.hashCode()) + (channelId == null ? 0 : channelId.hashCode());
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object x) {
        return x == this ? true : ((x instanceof BridgeSessionId) && hashCode() == x.hashCode() && StringUtils.equals(objectId, ((BridgeSessionId)x).objectId) && StringUtils.equals(channelId, ((BridgeSessionId)x).channelId));
    }

    @Override
    public String toString() {
        final StringBuilder z = new StringBuilder();
        z.append("{\"objectId\":");
        if (objectId == null) z.append("null");
        else JsonUtils.append(z.append('"'), objectId).append('"');
        z.append(",\"channelId\":");
        if (channelId == null) z.append("null");
        else JsonUtils.append(z.append('"'), channelId).append('"');
        return z.append('}').toString();
    }
}

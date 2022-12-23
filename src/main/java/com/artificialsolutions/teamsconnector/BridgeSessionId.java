package com.artificialsolutions.teamsconnector;


import java.io.Serializable;

import com.artificialsolutions.common.JsonUtils;


/**
 * Session ID for the bridge session, i.e. the session for a particular Teams interaction (conversion) started by a
 * particular user.
 */
public class BridgeSessionId implements Serializable {

    /**
     * Serial version ID for object (de)serialization.
     */
    private static final long serialVersionUID = -8467487829338348633L;

    /**
     * Account object ID within Azure Active Directory, as per
     * {@link com.microsoft.bot.schema.ChannelAccount#getAadObjectId()}
     */
    private final String objectId;

    /**
     * Channel id for the user or bot on this channel, as per
     * {@link com.microsoft.bot.schema.ChannelAccount#getId()}
     */
    private final String channelId;

    /**
     * The hash code.
     */
    private final int hash;

    /**
     * Creates an instance of this class.
     * 
     * @param objectId Account object ID within Azure Active Directory, as per
     * {@link com.microsoft.bot.schema.ChannelAccount#getAadObjectId()}
     * @param channelId Channel id for the user or bot on this channel, as per
     * {@link com.microsoft.bot.schema.ChannelAccount#getId()}
     */
    public BridgeSessionId(final String objectId, final String channelId) {
        this.objectId = objectId;
        this.channelId = channelId;
        this.hash = (objectId == null ? 0 : objectId.hashCode()) ^ (channelId == null ? 0 : channelId.hashCode());
    }

    /**
     * A hash code value for this object.
     * 
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * Compares this instance to the specified object. The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code BridgeSessionId} object that represents the same content as this object.
     *
     * @param x The object to compare this instance against.
     *
     * @return {@code true} if the given object represents a {@code BridgeSessionId} equivalent to this instance,
     * {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object x) {
        return x == this || (hashCode() == x.hashCode() && (x instanceof BridgeSessionId y) && equalStrings(objectId, y.objectId) && equalStrings(channelId, y.channelId));
    }

    /**
     * A string representation of this object. To be used for debug purposes only.
     */
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

    /**
     * Compares two strings null-safely.
     * 
     * @param s1 the first string or {@code null}.
     * @param s2 the second string or {@code null}.
     * 
     * @return {@code true} if either both {@code s1} and {@code s2} are {@code null} or both are {@code String}
     * instances and are equal.
     */
    private static boolean equalStrings(final String s1, final String s2) {
        return s1 == s2 || (s1 != null && s1.equals(s2));
    }
}

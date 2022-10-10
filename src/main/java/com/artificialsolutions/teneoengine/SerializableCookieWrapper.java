package com.artificialsolutions.teneoengine;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpCookie;
import java.util.UUID;

/**
 * A Serializable wrapper that contains a {@link java.net.HttpCookie} instance, which is not
 * Serializable. When an instance of this class is serializad/deserialized, the cookie is also
 * saved/reconstituted from the stream.
 */
public class SerializableCookieWrapper implements Serializable {

    /**
     * Serial version ID for object (de)serialization.
     */
    private static final long serialVersionUID = -6728275640862020173L;

    /**
     * The identifier of the instance.
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * The time point in milliseconds as per {@link java.lang.System#currentTimeMillis()} when the
     * cookie is assumed to have been constructed. This value is then used to adjust the max age of the
     * cookie when it is reconstituted from the stream during deserialization.
     */
    private final long estimatedCookieConstructionTimePoint;

    /**
     * The cookie.
     */
    private transient HttpCookie cookie;

    /**
     * Constructs an instance of the class.
     * 
     * @param cookie the cookie held by this instance.
     */
    public SerializableCookieWrapper(final HttpCookie cookie) {
        this.estimatedCookieConstructionTimePoint = System.currentTimeMillis() - 500;
        this.cookie = cookie;
    }

    /**
     * Returns the cookie held by this instance.
     * 
     * @return the cookie held by this instance.
     */
    public HttpCookie getCookie() {
        return cookie;
    }

    /**
     * The hash code of this instance which is equal to that of the contained cookie.
     * 
     * @see java.net.HttpCookie#hashCode()
     */
    @Override
    public int hashCode() {
        return cookie.hashCode();
    }

    /**
     * Compares two instances.
     * 
     * @return {@code true} if the cookies contained by both instances are equal in the sense of
     * {@link java.net.HttpCookie#equals(Object)} and {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object that) {
        return (this == that) || ((that instanceof SerializableCookieWrapper) && ((SerializableCookieWrapper)that).cookie.equals(cookie));
    }

    /**
     * Checks if both instances have the same internal UUID and cookie.
     * 
     * @param that another instance.
     * 
     * @return {@code true} if both instances have the same internal ID and cookie, {@code false} otherwise.
     */
    public boolean isSameAs(final SerializableCookieWrapper that) {
        return (this == that) || (uuid.equals(that.uuid) && cookie.equals(that.cookie));
    }

    /**
     * The string representation of this instance which is equal to that of the contained cookie as per
     * {@link java.net.HttpCookie#toString()}.
     * 
     * @return string representation of this instance.
     */
    @Override
    public String toString() {
        return cookie.toString();
    }

    /**
     * Saves this object to a stream (i.e., serializes it).
     * 
     * @serialData since Cookie is non-Serializable, its content is saved separately.
     *
     * @param oos the stream.
     * 
     * @throws IOException if an I/O error occurs.
     */
    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();

        oos.writeObject(cookie.getName());
        oos.writeObject(cookie.getValue());
        oos.writeObject(cookie.getComment());
        oos.writeObject(cookie.getCommentURL());
        oos.writeObject(cookie.getDomain());
        oos.writeObject(cookie.getPath());
        oos.writeObject(cookie.getPortlist());

        oos.writeBoolean(cookie.getDiscard());
        oos.writeBoolean(cookie.getSecure());
        oos.writeBoolean(cookie.isHttpOnly());

        oos.writeInt(cookie.getVersion());

        oos.writeLong(cookie.getMaxAge());
    }

    /**
     * Reconstitutes this object from a stream (i.e., deserializes it).
     * 
     * @serialData the Cookie is non-Serializable so its content is restored here separately and its max
     * age is recalculated on the best-effort basis.
     *
     * @param ois the stream.
     * 
     * @throws IOException if the class of a serialized object could not be found.
     * @throws ClassNotFoundException if an I/O error occurs.
     */
    private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        final String name = (String)ois.readObject(), value = (String)ois.readObject();
        cookie = new HttpCookie(name, value);

        cookie.setComment((String)ois.readObject());
        cookie.setCommentURL((String)ois.readObject());
        cookie.setDomain((String)ois.readObject());
        cookie.setPath((String)ois.readObject());
        cookie.setPortlist((String)ois.readObject());

        cookie.setDiscard(ois.readBoolean());
        cookie.setSecure(ois.readBoolean());
        cookie.setHttpOnly(ois.readBoolean());

        cookie.setVersion(ois.readInt());

        long maxAgeSecs = ois.readLong();
        if (maxAgeSecs > 0) {
            maxAgeSecs -= (System.currentTimeMillis() - estimatedCookieConstructionTimePoint) / 1000;
            if (maxAgeSecs < 0) maxAgeSecs = 0;
        }
        cookie.setMaxAge(maxAgeSecs);
    }
}

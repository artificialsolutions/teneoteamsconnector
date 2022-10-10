package com.artificialsolutions.teneoengine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Serializable in-memory implementation of {@link java.net.CookieStore}.
 */
public class TeneoEngineCookieStore implements Serializable, CookieStore {

    /**
     * Serial version ID for object (de)serialization.
     */
    private static final long serialVersionUID = -8163525301237312094L;

    /**
     * The reentrant lock to handle synchronizations.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Mapping from URI to cookie wrappers. Cookie wrappers are here an array list since the number of
     * cookies collected here is normally very small (usually one or two cookies) so there is no need to
     * use sets which are more heavy-weight implementation-wise than array lists.
     */
    private transient volatile Map<URI, ArrayList<SerializableCookieWrapper>> uriToCookieWrappers;

    /**
     * Cookie wrappers not associated with any URI. Cookie wrappers are here an array list since the
     * number of cookies collected here is normally very small (usually one or two cookies) so there is
     * no need to use sets which are more heavy-weight implementation-wise than array lists.
     */
    private transient volatile ArrayList<SerializableCookieWrapper> domainCookieWrappers;

    /**
     * The logger.
     */
    private transient Logger logger;

    /**
     * Creates an instance of this class.
     */
    public TeneoEngineCookieStore() {
        uriToCookieWrappers = new HashMap<>(4);
        domainCookieWrappers = new ArrayList<>(4);
        logger = LoggerFactory.getLogger(TeneoEngineCookieStore.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final URI uri, final HttpCookie cookie) throws NullPointerException {
        if (cookie.getDomain() != null && uri != null && !isDomainMatched(cookie, uri)) {
            logger.warn("Cookie {} does not domain-match URI {}", cookie, uri);
        }
        final boolean expired = cookie.hasExpired();
        lock.lock();
        try {
            if (expired) {
                if (cookie.getDomain() != null) removeWrapper(domainCookieWrappers, cookie);
                if (uri != null) {
                    final URI effectiveUri = getEffectiveURI(uri);
                    final ArrayList<SerializableCookieWrapper> wrappers = uriToCookieWrappers.get(effectiveUri);
                    if (wrappers != null) {
                        removeWrapper(wrappers, cookie);
                        if (wrappers.isEmpty()) uriToCookieWrappers.remove(effectiveUri);
                    }
                }
            } else {
                final SerializableCookieWrapper cwr = new SerializableCookieWrapper(cookie);
                if (cookie.getDomain() != null) addReplaceWrapper(domainCookieWrappers, cwr);
                if (uri != null) {
                    final URI effectiveUri = getEffectiveURI(uri);
                    ArrayList<SerializableCookieWrapper> wrappers = uriToCookieWrappers.get(effectiveUri);
                    if (wrappers != null) addReplaceWrapper(wrappers, cwr);
                    else {
                        uriToCookieWrappers.put(effectiveUri, wrappers = new ArrayList<SerializableCookieWrapper>(4));
                        wrappers.add(cwr);
                    }
                }
                // The java.net.CookieStore documentation does not say what to do if the cookie
                // has both a domain (which can even be incongruent with the URI) and a URI. The
                // assumption implemented here it that it should be added both as a URI linked
                // cookie and as a domain linked one. A wrapper instance with the same ID is added
                // in this case, which makes it possible to remove the cookie from the domain-based
                // repo when it is removed from the URI based one.
            }
        } finally {
            lock.unlock();
            if (logger.isDebugEnabled()) {
                if (expired) logger.debug("add() called for URI {} and expired cookie {}, URI repo: {}, domain repo: {}", uri, cookie, uriToCookieWrappers, domainCookieWrappers);
                else logger.debug("add() called for URI {} and valid cookie {}, URI repo: {}, domain repo: {}", uri, cookie, uriToCookieWrappers, domainCookieWrappers);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HttpCookie> get(final URI uri) throws NullPointerException {
        if (uri == null) throw new NullPointerException("uri==null");
        List<HttpCookie> cookies = null;
        lock.lock();
        try {
            final boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
            cookies = new ArrayList<>();
            for (int n = domainCookieWrappers.size() - 1; n >= 0; n--) {
                final HttpCookie c = domainCookieWrappers.get(n).getCookie();
                if (c.hasExpired()) domainCookieWrappers.remove(n);
                else if ((secureLink || !c.getSecure()) && isDomainMatched(c, uri) && !cookies.contains(c)) cookies.add(c);
            }
            if (uriToCookieWrappers.size() > 0) {
                final URI effectiveUri = getEffectiveURI(uri);
                final ArrayList<SerializableCookieWrapper> wrappers = uriToCookieWrappers.get(effectiveUri);
                if (wrappers != null) {
                    for (int n = wrappers.size() - 1; n >= 0; n--) {
                        final HttpCookie c = wrappers.get(n).getCookie();
                        if (c.hasExpired()) wrappers.remove(n);
                        else if ((secureLink || !c.getSecure()) && !cookies.contains(c)) cookies.add(c);
                    }
                    if (wrappers.isEmpty()) uriToCookieWrappers.remove(effectiveUri);
                }
            }
        } finally {
            lock.unlock();
            if (cookies == null) logger.warn("get() called for URI {}, no chance to obtain cookies, URI repo: {}, domain repo: {}", uri, uriToCookieWrappers, domainCookieWrappers);
            else if (logger.isDebugEnabled()) {
                logger.debug("get() called for URI {}, obtained {} cookies: {}, URI repo: {}, domain repo: {}", uri, cookies.size(), cookies, uriToCookieWrappers, domainCookieWrappers);
            }
        }
        return cookies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HttpCookie> getCookies() {
        List<HttpCookie> cookies = null;
        lock.lock();
        try {
            cookies = new ArrayList<>();
            if (domainCookieWrappers.size() > 0) {
                for (int n = domainCookieWrappers.size() - 1; n >= 0; n--) {
                    final HttpCookie c = domainCookieWrappers.get(n).getCookie();
                    if (c.hasExpired()) domainCookieWrappers.remove(n);
                    else if (!cookies.contains(c)) cookies.add(c);
                }
            }
            if (uriToCookieWrappers.size() > 0) {
                final Iterator<ArrayList<SerializableCookieWrapper>> iter = uriToCookieWrappers.values().iterator();
                do {
                    final ArrayList<SerializableCookieWrapper> wrappers = iter.next();
                    for (int n = wrappers.size() - 1; n >= 0; n--) {
                        final HttpCookie c = wrappers.get(n).getCookie();
                        if (c.hasExpired()) wrappers.remove(n);
                        else if (!cookies.contains(c)) cookies.add(c);
                    }
                    if (wrappers.isEmpty()) iter.remove();
                } while (iter.hasNext());
            }
        } finally {
            lock.unlock();
            if (cookies == null) logger.warn("getCookies() called, no chance to obtain cookies, URI repo: {}, domain repo: {}", uriToCookieWrappers, domainCookieWrappers);
            else if (logger.isDebugEnabled()) {
                logger.debug("getCookies() called, obtained {} cookies: {}, URI repo: {}, domain repo: {}", cookies.size(), cookies, uriToCookieWrappers, domainCookieWrappers);
            }
        }
        return cookies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<URI> getURIs() {
        List<URI> uris = null;
        lock.lock();
        try {
            int n = uriToCookieWrappers.size();
            uris = new ArrayList<>(n);
            if (n > 0) {
                final Iterator<Map.Entry<URI, ArrayList<SerializableCookieWrapper>>> iter = uriToCookieWrappers.entrySet().iterator();
                do {
                    final Map.Entry<URI, ArrayList<SerializableCookieWrapper>> e = iter.next();
                    final ArrayList<SerializableCookieWrapper> wrappers = e.getValue();
                    for (n = wrappers.size() - 1; n >= 0; n--) {
                        final HttpCookie c = wrappers.get(n).getCookie();
                        if (c.hasExpired()) wrappers.remove(n);
                    }
                    if (wrappers.isEmpty()) iter.remove();
                    else uris.add(e.getKey());
                } while (iter.hasNext());
            }
        } finally {
            lock.unlock();
            if (uris == null) logger.warn("getURIs() called, no chance to obtain URIs");
            else if (logger.isDebugEnabled()) {
                logger.debug("getURIs() called, obtained {} URIs: {}, URI repo: {}, domain repo: {}", uris.size(), uris);
            }
        }
        return uris;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final URI uri, final HttpCookie cookie) {
        boolean bRemovedFromUri = false, bRemovedFromDomain = false;
        lock.lock();
        try {
            if (uri != null) {
                if (uriToCookieWrappers.size() > 0) {
                    final SerializableCookieWrapper removedWrapper;
                    final URI effectiveUri = getEffectiveURI(uri);
                    final ArrayList<SerializableCookieWrapper> wrappers = uriToCookieWrappers.get(effectiveUri);
                    if (wrappers == null) removedWrapper = null;
                    else {
                        if ((removedWrapper = removeWrapper(wrappers, cookie)) != null) bRemovedFromUri = true;
                        if (wrappers.isEmpty()) uriToCookieWrappers.remove(effectiveUri);
                    }
                    if (removedWrapper != null && cookie.getDomain() != null) {
                        // Checking for cookie.getDomain() != null is done to avoid the unnecessary
                        // loop since domainCookieWrappers can only contain cookies with domain
                        for (int n = domainCookieWrappers.size() - 1; n >= 0; n--) {
                            if (domainCookieWrappers.get(n).isSameAs(removedWrapper)) {
                                // Remove, only if it the same instance since it means it was added in one go
                                domainCookieWrappers.remove(n);
                                break;
                            }
                        }
                    }
                }
            } else {
                if (removeWrapper(domainCookieWrappers, cookie) != null) bRemovedFromDomain = true;
            }
        } finally {
            lock.unlock();
            if (logger.isDebugEnabled()) {
                logger.debug("remove() called for URI {} and cookie {}, removed from URI repo: {}, removed from domain repo: {}, URI repo: {}, domain repo: {}", uri, cookie, bRemovedFromUri, bRemovedFromDomain, uriToCookieWrappers, domainCookieWrappers);
            }
        }
        return bRemovedFromUri || bRemovedFromDomain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll() {
        if (logger.isDebugEnabled()) {
            logger.debug("Calling removeAll() for URI repo {} and domain remo {}", uriToCookieWrappers, domainCookieWrappers);
        }
        lock.lock();
        try {
            boolean bChanged = false;
            if (uriToCookieWrappers.size() > 0) {
                uriToCookieWrappers.clear();
                bChanged = true;
            }
            if (domainCookieWrappers.size() > 0) {
                domainCookieWrappers.clear();
                bChanged = true;
            }
            return bChanged;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the internal lock object, to be re-used in other synchronizations if needed.
     * 
     * @return the internal lock object.
     */
    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Returns a String representation of the content of this object. To be used for debug purposes
     * only.
     * 
     * @return a String representation of the content of this object.
     */
    @Override
    public String toString() {
        lock.lock();
        try {
            return getClass() + "{uriToCookieWrappers: " + uriToCookieWrappers + ", domainCookieWrappers: " + domainCookieWrappers + '}';
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets a URL with scheme and host only.
     * 
     * @param uri the full URL.
     * 
     * @return the URL with scheme and host only.
     */
    private URI getEffectiveURI(final URI uri) {
        try {
            // scheme:"http", authority:uri.getHost(), path, query, fragment are null:
            return new URI("http", uri.getHost(), null, null, null);
        } catch (final URISyntaxException ignored) {
            return uri;
        }
    }

    /**
     * Replaces the last cookie wrapper if the list already contains one or adds a new one otherwise.
     * 
     * @param wrappers the list of cookie wrappers.
     * @param cwr the cookie wrapper to add,
     */
    private static void addReplaceWrapper(final ArrayList<SerializableCookieWrapper> wrappers, final SerializableCookieWrapper cwr) {
        final int n = wrappers.lastIndexOf(cwr);
        if (n == -1) wrappers.add(cwr);
        else wrappers.set(n, cwr);
    }

    /**
     * Removes the last cookie wrapper containing the given cookie. Assumption is that there only one
     * wrapper with that cookie.
     * 
     * @param wrappers the list of cookie wrappers to remove the wrapper from.
     * @param c the cookie.
     * 
     * @return the wrapper instance removed or {@code null} if there was no wrapper containing the
     * cookie in question.
     */
    private static SerializableCookieWrapper removeWrapper(final ArrayList<SerializableCookieWrapper> wrappers, final HttpCookie c) {
        for (int n = wrappers.size() - 1; n >= 0; n--) {
            if (c.equals(wrappers.get(n).getCookie())) return wrappers.remove(n);
        }
        return null;
    }

    /**
     * Matches the cookie domain and a URI.
     * 
     * @param c the cookie.
     * @param uri the URI.
     * 
     * @return {@code true} if there is a match, {@code false} otherwise.
     */
    private static boolean isDomainMatched(final HttpCookie c, final URI uri) {
        // c.getVersion() can be 0 or 1
        return c.getVersion() == 0 ? netscapeDomainMatches(c.getDomain(), uri.getHost()) : HttpCookie.domainMatches(c.getDomain(), uri.getHost());
    }

    /**
     * Checks if the given domain and host match accoring to the logic implemented in the Netscape
     * browser.
     * 
     * @param domain the domain.
     * @param host the host.
     * 
     * @return {@code true} if the domain and the host match, {@code false} otherwise.
     */
    private static boolean netscapeDomainMatches(final String domain, final String host) {
        if (domain == null || host == null) return false;
        final boolean bLocalDomain = ".local".equalsIgnoreCase(domain);
        int embeddedDotInDomain = domain.indexOf('.');
        if (embeddedDotInDomain == 0) embeddedDotInDomain = domain.indexOf('.', 1);
        if (!bLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length() - 1)) return false;
        if (host.indexOf('.') == -1 && bLocalDomain) return true;
        final int lengthDiff = host.length() - domain.length();
        if (lengthDiff == 0) return host.equalsIgnoreCase(domain);
        if (lengthDiff > 0) return (host.substring(lengthDiff).equalsIgnoreCase(domain));
        if (lengthDiff == -1) return (domain.charAt(0) == '.' && host.equalsIgnoreCase(domain.substring(1)));
        return false;
    }

    /**
     * Saves this object to a stream (i.e., serializes it).
     *
     * @serialData cookie wrapper repos are serialized in a controlled way to provide for correct
     * synchronization.
     * 
     * @param oos the stream.
     * 
     * @throws IOException if an I/O error occurs.
     */
    private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        lock.lock();
        try {
            oos.writeObject(domainCookieWrappers);
            oos.writeObject(uriToCookieWrappers);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reconstitutes this object from a stream (i.e., deserializes it).
     * 
     * @serialData cookie wrapper repos are deserialized in a controlled way and expired sessions are
     * removed from them.
     * 
     * @param ois the stream.
     * 
     * @throws IOException if the class of a serialized object could not be found.
     * @throws ClassNotFoundException if an I/O error occurs.
     */
    private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        logger = LoggerFactory.getLogger(TeneoEngineCookieStore.class);

        // Removes expired cookies
        final Consumer<ArrayList<SerializableCookieWrapper>> filterWrappers = wrappers -> {
            int n = wrappers.size();
            while (--n >= 0) {
                if (wrappers.get(n).getCookie().hasExpired()) wrappers.remove(n);
            }
        };

        domainCookieWrappers = (ArrayList<SerializableCookieWrapper>)ois.readObject();
        filterWrappers.accept(domainCookieWrappers);

        // Removes the entries where all the cookies have expired
        final Consumer<Map<URI, ArrayList<SerializableCookieWrapper>>> filterMap = map -> {
            for (final Iterator<ArrayList<SerializableCookieWrapper>> iter = map.values().iterator(); iter.hasNext();) {
                final ArrayList<SerializableCookieWrapper> wrappers = iter.next();
                filterWrappers.accept(wrappers);
                if (wrappers.isEmpty()) iter.remove();
            }
        };

        uriToCookieWrappers = (Map<URI, ArrayList<SerializableCookieWrapper>>)ois.readObject();
        filterMap.accept(uriToCookieWrappers);
    }
}

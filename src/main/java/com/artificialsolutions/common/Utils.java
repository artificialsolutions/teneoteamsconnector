package com.artificialsolutions.common;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Class containing different useful static methods.
 */
public class Utils {

    /**
     * Gets a <code>File</code> object pointing to a file located in the folder containing all the
     * packages of this application. For example, assume your application has the following
     * folder/package/Class structure:
     * 
     * <pre>
     * WEB-INF/
     * WEB-INF/classes/
     * WEB-INF/classes/com/foo/Class1.class
     * WEB-INF/classes/com/bar/Class2.class
     * WEB-INF/classes/com/artificialsolutions/common/Utils.class
     * WEB-INF/classes/myConfigFile.properties
     * </pre>
     * 
     * The folder containing all the packages in this case is <code>WEB-INF/classes/</code>. In some
     * cases you might want to read a file located there, like <code>myConfigFile.properties</code> in
     * this example. Using that folder as a relative folder of the file might not always work. So this
     * method provides a way to access that file no matter how the application is deployed.
     * 
     * @param classLoader the class loader of the <i>class</i> object whose definition (class file) is
     * located in any of the packages in whose root folder the given file is located. In this example it
     * can be obtained as <code>Class1.class.getClassLoader()</code>,
     * <code>Class2.class.getClassLoader()</code> or <code>Utils.class.getClassLoader()</code>.
     * @param fileName the name of the file.
     * 
     * @return the <code>File</code> object.
     * 
     * @throws RuntimeException if the <code>File</code> object could not be created.
     */
    public static File getInternalFile(final ClassLoader classLoader, final String fileName) throws RuntimeException {
        final URL url = classLoader.getResource(fileName);
        if (url == null) {
            throw new RuntimeException("Failure to read file [" + fileName + "] as resource, no URL could be constructed for it; the URL for [.] would be [" + classLoader.getResource(".") + ']');
        }
        final URI uri;
        try {
            uri = url.toURI();
        } catch (final Exception exc) {
            throw new RuntimeException("Failure to read file [" + fileName + "] as resource, bad URI from URL [" + url + ']', exc);
        }
        try {
            return new File(uri);
        } catch (final Exception exc) {
            throw new RuntimeException("Failure to read file [" + fileName + "] as resource, bad URI [" + uri + "] from URL [" + url + "] as argument for File constructor", exc);
        }
    }
}

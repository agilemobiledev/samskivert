//
// $Id: Config.java,v 1.4 2001/08/08 23:46:00 mdb Exp $

package com.samskivert.util;

import java.io.IOException;
import java.util.*;

import com.samskivert.Log;

/**
 * The config class provides a unified interaface to application
 * configuration information. It takes care of loading properties files
 * from locations in the classpath and binding the properties in those
 * files into the global config namespace. It also provides access to more
 * datatypes than simply strings, handling the parsing of ints as well as
 * int arrays and string arrays.
 *
 * <p> An application should construct a single instance of
 * <code>Config</code> and use it to access all of its configuration
 * information.
 */
public class Config
{
    /**
     * Constructs a new config object which can be used immediately by
     * binding properties files into the namespace and subsequently
     * requesting values.
     */
    public Config ()
    {
    }

    /**
     * Binds the specified properties file into the namespace with the
     * specified name. If the properties file in question contains a
     * property of the name <code>foo.bar</code> and the file is bound
     * into the namespace under <code>baz</code>, then that property would
     * be accessed as <code>baz.foo.bar</code>.
     *
     * @param name the root name for all properties in this file.
     * @param path the path to the properties file which must live
     * somewhere in the classpath. For example: <code>foo/bar/baz</code>
     * would indicate a file named "foo/bar/baz.properties" living in the
     * classpath.
     * @param inherit if true, the properties file will be loaded using
     * {@link ConfigUtil#loadInheritedProperties} rather than {@link
     * ConfigUtil#loadProperties}.
     *
     * @exception IOException thrown if an error occurrs loading the
     * properties file (like it doesn't exist or cannot be accessed).
     */
    public void bindProperties (String name, String path, boolean inherit)
        throws IOException
    {
        // append the file suffix onto the path
        path += PROPS_SUFFIX;
        // load the properties file
        Properties props = ConfigUtil.loadProperties(path);
        if (props == null) {
            throw new IOException("Unable to load properties file: " + path);
        }
        // bind the properties instance
        _props.put(name, props);
    }

    /**
     * A backwards compatibility method that does not use inherited
     * properties loading.
     *
     * @see #bindProperties(String,String,boolean)
     */
    public void bindProperties (String name, String path)
        throws IOException
    {
        bindProperties(name, path, false);
    }

    /**
     * Fetches and returns the value for the specified configuration
     * property. If the value is not specified in the associated
     * properties file, the supplied default value is returned instead. If
     * the property specified in the file is poorly formatted (not and
     * integer, not in proper array specification), a warning message will
     * be logged and the default value will be returned.
     *
     * @param the fully qualified name of the property (fully qualified
     * meaning that it contains the namespace identifier as well), for
     * example: <code>foo.bar.baz</code>.
     * @param defval the value to return if the property is not specified
     * in the config file.
     *
     * @return the value of the requested property.
     */
    public int getValue (String name, int defval)
    {
        String val = resolveProperty(name);

        // if it's not specified, we return the default
        if (val == null) {
            return defval;
        }

        // otherwise parse  it into an integer
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            Log.warning("Malformed integer property [fqn=" + name +
                        ", value=" + val + "].");
            return defval;
        }
    }

    /**
     * Fetches and returns the value for the specified configuration
     * property. If the value is not specified in the associated
     * properties file, the supplied default value is returned instead.
     *
     * @param the fully qualified name of the property (fully qualified
     * meaning that it contains the namespace identifier as well), for
     * example: <code>foo.bar.baz</code>.
     * @param defval the value to return if the property is not specified
     * in the config file.
     *
     * @return the value of the requested property.
     */
    public String getValue (String name, String defval)
    {
        String val = resolveProperty(name);
        // if it's not specified, we return the default
        return (val == null) ? defval : val;
    }

    /**
     * Fetches and returns the value for the specified configuration
     * property. If the value is not specified in the associated
     * properties file, the supplied default value is returned instead. If
     * the property specified in the file is poorly formatted (not and
     * integer, not in proper array specification), a warning message will
     * be logged and the default value will be returned.
     *
     * @param the fully qualified name of the property (fully qualified
     * meaning that it contains the namespace identifier as well), for
     * example: <code>foo.bar.baz</code>.
     * @param defval the value to return if the property is not specified
     * in the config file.
     *
     * @return the value of the requested property.
     */
    public int[] getValue (String name, int[] defval)
    {
        String val = resolveProperty(name);

        // if it's not specified, we return the default
        if (val == null) {
            return defval;
        }

        // otherwise parse it into an array of ints
        int[] result = StringUtil.parseIntArray(val);
        if (result == null) {
            Log.warning("Malformed int array property [fqn=" + name +
                        ", value=" + val + "].");
            return defval;
        }

        return result;
    }

    /**
     * Fetches and returns the value for the specified configuration
     * property. If the value is not specified in the associated
     * properties file, the supplied default value is returned instead. If
     * the property specified in the file is poorly formatted (not and
     * integer, not in proper array specification), a warning message will
     * be logged and the default value will be returned.
     *
     * @param key the fully qualified name of the property (fully qualified
     * meaning that it contains the namespace identifier as well), for
     * example: <code>foo.bar.baz</code>.
     * @param defval the value to return if the property is not specified
     * in the config file.
     *
     * @return the value of the requested property.
     */
    public String[] getValue (String name, String[] defval)
    {
        String val = resolveProperty(name);

        // if it's not specified, we return the default
        if (val == null) {
            return defval;
        }

        // otherwise parse it into an array of strings
        String[] result = StringUtil.parseStringArray(val);
        if (result == null) {
            Log.warning("Malformed string array property [fqn=" + name +
                        ", value=" + val + "].");
            return defval;
        }

        return result;
    }

    /**
     * Looks up the specified string-valued configuration entry,
     * loads the class with that name and instantiates a new instance
     * of that class, which is returned.
     *
     * @param key the fully qualified name of the property (fully qualified
     * meaning that it contains the namespace identifier as well), for
     * example: <code>foo.bar.baz</code>.
     * @param defcname the class name to use if the property is not
     * specified in the config file.
     *
     * @exception Exception thrown if any error occurs while loading
     * or instantiating the class.
     */
    public Object instantiateValue (String name, String defcname)
	throws Exception
    {
	return Class.forName(getValue(name, defcname)).newInstance();
    }

    /**
     * Returns an iterator that returns all of the configuration keys that
     * match the specified prefix. The prefix should at least contain a
     * namespace identifier but can contain further path components to
     * restrict the iteration. For example: <code>foo</code> would iterate
     * over every property key in the properties file that was bound to
     * <code>foo</code>. <code>foo.bar</code> would iterate over every
     * property key in the <code>foo</code> property file that began with
     * the string <code>bar</code>.
     *
     * <p> If an invalid or non-existent namespace identifier is supplied,
     * a warning will be logged and an empty iterator.
     */
    public Iterator keys (String prefix)
    {
        String id = prefix;
        String key = "";

        // parse the key prefix if one was provided
        int didx = prefix.indexOf(".");
        if (didx != -1) {
            id = prefix.substring(0, didx);
            key = prefix.substring(didx+1);
        }

        Properties props = (Properties)_props.get(id);
        if (props == null) {
            Log.warning("No property file bound to top-level name " +
                        "[name=" + id + ", key=" + key + "].");
            return new Iterator() {
                public boolean hasNext () { return false; }
                public Object next () { return null; }
                public void remove () { /* do nothing */ };
            };
        }

        return new PropertyIterator(key, props.keys());
    }

    protected static class PropertyIterator implements Iterator
    {
        public PropertyIterator (String prefix, Enumeration enum)
        {
            _prefix = prefix;
            _enum = enum;
            scanToNext();
        }

        public boolean hasNext ()
        {
            return (_next != null);
        }

        public Object next ()
        {
            String next = _next;
            scanToNext();
            return next;
        }

        public void remove ()
        {
            // not supported
        }

        protected void scanToNext ()
        {
            // assume that nothing is left
            _next = null;

            while (_enum.hasMoreElements()) {
                String next = (String)_enum.nextElement();
                if (next.startsWith(_prefix)) {
                    _next = next;
                    break;
                }
            }
        }

        protected String _prefix;
        protected Enumeration _enum;
        protected String _next;
    }

    protected String resolveProperty (String name)
    {
        int didx = name.indexOf(".");
        if (didx == -1) {
            Log.warning("Invalid fully qualified property name " +
                        "[name=" + name + "].");
            return null;
        }

        String id = name.substring(0, didx);
        String key = name.substring(didx+1);

        Properties props = (Properties)_props.get(id);
        if (props == null) {
            Log.warning("No property file bound to top-level name " +
                        "[name=" + id + ", key=" + key + "].");
            return null;
        }

        return props.getProperty(key);
    }

    public static void main (String[] args)
    {
        Config config = new Config();
        try {
            config.bindProperties("test", "com/samskivert/util/test");

            System.out.println("test.prop1: " +
                               config.getValue("test.prop1", 1));
            System.out.println("test.prop2: " +
                               config.getValue("test.prop2", "two"));

            int[] ival = new int[] { 1, 2, 3 };
            ival = config.getValue("test.prop3", ival);
            System.out.println("test.prop3: " + StringUtil.toString(ival));

            String[] sval = new String[] { "one", "two", "three" };
            sval = config.getValue("test.prop4", sval);
            System.out.println("test.prop4: " + StringUtil.toString(sval));

            System.out.println("test.prop5: " +
                               config.getValue("test.prop5", "undefined"));

            Iterator iter = config.keys("test.prop2");
            while (iter.hasNext()) {
                System.out.println(iter.next());
            }

            iter = config.keys("test.prop");
            while (iter.hasNext()) {
                System.out.println(iter.next());
            }

            iter = config.keys("test");
            while (iter.hasNext()) {
                System.out.println(iter.next());
            }

        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }

    protected Hashtable _props = new Hashtable();

    protected static final String PROPS_SUFFIX = ".properties";
}

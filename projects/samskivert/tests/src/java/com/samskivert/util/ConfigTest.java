//
// $Id: ConfigTest.java,v 1.2 2002/03/28 21:50:27 mdb Exp $

package com.samskivert.util;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Tests the {@link Config} class.
 */
public class ConfigTest extends TestCase
{
    public ConfigTest ()
    {
        super(ConfigTest.class.getName());
    }

    public void runTest ()
    {
        Config config = new Config("rsrc/util/test");

        System.out.println("prop1: " + config.getValue("prop1", 1));
        System.out.println("prop2: " + config.getValue("prop2", "two"));

        int[] ival = new int[] { 1, 2, 3 };
        ival = config.getValue("prop3", ival);
        System.out.println("prop3: " + StringUtil.toString(ival));

        String[] sval = new String[] { "one", "two", "three" };
        sval = config.getValue("prop4", sval);
        System.out.println("prop4: " + StringUtil.toString(sval));

        System.out.println("prop5: " + config.getValue("prop5", "undefined"));

        // now set some properties
        config.setValue("prop1", 15);
        System.out.println("prop1: " + config.getValue("prop1", 1));
        config.setValue("prop2", "three");
        System.out.println("prop2: " + config.getValue("prop2", "two"));

        Iterator iter = config.keys();
        System.out.println("Keys: " + StringUtil.toString(iter));
    }

    public static Test suite ()
    {
        return new ConfigTest();
    }

    public static void main (String[] args)
    {
        ConfigTest test = new ConfigTest();
        test.runTest();
    }
}

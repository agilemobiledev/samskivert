//
// $Id: StringTool.java,v 1.15 2004/02/25 13:16:32 mdb Exp $
//
// samskivert library - useful routines for java programs
// Copyright (C) 2001 Michael Bayne
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.velocity;

import java.text.NumberFormat;

import com.samskivert.servlet.util.HTMLUtil;
import com.samskivert.util.StringUtil;

/**
 * Provides simple string funtions like <code>blank()</code>.
 */
public class StringTool
{
    public StringTool ()
    {
        _percFormat = NumberFormat.getPercentInstance();
        _percFormat.setMinimumFractionDigits(2);
    }

    /**
     * Returns true if the supplied string is blank, false if not.
     */
    public static boolean blank (String text)
    {
        return StringUtil.blank(text);
    }

    /**
     * URL encodes the supplied text.
     */
    public static String urlEncode (String text)
    {
        return StringUtil.encode(text);
    }

    /**
     * Converts an integer to a string.
     */
    public String valueOf (int value)
    {
        return String.valueOf(value);
    }

    /**
     * Convert a float to a nicely formated percent string.
     * Examples:
     * .34     "34.00%"
     * .341    "34.10%"
     * .34121  "34.121%"
     */
    public String percent (float value)
    {
        return _percFormat.format(value);
    }

    /**
     * Adds &lt;p&gt; tags between each pair of consecutive newlines.
     */
    public static String parafy (String text)
    {
        return HTMLUtil.makeParagraphs(text);
    }

    /**
     * Adds a &lt;br&gt; tag before every newline.
     */
    public static String delineate (String text)
    {
        return HTMLUtil.makeLinear(text);
    }

    /**
     * Converts a float to a reasonably formatted string.
     */
    public static String format (float value)
    {
        return NumberFormat.getInstance().format(value);
    }

    /**
     * Truncates the supplied text at the specified length, appending the
     * specified "elipsis" indicator to the text if truncated.
     */
    public static String truncate (String text, int length, String append)
    {
        return StringUtil.truncate(text, length, append);
    }

    /**
     * Restrict all HTML from the specified String.
     */
    public static String restrictHTML (String text)
    {
        return StringUtil.restrictHTML(text);
    }
    
    /** For formatting percentages. */
    protected NumberFormat _percFormat;
}

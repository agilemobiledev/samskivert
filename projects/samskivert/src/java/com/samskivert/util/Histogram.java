//
// $Id: Histogram.java,v 1.1 2002/02/18 06:22:56 mdb Exp $
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

package com.samskivert.util;

import java.util.Arrays;

/**
 * Used for tracking a set of values that fall into a discrete range of
 * values.
 */
public class Histogram
{
    /**
     * Constructs a histogram that will track values with a granularity
     * equal to the <code>bucketWidth</code> from <code>minValue</code> to
     * <code>minValue + bucketWidth*bucketCount</code>.
     */
    public Histogram (int minValue, int bucketWidth, int bucketCount)
    {
        _minValue = minValue;
        _maxValue = minValue + bucketWidth*bucketCount;
        _bucketWidth = bucketWidth;
        _buckets = new int[bucketCount];
    }

    /**
     * Registers a value with this histogram.
     */
    public void addValue (int value)
    {
        if (value < _minValue) {
            _buckets[0]++;
        } else if (value >= _maxValue) {
            _buckets[_buckets.length-1]++;
        } else {
            _buckets[(value-_minValue)/_bucketWidth]++;
        }
    }

    /**
     * Clears the values from this histogram.
     */
    public void clear ()
    {
        Arrays.fill(_buckets, 0);
    }

    /**
     * Returns the array containing the bucket values. The zeroth element
     * contains the count of all values less than <code>minValue</code>,
     * the subsequent <code>bucketCount</code> elements contain the count
     * of values falling into those buckets and the last element contains
     * values greater than or equal to <code>maxValue</code>.
     */
    public int[] getBuckets ()
    {
        return _buckets;
    }

    /**
     * Returns a string representation of this histogram.
     */
    public String toString ()
    {
        return "[min=" + _minValue + ", max=" + _maxValue +
            ", bwidth=" + _bucketWidth +
            ", buckets=" + StringUtil.toString(_buckets) + "]";
    }

    /** The minimum value tracked by this histogram. */
    protected int _minValue;

    /** The maximum value tracked by this histogram. */
    protected int _maxValue;

    /** The size of each histogram bucket. */
    protected int _bucketWidth;

    /** The histogram buckets. */
    protected int[] _buckets;
}
//
// $Id: CommandEvent.java,v 1.1 2001/12/20 00:38:03 mdb Exp $

package com.samskivert.swing;

import java.awt.event.ActionEvent;

/**
 * An action event with an associated argument. Often times with
 * controllers, one wants to post a command with an associated object
 * (which we call the argument), but action event provides no mechanism to
 * do so. So this class is provided for such situations.
 */
public class CommandEvent extends ActionEvent
{
    public CommandEvent (Object source, String command, Object argument)
    {
        super(source, ActionEvent.ACTION_PERFORMED, command);
        _argument = argument;
    }

    /**
     * Returns the argument provided to the command event at construct
     * time.
     */
    public Object getArgument ()
    {
        return _argument;
    }

    /** The argument to this command event. */
    protected Object _argument;
}
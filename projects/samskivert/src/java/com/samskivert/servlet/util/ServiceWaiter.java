//
// $Id: ServiceWaiter.java,v 1.2 2003/08/13 01:55:05 ray Exp $

package com.samskivert.servlet.util;

import com.samskivert.Log;
import com.samskivert.servlet.RedirectException;
import com.samskivert.util.ResultListener;

/**
 * A handy base class for issuing server-side service requests and
 * awaiting their responses from within a servlet.
 *
 * <em>Note:</em> You might think that it would be keen to use this
 * anonymously like so:
 *
 * <pre>
 * ServiceWaiter waiter = new ServiceWaiter() {
 *     public void handleSuccess (int invid, ...) {
 *         // handle success
 *     }
 *     // ...
 * };
 * </pre>
 *
 * But that won't work because public methods in anonymous inner classes
 * do not have public visibility and so the invocation manager will not be
 * able to reflect your response methods when the time comes to deliver
 * the response. Unfortunately, there appears to be no manner in which to
 * instruct the Java compiler to give public scope to an anonymous inner
 * class rather than default scope. Sigh.
 */
public class ServiceWaiter
    implements ResultListener
{
    /** Timeout to specify when you don't want a timeout. Use at your own
     * risk. */
    public static final int NO_TIMEOUT = -1;

    /**
     * Construct a ServiceWaiter with the default (30 second) timeout.
     */
    public ServiceWaiter ()
    {
        this(DEFAULT_WAITER_TIMEOUT);
    }

    /**
     * Construct a ServiceWaiter with the specified timeout.
     *
     * @param timeout the timeout, in seconds.
     */
    public ServiceWaiter (int timeout)
    {
        setTimeout(timeout);
    }

    /**
     * Change the timeout being used for this ServiceWaiter after it
     * has been constructed.
     */
    public void setTimeout (int timeout)
    {
        _timeout = timeout;
    }

    /**
     * Reset the service waiter so that it can be used again.
     */
    public void reset ()
    {
        _success = 0;
        _argument = null;
    }

    /**
     * Marks the request as successful and posts the supplied response
     * argument for perusal by the caller.
     */
    public synchronized void postSuccess (Object arg)
    {
        _success = 1;
        _argument = arg;
        notify();
    }

    /**
     * Marks the request as failed and posts the supplied response
     * argument for perusal by the caller.
     */
    public synchronized void postFailure (Object arg)
    {
        _success = -1;
        _argument = arg;
        notify();
    }

    /**
     * Returns the argument posted by the waiter when the response
     * arrived.
     */
    public Object getArgument ()
    {
        return _argument;
    }

    /**
     * Blocks waiting for the response.
     *
     * @return true if a success response was posted, false if a failure
     * repsonse was posted.
     */
    public synchronized boolean awaitResponse ()
        throws RedirectException
    {
        while (_success == 0) {
            try {
                // wait for the response, timing out after a while
                if (_timeout == NO_TIMEOUT) {
                    wait();

                } else {
                    wait(1000L * _timeout);
                }

                // if we get here without some sort of response, then
                // we've timed out and we should freak out
                if (_success == 0) {
                    Log.warning("Service waiter timed out.");
                    Thread.dumpStack();
                    throw new RedirectException(getTimeoutRedirectURL());
                }

            } catch (InterruptedException ie) {
                // fall through and wait again
            }
        }

        return (_success > 0);
    }

    /**
     * Get the page to which we redirect on timeout.
     */
    protected String getTimeoutRedirectURL ()
    {
        // TODO
        return "/";
    }

    // documentation inherited from interface ResultListener
    public void requestCompleted (Object result)
    {
        postSuccess(result);
    }

    // documentation inherited from interface ResultListener
    public void requestFailed (Exception cause)
    {
        postFailure((cause != null) ? cause.getMessage() : null);
    }

    /** Whether or not the response succeeded; positive for success,
     * negative for failure, zero means we haven't received the response
     * yet. */
    protected int _success = 0;

    /** The argument posted by the waiter upon receipt of the response. */
    protected Object _argument;

    /** How many seconds to wait before giving up the ghost. */
    protected int _timeout;

    /** If a response is not received within the specified timeout, an
     * exception is thrown which redirects the user to an internal error
     * page. */
    protected static final int DEFAULT_WAITER_TIMEOUT = 30;
}

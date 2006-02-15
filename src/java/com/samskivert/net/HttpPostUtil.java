//
// $Id: HttpPostUtil.java,v 1.2 2003/10/08 23:52:49 ray Exp $

package com.samskivert.net;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;

import com.samskivert.util.ServiceWaiter;

/**
 * Contains utility methods for doing a form post.
 */
public class HttpPostUtil
{
    /**
     * Return the results of a form post. Note that the http request takes
     * place on another thread, but this thread blocks until the results
     * are returned or it times out.
     *
     * @param url from which to make the request.
     * @param submission the entire submission eg "foo=bar&baz=boo&futz=foo".
     * @param timeout time to wait for the response, in seconds, or -1
     * for forever.
     */
    public static String httpPost (final URL url, final String submission,
                                   int timeout)
        throws IOException, ServiceWaiter.TimeoutException
    {
        final ServiceWaiter waiter = new ServiceWaiter(
            (timeout < 0) ? ServiceWaiter.NO_TIMEOUT : timeout);
        Thread tt = new Thread() {
            public void run () {
                try {
                    HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestProperty(
                        "Content-Type", "application/x-www-form-urlencoded");

                    DataOutputStream out = new DataOutputStream(
                        conn.getOutputStream());
                    out.writeBytes(submission);
                    out.flush();
                    out.close();

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                    StringBuffer buf = new StringBuffer();
                    for (String s; null != (s = reader.readLine()); ) {
                        buf.append(s);
                    }
                    reader.close();

                    waiter.postSuccess(buf.toString()); // yay
    
                } catch (IOException e) {
                    waiter.postFailure(e); // boo
                }
            }
        };

        tt.start();

        if (waiter.waitForResponse()) {
            return (String) waiter.getArgument();

        } else {
            throw (IOException) waiter.getArgument();
        }
    }
}
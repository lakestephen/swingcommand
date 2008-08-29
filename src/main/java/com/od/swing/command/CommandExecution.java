package com.od.swing.command;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 27-Jun-2008
 * Time: 17:43:57
 */
public interface CommandExecution {

    /**
     * Called in a background thread during command execution, to do the main task processing
     * Command classes should implement implement this method to do the asynchronous processing required
     *
     * @throws Exception, to abort execution if an error condition occurs
     */
    void doInBackground() throws Exception;

    /**
     * Called in the Swing event thread during command execution
     * Gives the command a chance to update the Swing UI views & models once asynchronous processing is completed
     *
     * @throws Exception, to abort execution if an error condition occurs
     */
    void done() throws Exception;
}

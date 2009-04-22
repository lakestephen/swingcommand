/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand2;


/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 27-Jun-2008
 * Time: 17:43:57
 */
public interface AsyncExecution extends Execution {

    /**
     * Called in a background thread during command execution, to do the asynchronous task processing
     * Command classes should implement this method to do the asynchronous processing required
     * before doInEventThread is called to update the UI
     *
     * @throws Exception, to abort execution if an error condition occurs
     */
    void doInBackground() throws Exception;

}
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
 * Date: 09-Sep-2008
 * Time: 14:50:21
 *
 * CommandExecution which are cancellable may implement this interface
 *
 * n.b. DefaultCompositeCommand checks whether child commands implement this interface when the composite command
 * execution is cancelled
 */
public interface Cancellable {

    /**
    * Call this method to request execution be cancelled
    */
    void cancel();

    /**
     * @return true, if this execution was cancelled
     */
    boolean isCancelled();

    /**
     * @return true, if the command supports cancellation, and it has not yet been cancelled
     */
    boolean isCancellable();
}
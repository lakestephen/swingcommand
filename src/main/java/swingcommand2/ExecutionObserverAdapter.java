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
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * An adapter in the sense of Swing listener adapters - can be exteded by classes
 * which want to inherit a default implementation of the observer methods, overriding
 * only those which they are interested in
 */
public class ExecutionObserverAdapter implements ExecutionObserver {

    public void pending(Execution commandExecution) {}

    public void started(Execution commandExecution) {}

    public void progress(Execution commandExecution, String progressDescription) {}

    public void success(Execution commandExecution) {}

    public void error(Execution commandExecution, Throwable error) {}

    public void done(Execution commandExecution) {}
}
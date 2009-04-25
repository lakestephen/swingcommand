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
 * An adapter, in the sense of Swing listener adapters, which can be exteded by classes
 * which want to inherit a default implementation of the observer methods, overriding
 * only those which they are interested in
 */
public class TaskListenerAdapter<P> implements TaskListener<P> {

    public void pending(Task commandExecution) {}

    public void started(Task commandExecution) {}

    public void progress(Task commandExecution, P progressDescription) {}

    public void success(Task commandExecution) {}

    public void error(Task commandExecution, Throwable error) {}

    public void finished(Task commandExecution) {}
}
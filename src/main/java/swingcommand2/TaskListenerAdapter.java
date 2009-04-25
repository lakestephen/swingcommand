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
public class TaskListenerAdapter implements TaskListener {

    public void pending(SimpleTask commandExecution) {}

    public void started(SimpleTask commandExecution) {}

    public void progress(SimpleTask commandExecution, String progressDescription) {}

    public void success(SimpleTask commandExecution) {}

    public void error(SimpleTask commandExecution, Throwable error) {}

    public void finished(SimpleTask commandExecution) {}
}
package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 */
class DefaultCommandController<E> implements CommandController<E> {

    public void commandStarting(String commandName, E commandExecution) {
    }

    public void commandStopped(String commandName, E commandExecution) {
    }

    public void commandError(String commandName, E commandExecution, Throwable t) {
    }
}

package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 */
class DefaultCommandController<E> implements CommandController<E> {

    public boolean commandStarting(E commandExecution, String message) {
        return true;
    }

    public void commandStopped(E commandExecution, String message) {}

    public void handleCommandError(E commandExecution, String errorMessage, Throwable t) {}
    
}

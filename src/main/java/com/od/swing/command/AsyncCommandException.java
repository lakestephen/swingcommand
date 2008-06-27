package com.od.swing.command;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 */
public class AsyncCommandException extends Exception {

    public AsyncCommandException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}

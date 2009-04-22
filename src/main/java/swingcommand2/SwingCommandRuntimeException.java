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
 * User: nick
 * Date: 29-Aug-2008
 * Time: 23:37:05
 * To change this template use File | Settings | File Templates.
 */
public class SwingCommandRuntimeException extends RuntimeException {

    public SwingCommandRuntimeException(String message) {
        super(message);
    }

    public SwingCommandRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
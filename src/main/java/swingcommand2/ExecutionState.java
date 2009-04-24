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
 * Date: 14-Sep-2008
 * Time: 18:07:43
 * To change this template use File | Settings | File Templates.
 */
public enum ExecutionState {

    NOT_RUN,
    PENDING,
    STARTED,
    SUCCESS,
    ERROR;

    public boolean isFinalState() {
        return this == SUCCESS || this == ERROR;
    }
}
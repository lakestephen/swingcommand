/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 23-Sep-2008
* Time: 09:54:05
*/
public interface SequentialExecution<C extends CommandExecution> extends CompositeExecution<C> {

      public C getCurrentExecution();
}

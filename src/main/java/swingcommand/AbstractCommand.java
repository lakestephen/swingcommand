/**
 *  Copyright (C) Nick Ebbutt September 2009
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/filtertable
 *
 *  This file is part of ObjectDefinitions Ltd. FilterTable.
 *
 *  FilterTable is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ObjectDefinitions Ltd. FilterTable is distributed in the hope that it will
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with ObjectDefinitions Ltd. FilterTable.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package swingcommand;

import javax.swing.*;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 24-Sep-2008
 * Time: 14:57:06
 *
 * Abstract superclass for classes which run synchronously in the Swing event thread.
 *
 * Note: Commands derived from AbstractCommand will only run synchronously if executed
 * from the event thread. If execute is called from a background thread, it will not be synchronous.
 * See notes below.
 *
 * createExecution() and doInEventThread() are both guaranteed to be called in the event thread,
 * even if a background thread calls execute.
 *
 * If executed from a background thread, createExecution is called during an invokeAndWait() (so the calling
 * thread blocks until the Execution is created), but doInEventThread is executed with invokeLater().
 *
 * This allows the created Execution to be returned, to meet the contract of Command interface, while
 * not blocking the background thread while invokeLater executes, which may be doing significant work updating
 * the UI.
 */
public abstract class AbstractCommand<E extends CommandExecution> extends CommandBase<E> implements Command<E> {

    private Map<E,List<ExecutionObserver<? super E>>> executionToObserversMap = new Hashtable<E,List<ExecutionObserver<? super E>>>();

    public E execute(ExecutionObserver<? super E>... executionObservers) {
        E execution = createExecutionInEventThread();
        Runnable executeRunnable = createExecutionRunnable(execution, executionObservers);
        runInEventThread(executeRunnable);
        return execution;
    }

    private void runInEventThread(Runnable executeRunnable) {
        if ( SwingUtilities.isEventDispatchThread() ) {
            executeRunnable.run();
        } else {
            //if command kicked off on a subthread we don't want to block it on the event thread
            //longer than necessary for performance reasons, so use invoke later
            SwingUtilities.invokeLater(executeRunnable);
        }
    }

    private Runnable createExecutionRunnable(final E execution, final ExecutionObserver<? super E>... executionObservers) {
        Runnable executeRunnable = new Runnable() {
            public void run() {
                List<ExecutionObserver<? super E>> observers = executionObserverSupport.getExecutionObserverSnapshot();
                observers.addAll(Arrays.asList(executionObservers));
                executionToObserversMap.put(execution,  observers);
                try {
                    execution.setState(ExecutionState.PENDING);
                    ExecutionObserverSupport.firePending(observers, execution);
                    execution.setState(ExecutionState.STARTED);
                    ExecutionObserverSupport.fireStarted(observers, execution);
                    execution.doInEventThread();
                    execution.setState(ExecutionState.SUCCESS);
                    ExecutionObserverSupport.fireSuccess(observers, execution);
                } catch ( Throwable t) {
                    execution.setExecutionException(t);
                    execution.setState(ExecutionState.ERROR);
                    ExecutionObserverSupport.fireError(observers, execution, t);
                    ExecutionObserverSupport.fireDone(observers, execution);
                } finally {
                    executionToObserversMap.remove(execution);
                }
            }
        };
        return executeRunnable;
    }

    protected void fireProgress(E commandExecution) {
        List<ExecutionObserver<? super E>> observers = executionToObserversMap.get(commandExecution);
        if ( observers != null ) {
            ExecutionObserverSupport.fireProgress(observers, commandExecution);
        } else {
            throw new SwingCommandRuntimeException("fireProgress called for unknown execution " + commandExecution);
        }
    }
}

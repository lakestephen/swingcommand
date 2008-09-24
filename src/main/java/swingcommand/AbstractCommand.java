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

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 24-Sep-2008
 * Time: 14:57:06
 */
public abstract class AbstractCommand<E extends CommandExecution> implements Command<E> {

    private ExecutionObserverSupport<E> observerSupport = new ExecutionObserverSupport<E>();
    private Map<E,List<ExecutionObserver<? super E>>> executionToObserversMap = new Hashtable<E,List<ExecutionObserver<? super E>>>();

    public void addExecutionObserver(ExecutionObserver<? super E>... executionObservers) {
        observerSupport.addExecutionObservers(executionObservers);
    }

    public void removeExecutionObserver(ExecutionObserver<? super E>... executionObservers) {
        observerSupport.removeExecutionObservers(executionObservers);
    }

    public E execute(ExecutionObserver<? super E>... executionObservers) {
        E execution = safeCreateExecution();

        List<ExecutionObserver<? super E>> observers = observerSupport.getExecutionObserverSnapshot();
        observers.addAll(Arrays.asList(executionObservers));
        executionToObserversMap.put(execution,  observers);
        try {
            ExecutionObserverSupport.firePending(observers, execution);
            ExecutionObserverSupport.fireStarted(observers, execution);
            execution.doInEventThread();
            ExecutionObserverSupport.fireSuccess(observers, execution);
        } catch ( Throwable t) {
            execution.setExecutionException(t);
            ExecutionObserverSupport.fireError(observers, execution, t);
            ExecutionObserverSupport.fireDone(observers, execution);
        } finally {
            executionToObserversMap.remove(execution);
        }
        return execution;
    }

    private E safeCreateExecution() {
        E execution;
        try {
            execution = createExecution();
        } catch ( Throwable t) {
            throw new SwingCommandRuntimeException("Failed to createCommandExecution", t);
        }
        return execution;
    }

    protected abstract E createExecution();


    protected void fireProgress(E commandExecution) {
        List<ExecutionObserver<? super E>> observers = executionToObserversMap.get(commandExecution);
        if ( observers != null ) {
            ExecutionObserverSupport.fireProgress(observers, commandExecution);
        } else {
            throw new SwingCommandRuntimeException("fireProgress called for unknown execution " + commandExecution);
        }
    }
}

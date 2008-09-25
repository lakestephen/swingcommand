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

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 25-Sep-2008
 * Time: 09:47:22
 *
 * Some shared methods for
 */
public abstract class CommandBase<E extends CommandExecution> {

    ExecutionObserverSupport<E> executionObserverSupport = new ExecutionObserverSupport<E>();

    /**
     * @return an Execution for this asynchronous command
     */
    protected abstract E createExecution();

    /**
     * Create an execution.
     * It is important this is done on the event thread because, while creating
     * the execution, state from the ui models or components likely has to be copied/cloned to use as
     * parameters for the async processing. For safety only the event thread should interact with ui
     * components/models. Cloning state from the ui models ensures the background thread has its own
     * copy during execution, and there are no potential race conditions
     */
    protected E createExecutionInEventThread() {

        class CreateExecutionRunnable implements Runnable {

            volatile E execution;

            public E getExecution() {
                return execution;
            }

            public void run() {
                execution = createExecution();
            }
        }

        CreateExecutionRunnable r = new CreateExecutionRunnable();
        Throwable t = ExecutionObserverSupport.executeSynchronouslyOnEventThread(r, false);
        E execution = (E)r.getExecution();  //for some reason some jdk need the cast to E to compile
        if ( t != null ) {
            throw new SwingCommandRuntimeException("Cannot run swingcommand \" + getClass().getName() + \" createExecution() threw an exception");
        } else if ( execution == null ) {
            throw new SwingCommandRuntimeException("Cannot run swingcommand " + getClass().getName() + " createExecution() returned null");
        }
        return execution;
    }


    public void addExecutionObserver(ExecutionObserver<? super E>... executionObservers) {
        executionObserverSupport.addExecutionObservers(executionObservers);
    }

    public void removeExecutionObserver(ExecutionObserver<? super E>... executionObservers) {
        executionObserverSupport.removeExecutionObservers(executionObservers);
    }
}

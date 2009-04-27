/*
 * Copyright 2009 Object Definitions Ltd.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package swingcommand;

import javax.swing.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 22-Apr-2009
 * Time: 19:50:53
 * 
 */
public abstract class SwingCommand<P> {

    private static ExecutorService DEFAULT_ASYNC_EXECUTOR = Executors.newCachedThreadPool();
    private static Executor DEFAULT_SIMPLE_EXECUTOR = new IfSubThreadInvokeLaterExecutor();
    private static ExecutorFactory DEFAULT_EXECUTOR_FACTORY = new DefaultExecutorFactory();

    private final List<TaskListener<? super P>> taskListeners = new ArrayList<TaskListener<? super P>>();

    private volatile Executor executor;

    public SwingCommand() {
    }

    /**
     * @param executor Executor to run this command
     */
    public SwingCommand(Executor executor) {
        this.executor = executor;
    }

    /**
     * @param executor, the Executor used to run this command
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Task<P> execute() {
        return execute(executor, Collections.EMPTY_LIST);
    }

    public Task<P> execute(Executor executor) {
        return execute(executor, Collections.EMPTY_LIST);
    }

    public Task<P> execute(ExecutorFactory executorFactory) {
        return execute(executorFactory, Collections.EMPTY_LIST);
    }

    public Task<P> execute(TaskListener<? super P> taskListener) {
        return execute(executor, taskListener);
    }

    public Task<P> execute(Executor executor, TaskListener<? super P> taskListener) {
        return execute(executor, getList(taskListener));
    }

    public Task<P> execute(ExecutorFactory executorFactory, TaskListener<? super P> taskListener) {
        return execute(executorFactory, getList(taskListener));
    }

    public Task<P> execute(Executor executor, List<TaskListener<? super P>> taskListeners) {
        Task<P> task = doCreateTask();
        if (executor == null) {
            executor = getExecutor(task);
        }
        executeCommand(executor, task, taskListeners);
        return task;
    }

    public Task<P> execute(ExecutorFactory executorFactory, List<TaskListener<? super P>> taskListeners) {
        Task<P> task = doCreateTask();
        Executor executor = executorFactory.getExecutor(task);
        executeCommand(executor, task, taskListeners);
        return task;
    }

    private List<TaskListener<? super P>> getList(TaskListener<? super P> taskListener) {
        List<TaskListener<? super P>> l = new ArrayList<TaskListener<? super P>>();
        l.add(taskListener);
        return l;
    }

    private Task<P> doCreateTask() {
        try {
            return createTask();
        } catch ( Throwable t) {
            throw new SwingCommandRuntimeException("Failed to run SwingCommand, createTask() threw an exeception", t);
        }
    }

    protected Executor getExecutor(Task task) {
        return DEFAULT_EXECUTOR_FACTORY.getExecutor(task);
    }

    public final void addTaskListener(TaskListener<? super P> taskListener) {
        synchronized (this.taskListeners) {
            this.taskListeners.add(taskListener);
        }
    }

    public final void removeTaskListener(TaskListener<? super P> taskListener) {
        synchronized (this.taskListeners) {
            this.taskListeners.remove(taskListener);
        }
    }

    private List<TaskListener<? super P>> getListenerSnapshot()  {
        synchronized (taskListeners) {
            return new ArrayList<TaskListener<? super P>>(taskListeners);
        }
    }

    /**
     * @return an Execution for this asynchronous command
     */
    protected abstract Task<P> createTask();


    private void executeCommand(Executor executor, Task<P> execution, List<TaskListener<? super P>> taskListeners) {

        //get a snapshot list of the execution observers which will receive the events for this execution
        final List<TaskListener<? super P>> allListeners = getListenerSnapshot();
        allListeners.addAll(taskListeners);

        //create a new execution controller for this execution
        ExecutionManager executionManager = new ExecutionManager(executor, execution, allListeners);
        executionManager.executeCommand();
    }

    static class IfSubThreadInvokeLaterExecutor implements Executor {

        public void execute(Runnable command) {
            if (SwingUtilities.isEventDispatchThread()) {
                command.run();
            } else {
                //if command kicked off on a subthread we don't want to block it on the event thread
                //longer than necessary for performance reasons, so use invoke later rather than invokeAndWait
                SwingUtilities.invokeLater(command);
            }
        }
    }

    static class DefaultExecutorFactory implements ExecutorFactory {
        public Executor getExecutor(Task e) {
            return (e instanceof BackgroundTask) ? DEFAULT_ASYNC_EXECUTOR : DEFAULT_SIMPLE_EXECUTOR;
        }
    }

    public static interface ExecutorFactory {
        Executor getExecutor(Task e);
    }


    class ExecutionManager {

        private final Executor executor;
        private final Task<P> task;
        private final List<TaskListener<? super P>> taskListeners;

        public ExecutionManager(Executor executor, Task<P> task, List<TaskListener<? super P>> taskListeners) {
            this.executor = executor;
            this.task = task;
            this.taskListeners = taskListeners;
        }

        /**
         * This object is used to synchronize memory for each stage of the command processing,
         * This ensures that any state updated during each stage is flushed to shared heap memory before the next stage executes
         * (Since the next stage will executed in a different thread such state changes would not otherwise be guaranteed to be visible)
         */
        private final Object memorySync = new Object();

        public void executeCommand() {

            task.addTaskListeners(taskListeners);

            //Call fire pending before spawning a new thread. Provided execute was called on the
            //event thread, no more ui work can possibly get finished before fireStarting is called
            //If fireStarting is used, for example, to disable a button, this guarantees that the button will be
            //disabled before the action listener triggering the swingcommand returns.
            //otherwise the user might be able to click the button again before the fireStarting callback
            task.setExecutionState(ExecutionState.PENDING);
            TaskListenerSupport.firePending(task.getTaskListeners(), task);

             executor.execute(new Runnable() {
                public void run() {
                    doExecuteTask();
                }
            });
        }
    
        private void doExecuteTask() {
            //this try block makes sure we always call end up calling fireDone
            try {
                setExecutionState(ExecutionState.STARTED);
                TaskListenerSupport.fireStarted(task.getTaskListeners(), task);

                if ( task instanceof BackgroundTask) {
                    synchronized (memorySync) {
                        //STAGE1  - in the current swingcommand processing thread
                        ((BackgroundTask) task).doInBackground();
                    }
                }

                //STAGE2 - this needs to be finished on the event thread
                runDoInEventThread(task);

                setExecutionState(ExecutionState.SUCCESS);
                TaskListenerSupport.fireSuccess(task.getTaskListeners(), task);
            } catch (Throwable t ) {
                setTaskException(t);
                setExecutionState(ExecutionState.ERROR);
                TaskListenerSupport.fireError(task.getTaskListeners(), task, t);
            } finally {
                TaskListenerSupport.fireDone(task.getTaskListeners(), task);
            }
        }

        private void setTaskException(Throwable t) {
            if ( t instanceof SwingCommandException) {
                task.setExecutionException(t.getCause());
            } else {
                task.setExecutionException(t);
            }
        }

        private void setExecutionState(final ExecutionState newState) {
            TaskListenerSupport.executeSynchronouslyOnEventThread(new Runnable(){
                public void run() {
                    task.setExecutionState(newState);
                }
            });
        }

        private void runDoInEventThread(final Task task) throws Exception {
            class DoAfterExecuteRunnable implements Runnable {
                volatile Throwable throwable;

                public void run() {
                    synchronized (memorySync) {  //make sure the event thread sees the latest state
                        try {
                            task.doInEventThread();
                        } catch (Throwable e) {
                            throwable = e;
                        }
                    }
                }
            }
            DoAfterExecuteRunnable doAfterExecuteRunnable = new DoAfterExecuteRunnable();
            TaskListenerSupport.executeSynchronouslyOnEventThread(doAfterExecuteRunnable);
            if (doAfterExecuteRunnable.throwable != null) {
                throw new SwingCommandException("Failed while invoking runDone() on " + getClass().getName(), doAfterExecuteRunnable.throwable);
            }
        }

    }

    static class SwingCommandRuntimeException extends RuntimeException {

        public SwingCommandRuntimeException(String message) {
            super(message);
        }

        public SwingCommandRuntimeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class SwingCommandException extends Exception {

        public SwingCommandException(String message) {
            super(message);
        }

        public SwingCommandException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

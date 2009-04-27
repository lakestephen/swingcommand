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
import java.util.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt (refactored from EBondMarketService)
 * Date: 23-Apr-2009
 * Time: 10:55:25
 */
public abstract class CompositeCommandTask<P> extends BackgroundTask<P> {

    private static final Executor SYNCHRONOUS_EXECUTOR = new Executor() {
        public void execute(Runnable command) {
            command.run();
        }
    };

    private static final Executor INVOKE_AND_WAIT_EXECUTOR = new IfSubThreadInvokeAndWaitExecutor();
    private static final SwingCommand.ExecutorFactory COMPOSITE_EXECUTOR_FACTORY = new CompositeExecutorFactory();

    private List<SwingCommand> childCommands = new ArrayList<SwingCommand>();
    private int totalChildCommands = childCommands.size();
    private int currentCommandId;
    private TaskListenerProxy taskListenerProxy = new TaskListenerProxy();

    public CompositeCommandTask() {
    }

    public CompositeCommandTask(SwingCommand... commands) {
        childCommands.addAll(Arrays.asList(commands));
    }

    public CompositeCommandTask(Collection<SwingCommand> commands) {
        childCommands.addAll(commands);
    }

    /**
     * Execute the child commands here off the swing thread.
     * This will not start a new subthread - the child commands will run synchronously in the parent command's execution thread.
     *
     * @throws Exception
     */
    public void doInBackground() throws Exception {
        currentCommandId = 0;
        for (final SwingCommand command : childCommands) {
            currentCommandId++;

            //we are not in event thread here, so this call should be synchronous
            //noinspection unchecked
            command.execute(COMPOSITE_EXECUTOR_FACTORY, taskListenerProxy);

            if (taskListenerProxy.isErrorOccurred()) {
                throw new CompositeCommandException(taskListenerProxy.getLastCommandError());
            }

            if (taskListenerProxy.isLastCommandCancelled()) {
                cancel();
            }

            if (isCancelled()) {
                break;
            }
        }
    }

    public void doInEventThread() throws Exception {
    }

    public void addCommand(SwingCommand command) {
        childCommands.add(command);
    }

    public void addCommands(SwingCommand... commands) {
        childCommands.addAll(Arrays.asList(commands));
    }

    public void addCommands(Collection<SwingCommand> commands) {
        childCommands.addAll(commands);
    }

    public Task getCurrentChildTask() {
        return taskListenerProxy.getCurrentChildTask();
    }

    public int getCompletedCommandCount() {
        return currentCommandId;
    }

    public int getTotalCommands() {
        return totalChildCommands;
    }

    public List<SwingCommand> getChildCommands() {
        return Collections.unmodifiableList(childCommands);
    }

    public void doCancel() {
        taskListenerProxy.getCurrentChildTask().cancel();
    }

    protected abstract P getProgress(int currentCommandId, int totalCommands, Task currentChildCommand);

    /**
     * Receives execution observer events from child commands and fires step reached events to
     * this composites observers
     */
    private class TaskListenerProxy extends TaskListenerAdapter {
        private volatile boolean errorOccurred;
        private volatile Task currentChildTask;
        private volatile boolean lastCommandCancelled;
        private volatile Throwable lastCommandError;

        public TaskListenerProxy() {
        }

        @Override
        public void started(Task task) {
            this.currentChildTask = task;
            fireProgress(getProgress(currentCommandId, totalChildCommands, task));
        }

        @Override
        public void finished(Task task) {
            lastCommandCancelled = task.isCancelled();
        }

        @Override
        public void error(Task task, Throwable e) {
            errorOccurred = true;
            lastCommandError = e;
        }

        public boolean isErrorOccurred() {
            return errorOccurred;
        }

        public Task getCurrentChildTask() {
            return currentChildTask;
        }

        public boolean isLastCommandCancelled() {
            return lastCommandCancelled;
        }

        public Throwable getLastCommandError() {
            return lastCommandError;
        }
    }

    private static class CompositeCommandException extends Exception {

        private CompositeCommandException(Throwable cause) {
            super("Error while executing composite command", cause);
        }
    }

    private static class CompositeExecutorFactory implements SwingCommand.ExecutorFactory {

        public Executor getExecutor(Task e) {
            if (e instanceof BackgroundTask) {
                return SYNCHRONOUS_EXECUTOR;
            } else {
                return INVOKE_AND_WAIT_EXECUTOR;
            }
        }
    }

    /**
     * Created by IntelliJ IDEA.
     * User: Nick Ebbutt (refactored from EBondMarketService)
     * Date: 23-Apr-2009
     * Time: 11:43:44
     */
    static class IfSubThreadInvokeAndWaitExecutor implements Executor {
        public void execute(Runnable command) {
            if (SwingUtilities.isEventDispatchThread()) {
                command.run();
            } else {
                //if command kicked off on a subthread we don't want to block it on the event thread
                //longer than necessary for performance reasons, so use invoke later rather than invokeAndWait
                try {
                    SwingUtilities.invokeAndWait(command);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
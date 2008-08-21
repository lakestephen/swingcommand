package com.od.swing.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 * Can be used to combine child async commands so that they execute together as one unit
 * The child commands are executed in the order they are added
 *
 * All child command in the composite a provided with a proxy LifeCycleMonitor by the parent command
 * during processing. This allows the parent command to fire step reached events on its life cycle monitors when each child task starts
 * (e.g so a progress bar can repaint each time a child task completes).
 *
 * If a child command throws an exception, this will abort processing, and the exception will be propagated to the parents'
 * LifeCycleMonitor. 
 *
 * Child tasks' LifeCycleMonitor instances will recieve events as normal as each child task is processed.
 *
 * The execution for CompositeAsyncCommand implements Cancelable
 * Cancelling the execution will cause the command to abort after the currently processing child command finished execution
 */
public abstract class CompositeAsyncCommand extends AbstractAsynchronousCommand<CompositeExecution> {

    private List<Command> childCommands = new ArrayList<Command>(3);
    private boolean abortOnError;

    public CompositeAsyncCommand(String name, Command... childCommands) {
        super(name);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public CompositeAsyncCommand(String name, boolean isSynchronousMode, Command... childCommands) {
        super(name, isSynchronousMode);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public CompositeAsyncCommand(String name, boolean isSynchronousMode, CommandController<? super com.od.swing.command.CompositeExecution> commandController, Command... childCommands) {
        super(name, isSynchronousMode, commandController);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public void addCommand(Command command) {
        childCommands.add(command);
    }

    public void addCommands(Command... commands) {
        childCommands.addAll(Arrays.asList(commands));
    }

    public void addCommands(Collection<Command> commands) {
        childCommands.addAll(commands);
    }

    /**
     * @param abortOnError - whether an error in a command causes the remaining commands to be aborted.
     */
    public void setAbortOnError(boolean abortOnError) {
        this.abortOnError = abortOnError;
    }

    public com.od.swing.command.CompositeExecution createExecution() {
        return new CompositeExecution();
    }

    class CompositeExecution implements com.od.swing.command.CompositeExecution {

        private int totalChildCommands = childCommands.size();
        private int currentCommandId;
        private Command currentCommand;
        private volatile boolean isCancelled;

         /**
         * Execute the child commands here off the swing thread.
         * This will not start a new subthread - the child commands will run synchronously in the parent command's execution thread.
         *
         * @throws Exception
         */
        public void doExecuteAsync() throws Exception {
            LifeCycleMonitorProxy lifeCycleProxy = new LifeCycleMonitorProxy(this);
            currentCommandId = 0;
            for (Command command : childCommands) {
                currentCommand = command;
                currentCommandId++;
                command.execute(lifeCycleProxy);  //we are not in event thread here, so this should be synchronous

                //abort processing if a command has generated an error via the TaskServicesProxy
                if (lifeCycleProxy.isErrorOccurred() && abortOnError || isCancelled) {
                    break;
                }
            }
        }

        public String getCurrentCommandDescription() {
            return currentCommand.toString();
        }

        public int getCurrentCommand() {
            return currentCommandId;
        }

        public int getTotalCommands() {
            return totalChildCommands;
        }

        public void cancelExecution() {
            isCancelled = true;
        }

        public void doAfterExecute() throws Exception {
        }


        private class LifeCycleMonitorProxy implements LifeCycleMonitor  {
            private boolean errorOccurred;
            private CompositeExecution commandExecution;

            public LifeCycleMonitorProxy(CompositeExecution commandExecution) {
                this.commandExecution = commandExecution;
            }

            public void started(String commandName, Object commandExecution) {
                fireStepReached(commandName, this.commandExecution);
            }

            public void stepReached(String commandName, Object commandExecution) {
            }

            public void ended(String commandName, Object commandExecution) {
            }

            public void error(String commandName, Object commandExecution, Throwable e) {
                errorOccurred = true;
                fireError(commandName, this.commandExecution, e);
            }

            public boolean isErrorOccurred() {
                return errorOccurred;
            }
        }

    }
}



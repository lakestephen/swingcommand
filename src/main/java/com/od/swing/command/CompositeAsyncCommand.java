package com.od.swing.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

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
 */
public abstract class CompositeAsyncCommand extends AbstractAsynchronousCommand<CompositeAsyncCommand.CompositeExecution> {

    private List<Command> childCommands = new ArrayList<Command>(3);
    private boolean abortOnError;

    public CompositeAsyncCommand(String name, boolean isSynchronousMode, Command... childCommands) {
        super(name, isSynchronousMode);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public CompositeAsyncCommand(String name, boolean isSynchronousMode, CommandController<? super CompositeExecution> commandController, Command... childCommands) {
        super(name, isSynchronousMode, commandController);
        this.childCommands.addAll(Arrays.asList(childCommands));
    }

    public void addCommand(AbstractAsynchronousCommand asynchronousCommand) {
        childCommands.add(asynchronousCommand);
    }

    /**
     * @param abortOnError - whether an error in a command causes the remaining commands to be aborted.
     */
    public void setAbortOnError(boolean abortOnError) {
        this.abortOnError = abortOnError;
    }

    public CompositeExecution createExecution() {
        return new CompositeExecution();
    }

    public class CompositeExecution implements CommandExecution {

        private int totalChildCommands = childCommands.size();
        private int currentCommandId;
        private Command currentCommand;

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
                if (lifeCycleProxy.isErrorOccurred() && abortOnError) {
                    break;
                }
            }
        }

        public Command getCurrentCommand() {
            return currentCommand;
        }

        public int getCurrentCommandId() {
            return currentCommandId;
        }

        public int getTotalCommands() {
            return totalChildCommands;
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



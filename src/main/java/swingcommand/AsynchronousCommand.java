package swingcommand;

import java.util.concurrent.Executor;


/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 *
 *  @param <E> The type of AsynchronousExecution this asynchronous command will use
 */
public interface AsynchronousCommand<E extends AsynchronousExecution> extends Command<E> {

    /**
     * @param executor executor to use for this execution only
     * @param executionObservers extra observers to be notified during this execution only
     * @return an object which represents the result of this command execution, or for asynchronous commands, the execution in progress
     */
    E execute(Executor executor, ExecutionObserver<? super E>... executionObservers);

    /**
     * @param executor the default executor to use for this Command
     */
    void setExecutor(Executor executor);
}

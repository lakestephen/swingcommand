package swingcommand;

import java.util.concurrent.Executor;


/**
 * @author Nick Ebbutt, Object Definitions Ltd. http://www.objectdefinitions.com
 */
public interface AsynchronousCommand<E extends AsynchronousExecution> extends Command<E> {

    /**
     * @param executor executor to use for this execution only
     * @param executionObservers extra observers to be notified during this execution only
     * @return an object which represents the result of this command execution, or for asynchronous commands, the execution in progress
     */
    E execute(Executor executor, ExecutionObserver<? super E>... executionObservers);
}

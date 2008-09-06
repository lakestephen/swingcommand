package swingcommand;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 27-Jun-2008
 * Time: 09:49:24
 */
interface CommandExecutor<E> {

    List<ExecutionObserver<? super E>> getExecutionObservers();

    void executeCommand();
}

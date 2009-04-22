package swingcommand2;

/**
     * One ExecutionManager exists per execution
 * It maintains the set of observers for the execution, and contains the logic to run the execution, while notifying ExecutionObserver of the progress
 */
interface ExecutionManager<E> {

    void executeCommand();
}

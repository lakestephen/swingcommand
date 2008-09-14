package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 09-Sep-2008
 * Time: 14:50:21
 */
public interface Cancellable {
    
    /**
    * Call this method to request execution be cancelled
    */
    void cancel();

    /**
     * @return true, if this execution was cancelled
     */
    boolean isCancelled();

    /**
     * @return true, if the command supports cancellation
     */
    boolean isCancellable();
}

package swingcommand;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Aug-2008
 * Time: 17:43:43
 */
public interface CompositeExecution<C> extends AsynchronousExecution {

    /**
     * @return the child command, or execution for AsynchronousCommand instances
     */
   public C getCurrentChildExecution();

   public int getCurrentChildId();

   public int getTotalChildren();
    
}

package swingcommand;

/**
 * Created by IntelliJ IDEA.
* User: Nick Ebbutt
* Date: 23-Sep-2008
* Time: 09:54:05
*/
public interface SequentialExecution<C extends CommandExecution> extends CompositeExecution<C> {

      public C getCurrentExecution();
}

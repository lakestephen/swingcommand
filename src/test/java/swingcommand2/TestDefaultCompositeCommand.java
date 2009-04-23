/**
 *  This file is part of ObjectDefinitions SwingCommand
 *  Copyright (C) Nick Ebbutt September 2009
 *  Licensed under the Academic Free License version 3.0
 *  http://www.opensource.org/licenses/afl-3.0.php
 *
 *  nick@objectdefinitions.com
 *  http://www.objectdefinitions.com/swingcommand
 */

package swingcommand2;


/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 30-Aug-2008
 * Time: 00:16:27
 * To change this template use File | Settings | File Templates.
 */
public class TestDefaultCompositeCommand extends CommandTest {

   public void testSimpleComposite() {

       final SwingCommand child1 = new SwingCommand() {
           protected Execution createExecution() {
               return new BackgroundExecution() {

                   public void setState(ExecutionState executionState) {
                       System.out.println("Called " + executionState);
                        super.setState(executionState);
                   }

                   public void doInBackground() throws Exception {
                       System.out.println("Called do in Background");
                   }

                   public void doInEventThread() throws Exception {
                       System.out.println("Called do in Event Thread");
                   }
               };
           }
       };

       SwingCommand compositeCommand = new SwingCommand() {
           protected Execution createExecution() {
               return new CompositeExecution(child1);
           }
       };
       compositeCommand.execute();
   }
}
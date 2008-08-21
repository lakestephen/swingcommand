package com.od.swing.command;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 21-Aug-2008
 * Time: 17:43:43
 */
public interface CompositeExecution extends CommandExecution {

    /**
     * Call this method to request the the execution be cancelled after the
     * currently executing child command completes
     */
   void cancelExecution();

   public String getCurrentCommandDescription();

   public int getCurrentCommand();

   public int getTotalCommands();
    
}

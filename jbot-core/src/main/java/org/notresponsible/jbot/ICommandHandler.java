package org.notresponsible.jbot;

/**
 * Created by dwimsey on 2/7/16.
 */
public interface ICommandHandler {
	/**
	 * Attempts to handle a command, returns true if the command was handled by the handler called
	 * @param replyHandler
	 * @param commandName
	 * @param args
	 * @return
	 */
	boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args);
}

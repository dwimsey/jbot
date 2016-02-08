package org.notresponsible.jbot;

/**
 * Created by dwimsey on 2/7/16.
 */
public interface ICommandResponseMessageHandler {
	void sendReply(String basicTextResponse, boolean isPrivate);
	Object getSessionHandle();
	Object getEventHandle();
}

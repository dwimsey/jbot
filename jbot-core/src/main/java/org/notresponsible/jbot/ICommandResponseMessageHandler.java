package org.notresponsible.jbot;

/**
 * Created by dwimsey on 2/7/16.
 */
public interface ICommandResponseMessageHandler {
	void setReply(String basicTextResponse);
	Object getSessionHandle();
	Object getEventHandle();
}

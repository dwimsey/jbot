package org.notresponsible.jbot.plugins;

import org.apache.log4j.Logger;
import org.notresponsible.jbot.ICommandHandler;
import org.notresponsible.jbot.ICommandResponseMessageHandler;
import org.notresponsible.jbot.IJBotPlugin;
import org.notresponsible.jbot.JBotService;

import java.util.Properties;

/**
 * Created by dwimsey on 3/7/16.
 *
 * This is a simple example plugin which just responds to an incoming !HelloWorld command with:
 * Hello World!
 *
 */
public class HelloWorldPlugin implements IJBotPlugin {
	private static final Logger LOG = Logger.getLogger(HelloWorldPlugin.class);

	private Properties props = null;
	JBotService jbotService = null;
	boolean isRunning = false;

	public void prepare(Properties pluginProperties, JBotService hostService) {
		this.props = pluginProperties;
		this.jbotService = hostService;

		jbotService.addCommandHandler("HelloWorld", new ICommandHandler() {
			@Override
			public boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args) {
				Object internalObject = replyHandler.getEventHandle();
				String a = internalObject.getClass().getCanonicalName();
				// This reply will return in the same way the message was received, meaning it will go to a group
				// if this was a group message or directly back to the sender if this was a one on one direct message.
				replyHandler.sendReply("Hello World!", false);
				// This will go directly back to the original sender, privately if possible
				replyHandler.sendReply("Hello World Message class: " + internalObject.getClass().getClassLoader().toString(), true);
				return true;
			}
		});
	}

	public String getPluginName() {
		return("HelloWorld");
	}

	public Object getPluginParameter(String parameterName) {
		return null;
	}

	public void start() {
		isRunning = true;
	}

	public void stop() {
		isRunning = false;
	}

	public boolean isRunning() {
		return this.isRunning;
	}
}

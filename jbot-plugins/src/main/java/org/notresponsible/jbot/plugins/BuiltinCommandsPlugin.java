package org.notresponsible.jbot.plugins;

import org.apache.log4j.Logger;
import org.notresponsible.jbot.ICommandHandler;
import org.notresponsible.jbot.ICommandResponseMessageHandler;
import org.notresponsible.jbot.IJBotPlugin;
import org.notresponsible.jbot.JBotService;

import java.util.Properties;

/**
 * Created by dwimsey on 2/8/16.
 */
public class BuiltinCommandsPlugin implements IJBotPlugin {
	private static final Logger LOG = Logger.getLogger(BuiltinCommandsPlugin.class);

	private Properties props = null;
	JBotService jbotService = null;
	boolean isRunning = false;

	public void prepare(Properties pluginProperties, JBotService hostService) {
		this.props = pluginProperties;
		this.jbotService = hostService;
	}

	public String getPluginName() {
		return("BuiltinCommandsPlugin");
	}

	public Object getPluginParameter(String parameterName) {
		return null;
	}

	public void start() {
		isRunning = true;
		jbotService.addCommandHandler("die", new ICommandHandler() {

			public boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args) {
				replyHandler.sendReply("Shutting down, good night.", false);
				jbotService.stop();
				return true;
			}
		});

		jbotService.addCommandHandler("msgd", new ICommandHandler() {
			@Override
			public boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args) {
				Object internalObject = replyHandler.getEventHandle();
				String a = internalObject.getClass().getCanonicalName();
				replyHandler.sendReply("Message class: " + a, true);
				return true;
			}
		});
	}

	public void stop() {
		isRunning = false;
	}

	public boolean isRunning() {
		return this.isRunning;
	}
}

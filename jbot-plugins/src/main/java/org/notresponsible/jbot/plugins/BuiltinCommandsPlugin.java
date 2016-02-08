package org.notresponsible.jbot.plugins;

import com.ullink.slack.simpleslackapi.SlackSession;
import org.apache.log4j.Logger;
import org.notresponsible.jbot.ICommandHandler;
import org.notresponsible.jbot.ICommandResponseMessageHandler;
import org.notresponsible.jbot.IJBotPlugin;
import org.notresponsible.jbot.jBotService;

import java.util.Properties;

/**
 * Created by dwimsey on 2/8/16.
 */
public class BuiltinCommandsPlugin implements IJBotPlugin {
	private static final Logger LOG = Logger.getLogger(BuiltinCommandsPlugin.class);

	private Properties props = null;
	jBotService jbotService = null;
	boolean isRunning = false;

	public void prepare(Properties pluginProperties, jBotService hostService) {
		this.props = pluginProperties;
		this.jbotService = hostService;
	}

	public String getPluginName() {
		return("BuiltinCommandsPlugin");
	}

	public void start() {
		isRunning = true;
		jbotService.addCommandHandler("die", new ICommandHandler() {

			public boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args) {
				replyHandler.setReply("Shutting down, good night.");
				jbotService.stop();
				return true;
			}
		});


		jbotService.addCommandHandler("msgd", new ICommandHandler() {
			@Override
			public boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args) {
				Object internalObject = replyHandler.getEventHandle();
				String a = internalObject.getClass().getCanonicalName();
				replyHandler.setReply("Message class: " + a);
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

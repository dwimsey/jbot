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
	private static final int RESTART_EXIT_CODE = 27;	// this value is returned from the service executable when are
														// stop has occurred as a response to a restart request, so the
														// script will attempt to restart the service, which will pickup
														// environment changes
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

		jbotService.addCommandHandler("restart", new ICommandHandler() {
			public boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args) {
				replyHandler.sendReply("brb, restarting ...", false);
				// Exit code 27 can be used with the external jbot.sh/jbot.cmd scripts to restart jbot on request from
				// a channel using this command.
				jbotService.stop(RESTART_EXIT_CODE);
				return true;
			}
		});

		jbotService.addCommandHandler("msgd", new ICommandHandler() {
			@Override
			public boolean processCommand(ICommandResponseMessageHandler replyHandler, String commandName, String[] args) {
				Object internalObject = replyHandler.getEventHandle();
				String a = internalObject.getClass().getCanonicalName();
				replyHandler.sendReply("Message class: " + a, true);
				replyHandler.sendReply("Message class: " + internalObject.getClass().getClassLoader().toString(), true);
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

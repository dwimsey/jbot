package org.notresponsible.jbot.plugins;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackUser;
import org.apache.log4j.Logger;
import org.notresponsible.jbot.ICommandResponseMessageHandler;
import org.notresponsible.jbot.IJBotPlugin;

import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import org.notresponsible.jbot.JBotService;

import java.io.IOException;
import java.util.*;

/**
 * Created by dwimsey on 2/6/16.
 */
public class SlackBotPlugin implements IJBotPlugin {
	private static final Logger LOG = Logger.getLogger(SlackBotPlugin.class);

	private Properties props = null;
	JBotService jbotService = null;

	String _cmdPrefix = "!";
	String _authToken = "No+Token+Configured";

	public void prepare(Properties pluginProperties, JBotService hostService) {
		this.props = pluginProperties;
		this.jbotService = hostService;

		_cmdPrefix = props.getProperty("CommandPrefix", _cmdPrefix);
		_authToken = props.getProperty("AuthenticationToken", _authToken);
	}

	public String getPluginName() {
		return("SlackBotPlugin");
	}

	public Object getPluginParameter(String parameterName) {
		if("session".equals(parameterName) == true) {
			return(session);
		}
		return null;
	}

	private SlackSession session = null;
	public void start() {
		session = SlackSessionFactory.createWebSocketSlackSession(_authToken);

		session.addMessagePostedListener(new SlackMessagePostedListener() {
			@Override
			public void onEvent(final SlackMessagePosted event, final SlackSession session) {
				SlackUser senderUser = event.getSender();
				String senderUserName = senderUser.getUserName();
				SlackChannel senderChannelObject = event.getChannel();
				String senderChannel = senderChannelObject.getName();
				String senderMessage = event.getMessageContent();

				if(senderMessage.startsWith(_cmdPrefix) == true) {
					ICommandResponseMessageHandler responseHandler = new ICommandResponseMessageHandler() {
						SlackSession _session = session;
						SlackMessagePosted _event = event;

						public void sendReply(String basicTextResponse, boolean isPrivate) {
							if(isPrivate == true) {
								_session.sendMessageToUser(_event.getSender(), basicTextResponse, null);
							} else {
								_session.sendMessage(_event.getChannel(), basicTextResponse, null);
							}
						}

						public Object getSessionHandle() {
							return _session;
						}

						public Object getEventHandle() {
							return _event;
						}
					};

					// this is a possible command, pass it to the command processor
					String[] parts = senderMessage.split("\\s+", 2);
					jbotService.processCommand(responseHandler, parts[0].replaceFirst(_cmdPrefix, ""), (parts.length > 1 ? parts[1] : null));
				}

				LOG.info("<" + senderUserName + "/" + senderChannel + "> " + senderMessage);
			}
		});
		try {
			session.connect();
		} catch (IOException e) {
			LOG.error("Could not connect to slack service.", e);
		}
	}

	public void stop() {
		if(session != null) {
			try {
				session.disconnect();
			} catch (IOException e) {
				LOG.error("Could not disconnect from slack.", e);
			}
			session = null;
		}
	}

	public boolean isRunning() {
		if(session == null) {
			return false;
		} else {
			return true;
		}
	}
}

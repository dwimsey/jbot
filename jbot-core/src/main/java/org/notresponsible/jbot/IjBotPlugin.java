package org.notresponsible.jbot;

import java.util.Properties;

/**
 * Created by dwimsey on 2/6/16.
 */
public interface IJBotPlugin {
	String getPluginName();
	void prepare(Properties pluginProperties, JBotService hostService);
	void start();
	void stop();
	boolean isRunning();

	Object getPluginParameter(String parameterName);
}

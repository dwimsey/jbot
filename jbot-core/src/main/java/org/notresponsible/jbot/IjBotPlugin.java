package org.notresponsible.jbot;

import java.util.Properties;

/**
 * Created by dwimsey on 2/6/16.
 */
public interface IJBotPlugin {
	String getPluginName();
	void prepare(Properties pluginProperties, jBotService hostService);
	void start();
	void stop();
	boolean isRunning();

}

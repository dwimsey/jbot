package org.notresponsible.jbot;

import org.apache.log4j.Logger;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JBotService
{
	private static final Logger LOG = Logger.getLogger(JBotService.class);

	private JBotRuntimeState runtimeState = JBotRuntimeState.NEW;
	InternalServiceThread coordinatorThread = null;
	private Properties props = null;

	public JBotService(Properties newProperties) {
		this.props = newProperties;
	}

	private Map<String, IJBotPlugin> loadedPlugins = new HashMap<String, IJBotPlugin>();

	public JBotRuntimeState getRuntimeState() {
		synchronized (this) {
			return(runtimeState);
		}
	}

	// -1 means we've never been STARTED, anything else is the exit code of the most recent attempt to start, 0 indicates as clean shutdown.
	// If the service is running, this value is undetermined.
	int _exitCode = -1;

	void init() throws IOException, IllegalAccessException, InstantiationException {
		synchronized (this) {
			if (runtimeState != JBotRuntimeState.NEW) {
				throw new IllegalAccessError("Thread can not be initialized, thread is not new: " + runtimeState.toString());
			}
			runtimeState = JBotRuntimeState.INITIALIZING;
		}
		LOG.info( "jBot initializing ..." );
		//Reflections reflections = new Reflections("org.notresponsible.jbot.plugins");
		Reflections reflections = new Reflections();
		Class classToLoad = null;
		String pluginClassName = null;

		String pluginDirectory = props.getProperty("PluginDirectory", "");
		if("".equals(pluginDirectory) == false) {
			LOG.info("Loading plugins jars ... ");
			URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();

			String pattern = "(.*)(jbot-plugin-)(.*)\\.jar";
			pattern = props.getProperty("PluginFilenamePattern", pattern);

			// Create a Pattern object
			final Pattern regexPattern = Pattern.compile(pattern);

			Files.walk(Paths.get(pluginDirectory)).forEach(new Consumer<Path>() {
				@Override
				public void accept(Path pathObj) {
					String path = pathObj.toString();

					// Now create matcher object.
					Matcher m = regexPattern.matcher(path);
					if (m.find()) {
						LOG.debug("Found plugin jar: " + path);

						File f = new File(path);
						URLClassLoader urlCl = null;
						try {
							urlCl = new URLClassLoader(new URL[]{f.toURL()}, System.class.getClassLoader());
						} catch (MalformedURLException e) {
							LOG.warn("Could not load plugin jar: " + f.toString(), e);
						}

						LOG.info("Loaded jar for: " + urlCl.toString());
					}
				}
			});
		}

		LOG.info("Searching for plugins ...");
		Set<Class<? extends IJBotPlugin>> modules = reflections.getSubTypesOf(IJBotPlugin.class);

		LOG.info("Enumerating plugin interfaces ...");
		for(Class<? extends IJBotPlugin> pluginModule : modules) {
			pluginClassName = pluginModule.getCanonicalName();
			LOG.debug("Plugin class: " + pluginClassName);
			classToLoad = null;
			try {
				classToLoad = Class.forName (pluginClassName, true, pluginModule.getClassLoader());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				LOG.error("Could not instantiate plugin: " + pluginClassName + ": " + e.getMessage());
				continue;
			}

			IJBotPlugin pluginInstance = (IJBotPlugin) classToLoad.newInstance();

			if(pluginInstance == null) {
				LOG.error("Could not instantiate plugin: " + pluginClassName + " from: " + classToLoad.toString());
				continue;
			}

			loadedPlugins.put(pluginClassName, pluginInstance);
			LOG.info("Instantiated plugin: " + pluginInstance.getPluginName());

			pluginInstance.prepare(props, this);
		}

		LOG.info("Bot initialized, setting state to STOPPED");
		runtimeState = JBotRuntimeState.STOPPED;
	}

	void start() throws IOException, InterruptedException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		synchronized (this) {
			if (runtimeState != JBotRuntimeState.STOPPED) {
				throw new IllegalAccessError("Thread can not be started, thread is not stopped: " + runtimeState.toString());
			}
			runtimeState = JBotRuntimeState.STARTING;
			_exitCode = 0;
			coordinatorThread = new InternalServiceThread(this);
		}
		coordinatorThread.start();
	}

	public byte getExitCode() {
		synchronized (this) {
			return (byte)_exitCode;
		}
	}

	public Connection getBotJdbcConnection() throws ClassNotFoundException, SQLException, InvalidPropertiesFormatException {
		String _JDBCConnectionString =  props.getProperty("JBot.ConnectionString");
		String _JDBCDriverClass =  props.getProperty("JBot.DriverClass");

		if(_JDBCConnectionString == null) {
			throw new InvalidPropertiesFormatException("JBot.ConnectionString is missing or empty.  JBot database connection is unavailable.");
		}

		if(_JDBCConnectionString.isEmpty() == true) {
			throw new InvalidPropertiesFormatException("JBot.ConnectionString is empty.  JBot database connection is unavailable.");
		}

		if(_JDBCDriverClass != null) {
			// this will load the driver for connecting to DTH, in case it hasn't already been done and can not be found
			// already on the CLASSPATH
			Class.forName(_JDBCDriverClass);
		}

		return(DriverManager.getConnection(_JDBCConnectionString));
	}
	private class InternalServiceThread extends Thread {
		JBotService botService = null;
		InternalServiceThread(JBotService hostService) {
			this.botService = hostService;
		}

		public void run() {
			_exitCode = 0;

			LOG.info("Starting plugins ...");
			for(Map.Entry<String, IJBotPlugin> pluginEntry : loadedPlugins.entrySet()) {
				String className = pluginEntry.getKey();
				IJBotPlugin pluginInstance = pluginEntry.getValue();
				LOG.debug(className);
				pluginInstance.start();
			}

			synchronized (this) {
				runtimeState = JBotRuntimeState.RUNNING;
			}

			boolean keepGoing = true;
			LOG.info("Bot is online.");
			while (keepGoing == true)
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					LOG.warn("Interrupted sleep for bot service thread", e);
				}

				synchronized (this) {
					if(runtimeState != JBotRuntimeState.RUNNING) {
						keepGoing = false;
					}
				}
			}

			if(this.botService.runtimeState == JBotRuntimeState.STOPPING) {
				LOG.info("Stopping plugins ...");
				for (Map.Entry<String, IJBotPlugin> pluginEntry : loadedPlugins.entrySet()) {
					String className = pluginEntry.getKey();
					IJBotPlugin pluginInstance = pluginEntry.getValue();
					LOG.info(className);
					pluginInstance.stop();
				}

				synchronized (this) {
					this.botService.runtimeState = JBotRuntimeState.STOPPED;
				}
			}

			LOG.info("jBot exiting: " + _exitCode);
		}
	}

	public void stop() {
		this.stop(0);
	}

	public void stop(int exitCode) {
		synchronized (this) {
			if(this.runtimeState != JBotRuntimeState.RUNNING) {
				throw new IllegalAccessError("Thread can not be stopped at this time, thread is not running: " + runtimeState.toString());
			}
			runtimeState = JBotRuntimeState.STOPPING;
			_exitCode = exitCode;
		}
	}

	public void join() throws InterruptedException {
		Thread cThread = null; // used to hold a copy of coordinatorThread so we can be thread safe and work outside the synchronized block
		synchronized (this) {
			cThread = coordinatorThread;
		}
		if(cThread != null) {
			cThread.join(); // This join CAN NOT BE inside the synchronized block or it will deadlock the process in multithreaded environments!  this does lead to a minor race condition possibility where coordinatorThread may already be dead.
		}
	}

	public Map<String, String> getHelpCommands() {
		Map<String, String> returnValue = new HashMap<String, String>();
		for (Map.Entry<String, List<ICommandHandler>> cmdEntry : commandHandlers.entrySet()) {
			String cmdName = cmdEntry.getKey();
			String cmdHelp = "Help not yet implemented for this command.";
			returnValue.put(cmdName, cmdHelp);
		}
		return returnValue;
	}

	private Map<String, List<ICommandHandler>> commandHandlers = new HashMap<String, List<ICommandHandler>>();
	public void addCommandHandler(String commandName, ICommandHandler commandHandler) {
		List<ICommandHandler> cmdList;
		if(commandHandlers.containsKey(commandName) == true) {
			cmdList = commandHandlers.get(commandName);
		} else {
			cmdList = new ArrayList<ICommandHandler>();
			commandHandlers.put(commandName, cmdList);
		}
		cmdList.add(commandHandler);
	}

	private ArrayList<ICommandHandler> rawMsgHandlers = new ArrayList<ICommandHandler>();
	public void addRawMessageHandler(String commandName, ICommandHandler commandHandler) {
		rawMsgHandlers.add(commandHandler);
	}

	public IJBotPlugin findPluginByClassName(String className) {
		for (Map.Entry<String, IJBotPlugin> pluginEntry : loadedPlugins.entrySet()) {
			String entryClassName = pluginEntry.getKey();
			if(className.equals(entryClassName) == true) {
				return(pluginEntry.getValue());
			}
		}
		return(null);
	}

	public IJBotPlugin findPluginByName(String className) {

		for (IJBotPlugin pluginInstance : loadedPlugins.values()) {
			if(className.equals(pluginInstance.getPluginName()) == true) {
				return(pluginInstance);
			}
		}
		return(null);
	}

	public void processCommand(ICommandResponseMessageHandler replyHandler, String commandName,  String... args) {
		StringBuilder builder = new StringBuilder();
		for(String s : args) {
			builder.append(s);
		}

		boolean processed = false;
		if(commandHandlers.containsKey(commandName) == true) {
			List<ICommandHandler> cmdList = commandHandlers.get(commandName);
			for(ICommandHandler iCmd : cmdList) {
				LOG.debug("Calling command processor: " + commandName + ": " + iCmd.toString());
				if(iCmd.processCommand(replyHandler, commandName, args) == true) {
					processed = true;
					// returning true means the command was handled, break out of the loop
					break;
				}
			}
		}
		if(processed == false) {
			LOG.info("Unhandled command: " + commandName + " args: " + builder.toString());
		}
	}

	public void processRawMessage(ICommandResponseMessageHandler replyHandler, String commandName,  String... args) {
		for(ICommandHandler iCmd : rawMsgHandlers) {
			// We call the raw message handler for every message, not trying to do a command match, thats up to the
			// handler to deal with
			LOG.debug("Calling raw message processor: " + commandName + ": " + iCmd.toString());
			if(iCmd.processCommand(replyHandler, commandName, args) == true) {
				// returning true means the command was handled, break out of the loop and do the same
				return;
			}
		}
	}
}

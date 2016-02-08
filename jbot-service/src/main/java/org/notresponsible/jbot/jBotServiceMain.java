package org.notresponsible.jbot;

import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class JBotServiceMain
{
	private static final Logger LOG = Logger.getLogger(JBotServiceMain.class);

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure(); // Basic console output for log4j
		LogManager.getRootLogger().setLevel(Level.INFO);

		Options options = new Options();
		options.addOption("help", false, "Display help");
		options.addOption("defaults", false, "Show defaults file");
		options.addOption("nodefaults", false, "Ignore defaults file");
		options.addOption("properties", true, "Properties file to load for configuration properties");
		options.addOption("P", true, "Connection profile to use, if not default.");
		options.addOption("nc", false, "Don'coordinatorThread display rows modified counts for cleaner output.");

		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		try {
			line = parser.parse(options, args);
		} catch(UnrecognizedOptionException uoe) {
			System.err.println("Error with options specified: " + uoe.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "jbot", options );
			return;
		}
		if(line.hasOption("help") == true) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "jbot", options );
			return;
		}

		String propertiesFile = "jbot.properties";

		Properties newProperties = new Properties();
		if(line.hasOption("defaults") == true) {
			newProperties = PropertyResources.loadPropertiesResourceFile(newProperties, propertiesFile);
			StringWriter writer = new StringWriter();
			newProperties.list(new PrintWriter(writer));
			System.out.println("sqlcmd defaults:");
			System.out.println(writer.getBuffer().toString());
			return;
		}

		if(line.hasOption( "nodefaults" ) == false) {
			Properties p = PropertyResources.loadPropertiesResourceFile(newProperties, propertiesFile);
			if(p != null) {
				// If p is NULL, it means we couldn'coordinatorThread load the file specified, so we leave props as it is, so defaults
				// pass through.
				// In this case however, p is not NULL, so update props with the new list
				newProperties = p;
			}
			String home = System.getProperty("user.home");
			File propsFile = new File(home, "." + propertiesFile);
			newProperties = PropertyResources.loadPropertiesFile(newProperties, propsFile.getAbsolutePath());
		}

		if( line.hasOption( "properties" ) == true) {
			if (line.getOptionValue("properties") != null) {
				propertiesFile = line.getOptionValue("properties");
				newProperties = PropertyResources.loadPropertiesFile(newProperties, propertiesFile);
			}
		}


		JBotService bot = new JBotService(newProperties);
		bot.init();
		bot.start();
		bot.join();
		System.exit(bot.getExitCode());
	}
}

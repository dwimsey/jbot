package org.notresponsible.jbot;

import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class jBotServiceMain
{
	private static final Logger LOG = Logger.getLogger(jBotServiceMain.class);

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure(); // Basic console output for log4j
		LogManager.getRootLogger().setLevel(Level.INFO);

		Options options = new Options();
		options.addOption("help", false, "Display help");
		options.addOption("defaults", false, "Show defaults file");
		options.addOption("nodefaults", false, "Ignore defaults file");
		options.addOption("properties", true, "Properties file to load for configuration properties");
		options.addOption("o", true, "Output format: csv, psv, tsv, json, sql, pretty");
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


		jBotService bot = new jBotService(newProperties);
		bot.init();
		bot.start();
		bot.join();
		System.exit(bot.getExitCode());
	}
}

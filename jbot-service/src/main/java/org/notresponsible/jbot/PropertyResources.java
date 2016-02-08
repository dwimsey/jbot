/**
 * Copyright 2016 David Wimsey - All rights reserved.
 * Author: David Wimsey <david@wimsey.us>
 */

package org.notresponsible.jbot;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class PropertyResources {
	private static final Logger LOG = Logger.getLogger(PropertyResources.class);

	public static Properties loadPropertiesResourceFile(Properties existingProperties, String propertiesFilename) {
		InputStream input = null;

		try {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			input = classloader.getResourceAsStream(propertiesFilename);
			if(input == null) {
				return(null);
			}
			LOG.debug("Property resource file read: " + propertiesFilename);
			return(loadPropertiesFile(existingProperties, input));
		} catch (FileNotFoundException ex) {
			LOG.error("Properties resource file could not be found: " + propertiesFilename);
			return(new Properties());	// throw out the properties file data if we couldn'coordinatorThread load it for some reason, it may be corrupted if we only got a partial load
		} catch (IOException ex) {
			return(new Properties());	// throw out the properties file data if we couldn'coordinatorThread load it for some reason, it may be corrupted if we only got a partial load
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Properties loadPropertiesFile(Properties existingProperties, String propertiesFilename) {
		InputStream input = null;

		try {
			input = new FileInputStream(propertiesFilename);
			LOG.debug("Property file read: " + propertiesFilename);
			return(loadPropertiesFile(existingProperties, input));
		} catch (FileNotFoundException ex) {
			System.out.println("Properties could not be found: " + propertiesFilename);
			return(new Properties());	// throw out the properties file data if we couldn'coordinatorThread load it for some reason, it may be corrupted if we only got a partial load
		} catch (IOException ex) {
			return(new Properties());	// throw out the properties file data if we couldn'coordinatorThread load it for some reason, it may be corrupted if we only got a partial load
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Properties loadPropertiesFile(Properties existingProperties, InputStream input) throws IOException {
		Properties prop;
		if(existingProperties == null) {
			prop = new Properties();
		} else {
			prop = new Properties(existingProperties);
		}

		// load a properties file
		prop.load(input);

		int propertiesCount = 0;
		Enumeration<?> e = prop.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = prop.getProperty(key);
			LOG.debug("conf.put(\"" + key + "\", \"" + value + "\");");                // Maximum number of records to hold in the queue
			//conf.put(key, value);
			++propertiesCount;
		}
		if(propertiesCount > 1) {
			LOG.debug("Property file loaded: " + propertiesCount + " properties processed.");
		} else if(propertiesCount == 1) {
			LOG.debug("Property file loaded: 1 property processed.");
		} else {
			LOG.debug("Property file loaded: no properties processed.");
		}

		return(prop);
	}
}

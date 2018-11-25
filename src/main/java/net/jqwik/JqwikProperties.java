package net.jqwik;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class JqwikProperties {

	private static final String[] SUPPORTED_PROPERTIES = new String[]{
		"database",
		"rerunFailuresWithSameSeed",
		"runFailuresFirst",
		"defaultTries",
		"defaultMaxDiscardRatio",
		"useJunitPlatformReporter"
	};

	private static final String PROPERTIES_FILE_NAME = "jqwik.properties";
	private static final Logger LOG = Logger.getLogger(JqwikProperties.class.getName());

	private static final String DEFAULT_DATABASE_PATH = ".jqwik-database";
	private static final String DEFAULT_RERUN_FAILURES_WITH_SAME_SEED = "true";
	private static final String DEFAULT_RERUN_FAILURES_FIRST = "false";
	private static final String DEFAULT_TRIES = "1000";
	private static final String DEFAULT_MAX_DISCARD_RATIO = "5";

	// TODO: Change default to true as soon as Gradle has support for platform reporter
	// see https://github.com/gradle/gradle/issues/4605
	private static final String DEFAULT_USE_JUNIT_PLATFORM_REPORTER = "false";

	private String databasePath;
	private boolean rerunFailuresWithSameSeed;
	private boolean runFailuresFirst;
	private int defaultTries;
	private int defaultMaxDiscardRatio;
	private boolean useJunitPlatformReporter;

	public String databasePath() {
		return databasePath;
	}

	public boolean rerunFailuresWithSameSeed() {
		return rerunFailuresWithSameSeed;
	}

	public boolean runFailuresFirst() {
		return runFailuresFirst;
	}

	public int defaultTries() {
		return defaultTries;
	}

	public int defaultMaxDiscardRatio() {
		return defaultMaxDiscardRatio;
	}

	public boolean useJunitPlatformReporter() {
		return useJunitPlatformReporter;
	}

	JqwikProperties() {
		this(PROPERTIES_FILE_NAME);
	}

	JqwikProperties(String fileName) {
		loadProperties(fileName);
	}

	private void loadProperties(String propertiesFileName) {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFileName);
		if (inputStream == null) {
			LOG.info(String.format("No Jqwik properties file [%s] found.", propertiesFileName));
			inputStream = new ByteArrayInputStream("".getBytes());
		}

		Properties properties = new Properties();
		try {
			properties.load(inputStream);
			warnOnUnsupportedProperties(properties);
			databasePath = properties.getProperty("database", DEFAULT_DATABASE_PATH);
			rerunFailuresWithSameSeed = Boolean.parseBoolean(properties.getProperty("rerunFailuresWithSameSeed", DEFAULT_RERUN_FAILURES_WITH_SAME_SEED));
			runFailuresFirst = Boolean.parseBoolean(properties.getProperty("runFailuresFirst", DEFAULT_RERUN_FAILURES_FIRST));
			defaultTries = Integer.parseInt(properties.getProperty("defaultTries", DEFAULT_TRIES));
			defaultMaxDiscardRatio = Integer.parseInt(properties.getProperty("defaultMaxDiscardRatio", DEFAULT_MAX_DISCARD_RATIO));
			useJunitPlatformReporter = Boolean.parseBoolean(properties.getProperty("useJunitPlatformReporter", DEFAULT_USE_JUNIT_PLATFORM_REPORTER));
		} catch (IOException ioe) {
			LOG.log(Level.WARNING, String.format("Error while reading properties file [%s] found.", propertiesFileName), ioe);
		}

	}

	private void warnOnUnsupportedProperties(Properties properties) {
		for (String propertyName : properties.stringPropertyNames()) {
			if (!Arrays.asList(SUPPORTED_PROPERTIES).contains(propertyName)) {
				String message = String.format("Property [%s] is not supported in '%s' file", propertyName, PROPERTIES_FILE_NAME);
				LOG.log(Level.WARNING, message);
			}
		}
	}

}

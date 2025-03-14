package com.vrivoire.imdbsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Vincent
 */
public enum Config {
	WEB_BROWSER,
	ICON,
	REPORT_NAME,
	PATTERN,
	SUPPORTED_EXTENSIONS,
	TORRENTS,
	IGNORED_FOLDERS,
	OUTPUT_JSON_FILE,
	IMDBSEARCH_PY_PATH,
	VERSION_JQUERY_UI,
	VERSION_JQUERY,
	VERSION_DATATABLES,
	VERSION_DATATABLES_COLREORDER,
	DB_PROTOCOL,
	DB_URL,
	IS_IMAGES_EMBEDED,
	IS_SLIM;

	private static final Logger LOG = LogManager.getLogger(Config.class);
	private static final Map<String, Object> MAP = new TreeMap<>();
	private static Thread thread = null;

	public static void configure() {
		try {
			final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

			InputStream is = new NameYearBean().getClass().getResourceAsStream("/config.json");
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader reader = new BufferedReader(isr);
			StringBuilder sb = new StringBuilder();
			reader.lines().forEachOrdered((String line) -> {
				sb.append(line);
			}
			);
			String defaults = sb.toString();
			MAP.putAll(objectMapper.readValue(defaults, new TypeReference<Map<String, Object>>() {
			}));
			LOG.info("Default configuration " + (defaults != null && !defaults.isEmpty() ? "" : "not") + " found");

			File file = new File("config.json");
			if (!file.exists()) {
				file = new File("bin/config.json");
			}
			if (!file.exists() && defaults == null) {
				LOG.fatal("Config file not found. exiting.");
				if (thread != null) {
					try {
						thread.interrupt();
					} catch (SecurityException e) {
						LOG.error(e.getMessage(), e);
					}
				}
				System.exit(-1);
			}
			if (file.exists()) {
				LOG.info("Config file found in: " + file.getAbsolutePath());

				MAP.putAll(objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {
				}));

				@SuppressWarnings("unchecked")
				List<String> extensions = (List<String>) SUPPORTED_EXTENSIONS.get();
				MAP.put(SUPPORTED_EXTENSIONS.name(), extensions);

				StartFileWatchDog(file);
			} else {
				LOG.info("Using default configuration.");
				MAP.putAll(objectMapper.readValue(defaults, new TypeReference<Map<String, Object>>() {
				}));
			}
			Map<String, Object> mapProperties = new HashMap<>();
			System.getenv().entrySet().forEach(x -> {
				mapProperties.put((String) x.getKey().toUpperCase(), x.getValue());
			});
			MAP.entrySet().stream().filter(x -> (x.getValue() instanceof String && !x.getKey().equals("PATTERN"))).forEachOrdered(x -> {
				MAP.put(x.getKey(), fill((String) x.getValue(), mapProperties));
			});
			LOG.debug("0 - Configuration:\n" + System.getenv());
			LOG.debug("1 - Configuration:\n" + MAP);
			LOG.debug("2 - Configuration:\n" + objectMapper.writeValueAsString(MAP));

		} catch (IOException ex) {
			LOG.fatal(ex.getMessage(), ex);
			if (thread != null) {
				try {
					thread.interrupt();
				} catch (SecurityException e) {
					LOG.error(e.getMessage(), e);
				}
			}
			System.exit(-1);
		}
	}

	public static String fill(String template, Map<String, Object> movieMap) {
		var sub = new StringSubstitutor(movieMap);
		return sub.replace(template);
	}

	private static void StartFileWatchDog(File file) {
		thread = new Thread("fileWatchDog") {
			@Override
			public void run() {
				try {
					LOG.info("Thread " + getName() + " started.");
					WatchService watchService = FileSystems.getDefault().newWatchService();
					Path path = Paths.get(file.getParentFile().getAbsolutePath());
					path.register(
							watchService,
							StandardWatchEventKinds.ENTRY_CREATE,
							StandardWatchEventKinds.ENTRY_DELETE,
							StandardWatchEventKinds.ENTRY_MODIFY);
					WatchKey key;

					while ((key = watchService.take()) != null) {
						for (WatchEvent<?> event : key.pollEvents()) {
							if (event.kind().name().equals("ENTRY_MODIFY")) {
								LOG.info(getName() + ", event kind:" + event.kind() + ". File affected: " + event.context() + ".");
							}
						}
						key.reset();
					}
				} catch (IOException | InterruptedException ex) {
					LOG.fatal(ex.getMessage(), ex);
					if (thread != null) {
						try {
							thread.interrupt();
						} catch (SecurityException e) {
							LOG.error(e.getMessage(), e);
						}
					}
					System.exit(-1);
				}
			}
		};
		thread.start();
	}

	public String getString() {
		return (String) MAP.get(name());
	}

	public Boolean getBoolean() {
		return (Boolean) MAP.get(name());
	}

	public Object get() {
		return MAP.get(name());
	}
}

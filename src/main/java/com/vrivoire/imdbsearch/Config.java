package com.vrivoire.imdbsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *
 * @author Vincent
 */
public enum Config {
    CHROME_APPLICATION,
    ICON,
    REPORT_NAME,
    REPORT_HEADER,
    REPORT_BODY,
    REPORT_FOOTER,
    PATTERN,
    GLOB,
    SUPPORTED_EXTENSIONS,
    TORRENTS;

    private static final Logger LOG = LogManager.getLogger(Config.class);
    private static final Map<String, Object> MAP = new HashMap<>();

    static {
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
            LOG.debug("Default configuration:\n" + defaults);

            File file = new File("config.json");
            if (!file.exists()) {
                file = new File("bin/config.json");
            }
            if (!file.exists()) {
                LOG.fatal("Config file not found. exiting.");
                System.exit(-1);
            }
            LOG.info("Config file found in: " + file.getAbsolutePath());

            MAP.putAll(objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {
            }));
            LOG.debug("Configuration:\n" + objectMapper.writeValueAsString(MAP));
        }
        catch (IOException ex) {
            LOG.fatal(ex);
            System.exit(-1);
        }
    }

    public String getString() {
        return (String) MAP.get(name());
    }

    public Object get() {
        return MAP.get(name());
    }
}

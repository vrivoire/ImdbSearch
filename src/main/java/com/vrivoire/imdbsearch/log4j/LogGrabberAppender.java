package com.vrivoire.imdbsearch.log4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

@Plugin(name = "LogGrabber", category = "Core", elementType = "appender", printObject = true)
public class LogGrabberAppender extends AbstractAppender {

    protected static final Logger LOG = StatusLogger.getLogger();
    private static volatile LogGrabberAppender instance;
    private static JTextArea _textArea;
    private static List<String> logs = new ArrayList<>();

    private LogGrabberAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @PluginFactory
    public static LogGrabberAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filters") Filter filter) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        instance = new LogGrabberAppender(name, filter, layout, true, Property.EMPTY_ARRAY);
        return instance;
    }

    public static LogGrabberAppender getInstance() {
        return instance;
    }

    @Override
    public void append(final LogEvent event) {
        writeToTextArea(event);
    }

    public static void setPanel(JTextArea textArea) {
        _textArea = textArea;
    }

    public static void resetLogs() {
        logs = new ArrayList<>();
    }

    public static List<String> getLogs() {
        return logs;
    }

    private void writeToTextArea(LogEvent event) {
        Layout<? extends Serializable> layout = getLayout();
        byte[] toByteArray = layout.toByteArray(event);
        String formated = new String(toByteArray);
        if (_textArea != null) {
            _textArea.append(formated);
            logs.add(formated);
        } else {
            LOG.error("LogGrabber not properly setup, TextArea is null.");
            LOG.error(formated);
        }
    }
}

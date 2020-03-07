package com.vrivoire.imdbsearch.log4j;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;

import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

/**
 *
 * @author Vincent
 */
public class LogGrabber extends WriterAppender {

    private static JTextArea _textArea;
    private static List<String> logs = new ArrayList<>();

    /**
     *
     * @param textArea
     */
    public static void setPanel(JTextArea textArea) {
        _textArea = textArea;
    }

    public static void resetLogs() {
        logs = new ArrayList<>();
    }

    public static List<String> getLogs() {
        return logs;
    }

    /**
     *
     * @param event
     */
    @Override
    protected void subAppend(LoggingEvent event) {
        writeToTextArea(event);
    }

    /**
     *
     * @param event
     */
    @Override
    public void append(LoggingEvent event) {
        writeToTextArea(event);
    }

    private void writeToTextArea(LoggingEvent event) {
        String formated = layout.format(event);
        if (_textArea != null) {
            _textArea.append(formated);
            logs.add(formated);
        } else {
            System.err.println("LogGrabber not properly setup, TextArea is null.");
            System.err.println(formated);
        }
    }
}

// https://commons.apache.org/proper/commons-io/javadocs/api-2.4/org/apache/commons/io/input/Tailer.html
package com.vrivoire.imdbsearch;

import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.AdjustmentEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Vincent Rivoire
 */
public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);
    public static String default_path = System.getProperty("user.home") + File.separator + "Videos" + File.separator;
    public static Process pythonProcess;
    private static String[] _args;
    private final static JTextArea TEXT_AREA_LOGS = new JTextArea();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (pythonProcess != null && pythonProcess.isAlive()) {
                    LOG.info("Destroying Python process...");
                    pythonProcess.destroyForcibly();
                    LOG.info("Python process killed.");
                } else {
                    LOG.info("Python process already dead.");
                }
            }
        });
        System.setProperty("-J-Djava.util.Arrays.useLegacyMergeSort", "true");
        LogGrabberAppender.setPanel(TEXT_AREA_LOGS);
        Config.configure();
    }

    public static void main(String[] args) {
        try {
            LOG.info("---------------------------------------------------------------------------------");
            LOG.info("OS name:   " + System.getProperty("os.name") + ", version: " + System.getProperty("os.version") + ", architechture: " + System.getProperty("os.arch"));
            LOG.info("Java: " + System.getProperty("java.vendor") + ", version: " + System.getProperty("java.version") + ", home: " + System.getProperty("java.home"));
            LOG.info("args: " + Arrays.toString(args) + " " + args.length);
            _args = args;
            switch (args.length) {
                case 1 -> {
                    switch (args[0]) {
                        case "-h", "--help" -> {
                            LOG.info("Args: ");
                            LOG.info("-d --delete : Delete an entry of the DB history with the id Imsb.");
                            LOG.info("path(s)     : Scan path(s) to generate a HTML report.");
                            LOG.info("NONE        : No args display a file diaglog to select a path.");
                            LOG.info("-h --help   : Help");
                        }
                        case "-d", "--delete." -> {
                            RemoveImdbEntry te = new RemoveImdbEntry();
                            te.showDialog();
                        }
                        default -> {
                            Main main = new Main();
                            main.start();
                        }
                    }
                }
                case 0 -> {
                    Main main = new Main();
                    main.start();
                }
                default -> {
                    // scan folder(s)
                    Main main = new Main();
                    main.start();
                }
            }
        }
        catch (Exception ex) {
            LOG.fatal(ex.getMessage(), ex);
        }
    }

    public Main() throws Exception {
        createWindow();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }

    private void start() throws Exception {
        if (_args != null && _args.length > 0) {
            for (String _arg : _args) {
                if (validatePath(_arg)) {
                    process();
                    LogGrabberAppender.resetLogs();
                }
            }
        } else {
            while (validatePath(null)) {
                process();
                LogGrabberAppender.resetLogs();
            }
        }

        System.exit(0);
    }

    private void process() {
        long start = System.currentTimeMillis();
        List<NameYearBean> listFound = null;
        List<NameYearBean> listNotFound = null;
        try {
            var searchMovie = new SearchMovie();
            listFound = searchMovie.search();
            listNotFound = searchMovie.getNoFound();

            renameFile(listFound);
            renameFile(listNotFound);

            DbUtils.saveDB(listFound);

            var generateHtmlReport = new GenerateHtmlReport();
            generateHtmlReport.deleteReport();
            generateHtmlReport.generate(listFound, listNotFound);

            LOG.info("Found " + listFound.size() + " movie" + (listFound.size() > 1 ? "s" : ""));
            LOG.info("Not found " + listNotFound.size() + " movie" + (listNotFound.size() > 1 ? "s" : ""));

            String[] cmd = {'"' + Config.WEB_BROWSER.getString() + '"', '"' + default_path + "_report.html\""};
            LOG.info("cmd: " + Arrays.toString(cmd));
            Runtime.getRuntime().exec(cmd);
        }
        catch (Exception ioe) {
            LOG.fatal(ioe.getMessage(), ioe);
            System.exit(-1);
        }
        finally {
            float duration = System.currentTimeMillis() - start;
            int size = ((listFound == null || listFound.isEmpty()) ? 1 : listFound.size()) + ((listNotFound == null || listNotFound.isEmpty()) ? 0 : listNotFound.size());
            LOG.info("Took: " + duration + "ms, " + String.format("%.2f", duration / size) + "ms/film, found: " + size);
        }

    }

    private void createWindow() {
        JFrame frameLogs = new JFrame("ImdbSearch - Log window");
        URL imageURL = Main.class.getResource(Config.ICON.getString());
        if (imageURL != null) {
            frameLogs.setIconImage(new ImageIcon(imageURL).getImage());
        }
        frameLogs.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        LayoutManager layout = new FlowLayout();
        panel.setLayout(layout);

        TEXT_AREA_LOGS.setEditable(false);
        TEXT_AREA_LOGS.setBackground(Color.BLACK);
        TEXT_AREA_LOGS.setForeground(Color.ORANGE);
        TEXT_AREA_LOGS.setFont(new Font("Lucida Console", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(TEXT_AREA_LOGS, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().addAdjustmentListener((AdjustmentEvent e) -> {
            e.getAdjustable().setValue(e.getAdjustable().getMaximum());
        });
        frameLogs.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frameLogs.setSize(1000, 800);
        frameLogs.setLocationRelativeTo(null);
        frameLogs.setVisible(true);
    }

    private boolean createFileChooser() throws Exception {
        JFileChooser fileChooser = new JFileChooser(default_path);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select a directory to scan.");
        int option = fileChooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            default_path = file.getAbsolutePath() + File.separator;
            return true;
        } else {
            LOG.info("Open command cancelled by user.");
            return false;
        }
    }

    private boolean validatePath(String _arg) throws Exception {
        boolean isExit = true;
        if (_arg != null && !_arg.isBlank()) {
            default_path = _arg;
            var path = Path.of(default_path).toAbsolutePath().normalize();
            default_path = path.toString();
            var last = default_path.charAt(default_path.length() - 1);
            if (last != '/' && last != '\\') { //
                default_path += File.separator;
            }

            if (!path.toFile().exists() && !path.toFile().isDirectory()) {
                throw new FileNotFoundException("The path '" + path + "' does not exist or is not a directory.");
            }
            isExit = true;
        } else {
            isExit = createFileChooser();
        }

        LOG.info("Scanning directory: " + default_path);
        return isExit;
    }

    @SuppressWarnings("unchecked")
    private void renameFile(List<NameYearBean> list) {
        list.forEach((NameYearBean nameYearBean) -> {
            var originalName = nameYearBean.getOriginalName();
//			var name = nameYearBean.getMainOriginalTitle();
            String name = nameYearBean.getMainTitle();
            Integer year = nameYearBean.getMainYear();
            File file = nameYearBean.getFile();

            List<String> extensions = (List<String>) Config.SUPPORTED_EXTENSIONS.get();
            if ((file != null && !file.isDirectory()) && name != null) {
                for (String extension : extensions) {
                    if (originalName.toLowerCase().endsWith(extension.toLowerCase())) {
                        String newName = WordUtils.capitalize(
                                (name + " " + (year != null ? year : "") + "." + extension).toLowerCase()
                                        .replace('\\', ' ').replace('/', ' ').replace(':', ' ').replace('*', ' ').replace('?', ' ').replace('<', ' ').replace('>', ' ')
                                        .replace("  ", " ").replace('|', ' ').replace("\"", ""), new char[]{}
                        );
                        if (!newName.equalsIgnoreCase(originalName)) {
                            LOG.info("--> Renaming file '" + originalName + "' to '" + newName + "'");
                            File oldF = new File(Main.default_path + originalName);
                            if (oldF.exists()) {
                                File newF = new File((Main.default_path + newName).trim().replaceAll("\\s+", " "));
                                if (newF.toString().equalsIgnoreCase(oldF.toString())) {
                                    LOG.warn("--> The new file name '" + originalName + "' already exist.");
                                } else {
                                    var isRenamed = oldF.renameTo(newF);
                                    if (!isRenamed) {
                                        LOG.warn("--> Renaming file '" + originalName + "' failed.");
                                    } else {
                                        LOG.info("--> File renamed: " + oldF + " --> " + newF);
                                        nameYearBean.setOriginalName(newF.getName());
                                        try {
                                            BasicFileAttributeView attributes = Files.getFileAttributeView(newF.toPath(), BasicFileAttributeView.class);
                                            FileTime time = FileTime.fromMillis(Date.from(new Date().toInstant()).getTime());
                                            attributes.setTimes(time, time, time);
                                            LOG.info("File " + newF + " updated creation date");
                                        }
                                        catch (IOException ex) {
                                            LOG.warn(originalName + " " + ex.getMessage());
                                        }
                                    }
                                }
                            } else {
                                LOG.warn("--> The file '" + originalName + "' do not exist.");
                            }
                        }
                        break;
                    }
                }
            }
        }
        );
    }

}

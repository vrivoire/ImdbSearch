package com.vrivoire.imdbsearch;

import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.awt.event.AdjustmentEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * https://stackoverflow.com/questions/8380425/get-video-file-metadata-in-java/8380514
 *
 * @author Vincent Rivoire
 */
public class Main {

	private static final Logger LOG = LogManager.getLogger(Main.class);
	private static final String DIALOG_TITLE = "Select a directory to scan.";
	private final static JTextArea TEXT_AREA = new JTextArea();
	public static String default_path = SystemUtils.USER_HOME + SystemUtils.FILE_SEPARATOR + "Videos" + SystemUtils.FILE_SEPARATOR;
	private static String[] _args;
	private static JFrame frame;

	static {
		LogGrabberAppender.setPanel(TEXT_AREA);
	}

	public static void main(String[] args) {
		try {
			LOG.info("--------------------------------------------------------------------------------");
			_args = args;
			Main main = new Main();
			main.start();
		} catch (Exception ex) {
			LOG.fatal(ex.getMessage(), ex);
		}
	}

	public Main() throws Exception {
		Config.configure();
		createWindow();
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}

	private void start() throws FileNotFoundException, HeadlessException, InterruptedException {
		if (_args != null && _args.length > 0) {
			for (String _arg : _args) {
				LOG.info("_arg: " + _arg);
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
		try {
			var searchMovie = new SearchMovie();
			List<NameYearBean> list = searchMovie.search();
			var noFound = searchMovie.getNoFound();

			setNewFileName(list);
			setNewFileName(noFound);

			var generateHtmlReport = new GenerateHtmlReport();
			generateHtmlReport.deleteReport();
			generateHtmlReport.generate(list, noFound);

			LOG.info("Found " + list.size() + " movie" + (list.size() > 1 ? "s" : ""));
			LOG.info("Not found " + noFound.size() + " movie" + (noFound.size() > 1 ? "s" : ""));

			String[] cmd = {Config.WEB_BROWSER.getString(), '"' + default_path + "_report.html\""};
			Runtime.getRuntime().exec(cmd);
		} catch (IOException ioe) {
			LOG.fatal(ioe.getMessage(), ioe);
			System.exit(-1);
		}
	}

	private static void createWindow() {
		frame = new JFrame("ImdbSearch - Log window");
		URL imageURL = Main.class.getResource(Config.ICON.getString());
		if (imageURL != null) {
			frame.setIconImage(new ImageIcon(imageURL).getImage());
		}
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		LayoutManager layout = new FlowLayout();
		panel.setLayout(layout);

		TEXT_AREA.setEditable(false);
		TEXT_AREA.setBackground(Color.BLACK);
		TEXT_AREA.setForeground(Color.ORANGE);
		TEXT_AREA.setFont(new Font("Lucida Console", Font.PLAIN, 12));
		JScrollPane scrollPane = new JScrollPane(TEXT_AREA, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().addAdjustmentListener((AdjustmentEvent e) -> {
			e.getAdjustable().setValue(e.getAdjustable().getMaximum());
		});
		frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private static boolean createFileChooser() throws HeadlessException {
		JFileChooser fileChooser = new JFileChooser(default_path);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("Select a directory to scan.");
		int option = fileChooser.showOpenDialog(frame);
		if (option == JFileChooser.APPROVE_OPTION) {

			File file = fileChooser.getSelectedFile();
			default_path = file.getAbsolutePath() + File.separator;
			return true;
		} else {
			LOG.info("Open command cancelled by user.");
			return false;
		}
	}

	private boolean validatePath(String _arg) throws FileNotFoundException, HeadlessException {
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
	private void setNewFileName(List<NameYearBean> list) {
		list.forEach((NameYearBean nameYearBean) -> {
			var originalName = nameYearBean.getOriginalName();
			var name = nameYearBean.getName();

			List<String> extensions = (List<String>) Config.SUPPORTED_EXTENSIONS.get();
			for (String extension : extensions) {
				if (originalName.toLowerCase().endsWith(extension.toLowerCase())) {
					String newName = (name + " " + nameYearBean.getYear() + extension).toLowerCase();
					if (!newName.equalsIgnoreCase(originalName)) {
						LOG.info("Renaming file '" + originalName + "' to '" + newName + "'");
						var oldF = new File(Main.default_path + originalName);
						if (oldF.exists()) {
							var newF = new File(Main.default_path + newName);
							if (newF.toString().equalsIgnoreCase(oldF.toString())) {
								LOG.warn("--> The new file name '" + originalName + "' already exist.");
							} else {
								var isRenamed = oldF.renameTo(newF);
								if (!isRenamed) {
									LOG.warn("--> Renaming file '" + originalName + "' failed.");
								}
							}
						} else {
							LOG.warn("--> The file '" + originalName + "' do not exist.");
						}
					}
					break;
				}
			}
		});
	}

}

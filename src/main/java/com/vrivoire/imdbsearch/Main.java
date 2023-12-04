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
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import org.sqlite.SQLiteException;

/**
 *
 * @author Vincent Rivoire
 */
public class Main {

	private static final Logger LOG = LogManager.getLogger(Main.class);
	public static String default_path = System.getProperty("user.home") + File.separator + "Videos" + File.separator;
	private static String[] _args;
	private final static JTextArea TEXT_AREA_LOGS = new JTextArea();

	private static final int SQLITE_BUSY = 5; // The database file is locked
	private static final String SQL_UPDATE = "update films set title=?, year=?, kind=?, rating=?, cover_url=?, votes=?, runtimeHM=?, countries=?, genres=? where imdb_id=?;";
	private static final String SQL_SELECT_1 = "select * from films where imdb_id=?;";
	private static final String DDL1 = "drop index if exists idx_imdb_id;";
	private static final String DDL2 = """
										create table if not exists films (
											id integer primary key not null,
											title varchar(255) not null,
											imdb_id varchar(255) not null,
											year varchar(25) not null,
											kind varchar(8) not null,
											rating double not null,
											cover_url text,
											votes text,
											runtimeHM varchar(6),
											countries text,
											genres text
										);
            """;
	private static final String DDL3 = "create unique index idx_imdb_id on films(imdb_id);";

	static {
		System.setProperty("-J-Djava.util.Arrays.useLegacyMergeSort", "true");
		LogGrabberAppender.setPanel(TEXT_AREA_LOGS);
	}

	public static void main(String[] args) {
		try {
			LOG.info("--------------------------------------------------------------------------------");
			LOG.info("OS name:   " + System.getProperty("os.name") + ", version: " + System.getProperty("os.version") + ", architechture: " + System.getProperty("os.arch"));
			LOG.info("Java: " + System.getProperty("java.vendor") + ", version: " + System.getProperty("java.version") + ", home: " + System.getProperty("java.home"));
			LOG.info("args: " + Arrays.toString(args));
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

	private void start() throws Exception {
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
		long start = System.currentTimeMillis();
		List<NameYearBean> listFound = null;
		List<NameYearBean> listNotFound = null;
		try {
			var searchMovie = new SearchMovie();
			listFound = searchMovie.search();
			listNotFound = searchMovie.getNoFound();

			setNewFileName(listFound);
			setNewFileName(listNotFound);

			saveDB(listFound);

			var generateHtmlReport = new GenerateHtmlReport();
			generateHtmlReport.deleteReport();
			generateHtmlReport.generate(listFound, listNotFound);

			LOG.info("Found " + listFound.size() + " movie" + (listFound.size() > 1 ? "s" : ""));
			LOG.info("Not found " + listNotFound.size() + " movie" + (listNotFound.size() > 1 ? "s" : ""));

			String[] cmd = {'"' + Config.WEB_BROWSER.getString() + '"', '"' + default_path + "_report.html\""};
			LOG.info("cmd: " + Arrays.toString(cmd));
			Runtime.getRuntime().exec(cmd);
		} catch (Exception ioe) {
			LOG.fatal(ioe.getMessage(), ioe);
			System.exit(-1);
		} finally {
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
	private void setNewFileName(List<NameYearBean> list) {
		list.forEach((NameYearBean nameYearBean) -> {
			var originalName = nameYearBean.getOriginalName();
			var name = nameYearBean.getMainOriginalTitle();

			List<String> extensions = (List<String>) Config.SUPPORTED_EXTENSIONS.get();
			if (name != null) {
				for (String extension : extensions) {
					if (originalName.toLowerCase().endsWith(extension.toLowerCase())) {
						String newName = WordUtils.capitalize((name + " " + nameYearBean.getMainYear() + extension).toLowerCase()
								.replace('\\', ' ').replace('/', ' ').replace(':', ' ').replace('*', ' ').replace('?', ' ').replace('<', ' ').replace('>', ' ')
								.replace('|', ' '), new char[]{});
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
			}
		});
	}

	private void saveDB(List<NameYearBean> listFound) {
		if (!new File(Config.DB_URL.getString()).exists()) {
			DDLs();
		}
		GenerateHtmlReport generateHtmlReport = new GenerateHtmlReport();
		try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString())) {
			try (PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_1)) {
				for (NameYearBean nameYearBean : listFound) {
					Map<String, Object> mapFromBean = generateHtmlReport.getMapFromBean(nameYearBean);
					pstmt.setString(1, (String) mapFromBean.get("mainImdbid"));
					ResultSet rs = pstmt.executeQuery();
					if (rs.next()) {
						sqlUpdate(conn, mapFromBean, 0);
					} else {
						sqlInsert(conn, mapFromBean, 0);
					}
				}
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void sqlUpdate(Connection conn, Map<String, Object> mapFromBean, Integer countSQLiteException) {
		LOG.info("\t" + mapFromBean.get("mainImdbid") + "\t" + (String) mapFromBean.get("mainOriginalTitle"));
		try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE)) {
			int i = 0;
			pstmt.setString(++i, (String) mapFromBean.get("mainOriginalTitle"));
			pstmt.setString(++i, (String) mapFromBean.get("mainYear"));
			pstmt.setString(++i, (String) mapFromBean.get("mainKind"));
			pstmt.setDouble(++i, (Double) mapFromBean.get("mainRating"));
			pstmt.setString(++i, (String) mapFromBean.get("mainCoverUrl"));
			pstmt.setString(++i, (String) mapFromBean.get("mainVotes"));
			pstmt.setString(++i, (String) mapFromBean.get("runtimeHM"));
			pstmt.setString(++i, (String) mapFromBean.get("mainCountries"));
			pstmt.setString(++i, (String) mapFromBean.get("mainGenres"));

			pstmt.setString(++i, (String) mapFromBean.get("mainImdbid"));
			pstmt.executeUpdate();
		} catch (SQLiteException e) {
			busy(e, countSQLiteException, mapFromBean, conn, true);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private static final String SQL_INSERT = "insert into films(title, year, kind, rating, imdb_id, cover_url, votes, runtimeHM, countries, genres) values(?,?,?,?,?,?,?,?,?,?);";

	private void sqlInsert(Connection conn, Map<String, Object> mapFromBean, Integer countSQLiteException) {
		LOG.info("\t" + mapFromBean.get("mainImdbid") + "\t" + (String) mapFromBean.get("mainOriginalTitle"));
		try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT)) {
			int i = 0;
			pstmt.setString(++i, (String) mapFromBean.get("mainOriginalTitle"));
			pstmt.setString(++i, (String) mapFromBean.get("mainYear"));
			pstmt.setString(++i, (String) mapFromBean.get("mainKind"));
			pstmt.setDouble(++i, (Double) mapFromBean.get("mainRating"));
			pstmt.setString(++i, (String) mapFromBean.get("mainImdbid"));
			pstmt.setString(++i, (String) mapFromBean.get("mainCoverUrl"));
			pstmt.setString(++i, (String) mapFromBean.get("mainVotes"));
			pstmt.setString(++i, (String) mapFromBean.get("runtimeHM"));
			pstmt.setString(++i, (String) mapFromBean.get("mainCountries"));
			pstmt.setString(++i, (String) mapFromBean.get("mainGenres"));
			pstmt.executeUpdate();
		} catch (SQLiteException e) {
			busy(e, countSQLiteException, mapFromBean, conn, false);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}

	private void DDLs() {
		try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString())) {
			try (Statement stmt = conn.createStatement()) {
				boolean b = stmt.execute(DDL1);
			}
			try (Statement stmt = conn.createStatement()) {
				boolean b = stmt.execute(DDL2);
			}
			try (Statement stmt = conn.createStatement()) {
				boolean b = stmt.execute(DDL3);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			File file = new File(Config.DB_URL.getString());
			if (file.exists()) {
				file.delete();
			}
		}
	}

	private void busy(SQLiteException e, Integer countSQLiteException, Map<String, Object> mapFromBean, Connection conn, boolean isUpdate) {
		if (e.getErrorCode() == SQLITE_BUSY) {
			countSQLiteException++;
			if (countSQLiteException > 5) {
				LOG.error(e.getMessage() + " 5 times.", e);
			} else {
				try {
					Thread.sleep(250);
				} catch (InterruptedException ex) {
					// ignore
				}
				LOG.warn(mapFromBean.get("mainOriginalTitle") + " retrying " + countSQLiteException + " times");
				if (isUpdate) {
					sqlUpdate(conn, mapFromBean, countSQLiteException);
				} else {
					sqlInsert(conn, mapFromBean, countSQLiteException);
				}
			}
		} else {
			LOG.error(e.getMessage(), e);
		}
	}
}

package com.vrivoire.imdbsearch;

import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.svg.SVGGraphics2D;

/**
 *
 * @author Vincent Rivoire
 */
public class Main {

	private static final Logger LOG = LogManager.getLogger(Main.class);
	public static String default_path = System.getProperty("user.home") + File.separator + "Videos" + File.separator;
	private static String[] _args;
	private final static JTextArea TEXT_AREA_LOGS = new JTextArea();

	static final String SQL_SELECT = "select * from films where imdb_id=?;";
	private static final String SQL_UPDATE = "update films set title=?, year=?, kind=?, rating=?, cover_url=?, votes=?, runtimeHM=?, countries=?, genres=? where imdb_id=?;";
	private static final String SQL_INSERT = "insert into films(title, year, kind, rating, imdb_id, cover_url, votes, runtimeHM, countries, genres) values(?,?,?,?,?,?,?,?,?,?);";
	private static final String SQL_HISTOGRAM = """
                                          SELECT
											(case when rating < 0 then 0 else round(rating * 2,0) / 2 end) as rate,
											count(case when rating < 0 then 0 else round(rating,0) end) as rateCount
                                          FROM films
                                          group by (case when rating < 0 then 0 else round(rating * 2,0) / 2 end)
                                          order by rating;
                                          """;

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
	private static final String DDL4 = """
                                   ALTER TABLE films drop COLUMN time_stamp;
                                   ALTER TABLE films ADD column time_stamp TIMESTAMP DEFAULT DATETIME('now');
                                   """;

	static {
		System.setProperty("-J-Djava.util.Arrays.useLegacyMergeSort", "true");
		LogGrabberAppender.setPanel(TEXT_AREA_LOGS);
		Config.configure();
	}

	public static void main(String[] args) {
		try {
			LOG.info("--------------------------------------------------------------------------------");
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
		} catch (Exception ex) {
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
			String histogram = getHistogram();

			var generateHtmlReport = new GenerateHtmlReport();
			generateHtmlReport.deleteReport();
			generateHtmlReport.setStatsImage(histogram);
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
							File oldF = new File(Main.default_path + originalName);
							if (oldF.exists()) {
								File newF = new File(Main.default_path + newName);
								if (newF.toString().equalsIgnoreCase(oldF.toString())) {
									LOG.warn("--> The new file name '" + originalName + "' already exist.");
								} else {
									var isRenamed = oldF.renameTo(newF);
									if (!isRenamed) {
										LOG.warn("--> Renaming file '" + originalName + "' failed.");
									} else {
										LOG.info("File renamed: " + oldF + " --> " + newF);
										try {
											BasicFileAttributeView attributes = Files.getFileAttributeView(newF.toPath(), BasicFileAttributeView.class);
											FileTime time = FileTime.fromMillis(Date.from(new Date().toInstant()).getTime());
											attributes.setTimes(time, time, time);
											LOG.info("File " + newF + " updated creation date");
										} catch (IOException ex) {
											// Ignore
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

	private String getHistogram() {
		try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement stmtSelect = conn.prepareStatement(SQL_HISTOGRAM)) {
			List<Double> listRate = new ArrayList<>();
			List<Integer> listRateCount = new ArrayList<>();
			List<Double> listToto = new ArrayList<>();
			ResultSet rs = stmtSelect.executeQuery();
			while (rs.next()) {
				listRate.add(rs.getDouble("rate"));
				listRateCount.add(rs.getInt("rateCount"));
			}
			for (int i = 0; i < 21; i++) {
				listToto.add(i / 2.0);
			}
			for (int i = 0; i < listToto.size(); i++) {
				if (!listRate.contains(listToto.get(i))) {
					listRateCount.add(i, 0);
				}
			}

			LOG.info("listToto=" + listToto);
			LOG.info("listRate=" + listRate);
			LOG.info("listRateCount=" + listRateCount);

			DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
			for (int i = 0; i < listToto.size(); i++) {
				dataset.add(listRateCount.get(i), null, "", listToto.get(i));
			}
			JFreeChart chart = createChart(dataset);
			SVGGraphics2D g2 = new SVGGraphics2D(900, 400);
			Rectangle r = new Rectangle(0, 0, 900, 400);
			chart.draw(g2, r);

			return g2.getSVGElement();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private static JFreeChart createChart(CategoryDataset dataset) {

		JFreeChart chart = ChartFactory.createLineChart("Rating", null, null, dataset, PlotOrientation.VERTICAL, false, true, true);

		CategoryPlot plot = (CategoryPlot) chart.getPlot();

		// customise the range axis...
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setAutoRangeIncludesZero(false);

		// customise the renderer...
		StatisticalBarRenderer renderer = new StatisticalBarRenderer();
		renderer.setDrawBarOutline(true);
		renderer.setErrorIndicatorPaint(Color.black);
		renderer.setIncludeBaseInRange(true);
		plot.setRenderer(renderer);

		// ensure the current theme is applied to the renderer just added
		ChartUtils.applyCurrentTheme(chart);

		renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		renderer.setDefaultItemLabelsVisible(true);
		renderer.setDefaultItemLabelPaint(Color.yellow);
		renderer.setDefaultPositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.INSIDE6, TextAnchor.BOTTOM_CENTER));

		// set up gradient paints for series...
		GradientPaint gp0 = new GradientPaint(0.0f, 0.0f, Color.blue, 0.0f, 0.0f, new Color(0, 0, 64));
		renderer.setSeriesPaint(0, gp0);
		return chart;
	}

	private void saveDB(List<NameYearBean> listFound) {
		if (!new File(Config.DB_URL.getString()).exists()) {
			DDLs();
		}
		long start = System.currentTimeMillis();
		GenerateHtmlReport generateHtmlReport = new GenerateHtmlReport();
		try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString())) {
			try (PreparedStatement pstmtSelect = conn.prepareStatement(SQL_SELECT)) {
				try (PreparedStatement pstmtUpdate = conn.prepareStatement(SQL_UPDATE); PreparedStatement pstmtInsert = conn.prepareStatement(SQL_INSERT)) {
					List<String> imdbIdsUptade = new ArrayList<>();
					List<String> imdbIdsInsert = new ArrayList<>();
					int[] resultUpdate, resultInsert;
					for (NameYearBean nameYearBean : listFound) {
						Map<String, Object> mapFromBean = generateHtmlReport.getMapFromBean(nameYearBean);
						if (mapFromBean.get("mainImdbid") != null || mapFromBean.get("mainOriginalTitle") != null) {
							pstmtSelect.setString(1, (String) mapFromBean.get("mainImdbid"));
							ResultSet rs = pstmtSelect.executeQuery();
							if (rs.next()) {
								sqlUpdate(pstmtUpdate, mapFromBean);
								imdbIdsUptade.add((String) mapFromBean.get("mainImdbid"));
							} else {
								sqlInsert(pstmtInsert, mapFromBean);
								imdbIdsInsert.add((String) mapFromBean.get("mainImdbid"));
							}
						}
					}
					resultUpdate = pstmtUpdate.executeBatch();
					pstmtUpdate.clearBatch();
					resultInsert = pstmtInsert.executeBatch();
					pstmtInsert.clearBatch();

					StringBuilder sbUpdate = new StringBuilder("\n");
					for (int i = 0; i < resultUpdate.length; i++) {
						sbUpdate.append("Update: ImdbId=").append(imdbIdsUptade.get(i)).append(" -> ").append(status(resultUpdate[i])).append('\n');
					}
					LOG.info(sbUpdate.toString());
					StringBuilder sbInsert = new StringBuilder("\n");
					for (int i = 0; i < resultInsert.length; i++) {
						sbInsert.append("Insert: ImdbId=").append(imdbIdsInsert.get(i)).append(" -> ").append(status(resultInsert[i])).append('\n');
					}
					LOG.info(sbInsert.toString());
				}
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
		}
		float duration = System.currentTimeMillis() - start;
		int size = ((listFound == null || listFound.isEmpty()) ? 1 : listFound.size());
		LOG.info("DB - Took: " + duration + "ms, " + String.format("%.2f", duration / size) + "ms/film, found: " + size);
	}

	private String status(int i) {
		if (i >= 0) {
			return i + " commands processed successfully";
		} else if (i == Statement.SUCCESS_NO_INFO) {
			return "SUCCESS_NO_INFO";
		} else if (i == Statement.EXECUTE_FAILED) {
			return "EXECUTE_FAILED";
		}
		return "";
	}

	private void sqlUpdate(PreparedStatement pstmtUpdate, Map<String, Object> mapFromBean) throws SQLException {
		int i = 0;
		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainOriginalTitle"));
		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainYear"));
		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainKind"));
		pstmtUpdate.setDouble(++i, (Double) mapFromBean.get("mainRating"));
		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainCoverUrl"));
		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainVotes"));
		pstmtUpdate.setString(++i, (String) mapFromBean.get("runtimeHM"));
		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainCountries"));
		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainGenres"));

		pstmtUpdate.setString(++i, (String) mapFromBean.get("mainImdbid"));
		pstmtUpdate.addBatch();
	}

	private void sqlInsert(PreparedStatement pstmtInsert, Map<String, Object> mapFromBean) throws SQLException {
		int i = 0;
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainOriginalTitle"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainYear"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainKind"));
		pstmtInsert.setDouble(++i, (Double) mapFromBean.get("mainRating"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainImdbid"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainCoverUrl"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainVotes"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("runtimeHM"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainCountries"));
		pstmtInsert.setString(++i, (String) mapFromBean.get("mainGenres"));
		pstmtInsert.addBatch();
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

}

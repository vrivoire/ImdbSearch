package com.vrivoire.imdbsearch;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
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
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.svg.SVGGraphics2D;

public final class DbUtils {

    private static final Logger LOG = LogManager.getLogger(DbUtils.class);

    private static final String SQL_SELECT_0_RATE = "SELECT imdb_id FROM films where rating=0;";
    private static final String SQL_COUNT = "SELECT count(id) as count FROM films;";
    private static final String SQL_DELETE = "delete from films where imdb_id=?;";
    private static final String SQL_SELECT = "select * from films order by rating desc, votes desc;";
    private static final String SQL_SELECT_4_DELETE = "select * from films where imdb_id=?;";
    private static final String SQL_UPDATE = "update films set title=?, year=?, kind=?, rating=?, cover_url=?, votes=?, runtimeHM=?, countries=?, genres=?, is_on_drive=? where imdb_id=?;";
    private static final String SQL_UPDATE_FALSE = "update films set is_on_drive=false;";
    private static final String SQL_INSERT = "insert into films(title, year, kind, rating, imdb_id, cover_url, votes, runtimeHM, countries, genres, is_on_drive) values(?,?,?,?,?,?,?,?,?,?,?);";
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
                                   ALTER TABLE films drop COLUMN is_on_drive;
                                   ALTER TABLE films ADD column is_on_drive boolean DEFAULT false;
                                   """;

    public static String getHistogram() {
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

	    DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
	    for (int i = 0; i < listToto.size(); i++) {
		dataset.add(listRateCount.get(i), null, "", listToto.get(i));
	    }

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
	    SVGGraphics2D g2 = new SVGGraphics2D(900, 400);
	    Rectangle r = new Rectangle(0, 0, 900, 400);
	    chart.draw(g2, r);

	    return g2.getSVGElement();
	}
	catch (Exception e) {
	    LOG.error(e.getMessage(), e);
	}
	return null;
    }

    private static void deleteNoRates() {
	try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement pstmt1 = conn.prepareStatement(SQL_SELECT_0_RATE)) {
	    ResultSet rs = pstmt1.executeQuery();
	    while (rs.next()) {
		try (PreparedStatement pstmt2 = conn.prepareStatement(SQL_DELETE)) {
		    String imdbId = rs.getString("imdb_id");
		    LOG.info("Before deleting " + imdbId);
		    pstmt2.setString(1, imdbId);
		    int val = pstmt2.executeUpdate();
		    switch (val) {
			case 1 -> {
			    LOG.info("DELETE imdb_id='" + imdbId + " The row count for SQL Data Manipulation Language (DML) statements");
			}
			case 2 -> {
			    LOG.info("NOTHING imdb_id='" + imdbId + " 0 for SQL statements that return nothing");
			}
			default -> {
			    LOG.info("Return '" + val + "' not understandable for param imdb_id='" + imdbId + "'.");
			}
		    }
		}
	    }
	}
	catch (Exception e) {
	    LOG.error(e.getMessage(), e);
	}
    }

    public static void dbDelete(String imdbId, JFrame frame) {
	try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement pstmt1 = conn.prepareStatement(SQL_SELECT_4_DELETE)) {
	    pstmt1.setString(1, imdbId);
	    ResultSet rs = pstmt1.executeQuery();
	    if (rs.next()) {
		LOG.info("FOUND " + imdbId + "\t" + rs.getString("title") + "\t" + rs.getString("year"));
		try (PreparedStatement pstmt2 = conn.prepareStatement(SQL_DELETE)) {
		    pstmt2.setString(1, imdbId);
		    int val = pstmt2.executeUpdate();
		    LOG.info("Update val=" + val);
		    switch (val) {
			case 1 -> {
			    LOG.info("The row count for SQL Data Manipulation Language (DML) statements");
			    JOptionPane.showMessageDialog(null, "DELETE imdb_id='" + imdbId);
			}
			case 2 -> {
			    LOG.info("0 for SQL statements that return nothing");
			    JOptionPane.showMessageDialog(null, "NOTHING imdb_id='" + imdbId);
			}
			default -> {
			    LOG.info("Return '" + val + "' not understandable for param imdb_id='" + imdbId + "'.");
			    JOptionPane.showMessageDialog(null, "Return '" + val + "' not understandable for param imdb_id='" + imdbId + "'.");
			}
		    }
		}
	    } else {
		LOG.warn("NOT FOUND imdb_id='" + imdbId);
		frame.setVisible(false);
		JOptionPane.showMessageDialog(null, "NOT FOUND imdb_id='" + imdbId);
		frame.setVisible(true);

	    }
	}
	catch (Exception e) {
	    LOG.error(e.getMessage(), e);
	    System.exit(-1);
	}
    }

    public static void saveDB(List<NameYearBean> listFound) {
	if (!new File(Config.DB_URL.getString()).exists()) {
	    DDLs();
	}
	long start = System.currentTimeMillis();
	GenerateHtmlReport generateHtmlReport = new GenerateHtmlReport();

	try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString())) {
	    try (PreparedStatement pstmtUpdateFalse = conn.prepareStatement(SQL_UPDATE_FALSE)) {
		pstmtUpdateFalse.executeBatch();
		pstmtUpdateFalse.clearBatch();
	    }

	    try (PreparedStatement pstmtSelect = conn.prepareStatement(SQL_SELECT_4_DELETE)) {
		try (PreparedStatement pstmtUpdate = conn.prepareStatement(SQL_UPDATE); PreparedStatement pstmtInsert = conn.prepareStatement(SQL_INSERT)) {
		    List<String> imdbIdsUptade = new ArrayList<>();
		    List<String> imdbIdsInsert = new ArrayList<>();
		    int[] resultUpdate = new int[0], resultInsert = new int[0];
		    Map<String, Map<String, Object>> mapFromBeans = new HashMap<>();
		    for (NameYearBean nameYearBean : listFound) {
			Map<String, Object> mapFromBean = generateHtmlReport.getMapFromBean(nameYearBean);
			mapFromBeans.put((String) mapFromBean.get("mainImdbid"), mapFromBean);
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

		    try {
			resultUpdate = pstmtUpdate.executeBatch();
			pstmtUpdate.clearBatch();
		    }
		    catch (SQLException e) {
			LOG.error("pstmtUpdate: " + e.getMessage(), e);
		    }
		    try {
			resultInsert = pstmtInsert.executeBatch();
			pstmtInsert.clearBatch();
		    }
		    catch (SQLException e) {
			LOG.error("pstmtInsert: " + e.getMessage(), e);
		    }

		    StringBuilder sbUpdate = new StringBuilder("\n");
		    for (int i = 0; i < resultUpdate.length; i++) {
			sbUpdate.append("Update: ImdbId=").append(imdbIdsUptade.get(i)).append(" (").append(mapFromBeans.get(imdbIdsUptade.get(i)).get("mainTitle")).append(") -> ").append(status(resultUpdate[i])).append('\n');
		    }
		    LOG.info(sbUpdate.toString());
		    StringBuilder sbInsert = new StringBuilder("\n");
		    for (int i = 0; i < resultInsert.length; i++) {
			sbInsert.append("Insert: ImdbId=").append(imdbIdsInsert.get(i)).append(" (").append(mapFromBeans.get(imdbIdsUptade.get(i)).get("mainTitle")).append(") -> ").append(status(resultInsert[i])).append('\n');
		    }
		    LOG.info(sbInsert.toString());
		}
	    }

	    deleteNoRates();
	}
	catch (Exception e) {
	    LOG.error(e.getMessage(), e);
	}
	float duration = System.currentTimeMillis() - start;
	int size = ((listFound == null || listFound.isEmpty()) ? 1 : listFound.size());
	LOG.info("DB - Took: " + duration + "ms, " + String.format("%.2f", duration / size) + "ms/film, found: " + size);
    }

    public static List<Map<String, Object>> sqlFindAll() {
	List<Map<String, Object>> list = new ArrayList<>();
	try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT)) {
	    ResultSet rs = pstmt.executeQuery();
	    int rank = 0;
	    while (rs.next()) {
		Map<String, Object> map = new HashMap<>();
		list.add(map);
		map.put("rank", ++rank);
		map.put("id", rs.getInt("id"));
		map.put("mainOriginalTitle", rs.getString("title"));
		map.put("mainImdbid", rs.getString("imdb_id"));
		map.put("mainYear", rs.getString("year"));
		map.put("mainKind", rs.getString("kind"));
		map.put("mainRating", rs.getDouble("rating"));
		map.put("mainCoverUrl", rs.getString("cover_url"));
		map.put("mainVotes", rs.getString("votes"));
		map.put("runtimeHM", rs.getString("runtimeHM"));
		map.put("mainCountries", rs.getString("countries"));
		map.put("mainGenres", rs.getString("genres"));
//				map.put("isOnDrive", rs.getString("is_on_drive"));
	    }
	    LOG.info("Read all history, " + list.size() + " records.");
	}
	catch (SQLException e) {
	    LOG.error(e.getMessage(), e);
	}
	return list;
    }

    public static Integer historyCount() {
	try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement stmtCount = conn.prepareStatement(SQL_COUNT)) {
	    ResultSet rs = stmtCount.executeQuery();
	    if (rs.next()) {
		int count = rs.getInt("count");
		return count;
	    }
	}
	catch (SQLException e) {
	    LOG.error(e.getMessage(), e);
	}
	return null;
    }

    private static String status(int i) {
	if (i >= 0) {
	    return i + " commands processed successfully";
	} else if (i == Statement.SUCCESS_NO_INFO) {
	    return "SUCCESS_NO_INFO";
	} else if (i == Statement.EXECUTE_FAILED) {
	    return "EXECUTE_FAILED";
	}
	return "";
    }

    private static void sqlUpdate(PreparedStatement pstmtUpdate, Map<String, Object> mapFromBean) throws SQLException {
	int i = 0;
	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainTitle"));
	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainYear"));
	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainKind"));
	pstmtUpdate.setDouble(++i, (Double) mapFromBean.get("mainRating"));
	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainCoverUrl"));
	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainVotes"));
	pstmtUpdate.setString(++i, (String) mapFromBean.get("runtimeHM"));
	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainCountries"));
	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainGenres"));
	pstmtUpdate.setBoolean(++i, (Boolean) mapFromBean.get("isOnDrive"));

	pstmtUpdate.setString(++i, (String) mapFromBean.get("mainImdbid"));
	pstmtUpdate.addBatch();
    }

    private static void sqlInsert(PreparedStatement pstmtInsert, Map<String, Object> mapFromBean) throws SQLException {
	int i = 0;
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainTitle"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainYear"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainKind"));
	pstmtInsert.setDouble(++i, (Double) mapFromBean.get("mainRating"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainImdbid"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainCoverUrl"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainVotes"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("runtimeHM"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainCountries"));
	pstmtInsert.setString(++i, (String) mapFromBean.get("mainGenres"));
	pstmtInsert.setBoolean(++i, (Boolean) mapFromBean.get("isOnDrive"));
	pstmtInsert.addBatch();
    }

    private static void DDLs() {
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
	}
	catch (Exception e) {
	    LOG.error(e.getMessage(), e);
	    File file = new File(Config.DB_URL.getString());
	    if (file.exists()) {
		file.delete();
	    }
	}
    }

}

package com.vrivoire.imdbsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 *
 * @author Vincent
 */
public class GenerateHtmlReport {

	private static final Logger LOG = LogManager.getLogger(GenerateHtmlReport.class);
	private static final String MOVIES = "&#x1F4FD;";
	private static final String SERIES = "&#x1F4FA;";
	private static final String UNKNOWN = "&#x2753;";
	private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
	private static final String SQL_SELECT = "select * from films order by rating desc, votes desc;";
	private static final String SQL_COUNT = "SELECT count(id) as count FROM films;";
	private final String fullReportPath;

	static {
		suffixes.put(1_000L, "K");
		suffixes.put(1_000_000L, "M");
		suffixes.put(1_000_000_000L, "G");
		suffixes.put(1_000_000_000_000L, "T");
		suffixes.put(1_000_000_000_000_000L, "P");
		suffixes.put(1_000_000_000_000_000_000L, "E");
	}

	public GenerateHtmlReport() {
		fullReportPath = Main.default_path + Config.REPORT_NAME.getString();
	}

	public void deleteReport() {
		try {
			if (Files.exists(Paths.get(fullReportPath), LinkOption.NOFOLLOW_LINKS)) {
				Files.delete(Paths.get(fullReportPath));
			}
		} catch (IOException ex) {
			LOG.error(ex.getMessage(), ex);
		}
	}

	/**
	 *
	 * @param movieList
	 * @param notFound
	 * @throws IOException
	 */
	public void generate(List<NameYearBean> movieList, List<NameYearBean> notFound) throws IOException {
		var sb = new StringBuilder();
		notFound.forEach((var item) -> {
			var name = escapeHtml4(item.getName());
			var year = (item.getMainYear() != null ? item.getMainYear() : "");
			var originalName = escapeHtml4(item.getOriginalName());
			sb.append("<a href=\"https://www.imdb.com/find?ref_=nv_sr_fn&q=")
					.append(name.replace(' ', '+'))
					.append('+')
					.append(year)
					.append("&s=all\" target =\"_blank\"><button  class=\"ui-button ui-widget ui-corner-all\">");
			String kind = item.getMainKind();
			if (kind != null && kind.equals("series")) {
				sb.append(SERIES);
			} else if (kind != null && kind.equals("movie")) {
				sb.append(MOVIES);
			} else {
				sb.append(UNKNOWN).append(" '").append(item.getMainKind()).append("'");
			}
			sb.append("&nbsp;")
					.append(name)
					.append(" | ")
					.append(year)
					.append(" (")
					.append(originalName)
					.append(')')
					.append("</button></a><p/>");
		});

		List<String> logs = LogGrabberAppender.getLogs();
		StringBuilder sb1 = new StringBuilder("<code>");
		if (logs != null) {
			logs.forEach((log) -> {
				sb1.append(log).append("<br/>");
			});
			sb1.append("</code>");
		}

		String spaceUsed = NameYearBean.convertBytesToHumanReadable(FileUtils.sizeOfDirectory(new File(fullReportPath.substring(0, fullReportPath.lastIndexOf(System.getProperty("file.separator"))))));

		Map<String, Object> map = new HashMap<>();
		map.put("historyCount", historyCount());
		map.put("report_location", fullReportPath);
		map.put("spaceUsed", spaceUsed);
		map.put("foundCount", movieList.size());
		map.put("found_s", movieList.size() > 1 ? "s" : "");
		map.put("notFoundCount", notFound.size());
		map.put("notFoundCount_s", notFound.size() > 1 ? "s" : "");
		map.put("total", notFound.size() + movieList.size());
		if (Config.IS_LOG_ON.getBoolean()) {
			map.put("logsData", """
                    <h3>Logs</h3>
                    <div>
                        """ + sb1.toString() + """
						</br>
						<div style="left: 50%; transform: translate(-50%, -50%);" class="ui-button ui-widget ui-corner-all" onclick="window.scrollTo({top: 0, left: 0, behavior: 'smooth'});">&nbsp;&nbsp;Top&nbsp;&nbsp;</div>
                    </div>
                      """);
		} else {
			map.put("logsData", "");
		}

		map.put("NOT_FOUND", sb.toString());
		if (!Config.IS_IMAGES_EMBEDED.getBoolean()) {
			map.put("jqueryui_css", "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdnjs.cloudflare.com/ajax/libs/jqueryui/" + Config.VERSION_JQUERY_UI.getString() + "/themes/overcast/jquery-ui.min.css\"/>\n");
			map.put("jquery_js", "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jquery/" + Config.VERSION_JQUERY.getString() + "/jquery.min.js\"></script>\n");
			map.put("jqueryui_js", "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jqueryui/" + Config.VERSION_JQUERY_UI.getString() + "/jquery-ui.min.js\"></script>\n");

//			https://datatables.net/download/
			map.put("datatables_css", "<link href=\"https://cdn.datatables.net/" + Config.VERSION_DATATABLES.getString() + "/css/dataTables.jqueryui.min.css\" rel=\"stylesheet\">\n");
			map.put("datatables_colreorder_css", "<link href=\"https://cdn.datatables.net/colreorder/" + Config.VERSION_DATATABLES_COLREORDER.getString() + "/css/colReorder.jqueryui.min.css\" rel=\"stylesheet\">\n");
			map.put("datatables_js", "<script src=\"https://cdn.datatables.net/" + Config.VERSION_DATATABLES.getString() + "/js/dataTables.min.js\"></script>\n");
			map.put("datatables_jqueryui_js", "<script src=\"https://cdn.datatables.net/" + Config.VERSION_DATATABLES.getString() + "/js/dataTables.jqueryui.min.js\"></script>\n");
			map.put("datatables_colreorder_js", "<script src=\"https://cdn.datatables.net/colreorder/" + Config.VERSION_DATATABLES_COLREORDER.getString() + "/js/dataTables.colReorder.min.js\"></script>\n");

			map.put("babel_js", "<script src=\"https://unpkg.com/@babel/standalone/babel.min.js\"></script>\n");
		} else {
			map.put("jqueryui_css", base64ToHtml("https://cdnjs.cloudflare.com/ajax/libs/jqueryui/" + Config.VERSION_JQUERY_UI.getString() + "/themes/overcast/jquery-ui.min.css", "<link rel=\"stylesheet\" href=\"data:text/css;base64,", "\">\n"));
			map.put("jquery_js", base64ToHtml("https://cdnjs.cloudflare.com/ajax/libs/jquery/" + Config.VERSION_JQUERY.getString() + "/jquery.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));
			map.put("jqueryui_js", base64ToHtml("https://cdnjs.cloudflare.com/ajax/libs/jqueryui/" + Config.VERSION_JQUERY_UI.getString() + "/jquery-ui.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));

			map.put("datatables_css", base64ToHtml("https://cdn.datatables.net/" + Config.VERSION_DATATABLES.getString() + "/css/dataTables.jqueryui.min.css", "<link rel=\"stylesheet\" href=\"data:text/css;base64,", "\">\n"));
			map.put("datatables_colreorder_css", base64ToHtml("https://cdn.datatables.net/colreorder/" + Config.VERSION_DATATABLES_COLREORDER.getString() + "/css/colReorder.jqueryui.min.css", "<link rel=\"stylesheet\" href=\"data:text/css;base64,", "\">\n"));
			map.put("datatables_js", base64ToHtml("https://cdn.datatables.net/" + Config.VERSION_DATATABLES.getString() + "/js/dataTables.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));
			map.put("datatables_jqueryui_js", base64ToHtml("https://cdn.datatables.net/" + Config.VERSION_DATATABLES.getString() + "/js/dataTables.jqueryui.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));
			map.put("datatables_colreorder_js", base64ToHtml("https://cdn.datatables.net/colreorder/" + Config.VERSION_DATATABLES_COLREORDER.getString() + "/js/dataTables.colReorder.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));

			map.put("babel_js", base64ToHtml("https://unpkg.com/@babel/standalone/babel.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));
		}
		map.put("statsImage", getHistogram());

		BufferedReader reader = new BufferedReader(new InputStreamReader(new NameYearBean().getClass().getResourceAsStream("/index.ts")));
		StringBuilder sb2 = new StringBuilder();
		reader.lines().forEachOrdered((String line) -> {
			sb2.append(line);
		}
		);
		String indexTs = sb2.toString();
		map.put("index_ts", "\n<script type=\"text/babel\">\n" + indexTs + "\n</script>\n");

		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		Collections.sort(movieList, (var ovf2, var ovf1) -> (ovf1.getFileDate() > ovf2.getFileDate() ? 1 : -1));
		String jsonByDate = ow.writeValueAsString(getMapList(movieList));
		map.put("jsonByDate", "\n<script>\nvar jsonByDate = " + jsonByDate + "\n</script>\n");

		String jsonListAll = ow.writeValueAsString(sqlFindAll());
		map.put("jsonListAll", "\n<script>\nvar jsonListAll = " + jsonListAll + "\n</script>\n");

		LOG.info("Report file: " + Paths.get(fullReportPath));

		var index = Config.fill(read("/index.html"), map);
		Files.writeString(Paths.get(fullReportPath), index);
	}

	private String getHistogram() {
		try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement stmtSelect = conn.prepareStatement(Main.SQL_HISTOGRAM)) {
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
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private List<Map<String, Object>> getMapList(List<NameYearBean> movieList) {
		List<Map<String, Object>> list = new ArrayList<>();
		movieList.forEach(movie -> {
			Map<String, Object> mapFromBean = getMapFromBean(movie);
			list.add(mapFromBean);
		});
		return list;
	}

	public Map<String, Object> getMapFromBean(NameYearBean movie) {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> map = objectMapper.convertValue(movie, new TypeReference<Map<String, Object>>() {
		});

		map.entrySet().stream().filter((entry) -> (entry.getValue() instanceof String)).forEachOrdered((entry) -> {
			entry.setValue(escapeHtml4((String) entry.getValue()));
		});

		String type = (String) map.get("mainKind");
		if (type != null) {
			type = type.trim().toLowerCase();
			if (type.contains("series") || type.contains("TV Special")) {
				map.put("mainKind", SERIES);
				if (movie.getMainSeriesYears() == null || movie.getMainSeriesYears().isBlank()) {
					map.put("mainYear", map.get("mainYear"));
				} else {
					map.put("mainYear", movie.getMainSeriesYears() + (movie.getMainNumberOfSeasons() == null ? "" : " (<b>Seasons:</b> " + movie.getMainNumberOfSeasons() + ")"));
				}
			} else if (type.toLowerCase().contains("movie")) {
				map.put("mainKind", MOVIES);
			} else {
				map.put("mainKind", UNKNOWN + " '" + map.get("mainKind") + "'");
			}
		}
		map.put("mainYear", map.get("mainYear") == null ? "" : map.get("mainYear").toString());

		map.entrySet().stream().filter(entry -> (entry.getValue() != null && entry.getValue() instanceof List)).forEachOrdered(entry -> {
			String s = entry.getValue().toString().substring(1, entry.getValue().toString().length() - 1);
			map.put(entry.getKey(), s);
		});

		if (movie.getMainCountries() != null && !movie.getMainCountries().isEmpty()) {
			map.put("mainCountries", (movie.getMainCountries().size() > 1 ? ", <b>Countries:</b> " : ", <b>Country:</b> ") + map.get("mainCountries"));
		}
		if (map.get("mainCountries") == null) {
			map.put("mainCountries", "");
		}

		if (movie.getMainVotes() != null && movie.getMainVotes() >= 1) {
			String s = bigNumbersformat(movie.getMainVotes());
			s = '(' + s + " vote" + (s.equals("1") ? "" : 's') + ')';
			map.put("mainVotes", s);
		} else {
			map.put("mainVotes", "");
		}

		insertBase64(movie, map);

		if (movie.getFileCount() > 1) {
			map.put("fileCount", movie.getFileCount() + " files");
		} else {
			map.put("fileCount", null);
		}

		if (movie.getPlotPlot() != null) {
			map.put("plotPlot", movie.getPlotPlot().get(0));
		} else if (movie.getMainPlotOutline() != null) {
			map.put("plotPlot", movie.getMainPlotOutline().substring(0));
		}
		if (movie.getMainPlotOutline() != null && !movie.getMainPlotOutline().isEmpty()) {
			map.put("plotSynopsis", movie.getMainPlotOutline());
		} else {
			map.put("plotSynopsis", "");
		}

		map.put("mainStars", movie.getMainStars() == null ? "" : movie.getMainStars());
		map.put("isOnDrive", true);
		return map;
	}

	private String bigNumbersformat(long value) {
		//Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
		if (value == Long.MIN_VALUE) {
			return bigNumbersformat(Long.MIN_VALUE + 1);
		}
		if (value < 0) {
			return "-" + bigNumbersformat(-value);
		}
		if (value < 1000) {
			return Long.toString(value); //deal with easy case
		}
		Entry<Long, String> e = suffixes.floorEntry(value);
		Long divideBy = e.getKey();
		String suffix = e.getValue();

		long truncated = value / (divideBy / 10); //the number part of the output times 10
		boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
		return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
	}

	private void insertBase64(NameYearBean movie, Map<String, Object> map) {
		if (movie.getMainCoverUrl() != null) {
			try {
				if (Config.IS_IMAGES_EMBEDED.getBoolean()) {
					URL url = URI.create(movie.getMainCoverUrl()).toURL();
					String encodedString = urlToBase64(url.toString());
					map.put("mainCoverUrl", "data:image/x-icon;base64," + encodedString);
				} else {
					map.put("mainCoverUrl", movie.getMainCoverUrl());
				}
			} catch (IOException ex) {
				LOG.error(ex.getMessage(), ex);
			}
		}
	}

	private String base64ToHtml(String urlStr, String protocol, String post) throws MalformedURLException, IOException {
		return '\n' + protocol + urlToBase64(urlStr) + post;
	}

	private String urlToBase64(String urlStr) throws MalformedURLException, IOException {
		if (Config.IS_IMAGES_EMBEDED.getBoolean()) {
			var url = URI.create(urlStr).toURL();
			try (InputStream is = url.openStream();) {
				byte[] imageBytes = IOUtils.toByteArray(is);
				return Base64.getEncoder().encodeToString(imageBytes);
			}
		} else {
			return urlStr;
		}
	}

	private static String read(String resourceName) {
		StringBuilder builder = new StringBuilder();
		InputStream in = GenerateHtmlReport.class.getResourceAsStream(resourceName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		reader.lines().forEach(line -> {
			builder.append(line);
		});
		return builder.toString();
	}

	private List<Map<String, Object>> sqlFindAll() {
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
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
		}
		return list;
	}

	private Integer historyCount() {
		try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement stmtCount = conn.prepareStatement(SQL_COUNT)) {
			ResultSet rs = stmtCount.executeQuery();
			if (rs.next()) {
				int count = rs.getInt("count");
				return count;
			}
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}
}

package com.vrivoire.imdbsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;

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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static final String SQL_SELECT = "select * from films order by rating desc, votes desc;";
	private static final String SQL_COUNT = "SELECT count(id) as count FROM films;";
	private final String fullReportPath;
	private String base64String;

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
		map.put("historyrCount", historyrCount());
		map.put("report_location", fullReportPath);
		map.put("spaceUsed", spaceUsed);
		map.put("foundCount", movieList.size());
		map.put("found_s", movieList.size() > 1 ? "s" : "");
		map.put("notFoundCount", notFound.size());
		map.put("notFoundCount_s", notFound.size() > 1 ? "s" : "");
		map.put("total", notFound.size() + movieList.size());
		map.put("logsData", sb1.toString());
		map.put("NOT_FOUND", sb.toString());

		map.put("jqueryui_css", base64ToHtml("https://cdnjs.cloudflare.com/ajax/libs/jqueryui/" + Config.JQUERYUI_VER.getString() + "/themes/overcast/jquery-ui.min.css", "<link rel=\"stylesheet\" href=\"data:text/css;base64,", "\">\n"));
		map.put("jquery_js", base64ToHtml("https://cdnjs.cloudflare.com/ajax/libs/jquery/" + Config.JQUERY_VER.getString() + "/jquery.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));
		map.put("jqueryui_js", base64ToHtml("https://cdnjs.cloudflare.com/ajax/libs/jqueryui/" + Config.JQUERYUI_VER.getString() + "/jquery-ui.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));
		map.put("babel_js", base64ToHtml("https://unpkg.com/@babel/standalone/babel.min.js", "<script src=\"data:text/js;base64,", "\"></script>\n"));
		map.put("statsImage", "data:image/x-icon;base64," + base64String);

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
			if (type.contains("series")) {
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

		if (movie.getMainVotes() != null && movie.getMainVotes() >= 1) {
			String s = new DecimalFormat("###,###,###").format(movie.getMainVotes());
			s = '(' + s + " vote" + (s.equals("1") ? "" : 's') + ')';
			map.put("mainVotes", s);
		} else {
			map.put("mainVotes", "");
		}

//		if (movie.getMainRating() == 0.0) {
//			map.put("mainRating", movie.getMainRating() == 0.0 ? "" : movie.getMainRating());
//		}
		insertBase64(movie, map);

		if (movie.getFileCount() > 1) {
			map.put("fileCount", ", " + movie.getFileCount() + " files");
		} else {
			map.put("fileCount", "");
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

		return map;
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
		var url = URI.create(urlStr).toURL();
		try (InputStream is = url.openStream();) {
			byte[] imageBytes = IOUtils.toByteArray(is);
			return Base64.getEncoder().encodeToString(imageBytes);
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
			}
			LOG.info("Read all history, " + list.size() + " records.");
		} catch (SQLException e) {
			LOG.error(e.getMessage(), e);
		}
		return list;
	}

	void setStatsImage(String base64String) {
		this.base64String = base64String;
	}

	private Integer historyrCount() {
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

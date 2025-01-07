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

import static com.vrivoire.imdbsearch.Main.default_path;
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
		fullReportPath = default_path + Config.REPORT_NAME.getString();
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
		String spaceUsed = "";
		try {
			spaceUsed = NameYearBean.convertBytesToHumanReadable(FileUtils.sizeOfDirectory(new File(fullReportPath.substring(0, fullReportPath.lastIndexOf(System.getProperty("file.separator"))))));
		} catch (Exception ade) {
			LOG.error(ade.getMessage());
		}
		Map<String, Object> map = new HashMap<>();
		map.put("historyCount", DbUtils.historyCount());
		map.put("report_location", fullReportPath);
		map.put("spaceUsed", spaceUsed);
		map.put("foundCount", movieList.size());
		map.put("found_s", movieList.size() > 1 ? "s" : "");
		map.put("notFoundCount", notFound.size());
		map.put("notFoundCount_s", notFound.size() > 1 ? "s" : "");
		map.put("total", notFound.size() + movieList.size());
		if (Config.IS_LOG_ON.getBoolean()) {
			map.put("logsData", """
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
		map.put("statsImage", DbUtils.getHistogram());

		map.put("index_css", "\n<style>\n" + read("/index.css") + "\n</style>\n");
		map.put("index_ts", "\n<script type=\"text/babel\">\n" + read("/index.ts") + "\n</script>\n");

		for (NameYearBean nameYearBean : movieList) {
			if (nameYearBean.getMainOriginalTitle() == null) {
				nameYearBean.setMainOriginalTitle("_Error " + nameYearBean.getOriginalName());
			}
		}

		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		Collections.sort(movieList, (var ovf2, var ovf1) -> (ovf1.getFileDate() > ovf2.getFileDate() ? 1 : -1));
		String jsonByDate = ow.writeValueAsString(getMapList(movieList));
		map.put("jsonByDate", "\n<script>\nvar jsonByDate = " + jsonByDate + "\n</script>\n");

		Collections.sort(movieList, (var ovf2, var ovf1) -> (ovf1.getMainRating() > ovf2.getMainRating() ? 1 : -1));
		String jsonByRank = ow.writeValueAsString(getMapList(movieList));
		map.put("jsonByRank", "\n<script>\nvar jsonByRank = " + jsonByRank + "\n</script>\n");

		Collections.sort(movieList, (NameYearBean o1, NameYearBean o2) -> {
			return (o1.getMainOriginalTitle() != null && o2.getMainOriginalTitle() != null) ? o1.getMainOriginalTitle().compareToIgnoreCase(o2.getMainOriginalTitle()) : 0;
		});
		String jsonByName = ow.writeValueAsString(getMapList(movieList));
		map.put("jsonByName", "\n<script>\nvar jsonByName = " + jsonByName + "\n</script>\n");

		String jsonListAll = ow.writeValueAsString(DbUtils.sqlFindAll());
		map.put("jsonListAll", "\n<script>\nvar jsonListAll = " + jsonListAll + "\n</script>\n");

		map.put("json_iso_639_1", "\n<script>\nvar json_iso_639_1 = " + read("/iso_639-1.json") + "\n</script>\n");
		map.put("json_iso_639_2", "\n<script>\nvar json_iso_639_2 = " + read("/iso_639-2.json") + "\n</script>\n");
		map.put("ISO_3166_1_alpha_2", "\n<script>\nvar ISO_3166_1_alpha_2 = " + read("/ISO-3166-1.alpha2.json") + "\n</script>\n");
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
		if (map.get("plotPlot") == null) {
			map.put("plotPlot", "");
		}
		if (movie.getMainPlotOutline() != null && !movie.getMainPlotOutline().isEmpty()) {
			map.put("plotSynopsis", movie.getMainPlotOutline());
		} else {
			map.put("plotSynopsis", "");
		}

		map.put("mainStars", movie.getMainStars() == null ? "" : movie.getMainStars());
		map.put("isOnDrive", true);

		map.put("subTitles", movie.getSubTitles());
		map.put("audio", movie.getAudio());
		map.put("mainLanguageCodes", movie.getMainLanguageCodes());
		map.put("mainCountryCodes", movie.getMainCountryCodes());
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
		BufferedReader reader = new BufferedReader(new InputStreamReader(new NameYearBean().getClass().getResourceAsStream(resourceName)));
		StringBuilder sb2 = new StringBuilder();
		reader.lines().forEach((String line) -> {
			sb2.append(line);
		});
		return sb2.toString();
	}

}

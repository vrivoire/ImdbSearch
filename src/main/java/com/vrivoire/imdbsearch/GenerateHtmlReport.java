package com.vrivoire.imdbsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
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
	private final String fullReportPath;

	public GenerateHtmlReport() {
		fullReportPath = Main.default_path + Config.REPORT_NAME.getString();
//		toto("https://cdnjs.cloudflare.com/ajax/libs/jqueryui/1.13.2/themes/overcast/jquery-ui.min.css");
//		toto("https://cdnjs.cloudflare.com/ajax/libs/jquery/3.7.1/jquery.min.js");
//		toto("https://cdnjs.cloudflare.com/ajax/libs/jqueryui/1.13.2/jquery-ui.min.js");
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
		map.put("report_location", fullReportPath);
		map.put("spaceUsed", spaceUsed);
		map.put("foundCount", movieList.size());
		map.put("found_s", movieList.size() > 1 ? "s" : "");
		map.put("notFoundCount", notFound.size());
		map.put("notFoundCount_s", notFound.size() > 1 ? "s" : "");
		map.put("total", notFound.size() + movieList.size());
		map.put("logsData", sb1.toString());
		map.put("NOT_FOUND", sb.toString());
		var header = Config.fill(read(Config.REPORT_HEADER.getString()), map);

		StringBuilder stringBuilder = new StringBuilder();

		Collections.sort(movieList, (var ovf2, var ovf1) -> ovf1.getMainRating().compareTo(ovf2.getMainRating()));
		createBody(movieList, stringBuilder, 2);

		Collections.sort(movieList, (var ovf2, var ovf1) -> (ovf1.getFileDate() > ovf2.getFileDate() ? 1 : -1));
		createBody(movieList, stringBuilder, 3);

		LOG.info("Report file: " + Paths.get(fullReportPath));
		Files.writeString(Paths.get(fullReportPath), header + stringBuilder.toString() + read(Config.REPORT_FOOTER.getString()));
	}

	private StringBuilder createBody(List<NameYearBean> movieList, StringBuilder stringBuilder, int index) {
		stringBuilder.append("<div id = \"tabs-").append(index).append("\"><table><tbody>");
		List<Map<String, Object>> list = getMapList(movieList);
		list.forEach((var movieMap) -> {
			var body = Config.fill(read(Config.REPORT_BODY.getString()), movieMap);
			stringBuilder.append(body);
		});
		stringBuilder.append("</tbody></table></div>");
		return stringBuilder;
	}

	private List<Map<String, Object>> getMapList(List<NameYearBean> movieList) {
		List<Map<String, Object>> list = new ArrayList<>();
		movieList.forEach(movie -> {
			Map<String, Object> mapFromBean = getMapFromBean(movie);
			list.add(mapFromBean);
		});
		return list;
	}

	private Map<String, Object> getMapFromBean(NameYearBean movie) {
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
				if (movie.getMainSeriesYears() == null || movie.getMainSeriesYears().isBlank()) {;
					map.put("mainYear", type);
				} else {
					map.put("mainYear", movie.getMainSeriesYears() + (movie.getMainNumberOfSeasons() == null ? "" : " (<b>Seasons:</b> " + movie.getMainNumberOfSeasons() + ")"));
				}
			} else if (type.toLowerCase().contains("movie")) {
				map.put("mainKind", MOVIES);
			} else {
				map.put("mainKind", UNKNOWN + " '" + map.get("mainKind") + "'");
			}
		}

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() != null && entry.getValue() instanceof List) {
				String s = entry.getValue().toString().substring(1, entry.getValue().toString().length() - 1);
				map.put(entry.getKey(), s);
			}
		}

		if (movie.getMainCountries() != null && !movie.getMainCountries().isEmpty()) {
			map.put("mainCountries", (movie.getMainCountries().size() > 1 ? ", <b>Countries:</b> " : ", <b>Country:</b> ") + map.get("mainCountries"));
		}

		if (movie.getMainVotes() != null) {
			map.put("mainVotes", new DecimalFormat("###,###,###").format(movie.getMainVotes()));
		}

		insertBase64(movie, map);

		if (movie.getFileCount() > 1) {
			map.put("fileCount", ", <b>Count:</b> " + movie.getFileCount() + " files");
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

		return map;
	}

	private void insertBase64(NameYearBean movie, Map<String, Object> map) {
		if (movie.getMainCoverUrl() != null) {
			try {
				var url = URI.create(movie.getMainCoverUrl()).toURL();
				try (InputStream is = url.openStream();) {
					byte[] imageBytes = IOUtils.toByteArray(is);
					String encodedString = Base64.getEncoder().encodeToString(imageBytes);
					map.put("mainCoverUrl", "data:image/x-icon;base64," + encodedString);
				}
			} catch (IOException ex) {
				LOG.error(ex.getMessage(), ex);
			}
		}
	}

	private void toto(String urlStr) {
		try {
			var url = URI.create(urlStr).toURL();
			try (InputStream is = url.openStream();) {
				byte[] imageBytes = IOUtils.toByteArray(is);
				String encodedString = Base64.getEncoder().encodeToString(imageBytes);
				LOG.info(urlStr);
				LOG.info("data:image/x-icon;base64," + encodedString);
			}
		} catch (IOException ex) {
			LOG.error(ex.getMessage(), ex);
		}
	}

	private static String read(String resourceName) {
		var builder = new StringBuilder();
		var in = GenerateHtmlReport.class.getResourceAsStream(resourceName);
		var reader = new BufferedReader(new InputStreamReader(in));
		reader.lines().forEach(line -> {
			builder.append(line);
		});
		return builder.toString();
	}
}

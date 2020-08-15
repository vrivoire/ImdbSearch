package com.vrivoire.imdbsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 *
 * @author Vincent
 */
public class GenerateHtmlReport {

	private static final Logger LOG = LogManager.getLogger(GenerateHtmlReport.class);
	private static final String MOVIES = "&#x1F4F9;";
	private static final String SERIES = "&#x1F4FA;";
	private final String fullReportPath;

	/**
	 *
	 */
	public GenerateHtmlReport() {
		fullReportPath = Main.default_path + Config.REPORT_NAME.getString();
	}

	/**
	 *
	 */
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
			var year = (item.getYear() != null ? item.getYear() : "");
			var originalName = escapeHtml4(item.getOriginalName());
			sb.append("<a href=\"https://www.imdb.com/find?ref_=nv_sr_fn&q=")
					.append(name.replace(' ', '+'))
					.append('+')
					.append(year)
					.append("&s=all\" target =\"_blank\"><button  class=\"ui-button ui-widget ui-corner-all\">");
			if (item.getType().equals("series")) {
				sb.append(SERIES);
			} else {
				sb.append(MOVIES);
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

		Map<String, Object> map = new HashMap<>();
		map.put("report_location", fullReportPath);
		map.put("foundCount", movieList.size());
		map.put("found_s", movieList.size() > 1 ? "s" : "");
		map.put("notFoundCount", notFound.size());
		map.put("notFoundCount_s", notFound.size() > 1 ? "s" : "");
		map.put("total", notFound.size() + movieList.size());
		map.put("logsData", sb1.toString());
		map.put("NOT_FOUND", sb.toString());
		var header = fill(read(Config.REPORT_HEADER.getString()), map);

		StringBuilder stringBuilder = new StringBuilder();

		Collections.sort(movieList, (var ovf2, var ovf1) -> ovf1.getImdbRating().compareTo(ovf2.getImdbRating()));
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
			var body = fill(read(Config.REPORT_BODY.getString()), movieMap);
			stringBuilder.append(body);
		});
		stringBuilder.append("</tbody></table></div>");
		return stringBuilder;
	}

	private String fill(String template, Map<String, Object> movieMap) {
//		LOG.info("////////////////////////////////////");
//		for (String string : movieMap.keySet()) {
//			LOG.info(string + " : " + movieMap.get(string));
//		}
//		LOG.info("////////////////////////////////////");

		var sub = new StrSubstitutor(movieMap);
		var resolvedString = sub.replace(template);
		return resolvedString;
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
		if (((String) map.get("Type")).equals("series")) {
			map.put("Type", SERIES);
		} else {
			map.put("Type", MOVIES);
		}
		return map;
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

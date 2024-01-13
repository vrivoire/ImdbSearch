package com.vrivoire.imdbsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.CaseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Vincent
 */
public class SearchMovie {

	private static final Logger LOG = LogManager.getLogger(SearchMovie.class);
	private final List<NameYearBean> NOT_FOUND;
	private final Pattern PATTERN = Pattern.compile(Config.PATTERN.getString());

	public SearchMovie() {
		NOT_FOUND = new ArrayList<>();
	}

	public List<NameYearBean> search() throws Exception {
		Set<NameYearBean> movieSet = listFiles();
		List<NameYearBean> list = new ArrayList<>();

		newWay(movieSet, list);

		return list;
	}

	public List<NameYearBean> getNoFound() {
		return NOT_FOUND;
	}

	private Set<NameYearBean> listFiles() throws Exception {
		var path = Path.of(Main.default_path);
		Set<NameYearBean> nameYearBeanSet = new HashSet<>();
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			filter(path, nameYearBeanSet);
		}
		return nameYearBeanSet;
	}

	private void filter(Path path, Set<NameYearBean> nameYearBeanSet) throws IOException {
		@SuppressWarnings("unchecked")
		List<String> ignoredFolders = (List<String>) Config.IGNORED_FOLDERS.get();
		@SuppressWarnings("unchecked")
		List<String> extensions = (List<String>) Config.SUPPORTED_EXTENSIONS.get();
		StringBuilder sb = new StringBuilder("glob:**.{");
		extensions.forEach((extension) -> {
			sb.append(extension.strip().replace(".", "")).append(',');
		});
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append("}");

		var includeFilter = path.getFileSystem().getPathMatcher(sb.toString());

		Files.list(path).filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS) || includeFilter.matches(p))
				.forEach((Path p) -> {
					String fileName = p.getFileName().toString();
					List<String> tokenized = tokenize(fileName);
					String fullFileName = p.getParent().toString() + File.separator + fileName;
					File file = new File(fullFileName);
					if (file.isDirectory() && ignoredFolders.contains(fileName)) {
						LOG.info("Ignoring folder: " + fileName);
					} else {
						NameYearBean nameYearBean = getFileNameYear(tokenized, p.getFileName().toString(), file);
						nameYearBeanSet.add(nameYearBean);
					}
				});
	}

	@SuppressWarnings("unchecked")
	private List<String> tokenize(String fileName) {
		for (String torrent : (List<String>) Config.TORRENTS.get()) {
			final String fn = fileName.toLowerCase();
			final String t = torrent.toLowerCase();
			if (fn.contains(t)) {
				int index = fn.indexOf(t);
				fileName = fn.substring(0, index) + fn.substring(index + t.length());
			}
		}

		fileName = fileName.replace('_', ' ')
				.replace('.', ' ').replace('-', ' ').replace('(', ' ').replace(')', ' ')
				.replace('[', ' ').replace(']', ' ').replace('_', ' ')
				.trim()
				.replaceAll(" +", " ");
		StringTokenizer tokenizer = new StringTokenizer(fileName, " ");
		return (List<String>) IteratorUtils.toList(tokenizer.asIterator());
	}

	private NameYearBean getFileNameYear(List<String> tokenized, String originalName, File file) {
		Iterator iterator = tokenized.iterator();
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(iterator.next()).append(' ');
		Integer year = null;
		while (iterator.hasNext()) {
			String next = ((String) iterator.next()).trim();
			try {
				year = Integer.valueOf(next);
				if (year > 1800) {
					break;
				}
			} catch (NumberFormatException nfe) {
			}

			Matcher matcher = PATTERN.matcher(next);
			if (matcher.find()) {
				break;
			}
			stringBuilder.append(next).append(' ');
		}

		NameYearBean nameYearBean = new NameYearBean();
		nameYearBean.setIsDirectory(file.isDirectory());
		nameYearBean.setName(stringBuilder.toString().trim().toLowerCase());
		nameYearBean.setMainYear(year == null ? null : year);
		nameYearBean.setFileDate(file.lastModified());
		nameYearBean.setOriginalName(originalName);
		nameYearBean.setFile(file);

		if (file.isDirectory()) {
			nameYearBean.setSize(FileUtils.sizeOfDirectory(file));
			Collection<File> listFiles = FileUtils.listFiles(nameYearBean.getFile(), (String[]) Config.SUPPORTED_EXTENSIONS_SHORT.get(), true);
			nameYearBean.setFileCount(listFiles.size());
		} else {
			nameYearBean.setSize(file.length());
		}
		return nameYearBean;
	}

	//	*************************************************************************
	//	********************************** NEW **********************************
	//	*************************************************************************
	private void newWay(Set<NameYearBean> movieSet, List<NameYearBean> list) throws Exception {
		searchByNames(movieSet);
		Map<String, Map<String, Object>> jsonMap = readOutputJson();

//		movieSet.forEach(nameYearBean -> {
//			String searchKey = getSearchKey(nameYearBean);
//			Map<String, Object> map = jsonMap.get(searchKey);
//			if (map != null) {
//				map.keySet().stream().filter(subKey -> (subKey.endsWith("ERROR"))).map(subKey -> {
//					nameYearBean.setName(searchKey);
//					nameYearBean.setError(map.get(subKey).toString());
//					return subKey;
//				}).forEachOrdered(_item -> {
//					NOT_FOUND.add(nameYearBean);
//				});
//			} else {
//				LOG.error(searchKey + " -> map=null");
//			}
//		});
		NOT_FOUND.forEach(nameYearBean -> {
			movieSet.remove(nameYearBean);
		});

		Map<String, String> mapKeys = new TreeMap<>();

		movieSet.forEach(nameYearBean -> {
			String searchKey = getSearchKey(nameYearBean);
			Map<String, Object> map = jsonMap.get(searchKey);
			if (map == null) {
				LOG.warn(searchKey + " --> " + map);
			} else {
				map.keySet().forEach(key -> {
					mapKeys.put(key, map.get(key).getClass().getSimpleName());
				});
				nameYearBean.setName(searchKey);
				autoMapping(nameYearBean, mapKeys, map);
				LOG.info(nameYearBean);
				list.add(nameYearBean);
			}
		});
	}

	private void autoMapping(NameYearBean searchNameYearBean, Map<String, String> mapKeys, Map<String, Object> map) {
		Class<? extends Object> clazz = searchNameYearBean.getClass();
		Class cClazz;
		for (Map.Entry<String, String> entry2 : mapKeys.entrySet()) {
			String uCamelCase = CaseUtils.toCamelCase(entry2.getKey(), true, '.');
			if (!uCamelCase.contains("Error")) {
				cClazz = switch (entry2.getValue()) {
					case "ArrayList" ->
						List.class;
					case "Double" ->
						Double.class;
					case "Integer" ->
						Integer.class;
					case "LinkedHashMap" ->
						Map.class;
					default ->
						String.class;
				};
				try {
					Method setMethod = clazz.getMethod("set" + uCamelCase, new Class[]{cClazz});
					Object result = setMethod.invoke(searchNameYearBean, new Object[]{map.get(entry2.getKey())});
					if (result != null) {
						// Because: There may be more than one method with matching name and parameter types in a class because while the Java language
						// forbids a class to declare multiple methods with the same signature but different return types, the Java virtual machine does not.
						throw new IllegalArgumentException("The argument of class " + clazz.getName() + " MUST NOT have a return: " + result);
					}
				} catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
					LOG.error("Cannot set: " + searchNameYearBean + " message: " + e.getMessage(), e);
				}
			}
		}
	}

	private void searchByNames(Set<NameYearBean> movieSet) throws Exception {
		List<String> args = new ArrayList<>();
		args.add("python.exe");
		File file = new File(Config.IMDBSEARCHPY_PATH.getString());
		if (!file.exists()) {
			LOG.warn("NOT FOUND IMDBSEARCHPY_PATH=" + file.getAbsolutePath());
			file = new File("bin/" + Config.IMDBSEARCHPY_PATH.getString());
			if (!file.exists()) {
				throw new Exception("NOT FOUND IMDBSEARCHPY_PATH=" + file.getAbsolutePath());
			}
		}
		LOG.info("FOUND IMDBSEARCHPY_PATH=" + file.getAbsolutePath());
		args.add(file.getAbsolutePath());
		List<String> args2 = new ArrayList<>();
		movieSet.forEach(nby -> {
			args2.add(getSearchKey(nby));
		});
		args2.sort(null);
		args.addAll(args2);
		LOG.info("Command line: " + args);
		try {
			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = stdInput.readLine()) != null) {
				LOG.info("ImdbSearchPY\t" + line);
			}
			LOG.info("Exit value: " + process.waitFor() + ", info: " + process.info());
		} catch (IOException ex) {
			LOG.fatal(ex.getMessage(), ex);
			throw ex;
		}
	}

	private static String getSearchKey(NameYearBean nameYearBean) {
		return (nameYearBean.getName() + " " + (nameYearBean.getMainYear() == null ? "" : nameYearBean.getMainYear())).trim();
	}

	private Map<String, Map<String, Object>> readOutputJson() throws Exception {
		try {
			final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
			StringBuilder sb = new StringBuilder();
			Path jsonPath = Paths.get(Config.OUTPUT_JSON_FILE.getString());
			LOG.info("jsonPath = " + jsonPath.toString());
			List<String> allLines = Files.readAllLines(jsonPath, StandardCharsets.UTF_8);
			allLines.forEach(line -> {
				sb.append(line);
			});
			String jsonString = sb.toString();
			LOG.info("jsonString = " + (jsonString == null || jsonString.isBlank() ? "VIDE" : jsonString.length()));
			Map<String, Map<String, Object>> jsonMap = new TreeMap<>();
			jsonMap.putAll(objectMapper.readValue(jsonString, new TypeReference<Map<String, Map<String, Object>>>() {
			}));
			return jsonMap;
		} catch (IOException ex) {
			LOG.fatal(ex.getMessage(), ex);
			throw ex;
		}
	}

}

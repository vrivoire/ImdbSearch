package com.vrivoire.imdbsearch;

import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.media.AudioTrackInfo;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.media.TextTrackInfo;
import uk.co.caprica.vlcj.media.TrackInfo;
import uk.co.caprica.vlcj.media.UnknownTrackInfo;
import uk.co.caprica.vlcj.media.VideoTrackInfo;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.jna.NativeLibrary;

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
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.text.CaseUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.vrivoire.imdbsearch.Main.default_path;
import static com.vrivoire.imdbsearch.Main.pythonProcess;

/**
 *
 * @author Vincent
 */
public class SearchMovie {

	private static final Logger LOG = LogManager.getLogger(SearchMovie.class);
	private final List<NameYearBean> NOT_FOUND;
	private final Pattern PATTERN = Pattern.compile(Config.PATTERN.getString());

	static {
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:/Program Files/VideoLAN/VLC");
	}

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
		var path = Path.of(default_path);
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
				if (year >= 1890 && year <= Year.now().getValue()) {
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
			@SuppressWarnings("unchecked")
			Collection<File> listFiles = FileUtils.listFiles(nameYearBean.getFile(), ((String[]) ((List) Config.SUPPORTED_EXTENSIONS.get()).toArray(String[]::new)), true);
			nameYearBean.setFileCount(listFiles.size());
		} else {
			nameYearBean.setSize(file.length());
		}
		return nameYearBean;
	}

	private void newWay(Set<NameYearBean> movieSet, List<NameYearBean> list) throws Exception {
		searchByNames(movieSet);
		Map<String, Map<String, Object>> jsonMap = readOutputJson();
		NOT_FOUND.forEach(nameYearBean -> {
			movieSet.remove(nameYearBean);
		});

		Map<String, String> mapKeys = new TreeMap<>();
		List<VlcMeta> metaList = new ArrayList<>();
		movieSet.forEach(nameYearBean -> {
			String searchKey = getSearchKey(nameYearBean);
			Map<String, Object> map = jsonMap.get(searchKey);
			if (map == null) {
				LOG.warn(searchKey + " --> " + map);
			} else {
				map.keySet().forEach(key -> {
					mapKeys.put(key, map.get(key) == null ? null : map.get(key).getClass().getSimpleName());
				});
				nameYearBean.setName(searchKey);
				autoMapping(nameYearBean, mapKeys, map);
				metaList.add(getMetaData(nameYearBean));
				list.add(nameYearBean);
			}
		});
		LOG.info("1 metaList.size: " + metaList.size() + ", movieSet.size(): " + movieSet.size());
		while (!metaList.isEmpty()) {
			for (int i = 0; i < metaList.size(); i++) {
				VlcMeta meta = metaList.get(i);
				if (meta != null && (!meta.isAlive() || meta.isInterrupted())) {
					meta.getMediaPlayerComponent().release();
					metaList.remove(metaList.indexOf(meta));
					LOG.info("Thread finished " + meta.getName());
					meta = null;
				}
			}
		}
		LOG.info("2 metaList.size: " + metaList.size() + ", movieSet.size(): " + movieSet.size());

		for (NameYearBean nameYearBean : list) {
			LOG.info(nameYearBean);
		}
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
		File file = new File(Config.IMDBSEARCH_PY_PATH.getString());
		if (!file.exists()) {
			LOG.warn("NOT FOUND IMDBSEARCHPY_PATH=" + file.getAbsolutePath());
			file = new File("bin/" + Config.IMDBSEARCH_PY_PATH.getString());
			if (!file.exists()) {
				throw new Exception("NOT FOUND IMDBSEARCH_PY_PATH=" + file.getAbsolutePath());
			}
		}
		LOG.info("FOUND IMDBSEARCHPY_PATH=" + file.getAbsolutePath());
		args.add(file.getAbsolutePath());
		String path = movieSet.iterator().next().getFile().toString();
		path = path.substring(0, path.lastIndexOf('\\') + 1);
		args.add(path);
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
			pythonProcess = pb.start();

			try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()), 1024 * 40)) {
				String line;
				while ((line = stdInput.readLine()) != null) {
					LOG.info("ImdbSearchPY\t" + line);
				}
			} catch (IOException ex) {
				LOG.fatal(ex.getMessage(), ex);
				throw ex;
			}
		} finally {
			LOG.info("Exit value: " + pythonProcess.waitFor() + ", info: " + pythonProcess.info());
			if (pythonProcess != null && pythonProcess.isAlive()) {
				LOG.info("Destroying Python process...");
				pythonProcess.destroyForcibly();
			} else {
				LOG.info("Python process already dead.");
			}
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

	private VlcMeta getMetaData(NameYearBean nameYearBean) {
		VlcMeta meta = new VlcMeta(nameYearBean);
		meta.start();
		try {
			Thread.sleep(750);
		} catch (InterruptedException ex) {
			LOG.error(ex.getMessage(), ex);
		}
		return meta;
	}

	private class VlcMeta extends Thread {

		private final NameYearBean nameYearBean;
		private final EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

		public VlcMeta(NameYearBean nameYearBean) {
			super(nameYearBean.getFile().getName());
			this.nameYearBean = nameYearBean;
			LOG.info("\t" + nameYearBean.getFile().getName() + " startred.");
		}

		public EmbeddedMediaPlayerComponent getMediaPlayerComponent() {
			return mediaPlayerComponent;
		}

		@Override
		public void run() {
			try {
				String path;
				if (nameYearBean.getFile().isDirectory()) {
					@SuppressWarnings("unchecked")
					Collection<File> listFiles = FileUtils.listFiles(nameYearBean.getFile(), ((String[]) ((List) Config.SUPPORTED_EXTENSIONS.get()).toArray(String[]::new)), true);
					path = listFiles.isEmpty() ? "" : (listFiles.toArray(File[]::new)[0]).getAbsolutePath();
					LOG.info("\t" + nameYearBean.getFile().getName() + " Folder: " + nameYearBean.getFile().getName() + " -> " + path);
				} else {
					path = nameYearBean.getFile().getAbsolutePath();
				}
				LOG.info("\t" + nameYearBean.getFile().getName() + " Looking for " + new File(path).exists() + " " + path);

				mediaPlayerComponent.mediaPlayer().media().prepare(path);
				LOG.info("\t" + nameYearBean.getFile().getName() + " isParsed: " + mediaPlayerComponent.mediaPlayer().media().parsing().parse());
				mediaPlayerComponent.mediaPlayer().events().addMediaEventListener(new MediaEventAdapter() {

					@Override
					public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {
						LOG.info("\t" + nameYearBean.getFile().getName() + " VLC looking meta for " + nameYearBean.getFile().getName() + ", Status: " + newStatus);
						if (newStatus == MediaParsedStatus.DONE) {
							MediaPlayer mediaPlayer = mediaPlayerComponent.mediaPlayer();
							List<? extends TrackInfo> trackInfoList = mediaPlayer.media().info().tracks();
							if (!trackInfoList.isEmpty()) {
								Set<String> subTitleList = new TreeSet<>();
								Set<String> audioList = new TreeSet<>();
								String resolutionDescription;
								for (TrackInfo trackInfo : trackInfoList) {
									if (trackInfo != null) {
										switch (trackInfo) {
											case TextTrackInfo textTrackInfo -> {
												if (textTrackInfo.language() != null) {
													subTitleList.add(textTrackInfo.language());
												}
											}
											case AudioTrackInfo audioTrackInfo -> {
												if (audioTrackInfo.language() != null) {
													audioList.add(audioTrackInfo.language());
												}
											}
											case UnknownTrackInfo unknownTrackInfo ->
												LOG.info("\t" + nameYearBean.getFile().getName() + " UnknownTrackInfo: " + unknownTrackInfo);
											case VideoTrackInfo videoTrackInfo -> {
												nameYearBean.setHeight(videoTrackInfo.height());
												nameYearBean.setWidth(videoTrackInfo.width());

												//SD(Standard Definition)	480p	4:3	640 x 480
												//HD(High Definition)	720p	16:9	1280 x 720
												//Full HD (FHD)	1080p	16:9	1920 x 1080
												//QHD(Quad HD)	1440p	16:9	2560 x 1440
												//2K video	1080p	1:1.77	2048 x 1080
												//4K video or Ultra HD(UHD)	4K or 2160p	1:1.9	3840 x 2160
												//8K video or Full Ultra HD	8K or 4320p	16∶9	7680 x 4320
												switch (videoTrackInfo.height()) {
													case 240:
													case 360:
													case 480:
														resolutionDescription = "SD 480p";
														break;
													case 720:
														resolutionDescription = "HD 720p";
														break;
													case 1080:
														if (videoTrackInfo.width() == 1920) {
															resolutionDescription = "Full HD 1080p";
															break;
														} else if (videoTrackInfo.width() == 2048) {
															resolutionDescription = "2K video 1080p";
															break;
														}
													case 1440:
														resolutionDescription = "2K 1440p";
														break;
													case 2160:
														resolutionDescription = "4K 2160p";
														break;
													case 4320:
														resolutionDescription = "8K 4320p";
														break;
													default:
														resolutionDescription = null;
												}
												nameYearBean.setResolutionDescription(resolutionDescription);
												nameYearBean.setCodecDescription(videoTrackInfo.codecDescription());
											}
											default ->
												LOG.warn("\t" + nameYearBean.getFile().getName() + " - " + trackInfo.getClass().getName() + " NOT IMPLEMENTED: " + trackInfo);
										}
									}
								}
								String timeInHHMMSS = DurationFormatUtils.formatDuration(mediaPlayer.media().info().duration(), "HH:mm", true);
								nameYearBean.setTimeInHHMMSS(timeInHHMMSS == null ? "" : timeInHHMMSS);
								nameYearBean.setSubTitles(subTitleList);
								nameYearBean.setAudio(audioList);

								LOG.info("\t" + nameYearBean.getFile().getName() + ", " + nameYearBean.getCodecDescription() + ", " + nameYearBean.getWidth() + "x" + nameYearBean.getHeigth() + ", "
										+ nameYearBean.getResolutionDescription() + ", " + nameYearBean.getTimeInHHMMSS() + ", SubTitles[" + nameYearBean.getSubTitles() + "], Audio[" + nameYearBean.getAudio() + "]");
							} else {
								LOG.info("\t" + nameYearBean.getFile().getName() + ", Empty");
							}
						} else {
							LOG.info("\t" + nameYearBean.getFile().getName() + " , Status: " + newStatus);
						}
					}
				});
			} catch (Throwable ex) {
				LOG.error("\t" + nameYearBean.getFile().getName() + " " + ex.getMessage(), ex);
			}
		}
	}
}

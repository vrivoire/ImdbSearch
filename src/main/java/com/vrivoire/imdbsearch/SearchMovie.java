package com.vrivoire.imdbsearch;

import com.omertron.omdbapi.OMDBException;
import com.omertron.omdbapi.OmdbApi;
import com.omertron.omdbapi.model.OmdbVideoFull;
import com.omertron.omdbapi.tools.OmdbBuilder;
import com.omertron.omdbapi.tools.OmdbParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Vincent
 */
public class SearchMovie {

	private static final Logger LOG = LogManager.getLogger(SearchMovie.class);

	// BanMePlz
	// 6cc19241
	private final static OmdbApi OMDB_API = new OmdbApi("6cc19241");
	private final List<NameYearBean> NOT_FOUND;
	private final Pattern PATTERN = Pattern.compile(Config.PATTERN.getString());

	/**
	 *
	 */
	public SearchMovie() {
		NOT_FOUND = new ArrayList<>();
	}

	/**
	 *
	 * @return @throws FileNotFoundException
	 */
	public List<NameYearBean> search() throws FileNotFoundException {
		Set<NameYearBean> movieSet = listFiles();
		final List<NameYearBean> list = new ArrayList<>();
		movieSet.forEach((var nameYearBean) -> {
			try {
				if (!nameYearBean.getOriginalName().toLowerCase().contains("system volume information")) {
					NameYearBean search = searchiMDB(nameYearBean);
					list.add(search);
				}
			} catch (OMDBException | IllegalAccessException | InvocationTargetException ex) {
				NOT_FOUND.add(nameYearBean);
				LOG.info(nameYearBean);
				LOG.info(ex.getMessage());
				LOG.info(' ');
			}
		});
		return list;
	}

	/**
	 *
	 * @return
	 */
	public List<NameYearBean> getNoFound() {
		return NOT_FOUND;
	}

	private NameYearBean searchiMDB(NameYearBean nameYearBean) throws OMDBException, IllegalAccessException, InvocationTargetException {
		OmdbParameters build;
		Integer year = null;
		try {
			year = Integer.valueOf(nameYearBean.getYear());
		} catch (Exception nfe) {
			// ignore, no year
		}
		if (year != null && year > 1800) {
			build = new OmdbBuilder().setTitle(nameYearBean.getName()).setYear(year).build();
		} else {
			build = new OmdbBuilder().setTitle(nameYearBean.getName()).build();
		}

		LOG.info("Looking for: " + nameYearBean.getName() + " (" + nameYearBean.getOriginalName() + ") " + (nameYearBean.getYear() == null ? "" : nameYearBean.getYear()));
		LOG.info(nameYearBean);
		OmdbVideoFull info;
		try {
			info = OMDB_API.getInfo(build);
		} catch (OMDBException ex) {
			build = new OmdbBuilder().setTitle(nameYearBean.getName()).build();
			info = OMDB_API.getInfo(build);
		}
		BeanUtils.copyProperties(nameYearBean, info);
		return nameYearBean;
	}

	private Set<NameYearBean> listFiles() throws FileNotFoundException {
		var path = Path.of(Main.default_path);
		Set<NameYearBean> nameYearBeanSet = new HashSet<>();
		LOG.info(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) + " " + path);
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try {
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

			} catch (IOException ex) {
				LOG.error(ex.getMessage(), ex);
			}
		}
		return nameYearBeanSet;
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
			} catch (Exception nfe) {
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
		nameYearBean.setYear(year == null ? null : year.toString());
		nameYearBean.setFileDate(file.lastModified());
		nameYearBean.setOriginalName(originalName);
		if (file.isDirectory()) {
			nameYearBean.setSize(FileUtils.sizeOfDirectory(file));
		} else {
			nameYearBean.setSize(file.length());
		}
		return nameYearBean;
	}
}

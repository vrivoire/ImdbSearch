package com.vrivoire.imdbsearch;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NameYearBean {

	private static final long serialVersionUID = -8717940658283105093L;

	private String name;
	private String originalName;
	private long fileDate;
	private boolean isDirectory;
	private String size;
	private File file;
	private int fileCount = 1;
	private List<String> mainAkas = null;
	private String mainAspectRatio = "";
	private Map<String, String> mainBoxOffice = null;
	private List<String> mainCertificates = null;
	private List<String> mainColorInfo = null;
	private List<String> mainCountries = null;
	private List<String> mainCountryCodes = null;
	private String mainCoverUrl = null;
	private List<String> mainDirectors = null;
	private List<String> mainGenres = null;
	private String mainImdbid = null;
	private String mainKind = null;
	private List<String> mainLanguageCodes = null;
	private List<String> mainLanguages = null;
	private String mainLocalizedTitle = null;
	private String mainOriginalAirDate = null;
	private String mainOriginalTitle = null;
	private String mainPlotOutline = null;
	private Double mainRating;
	private List<String> mainRuntimes = null;
	private List<String> mainSoundMix = null;
	private List<String> mainStars = null;
	private String mainTitle = null;
	private Integer mainTop250Rank = null;
	private List<String> mainVideos = null;
	private Integer mainVotes = null;
	private List<String> mainWriters = null;
	private Integer mainYear = null;
	private List<String> plotPlot = null;
	private List<String> plotSynopsis = null;
	private Integer mainSeasons = null;
	private String mainSeriesYears = null;
	private Integer mainNumberOfSeasons = null;
	private String mainProductionStatusUpdated = null;
	private String mainProductionStatus = null;
	private String error = null;

	public NameYearBean() {
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	void setFileCount(int fileCount) {
		this.fileCount = fileCount;
	}

	public int getFileCount() {
		return fileCount;
	}

	public String getMainProductionStatusUpdated() {
		return mainProductionStatusUpdated;
	}

	public void setMainProductionStatusUpdated(String mainProductionStatusUpdated) {
		this.mainProductionStatusUpdated = mainProductionStatusUpdated;
	}

	public String getMainProductionStatus() {
		return mainProductionStatus;
	}

	public void setMainProductionStatus(String mainProductionStatus) {
		this.mainProductionStatus = mainProductionStatus;
	}

	/**
	 *
	 * @return
	 */
	public String getOriginalName() {
		return originalName;
	}

	/**
	 *
	 * @param originalName
	 */
	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getFileDate() {
		return fileDate;
	}

	public void setFileDate(long fileDate) {
		this.fileDate = fileDate;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setIsDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public void setSize(long bytes) {
		size = convertBytesToHumanReadable(bytes);
	}

	public static String convertBytesToHumanReadable(long bytes) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			return bytes + " B";
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		return String.format(Locale.ENGLISH, "%.1f %ciB", value / 1024.0, ci.current());
	}

	public String getSize() {
		return size;
	}

	public void setMainAkas(List<String> mainAkas) {
		this.mainAkas = mainAkas;
	}

	public List<String> getMainAkas() {
		return mainAkas;
	}

	public void setMainAspectRatio(String mainAspectRatio) {
		this.mainAspectRatio = mainAspectRatio;
	}

	public String getMainAspectRatio() {
		return mainAspectRatio == null ? "" : mainAspectRatio;
	}

	public void setMainBoxOffice(Map<String, String> mainBoxOffice) {
		this.mainBoxOffice = mainBoxOffice;
	}

	public Map<String, String> getMainBoxOffice() {
		return mainBoxOffice;
	}

	public void setMainCertificates(List<String> mainCertificates) {
		this.mainCertificates = mainCertificates;
	}

	public List<String> getMainCertificates() {
		return mainCertificates;
	}

	public void setMainColorInfo(List<String> mainColorInfo) {
		this.mainColorInfo = mainColorInfo;
	}

	public List<String> getMainColorInfo() {
		return mainColorInfo;
	}

	public void setMainCountries(List<String> mainCountries) {
		this.mainCountries = mainCountries;
	}

	public List<String> getMainCountries() {
		return mainCountries;
	}

	public void setMainCountryCodes(List<String> mainCountryCodes) {
		this.mainCountryCodes = mainCountryCodes;
	}

	public List<String> getMainCountryCodes() {
		return mainCountryCodes;
	}

	public void setMainCoverUrl(String mainCoverUrl) {
		this.mainCoverUrl = mainCoverUrl;
	}

	public String getMainCoverUrl() {
		if (mainCoverUrl != null) {
			int index = mainCoverUrl.substring(0, mainCoverUrl.lastIndexOf(".")).lastIndexOf(".");
			if (index != -1) {
				return mainCoverUrl.substring(0, index) + "._V1_SX300.jpg";
			} else {
				return mainCoverUrl;
			}
		} else {
			return mainCoverUrl;
		}
	}

	public void setMainDirectors(List<String> mainDirectors) {
		this.mainDirectors = mainDirectors;
	}

	public List<String> getMainDirectors() {
		return mainDirectors;
	}

	public void setMainGenres(List<String> mainGenres) {
		this.mainGenres = mainGenres;
	}

	public List<String> getMainGenres() {
		return mainGenres;
	}

	public void setMainImdbid(String mainImdbid) {
		this.mainImdbid = mainImdbid;
	}

	public String getMainImdbid() {
		return mainImdbid;
	}

	public void setMainKind(String mainKind) {
		this.mainKind = mainKind;
	}

	public String getMainKind() {
		return mainKind;
	}

	public void setMainLanguageCodes(List<String> mainLanguageCodes) {
		this.mainLanguageCodes = mainLanguageCodes;
	}

	public List<String> getMainLanguageCodes() {
		return mainLanguageCodes;
	}

	public void setMainLanguages(List<String> mainLanguages) {
		this.mainLanguages = mainLanguages;
	}

	public List<String> getMainLanguages() {
		return mainLanguages;
	}

	public void setMainLocalizedTitle(String mainLocalizedTitle) {
		this.mainLocalizedTitle = mainLocalizedTitle;
	}

	public String getMainLocalizedTitle() {
		return mainLocalizedTitle;
	}

	public void setMainOriginalAirDate(String mainOriginalAirDate) {
		this.mainOriginalAirDate = mainOriginalAirDate;
	}

	public String getMainOriginalAirDate() {
		return mainOriginalAirDate;
	}

	public void setMainOriginalTitle(String mainOriginalTitle) {
		this.mainOriginalTitle = mainOriginalTitle;
	}

	public String getMainOriginalTitle() {
		return mainOriginalTitle;
	}

	public void setMainPlotOutline(String mainPlotOutline) {
		this.mainPlotOutline = mainPlotOutline;
	}

	public String getMainPlotOutline() {
		return mainPlotOutline;
	}

	public void setMainRating(Double mainRating) {
		this.mainRating = mainRating;
	}

	public Double getMainRating() {
		return mainRating == null ? 0.0 : mainRating;
	}

	public void setMainRuntimes(List<String> mainRuntimes) {
		this.mainRuntimes = mainRuntimes;
	}

	public List<String> getMainRuntimes() {
		return mainRuntimes;
	}

	public String getRuntimeHM() {
		String s = (getMainRuntimes() == null ? "N/A" : getMainRuntimes().get(0));
		if (s == null || "".equals(s) || s.contains("N/A")) {
			s = "0";
		}
		s = s.replace("min", "").trim();
		try {
			s = LocalTime.MIN.plus(Duration.ofMinutes(Long.parseLong(s))).toString();
		} catch (NumberFormatException e) {
			s = LocalTime.MIN.plus(Duration.ofMinutes(0l)).toString();
		}
		s = s.replace(':', 'h') + 'm';
		return s;
	}

	public void setMainSoundMix(List<String> mainSoundMix) {
		this.mainSoundMix = mainSoundMix;
	}

	public List<String> getMainSoundMix() {
		return mainSoundMix;
	}

	public void setMainStars(List<String> mainStars) {
		this.mainStars = mainStars;
	}

	public List<String> getMainStars() {
		return mainStars;
	}

	public void setMainTitle(String mainTitle) {
		this.mainTitle = mainTitle;
	}

	public String getMainTitle() {
		return mainTitle;
	}

	public void setMainTop250Rank(Integer mainTop250Rank) {
		this.mainTop250Rank = mainTop250Rank;
	}

	public Integer getMainTop250Rank() {
		return mainTop250Rank;
	}

	public void setMainVideos(List<String> mainVideos) {
		this.mainVideos = mainVideos;
	}

	public List<String> getMainVideos() {
		return mainVideos;
	}

	public void setMainVotes(Integer mainVotes) {
		this.mainVotes = mainVotes;
	}

	public Integer getMainVotes() {
		return mainVotes;
	}

	public void setMainWriters(List<String> mainWriters) {
		this.mainWriters = mainWriters;
	}

	public List<String> getMainWriters() {
		return mainWriters;
	}

	public void setMainYear(Integer mainYear) {
		this.mainYear = mainYear;
	}

	public Integer getMainYear() {
		return mainYear;
	}

	public void setPlotPlot(List<String> plotPlot) {
		this.plotPlot = plotPlot;
	}

	public List<String> getPlotPlot() {
		return plotPlot;
	}

	public void setPlotSynopsis(List<String> plotSynopsis) {
		this.plotSynopsis = plotSynopsis;
	}

	public List<String> getPlotSynopsis() {
		return plotSynopsis;
	}

	public void setMainSeasons(Integer mainSeasons) {
		this.mainSeasons = mainSeasons;
	}

	public void setMainSeriesYears(String mainSeriesYears) {
		this.mainSeriesYears = mainSeriesYears;
	}

	public Integer getMainSeasons() {
		return mainSeasons;
	}

	public String getMainSeriesYears() {
		return mainSeriesYears;
	}

	public Integer getMainNumberOfSeasons() {
		return mainNumberOfSeasons;
	}

	public void setMainNumberOfSeasons(Integer mainNumberOfSeasons) {
		this.mainNumberOfSeasons = mainNumberOfSeasons;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NameYearBean [");
		builder.append("getError=").append(getError());
		builder.append(", getFile=").append(getFile());
		builder.append(", getFileCount=").append(getFileCount());
		builder.append(", getFileDate=").append(getFileDate());
		builder.append(", getMainAkas=").append(getMainAkas());
		builder.append(", getMainAspectRatio=").append(getMainAspectRatio());
		builder.append(", getMainBoxOffice=").append(getMainBoxOffice());
		builder.append(", getMainCertificates=").append(getMainCertificates());
		builder.append(", getMainColorInfo=").append(getMainColorInfo());
		builder.append(", getMainCountries=").append(getMainCountries());
		builder.append(", getMainCountryCodes=").append(getMainCountryCodes());
		builder.append(", getMainCoverUrl=").append(getMainCoverUrl());
		builder.append(", getMainDirectors=").append(getMainDirectors());
		builder.append(", getMainGenres=").append(getMainGenres());
		builder.append(", getMainImdbid=").append(getMainImdbid());
		builder.append(", getMainKind=").append(getMainKind());
		builder.append(", getMainLanguageCodes=").append(getMainLanguageCodes());
		builder.append(", getMainLanguages=").append(getMainLanguages());
		builder.append(", getMainLocalizedTitle=").append(getMainLocalizedTitle());
		builder.append(", getMainNumberOfSeasons=").append(getMainNumberOfSeasons());
		builder.append(", getMainOriginalAirDate=").append(getMainOriginalAirDate());
		builder.append(", getMainOriginalTitle=").append(getMainOriginalTitle());
		builder.append(", getMainPlotOutline=").append(getMainPlotOutline());
		builder.append(", getMainProductionStatus=").append(getMainProductionStatus());
		builder.append(", getMainProductionStatusUpdated=").append(getMainProductionStatusUpdated());
		builder.append(", getMainRating=").append(getMainRating());
		builder.append(", getMainRuntimes=").append(getMainRuntimes());
		builder.append(", getRuntimeHM=").append(getRuntimeHM());
		builder.append(", getMainSeasons=").append(getMainSeasons());
		builder.append(", getMainSeriesYears=").append(getMainSeriesYears());
		builder.append(", getMainSoundMix=").append(getMainSoundMix());
		builder.append(", getMainStars=").append(getMainStars());
		builder.append(", getMainTitle=").append(getMainTitle());
		builder.append(", getMainTop250Rank=").append(getMainTop250Rank());
		builder.append(", getMainVideos=").append(getMainVideos());
		builder.append(", getMainVotes=").append(getMainVotes());
		builder.append(", getMainWriters=").append(getMainWriters());
		builder.append(", getMainYear=").append(getMainYear());
		builder.append(", getName=").append(getName());
		builder.append(", getOriginalName=").append(getOriginalName());
		builder.append(", getPlotPlot=").append(getPlotPlot());
		builder.append(", getPlotSynopsis=").append(getPlotSynopsis());
		builder.append(", getSize=").append(getSize());
		builder.append(", isDirectory=").append(isDirectory());
		builder.append("]");
		return builder.toString();
	}

}

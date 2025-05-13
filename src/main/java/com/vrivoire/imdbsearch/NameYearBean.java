package com.vrivoire.imdbsearch;

import java.io.File;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
	private String mainAlternativeKind = null;
	private Set<String> mainLanguageCodes = new TreeSet<>();
	private List<String> mainLanguages = null;
	private String mainLocalizedTitle = null;
	private String mainOriginalAirDate = null;
	private String mainOriginalTitle = null;
	private String mainPlotOutline = null;
	private Double mainRating;
	private List<String> mainRuntimes = null;
	private List<String> mainSoundMix = null;
	private List<String> mainCasts = null;
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
	private Integer mainEpisode = null;
	private Integer mainNumberOfEpisodes = null;
	private String mainPreviousEpisode = null;
	private String mainNextEpisode = null;
	private Object mainSeason = null;
	private boolean isOnDrive = false;
	private String resolutionDescription = null;
	private Integer width = null;
	private Integer heigth = null;
	private String codecDescription = null;
	private String timeInHHMMSS = null;
	private Set<String> subTitles = new TreeSet<>();
	private Set<String> audio = new TreeSet<>();
	private Map<String, Map> mainEpisodeOf = null;
	private Integer mainBottom100Rank = null;

	public NameYearBean() {
	}

	public boolean isIsOnDrive() {
		return isOnDrive;
	}

	public String getCodecDescription() {
		return codecDescription;
	}

	public void setCodecDescription(String codecDescription) {
		this.codecDescription = codecDescription;
	}

	public void setIsOnDrive(boolean isOnDrive) {
		this.isOnDrive = isOnDrive;
	}

	public Integer getMainEpisode() {
		return mainEpisode;
	}

	public void setMainEpisode(Integer mainEpisode) {
		this.mainEpisode = mainEpisode;
	}

	public Integer getMainNumberOfEpisodes() {
		return mainNumberOfEpisodes;
	}

	public void setMainNumberOfEpisodes(Integer mainNumberOfEpisodes) {
		this.mainNumberOfEpisodes = mainNumberOfEpisodes;
	}

	public String getMainPreviousEpisode() {
		return mainPreviousEpisode;
	}

	public void setMainPreviousEpisode(String mainPreviousEpisode) {
		this.mainPreviousEpisode = mainPreviousEpisode;
	}

	public String getMainNextEpisode() {
		return mainNextEpisode;
	}

	public void setMainNextEpisode(String mainNextEpisode) {
		this.mainNextEpisode = mainNextEpisode;
	}

	public Object getMainSeason() {
		return mainSeason;
	}

	public void setMainSeason(Object mainSeason) {
		this.mainSeason = mainSeason;
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

	public String getOriginalName() {
		return originalName;
	}

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

	public String getMainAlternativeKind() {
		return mainAlternativeKind;
	}

	public void setMainAlternativeKind(String mainAlternativeKind) {
		this.mainAlternativeKind = mainAlternativeKind;
	}

	public Set<String> getMainLanguageCodes() {
		return mainLanguageCodes;
	}

	public void setMainLanguageCodes(List<String> mainLanguageCodes) {
		if (mainLanguageCodes != null) {
			this.mainLanguageCodes = new TreeSet<>(mainLanguageCodes);
		}
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
		s = s == null || s.equals("00h00m") ? getTimeInHHMMSS() : s;

		return s;
	}

	public void setMainSoundMix(List<String> mainSoundMix) {
		this.mainSoundMix = mainSoundMix;
	}

	public List<String> getMainSoundMix() {
		return mainSoundMix;
	}

	public void setMainCasts(List<String> mainCasts) {
		this.mainCasts = mainCasts;
	}

	public List<String> getMainCasts() {
		if (mainCasts != null && !mainCasts.isEmpty() && mainCasts.size() > 5) {
			return mainCasts.subList(0, 5);
		}
		return mainCasts;
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

	public String getResolutionDescription() {
		return resolutionDescription;
	}

	public void setResolutionDescription(String resolutionDescription) {
		this.resolutionDescription = resolutionDescription;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeigth() {
		return heigth;
	}

	public void setHeight(Integer heigth) {
		this.heigth = heigth;
	}

	public String getTimeInHHMMSS() {
		return timeInHHMMSS;
	}

	public void setTimeInHHMMSS(String timeInHHMMSS) {
		this.timeInHHMMSS = timeInHHMMSS;
	}

	void setSubTitles(Set<String> subTitles) {
		this.subTitles = subTitles;
	}

	public Set<String> getSubTitles() {
		return subTitles;
	}

	void setAudio(Set<String> audio) {
		this.audio = audio;
	}

	public Set<String> getAudio() {
		return audio;
	}

	public Map<String, Map> getMainEpisodeOf() {
		return mainEpisodeOf;
	}

	public void setMainEpisodeOf(Map<String, Map> mainEpisodeOf) {
		this.mainEpisodeOf = mainEpisodeOf;
	}

	public Integer getMainBottom100Rank() {
		return mainBottom100Rank;
	}

	public void setMainBottom100Rank(Integer mainBottom100Rank) {
		this.mainBottom100Rank = mainBottom100Rank;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NameYearBean [");
		builder.append("audio=").append(audio);
		builder.append(", codecDescription=").append(codecDescription);
		builder.append(", error=").append(error);
		builder.append(", file=").append(file);
		builder.append(", fileCount=").append(fileCount);
		builder.append(", fileDate=").append(fileDate);
		builder.append(", heigth=").append(heigth);
		builder.append(", isDirectory=").append(isDirectory);
		builder.append(", isOnDrive=").append(isOnDrive);
		builder.append(", mainAkas=").append(mainAkas);
		builder.append(", mainAlternativeKind=").append(mainAlternativeKind);
		builder.append(", mainAspectRatio=").append(mainAspectRatio);
		builder.append(", mainBottom100Rank=").append(mainBottom100Rank);
		builder.append(", mainBoxOffice=").append(mainBoxOffice);
		builder.append(", mainCertificates=").append(mainCertificates);
		builder.append(", mainColorInfo=").append(mainColorInfo);
		builder.append(", mainCountries=").append(mainCountries);
		builder.append(", mainCountryCodes=").append(mainCountryCodes);
		builder.append(", mainCoverUrl=").append(mainCoverUrl);
		builder.append(", mainDirectors=").append(mainDirectors);
		builder.append(", mainEpisode=").append(mainEpisode);
		builder.append(", mainEpisodeOf=").append(mainEpisodeOf);
		builder.append(", mainGenres=").append(mainGenres);
		builder.append(", mainImdbid=").append(mainImdbid);
		builder.append(", mainKind=").append(mainKind);
		builder.append(", mainLanguageCodes=").append(mainLanguageCodes);
		builder.append(", mainLanguages=").append(mainLanguages);
		builder.append(", mainLocalizedTitle=").append(mainLocalizedTitle);
		builder.append(", mainNextEpisode=").append(mainNextEpisode);
		builder.append(", mainNumberOfEpisodes=").append(mainNumberOfEpisodes);
		builder.append(", mainNumberOfSeasons=").append(mainNumberOfSeasons);
		builder.append(", mainOriginalAirDate=").append(mainOriginalAirDate);
		builder.append(", mainOriginalTitle=").append(mainOriginalTitle);
		builder.append(", mainPlotOutline=").append(mainPlotOutline);
		builder.append(", mainPreviousEpisode=").append(mainPreviousEpisode);
		builder.append(", mainProductionStatus=").append(mainProductionStatus);
		builder.append(", mainProductionStatusUpdated=").append(mainProductionStatusUpdated);
		builder.append(", mainRating=").append(mainRating);
		builder.append(", mainRuntimes=").append(mainRuntimes);
		builder.append(", mainSeason=").append(mainSeason);
		builder.append(", mainSeasons=").append(mainSeasons);
		builder.append(", mainSeriesYears=").append(mainSeriesYears);
		builder.append(", mainSoundMix=").append(mainSoundMix);
		builder.append(", mainCasts=").append(mainCasts);
		builder.append(", mainTitle=").append(mainTitle);
		builder.append(", mainTop250Rank=").append(mainTop250Rank);
		builder.append(", mainVideos=").append(mainVideos);
		builder.append(", mainVotes=").append(mainVotes);
		builder.append(", mainWriters=").append(mainWriters);
		builder.append(", mainYear=").append(mainYear);
		builder.append(", name=").append(name);
		builder.append(", originalName=").append(originalName);
		builder.append(", plotPlot=").append(plotPlot);
		builder.append(", plotSynopsis=").append(plotSynopsis);
		builder.append(", resolutionDescription=").append(resolutionDescription);
		builder.append(", serialVersionUID=").append(serialVersionUID);
		builder.append(", size=").append(size);
		builder.append(", subTitles=").append(subTitles);
		builder.append(", timeInHHMMSS=").append(timeInHHMMSS);
		builder.append(", width=").append(width);
		builder.append("]");
		return builder.toString();
	}

}

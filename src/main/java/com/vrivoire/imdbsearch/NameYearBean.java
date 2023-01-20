package com.vrivoire.imdbsearch;

import com.omertron.omdbapi.model.OmdbVideoFull;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Duration;
import java.time.LocalTime;

/**
 *
 * @author Vincent
 */
public class NameYearBean extends OmdbVideoFull {

	private static final long serialVersionUID = -8717940658283105093L;

	private String name;
	private String originalName;
	private long fileDate;
	private boolean isDirectory;
	private String size;

	/**
	 *
	 */
	public NameYearBean() {
	}

	public String getRuntimeHM() {
		String s = getRuntime();
		if (s == null || "".equals(s) || s.contains("N/A")) {
			s = "0";
		}
		s = s.replace("min", "").trim();
		return LocalTime.MIN.plus(Duration.ofMinutes(Long.parseLong(s))).toString();
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

	/**
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 *
	 * @param name
	 */
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

	void setSize(long bytes) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			size = bytes + " B";
			return;
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		size = String.format("%.1f %ciB", value / 1024.0, ci.current());
	}

	public String getSize() {
		return size;
	}

	public boolean isIsDirectory() {
		return isDirectory;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NameYearBean [");
		builder.append("getFileDate=").append(getFileDate());
		builder.append(", getName=").append(getName());
		builder.append(", getOriginalName=").append(getOriginalName());
		builder.append(", getSize=").append(getSize());
		builder.append(", isDirectory=").append(isDirectory());
		builder.append(", isIsDirectory=").append(isIsDirectory());
		builder.append(", getActors=").append(getActors());
		builder.append(", getAwards=").append(getAwards());
		builder.append(", getCountries=").append(getCountries());
		builder.append(", getDirector=").append(getDirector());
		builder.append(", getEpisode=").append(getEpisode());
		builder.append(", getGenre=").append(getGenre());
		builder.append(", getImdbRating=").append(getImdbRating());
		builder.append(", getImdbVotes=").append(getImdbVotes());
		builder.append(", getLanguages=").append(getLanguages());
		builder.append(", getMetascore=").append(getMetascore());
		builder.append(", getPlot=").append(getPlot());
		builder.append(", getRated=").append(getRated());
		builder.append(", getReleased=").append(getReleased());
		builder.append(", getRuntime=").append(getRuntime());
		builder.append(", getRuntimeHM=").append(getRuntimeHM());
		builder.append(", getSeason=").append(getSeason());
		builder.append(", getTomatoBoxOffice=").append(getTomatoBoxOffice());
		builder.append(", getTomatoConsensus=").append(getTomatoConsensus());
		builder.append(", getTomatoDvd=").append(getTomatoDvd());
		builder.append(", getTomatoFresh=").append(getTomatoFresh());
		builder.append(", getTomatoImage=").append(getTomatoImage());
		builder.append(", getTomatoMeter=").append(getTomatoMeter());
		builder.append(", getTomatoProduction=").append(getTomatoProduction());
		builder.append(", getTomatoRating=").append(getTomatoRating());
		builder.append(", getTomatoReviews=").append(getTomatoReviews());
		builder.append(", getTomatoRotten=").append(getTomatoRotten());
		builder.append(", getTomatoURL=").append(getTomatoURL());
		builder.append(", getTomatoUserMeter=").append(getTomatoUserMeter());
		builder.append(", getTomatoUserRating=").append(getTomatoUserRating());
		builder.append(", getTomatoUserReviews=").append(getTomatoUserReviews());
		builder.append(", getTomatoWebsite=").append(getTomatoWebsite());
		builder.append(", getWriter=").append(getWriter());
		builder.append(", getImdbID=").append(getImdbID());
		builder.append(", getPoster=").append(getPoster());
		builder.append(", getTitle=").append(getTitle());
		builder.append(", getType=").append(getType());
		builder.append(", getYear=").append(getYear());
		builder.append(", getError=").append(getError());
		builder.append(", isResponse=").append(isResponse());

		builder.append("]");
		return builder.toString();
	}

}

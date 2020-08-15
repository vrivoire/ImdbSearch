package com.vrivoire.imdbsearch;

import com.omertron.omdbapi.model.OmdbVideoFull;

import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 *
 * @author Vincent
 */
public class NameYearBean extends OmdbVideoFull {

	private static final long serialVersionUID = -8717940658283105093L;
	private transient static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private String name;
	private String originalName;
	private long fileDate;
	private boolean isDirectory;

	/**
	 *
	 */
	public NameYearBean() {
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

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 89 * hash + Objects.hashCode(this.name);
		hash = 89 * hash + Objects.hashCode(this.originalName);
		hash = 89 * hash + (int) (this.fileDate ^ (this.fileDate >>> 32));
		hash = 89 * hash + (this.isDirectory ? 1 : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final NameYearBean other = (NameYearBean) obj;
		if (this.fileDate != other.fileDate) {
			return false;
		}
		if (this.isDirectory != other.isDirectory) {
			return false;
		}
		if (!Objects.equals(this.name, other.name)) {
			return false;
		}
		if (!Objects.equals(this.originalName, other.originalName)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NameYearBean [");
		builder.append("fileDate=").append(fileDate);
		builder.append(", isDirectory=").append(isDirectory);
		builder.append(", name=").append(name);
		builder.append(", originalName=").append(originalName);
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
		builder.append("]");
		return builder.toString();
	}

}

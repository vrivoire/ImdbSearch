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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.originalName);
        hash = 89 * hash + (int) (this.fileDate ^ (this.fileDate >>> 32));
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
        builder.append("name=").append(name);
        builder.append(", originalName=").append(originalName);
        builder.append(", fileDate=").append(DATE_FORMATER.format(fileDate));
        builder.append(", getImdbRating=").append(getImdbRating());
        builder.append(", getYear=").append(getYear());
        builder.append("]");
        return builder.toString();
    }

}

package com.vrivoire.imdbsearch;

import com.vrivoire.imdbsearch.log4j.LogGrabberAppender;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JTextArea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoveImdbEntry {

	private static final Logger LOG = LogManager.getLogger(Main.class);
	static final String SQL_DELETE = "delete from films where imdb_id=?;";

	static {
		System.setProperty("-J-Djava.util.Arrays.useLegacyMergeSort", "true");
		LogGrabberAppender.setPanel(new JTextArea());
		Config.configure();
	}

	public static void main(String[] args) {
		String imdbId = "26683281";
		try (Connection conn = DriverManager.getConnection(Config.DB_PROTOCOL.getString() + Config.DB_URL.getString()); PreparedStatement pstmt1 = conn.prepareStatement(Main.SQL_SELECT)) {
			pstmt1.setString(1, imdbId);
			ResultSet rs = pstmt1.executeQuery();
			if (rs.next()) {
				LOG.info("FOUND " + imdbId + "\t" + rs.getString("title") + "\t" + rs.getString("year"));
				try (PreparedStatement pstmt2 = conn.prepareStatement(SQL_DELETE)) {
					pstmt2.setString(1, imdbId);
					int val = pstmt2.executeUpdate();
					LOG.info("Update val=" + val);
					if (val == 1) {
						LOG.info("The row count for SQL Data Manipulation Language (DML) statements");
					} else if (val == 2) {
						LOG.info("0 for SQL statements that return nothing");
					} else {
						LOG.info("Return '" + val + "' not understandable for param imdb_id='" + imdbId + "'.");
					}
				}
			} else {
				LOG.warn("NOT FOUND imdb_id='" + imdbId);
			}
			System.exit(0);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			System.exit(-1);
		}
	}

}

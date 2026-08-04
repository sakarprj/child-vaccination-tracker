package com.hospital.vaccination.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central JDBC connection helper.
 *
 * <p>Every DAO calls {@link #get()} to obtain a fresh {@link Connection}.
 * Credentials come from <code>src/main/resources/db.properties</code>, so
 * nothing sensitive is hard-coded in Java.
 */
public final class DBConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found on classpath " +
                                "(expected at src/main/resources/db.properties)");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Force-load the MySQL driver so DriverManager can see it.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "MySQL JDBC driver not on classpath — check pom.xml");
        }
    }

    private DBConnection() { /* utility class */ }

    /** @return a brand-new JDBC connection; caller must close it. */
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password"));
    }

    /**
     * Quick sanity check used by {@link com.hospital.vaccination.Main}
     * on startup. Returns true if we can open + close a connection.
     */
    public static boolean canConnect() {
        try (Connection c = get()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            System.err.println("DB connection failed: " + e.getMessage());
            return false;
        }
    }
}
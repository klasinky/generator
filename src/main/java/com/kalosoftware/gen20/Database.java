package com.kalosoftware.gen20;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 *
 * @author Carlos Cercado
 * @email cercadocarlos@gmail.com
 */
public class Database {
    
    private final static Logger LOGGER = Logger.getLogger(Database.class.getName());
    public static final String DRIVER = "org.postgresql.Driver";
    /*public static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/proyecto";
    public static final String USERNAME = "postgres";
    public static final String PASSWORD = "centauro";*/

    public static Connection getConection(String url, String username, String password) throws SQLException {
        try {
            java.lang.Class.forName(DRIVER);
            return DriverManager.getConnection(
                url,
                username, password);
        } catch (ClassNotFoundException ex) {
            LOGGER.severe("Error al registrar el driver de PostgreSQL: " + ex);
            return null;
        }
    }

}

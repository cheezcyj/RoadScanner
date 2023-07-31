package com.roadscanner.board.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class OracleDbUtil {
    private static final String CONFIG_FILE = "dbconfig.xml";

    public static Connection getConnection() throws SQLException {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(CONFIG_FILE));

            Element rootElement = doc.getDocumentElement();
            String dbUrl = rootElement.getElementsByTagName("dburl").item(0).getTextContent();
            String username = rootElement.getElementsByTagName("username").item(0).getTextContent();
            String password = rootElement.getElementsByTagName("password").item(0).getTextContent();

            Class.forName("oracle.jdbc.driver.OracleDriver");
            return DriverManager.getConnection(dbUrl, username, password);
        } catch (ParserConfigurationException | SAXException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new SQLException("Failed to connect to the database");
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

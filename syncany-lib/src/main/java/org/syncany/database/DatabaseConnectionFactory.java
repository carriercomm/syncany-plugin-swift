/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2013 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.syncany.config.Config;
import org.syncany.util.SqlRunner;

/**
 * @author pheckel
 *
 */
public class DatabaseConnectionFactory {
	public static final String DATABASE_DRIVER = "org.hsqldb.jdbcDriver";
	public static final String DATABASE_CONNECTION_FILE_STRING = "jdbc:hsqldb:file:%DATABASEFILE%;user=sa;password=;create=true;write_delay=false;hsqldb.write_delay=false;shutdown=true";
	public static final String DATABASE_CONNECTION_MEM_STRING = "jdbc:hsqldb:mem:%DATABASENAME%;user=sa;password=;create=true;write_delay=false;hsqldb.write_delay=false;shutdown=true";
	
	public static final String DATABASE_SCRIPT_RESOURCE = "/createtables.sql";
	
	static {
		try {
			Class.forName(DATABASE_DRIVER);
		}
		catch (Exception e) {
			throw new RuntimeException("Cannot load database driver: "+DATABASE_DRIVER, e);
		}
	}
		
	public static Connection createFileConnection(File databaseFile) {
		String connectionString = DATABASE_CONNECTION_FILE_STRING.replaceAll("%DATABASEFILE%", databaseFile.toString());			
		return createConnection(connectionString);
	}
	
	public static Connection createMemoryConnection(String databaseName) {
		String connectionString = DATABASE_CONNECTION_MEM_STRING.replaceAll("%DATABASENAME%", databaseName);			
		return createConnection(connectionString);
	} 
	
	private static Connection createConnection(String connectionString) {
		try {
			Connection connection = DriverManager.getConnection(connectionString);			
			connection.setAutoCommit(false);
			
			// Test and create tables
			if (!tablesExist(connection)) {
				createTables(connection);
			}
			
			return connection;
		}
		catch (SQLException e) {
			throw new RuntimeException("Cannot create new connection; database down?", e);
		}
	} 

	private static boolean tablesExist(Connection connection) {
		try {
			ResultSet resultSet = connection.prepareStatement("select count(*) from chunk").executeQuery();
			
			if (resultSet.next()) {
				return true;
			}
			else {
				return false;
			}
		}
		catch (SQLException e) {
			return false;
		}
	}
	
	private static void createTables(Connection sqlConnection) throws SQLException {
		InputStream inputStream = Config.class.getResourceAsStream(DATABASE_SCRIPT_RESOURCE);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		sqlConnection.setAutoCommit(true);
		new SqlRunner(sqlConnection, true).runScript(reader);
		
		sqlConnection.commit();
	}
}
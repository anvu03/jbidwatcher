package com.jbidwatcher.util.db;

/*
 * Copyright (c) 2000-2007, CyberFOX Software, Inc. All Rights Reserved.
 *
 * Developed by mrs (Morgan Schweers)
 */

import com.jbidwatcher.platform.Path;
import com.jbidwatcher.util.config.JConfig;

import java.sql.*;
import java.util.Properties;

public class Database {
  private static boolean sFirst = true;
  private String framework;
  private String driver;
  private String protocol;
  private Connection mConn;
  private boolean mNew;
  private boolean mysql = false;

  public boolean isMySQL() { return mysql; }

  public static void main(String[] args) {
    try {
      Database db = new Database("/Users/mrs/.jbidwatcher");
      db.shutdown();
    } catch(Exception e) {
      handleSQLException(e);
    }
  }

  public Database(String base) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
    /* the default framework is embedded*/
    framework = JConfig.queryConfiguration("db.framework", "embedded");
    driver = JConfig.queryConfiguration("db.driver", "org.apache.derby.jdbc.EmbeddedDriver");
    protocol = JConfig.queryConfiguration("db.protocol", "jdbc:derby:");
    mysql = driver.toLowerCase().contains("mysql");

    if(base == null) base = Path.getHomeDirectory("jbidwatcher");
    if(!mysql) {
      System.setProperty("derby.system.home", base);
      System.setProperty("derby.storage.pageCacheSize", "500");
      if (JConfig.debugging) {
        System.setProperty("derby.locks.monitor", "true");
        System.setProperty("derby.locks.deadlockTrace", "true");
        System.setProperty("derby.language.logStatementText", "true");
      }
    }
    setup();
  }

  private void setup() throws ClassNotFoundException, IllegalAccessException, InstantiationException, SQLException {
    /*
       The driver is installed by loading its class.
       In an embedded environment, this will start up Derby, since it is not already running.
     */
    Class.forName(driver).newInstance();
    if(sFirst) JConfig.log().logDebug("Loaded the appropriate driver.");

    Properties props = new Properties();
    props.setProperty("user", JConfig.queryConfiguration("db.user", "user1"));
    props.setProperty("password", JConfig.queryConfiguration("db.pass", "user1"));

    /*
       The connection specifies create=true to cause
       the database to be created. To remove the database,
       remove the directory 'jbdb' and its contents.
       The directory 'jbdb' will be created under
       the directory that the system property
       derby.system.home points to, or the current
       directory if derby.system.home is not set.
     */
    try {
      if(isMySQL()) {
        mConn = DriverManager.getConnection(protocol + "jbidwatcher", props);
      } else {
        mConn = DriverManager.getConnection(protocol + "jbdb", props);
      }
      mNew = false;
    } catch(SQLException se) {
      if(isMySQL()) {
        mConn = DriverManager.getConnection(protocol, props);
        Statement s = mConn.createStatement();
        s.executeUpdate("CREATE DATABASE jbidwatcher");
        mConn.close();
        mConn = DriverManager.getConnection(protocol + JConfig.queryConfiguration("db.mysql.database", "jbidwatcher"), props);
      } else {
        mConn = DriverManager.getConnection(protocol + "jbdb;create=true", props);
      }
      mNew = true;
    }
    if(sFirst) {
      JConfig.log().logDebug("Connected to " + (mNew?"and created ":"") + "database " + (isMySQL() ? JConfig.queryConfiguration("db.mysql.database", "jbidwatcher") : "jbdb") + " (JBidwatcher DataBase)");
    }

    mConn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
    boolean autoCommit = JConfig.queryConfiguration("db.autocommit", "true").equals("true");
    mConn.setAutoCommit(autoCommit);
    sFirst = false;
  }

  public Statement getStatement() {
    Statement rval = null;

    try {
      if(mConn != null) rval = mConn.createStatement();
    } catch(SQLException squee) {
      handleSQLException(squee);
    }

    return rval;
  }

  public void commit() {
    if(isMySQL()) return;

    try {
      mConn.commit();
    } catch(SQLException squee) {
      handleSQLException(squee);
    }
  }

  public void shutdown() {
    try {
      mConn.close();
      JConfig.log().logDebug("Closed connection");

      /*
         In embedded mode, an application should shut down Derby.
         If the application fails to shut down Derby explicitly,
         the Derby does not perform a checkpoint when the JVM shuts down, which means
         that the next connection will be slower.
         Explicitly shutting down Derby with the URL is preferred.
         This style of shutdown will always throw an "exception".
       */
      if (framework.equals("embedded")) {
        boolean gotSQLExc = false;
        try {
          DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException se) {
          gotSQLExc = true;
        }

        if (!gotSQLExc) {
          JConfig.log().logMessage("Database did not shut down normally");
        } else {
          JConfig.log().logDebug("Database shut down normally");
        }
      }
    } catch (Throwable e) {
      handleSQLException(e);
    }
    sFirst = true;
  }

  private static void handleSQLException(Throwable e) {
    JConfig.log().logDebug("exception thrown:");

    if (e instanceof SQLException) {
      printSQLError((SQLException) e);
    } else {
      e.printStackTrace();
    }
  }


  static void printSQLError(SQLException e) {
    while (e != null) {
      JConfig.log().logDebug(e.toString());
      e = e.getNextException();
    }
  }

  public PreparedStatement prepare(String statement) throws SQLException {
    return mConn.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS);
  }

  public Connection getConnection() {
    return mConn;
  }

  public static boolean saveDBConfig() {
    if(JConfig.queryConfiguration("temp.db.protocol") != null) {
      String[] keys = { "db.framework", "db.protocol", "db.driver", "db.user", "db.pass" };
      for(String key : keys) {
        JConfig.setConfiguration(key, JConfig.queryConfiguration("temp." + key));
      }
      JConfig.kill("jbidwatcher.created_db");
      return true;
    } else if(JConfig.queryConfiguration("temp.db.switch2derby", "false").equals("true")) {
      JConfig.kill("db.protocol");
      JConfig.kill("db.driver");
      JConfig.kill("db.user");
      JConfig.kill("db.pass");
    }
    return false;
  }
}

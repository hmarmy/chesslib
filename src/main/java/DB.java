
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

public class DB {

  public static Connection connect(final String dbName, boolean autocommit, boolean logging) {

    try {
      String dbString = "jdbc:hsqldb:hsql://localhost/" + dbName + ";allow_empty_batch=true;filepath="+dbName;
      Utils.info("[" + dbName + "] : OPENING DB CONNECTION " + dbString, "autocommit : " + autocommit);
      final Connection c = DriverManager.getConnection(dbString, "SA", "");
      Statement stmt = c.createStatement();
      stmt.executeUpdate("SET FILES LOG FALSE");
      stmt.executeUpdate("CHECKPOINT");
      c.commit();

      c.setAutoCommit(autocommit);
      tablesInfo(c, dbName);

      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          if (c != null)
            try {
              Utils.info("[" + dbName + "] : CLOSING DB CONNECTION ");
              c.commit();
              tablesInfo(c, dbName);
              c.close();
            } catch (SQLException e) {
              e.printStackTrace();
            }
        }
      });
      return c;
    } catch (SQLException e) {
      e.printStackTrace();
      System.exit(-1);
      return null;
    }
  }

  public static void createTable(Connection c, String tableName, String sql, boolean drop) throws SQLException {

    Statement stmt = c.createStatement();

    HashSet<String> tables = new HashSet<String>();

    DatabaseMetaData databaseMetaData = c.getMetaData();
    ResultSet res = databaseMetaData.getTables(null, null, null, new String[] { "TABLE" });

    while (res.next()) {
      String name = (res.getString("TABLE_NAME"));
      tables.add(name);
    }
    Utils.info("TABLES IN DB", tables);

    if (drop && tables.contains(tableName)) {
      stmt.executeUpdate("drop table " + tableName);
      stmt.executeUpdate(sql);
      c.commit();
      Utils.info("Table " + tableName + " dropped & re-created");
    } else if (!tables.contains(tableName)) {
      stmt.executeUpdate(sql);
      c.commit();
      Utils.info("Table " + tableName + " created");
    } else {
      Utils.info("Table " + tableName + " already exist");
    }
  }

  public static void dump(ResultSet rs) throws SQLException {
    ResultSetMetaData meta = rs.getMetaData();
    int colmax = meta.getColumnCount();
    int i;
    Object o = null;
    for (; rs.next();) {
      for (i = 0; i < colmax; ++i) {
        o = rs.getObject(i + 1); // Is SQL the first column is indexed
        System.out.print(o + " ");
      }

      System.out.println(" ");
    }
  } // void dump( ResultSet rs )

  public static void tablesInfo(Connection c, String dbName) throws SQLException {

    HashSet<String> tables = new HashSet<String>();

    DatabaseMetaData databaseMetaData = c.getMetaData();
    ResultSet res = databaseMetaData.getTables(null, null, null, new String[] { "TABLE" });

    while (res.next()) {
      String name = (res.getString("TABLE_NAME"));
      tables.add(name);
    }

    for (String table : tables) {
      ResultSet rs = c.createStatement().executeQuery("SELECT count(*) from " + table);
      rs.next();
      int rowCount = rs.getInt(1);
      Utils.info(dbName, table, rowCount);
    }
  }

  public static void shutdownDb(Connection c) throws Exception {
    c.commit();
    c.createStatement().executeQuery("SHUTDOWN");
  }
}

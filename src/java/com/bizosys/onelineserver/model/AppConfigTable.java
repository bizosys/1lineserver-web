package com.bizosys.onelineserver.model;

import java.util.List;
import java.sql.SQLException;

import com.oneline.dao.ReadObject;
import com.oneline.dao.ReadXml;
import com.oneline.dao.WriteBase;


public class AppConfigTable {

	/** The VO Class */
	public static final Class<AppConfig> clazz = AppConfig.class;

	/** The SQL Select statement */
	public static String sqlSelect =
		"select id, configtype, title, body, status	 from app_config";

	/** The SQL Select statements of all records */
	public static List<AppConfig> selectAll() throws SQLException {
		return new ReadObject<AppConfig>(clazz).execute(sqlSelect);
	}

	/** The SQL Select statements on indexed fields and primary keys */
	private static String sqlSelectByid = sqlSelect + " where id = ?";

	private static String sqlSelectByconfigtype = sqlSelect + " where configtype = ?";

	/** The SQL Insert statement with auto increment */
	private static String sqlInsert =
		"insert into app_config (configtype, title, body, status	 ) " + 
 		"values (?, ?, ?, ? )";

	/** The SQL Insert statement with primary key */
	private static String sqlInsertPK =
		"insert into app_config (id, configtype, title, body, status	 ) " + 
 		"values (?, ?, ?, ?, ? )";

	/** The SQL Update statement */
	private static String sqlUpdate =
		"update app_config SET configtype = ?, title = ?, body = ?, status = ? " + 
		"where id = ?	";



	/** The private constructor. All methods are static public */
	protected AppConfigTable() {
	}


	/** Sql select functions */
	public static AppConfig selectById( Object id) throws SQLException {
		Object record = new ReadObject<AppConfig>(clazz).selectByPrimaryKey(sqlSelectByid,id);
		if ( null == record) return null;
		return (AppConfig) record;
	}

	public static String selectXmlById( Object id) throws SQLException {
		Object record = new ReadXml<AppConfig>(clazz).selectByPrimaryKey(sqlSelectByid,id);
		if ( null == record) return null;
		return (String) record;
	}

	public static List<AppConfig> selectByConfigtype( Object configtype) throws SQLException {
		return new ReadObject<AppConfig>(clazz).execute(sqlSelectByconfigtype, new Object[]{configtype});
	}

	public static List<String> selectXmlByConfigtype( Object configtype) throws SQLException {
		return new ReadXml<AppConfig>(clazz).execute(sqlSelectByconfigtype, new Object[]{configtype});
	}


	/** Sql Insert with Auto increment function */
	public static void insert( AppConfig record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		record.id = sqlWriter.insert(sqlInsert, record.getNewPrint());
	}


	/** Sql Insert with PK function */
	public static void insertPK( AppConfig record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		sqlWriter.execute(sqlInsertPK, record.getNewPrintWithPK());
	}


	/** Sql Update function */
	public static void update( AppConfig record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		sqlWriter.execute(sqlUpdate, record.getExistingPrint());
	}

}
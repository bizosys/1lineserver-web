package com.bizosys.onelineserver.user;

import java.util.List;
import java.sql.SQLException;

import com.oneline.dao.ReadObject;
import com.oneline.dao.ReadXml;
import com.oneline.dao.WriteBase;


public class UserLoginTable {

	/** The VO Class */
	public static final Class<UserLogin> clazz = UserLogin.class;

	/** The SQL Select statement */
	public static String sqlSelect =
		"select id, active, loginid, password, profile	 from user_login";

	/** The SQL Select statements of all records */
	public static List<UserLogin> selectAll() throws SQLException {
		return new ReadObject<UserLogin>(clazz).execute(sqlSelect);
	}

	/** The SQL Select statements on indexed fields and primary keys */
	private static String sqlSelectByid = sqlSelect + " where id = ?";

	private static String sqlSelectByloginid = sqlSelect + " where loginid = ?";

	/** The SQL Insert statement with auto increment */
	private static String sqlInsert =
		"insert into user_login (active, loginid, password, profile	 ) " + 
 		"values (?, ?, ?, ? )";

	/** The SQL Insert statement with primary key */
	private static String sqlInsertPK =
		"insert into user_login (id, active, loginid, password, profile	 ) " + 
 		"values (?, ?, ?, ?, ? )";

	/** The SQL Update statement */
	private static String sqlUpdate =
		"update user_login SET active = ?, loginid = ?, password = ?, profile = ? " + 
		"where id = ?	";



	/** The private constructor. All methods are static public */
	protected UserLoginTable() {
	}


	/** Sql select functions */
	public static UserLogin selectById( Object id) throws SQLException {
		Object record = new ReadObject<UserLogin>(clazz).selectByPrimaryKey(sqlSelectByid,id);
		if ( null == record) return null;
		return (UserLogin) record;
	}

	public static String selectXmlById( Object id) throws SQLException {
		Object record = new ReadXml<UserLogin>(clazz).selectByPrimaryKey(sqlSelectByid,id);
		if ( null == record) return null;
		return (String) record;
	}

	public static UserLogin selectByLoginid( Object loginid) throws SQLException {
		Object record = new ReadObject<UserLogin>(clazz).selectByPrimaryKey(sqlSelectByloginid,loginid);
		if ( null == record) return null;
		return (UserLogin) record;
	}

	public static String selectXmlByLoginid( Object loginid) throws SQLException {
		Object record = new ReadXml<UserLogin>(clazz).selectByPrimaryKey(sqlSelectByloginid,loginid);
		if ( null == record) return null;
		return (String) record;
	}


	/** Sql Insert with Auto increment function */
	public static void insert( UserLogin record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		record.id = sqlWriter.insert(sqlInsert, record.getNewPrint());
	}


	/** Sql Insert with PK function */
	public static void insertPK( UserLogin record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		sqlWriter.execute(sqlInsertPK, record.getNewPrintWithPK());
	}


	/** Sql Update function */
	public static void update( UserLogin record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		sqlWriter.execute(sqlUpdate, record.getExistingPrint());
	}

}



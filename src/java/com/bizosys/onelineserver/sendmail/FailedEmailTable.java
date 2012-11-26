package com.bizosys.onelineserver.sendmail;

import java.util.List;
import java.sql.SQLException;

import com.oneline.dao.ReadObject;
import com.oneline.dao.ReadXml;
import com.oneline.dao.WriteBase;


public class FailedEmailTable {

	/** The VO Class */
	public static final Class<FailedEmail> clazz = FailedEmail.class;

	/** The SQL Select statement */
	public static String sqlSelect =
		"select id, mail_to as mailTo, mail_cc as mailCc, msg_subject as msgSubject, msg_body as msgBody, attachments	 from failed_email";

	/** The SQL Select statements of all records */
	public static List<FailedEmail> selectAll() throws SQLException {
		return new ReadObject<FailedEmail>(clazz).execute(sqlSelect);
	}

	/** The SQL Select statements on indexed fields and primary keys */
	private static String sqlSelectByid = sqlSelect + " where id = ?";

	private static String sqlSelectBytouchTime = sqlSelect + " where touchTime = ?";

	/** The SQL Insert statement with auto increment */
	private static String sqlInsert =
		"insert into failed_email (mail_to, mail_cc, msg_subject, msg_body, attachments	 ) " + 
 		"values (?, ?, ?, ?, ? )";

	/** The SQL Insert statement with primary key */
	private static String sqlInsertPK =
		"insert into failed_email (id, mail_to, mail_cc, msg_subject, msg_body, attachments	 ) " + 
 		"values (?, ?, ?, ?, ?, ? )";

	/** The SQL Update statement */
	private static String sqlUpdate =
		"update failed_email SET mail_to = ?, mail_cc = ?, msg_subject = ?, msg_body = ?, attachments = ? " + 
		"where id = ?	";



	/** The private constructor. All methods are static public */
	private FailedEmailTable() {
	}


	/** Sql select functions */
	public static FailedEmail selectById( Object id) throws SQLException {
		Object record = new ReadObject<FailedEmail>(clazz).selectByPrimaryKey(sqlSelectByid,id);
		if ( null == record) return null;
		return (FailedEmail) record;
	}

	public static String selectXmlById( Object id) throws SQLException {
		Object record = new ReadXml<FailedEmail>(clazz).selectByPrimaryKey(sqlSelectByid,id);
		if ( null == record) return null;
		return (String) record;
	}

	public static List<FailedEmail> selectByTouchTime( Object touchTime) throws SQLException {
		return new ReadObject<FailedEmail>(clazz).execute(sqlSelectBytouchTime, new Object[]{touchTime});
	}

	public static List<String> selectXmlByTouchTime( Object touchTime) throws SQLException {
		return new ReadXml<FailedEmail>(clazz).execute(sqlSelectBytouchTime, new Object[]{touchTime});
	}


	/** Sql Insert with Auto increment function */
	public static void insert( FailedEmail record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		record.id = sqlWriter.insert(sqlInsert, record.getNewPrint());
	}


	/** Sql Insert with PK function */
	public static void insertPK( FailedEmail record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		sqlWriter.execute(sqlInsertPK, record.getNewPrintWithPK());
	}


	/** Sql Update function */
	public static void update( FailedEmail record, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) {
			sqlWriter = new WriteBase();
		}

		sqlWriter.execute(sqlUpdate, record.getExistingPrint());
	}

}





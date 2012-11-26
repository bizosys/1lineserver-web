package com.bizosys.onelineserver.model;

import java.sql.SQLException;
import java.util.List;

import com.oneline.dao.ReadObject;

public class AppConfigTableExt extends AppConfigTable
{
	private static String sqlSelectByTitle = sqlSelect + " where title = ?";
	private static String sqlSelectByTypeAndTitle = sqlSelect + " where doctype = ? and title = ?";
	private static String sqlSelectByParentIdAndType = sqlSelect + " where parentid = ? and doctype = ?";
	private static String sqlSelectByid = sqlSelect + " where id = ?";
	
	private AppConfigTableExt() {
	}

	public static AppConfig selectByTitle(Object title) throws SQLException 
	{
		List<AppConfig> listL = new ReadObject<AppConfig>(clazz).execute(sqlSelectByTitle, new Object[]{title});
		if (listL == null || listL.isEmpty()) return null;
		return listL.get(0);
	}

	public static AppConfig selectByTypeAndTitle(String type, String title) throws SQLException 
	{
		List<AppConfig> listL = new ReadObject<AppConfig>(clazz).execute(sqlSelectByTypeAndTitle, new Object[]{type, title});
		if (listL == null || listL.isEmpty()) return null;
		return listL.get(0);
	}

	public static AppConfig selectByParentIdAndType(Integer id, String type) throws SQLException 
	{
		List<AppConfig> listL = new ReadObject<AppConfig>(clazz).execute(sqlSelectByParentIdAndType, new Object[]{id, type});
		if (listL == null || listL.isEmpty()) return null;
		return listL.get(0);
	}

	public static List<AppConfig> selectListByParentIdAndType(Integer id, String type) throws SQLException 
	{
		return new ReadObject<AppConfig>(clazz).execute(sqlSelectByParentIdAndType, new Object[]{id, type});
	}

	public static AppConfig selectById( Object id, @SuppressWarnings("rawtypes") Class classToFill) throws SQLException {
		AppConfig record = new ReadObject<AppConfig>(clazz).selectByPrimaryKey(
			sqlSelectByid, id);
		if ( null == record) return null;
		return record;
	}

}

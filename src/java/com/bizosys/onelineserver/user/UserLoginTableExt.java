package com.bizosys.onelineserver.user;

import java.sql.SQLException;

import com.oneline.dao.WriteBase;

public class UserLoginTableExt extends UserLoginTable
{
	private static String sqlUpdate =
		"update user_login SET active = ? where loginid = ?";
	
	public UserLoginTableExt() {
	}
	
	public static void activate( String loginId, WriteBase sqlWriter) throws SQLException {
		if ( sqlWriter == null ) sqlWriter = new WriteBase();
		sqlWriter.execute(sqlUpdate, new Object[] {"Y", loginId});
	}
	
}

package com.bizosys.onelineserver.user;

public class UserLogin {

	public Integer id;
	public String active;
	public String loginid;
	public String password;
	public String profile;

	/** Default constructor */
	public UserLogin() {
	}


	/** Constructor with primary keys (Insert with primary key)*/
	public UserLogin(Integer id,String active,String loginid,String password,
		String profile) {

		this.id = id;
		this.active = active;
		this.loginid = loginid;
		this.password = password;
		this.profile = profile;

	}


	/** Constructor with Non Primary keys (Insert with autoincrement)*/
	public UserLogin(String active,String loginid,String password,String profile) {

		this.active = active;
		this.loginid = loginid;
		this.password = password;
		this.profile = profile;

	}


	/** Params for (Insert with autoincrement)*/
	public Object[] getNewPrint() {
		return new Object[] {
			active, loginid, password, profile
		};
	}


	/** Params for (Insert with primary key)*/
	public Object[] getNewPrintWithPK() {
		return new Object[] {
			id, active, loginid, password, profile
		};
	}


	/** Params for (Update)*/
	public Object[] getExistingPrint() {
		return new Object[] {
			active, loginid, password, profile, id
		};
	}

}


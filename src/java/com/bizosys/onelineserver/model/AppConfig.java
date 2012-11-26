package com.bizosys.onelineserver.model;

public class AppConfig {

	public Integer id;
	public String configtype;
	public String title;
	public String body;
	public String status;

	/** Default constructor */
	public AppConfig() {
	}


	/** Constructor with primary keys (Insert with primary key)*/
	public AppConfig(Integer id,String configtype,String title,String body,
		String status) {

		this.id = id;
		this.configtype = configtype;
		this.title = title;
		this.body = body;
		this.status = status;

	}


	/** Constructor with Non Primary keys (Insert with autoincrement)*/
	public AppConfig(String configtype,String title,String body,String status) {

		this.configtype = configtype;
		this.title = title;
		this.body = body;
		this.status = status;

	}


	/** Params for (Insert with autoincrement)*/
	public Object[] getNewPrint() {
		return new Object[] {
			configtype, title, body, status
		};
	}


	/** Params for (Insert with primary key)*/
	public Object[] getNewPrintWithPK() {
		return new Object[] {
			id, configtype, title, body, status
		};
	}


	/** Params for (Update)*/
	public Object[] getExistingPrint() {
		return new Object[] {
			configtype, title, body, status, id
		};
	}

}

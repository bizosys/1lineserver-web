package com.bizosys.onelineserver.sendmail;

public class FailedEmail {

	public Integer id;
	public String mailTo;
	public String mailCc;
	public String msgSubject;
	public String msgBody;
	public String attachments;

	/** Default constructor */
	public FailedEmail() {
	}


	/** Constructor with primary keys (Insert with primary key)*/
	public FailedEmail(Integer id,String mailTo,String mailCc,String msgSubject,
		String msgBody,String attachments) {

		this.id = id;
		this.mailTo = mailTo;
		this.mailCc = mailCc;
		this.msgSubject = msgSubject;
		this.msgBody = msgBody;
		this.attachments = attachments;

	}


	/** Constructor with Non Primary keys (Insert with autoincrement)*/
	public FailedEmail(String mailTo,String mailCc,String msgSubject,String msgBody,
		String attachments) {

		this.mailTo = mailTo;
		this.mailCc = mailCc;
		this.msgSubject = msgSubject;
		this.msgBody = msgBody;
		this.attachments = attachments;

	}


	/** Params for (Insert with autoincrement)*/
	public Object[] getNewPrint() {
		return new Object[] {
			mailTo, mailCc, msgSubject, msgBody, attachments
		};
	}


	/** Params for (Insert with primary key)*/
	public Object[] getNewPrintWithPK() {
		return new Object[] {
			id, mailTo, mailCc, msgSubject, msgBody, attachments
		};
	}


	/** Params for (Update)*/
	public Object[] getExistingPrint() {
		return new Object[] {
			mailTo, mailCc, msgSubject, msgBody, attachments, id
		};
	}

}

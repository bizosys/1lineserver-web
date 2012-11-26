package com.bizosys.onelineserver.sendmail;

import com.oneline.util.StringUtils;

public class SmtpMsg {
	
	public String[] toAddresses = null;
	public String[] ccAddresses = null;
	public String subject = StringUtils.Empty;
	public String body = StringUtils.Empty;
	public String[] attachments = null;
	
	@SuppressWarnings("unused")
	private SmtpMsg() {}
	
	public SmtpMsg(FailedEmail fe) {
		if ( StringUtils.isEmpty(fe.mailTo) ) 
			this.toAddresses = StringUtils.getStrings(fe.mailTo);
		
		if ( StringUtils.isEmpty(fe.mailCc) ) 
			this.ccAddresses = StringUtils.getStrings(fe.mailCc);
		
		if ( StringUtils.isEmpty(fe.attachments) ) 
			this.attachments = StringUtils.getStrings(fe.attachments);
		
		if ( StringUtils.isEmpty(fe.msgSubject) ) this.subject = fe.msgSubject;
		if ( StringUtils.isEmpty(fe.msgBody) ) this.body = fe.msgBody;
	}
	
	public SmtpMsg (String[] toAddreses, String subject, String body) {
		if ( null != toAddreses ) this.toAddresses = toAddreses;
		if ( null != subject )this.subject = subject;
		if ( null != body )this.body = body;
	}

	public SmtpMsg (String[] toAddreses, String subject, 
	String body, String[] attachments) {
		
		this(toAddreses,subject,body);
		if ( null != attachments )this.attachments = attachments;
	}

	public SmtpMsg (String[] toAddreses, String[] ccAddresses,
			String subject, String body) {
				
		this(toAddreses,subject,body);
		if ( null != ccAddresses ) this.ccAddresses = ccAddresses;
	}
	
	public SmtpMsg (String[] toAddreses, String[] ccAddresses,
	String subject, String body, String[] attachments) {
		
		this(toAddreses,subject,body,attachments);
		if ( null != ccAddresses )this.ccAddresses = ccAddresses;
	}
	
	public FailedEmail toFailedEmails() {
		
		FailedEmail fe = new FailedEmailExt(
			( null == toAddresses) ? StringUtils.Empty : StringUtils.arrayToString(toAddresses),
			( null == ccAddresses) ? StringUtils.Empty :StringUtils.arrayToString(ccAddresses),
			( null == subject) ? StringUtils.Empty : subject,
			( null == body) ? StringUtils.Empty : body, 
			( null == attachments) ? StringUtils.Empty : StringUtils.arrayToString(attachments)
		);

		return fe;
	}
	
}

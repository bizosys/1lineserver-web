package com.bizosys.onelineserver.sendmail;

import org.apache.commons.lang.StringEscapeUtils;

import com.bizosys.onelineserver.service.Configuration;
import com.bizosys.onelineserver.service.ServiceFactory;
import com.oneline.dao.WriteBase;
import com.oneline.util.StringUtils;

public class FailedEmailExt extends FailedEmail {

	public FailedEmailExt(String to,String cc,String subject,
			String body,String attachments) {
		
		super(to, cc, subject, body, attachments);
		encode();
	}
	
	public void encode() {
		if ( ! StringUtils.isEmpty(mailTo)) mailTo = StringEscapeUtils.escapeSql(mailTo);
		if ( ! StringUtils.isEmpty(mailCc)) mailCc = StringEscapeUtils.escapeSql(mailCc);
		if ( ! StringUtils.isEmpty(msgSubject)) msgSubject = StringEscapeUtils.escapeSql(msgSubject);
		if ( ! StringUtils.isEmpty(msgBody)) msgBody = StringEscapeUtils.escapeSql(msgBody);
		if ( ! StringUtils.isEmpty(attachments)) 
			attachments = StringEscapeUtils.escapeSql(attachments);
	}
	
	public static void main(String[] args) throws Exception{
		FailedEmailExt email = new FailedEmailExt("abhinashakgmail.com",
			null,"Reset password","Please body xx", null);
		ServiceFactory.getInstance().serviceStart(new Configuration());
		FailedEmailTable.insert(email, new WriteBase());
	}
	
}

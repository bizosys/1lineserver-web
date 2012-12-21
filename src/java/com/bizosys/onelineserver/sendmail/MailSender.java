package com.bizosys.onelineserver.sendmail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.sendmail.SMTPAgent.TransportSession;
import com.bizosys.onelineserver.service.ServiceFactory;
import com.oneline.dao.WriteBase;
import com.oneline.util.Configuration;
import com.oneline.util.StringUtils;

public class MailSender {
	
	private static Logger LOG = MailLogger.L;
	private static MailSender instance = null;
	
	public static MailSender getInstance() {
		if ( null != instance) return instance;
		synchronized (MailSender.class) {
			if ( null != instance) return instance;
			instance = new MailSender();
		}
		return instance;
	}
	
	private String smtpHostAddress = null;
	private int smtpPort = -1;
	private boolean isSecure = false;
	private String mailUser = null;
	private String mailPasswd = null;
	
	
	private MailSender() {
		
    	Configuration conf = ServiceFactory.getInstance().getAppConfig();
		String smtpConfigLine = conf.get("smtp.config","");
		if ( StringUtils.isEmpty(smtpConfigLine)) return;

		List<String> params = StringUtils.fastSplit(smtpConfigLine,'|');
		if ( params.size() < 5) {
			LOG.fatal("Invalid SMTP configuration: " + smtpConfigLine + 
			"\nExpecting: smtpHostAddress|smtpPort|isSecure|mailUser|mailPasswd");
			System.exit(1);
		}
		this.smtpHostAddress = params.get(0);
		this.smtpPort = new Integer(params.get(1));
		this.isSecure = new Boolean(params.get(2));
		this.mailUser = params.get(3);
		this.mailPasswd = params.get(4);
		for ( int i=5; i< params.size(); i++) {
			this.mailPasswd = this.mailPasswd + params.get(i); 
		}		
	}
	
	public boolean send(SmtpMsg msg) {
		if ( null == msg) return false;
		
		if ( StringUtils.isEmpty(smtpHostAddress)) {
			LOG.warn("Mail setting is missing. Storing as failed mails.\n" +
				"In your site.xml make an entry for smtp.config. Refer default.xml for sample configuration.");
			return false;
		}
		ArrayList<SmtpMsg> msgL = new ArrayList<SmtpMsg>(1);
		msgL.add(msg);
		try {
			SMTPAgent.send(smtpHostAddress, smtpPort, mailUser, mailPasswd, isSecure, msgL);
			return true;
		} catch (MessagingException ex) {
			LOG.warn(ex);
			FailedEmail fe = msg.toFailedEmails();
			try {
				FailedEmailTable.insert(fe, new WriteBase());
			} catch (SQLException sqlEx) {
				StringBuilder sb = new StringBuilder(2048);
				sb.append("FailedEmailInsert").append('\n');
				for (Object fld : fe.getNewPrint()) {
					if ( null != fld ) sb.append(fld.toString()).append("\r\n");
					else sb.append("\r\n");
				}
				LOG.fatal(sb.toString(), sqlEx);
			}
			return true;
		}
	}
	
	public TransportSession connect() throws MessagingException {
		return SMTPAgent.connect(
				this.smtpHostAddress, smtpPort, mailUser, mailPasswd, isSecure);
	}
	
}

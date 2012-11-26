package com.bizosys.onelineserver.sendmail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;

public class SMTPAgent {

	private static Logger LOG = MailLogger.L;
	

	public static TransportSession connect(String smtpAddress, int smtpPort,String mailUser,  
			String mailPasswd, boolean isSecure) throws MessagingException {

		LOG.debug("Creating a session to " + smtpAddress + ":" + smtpPort);
		Properties props = System.getProperties();
		props.put("mail.transport.protocol", "smtp");
		if ( isSecure ) {
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtps.auth", "true");
		}
		Session session = Session.getDefaultInstance(props);

		Transport transport = session.getTransport();;
		LOG.debug("Connecting to smtp server -" + mailUser);
		transport.connect(smtpAddress,smtpPort, mailUser, mailPasswd);
		InternetAddress fromAdress = new InternetAddress(mailUser);
		return new TransportSession(session, transport, fromAdress);
	}
	
	/**
	 * Send the text/html mail to the appropriate recipients with attachment
	 * @param smtpAddress
	 * @param mailUser
	 * @param mailPasswd
	 * @param toAddresses
	 * @param ccAddresses
	 * @param subject
	 * @param body
	 * @param attachments
	 * @throws MessagingException
	 */
	public static void send( String smtpAddress, int smtpPort,String mailUser,  
		String mailPasswd, boolean isSecure, List<SmtpMsg> mails) 
		throws MessagingException {
		
		TransportSession transport = null;
		
		try {
			transport = connect(smtpAddress, smtpPort, mailUser, mailPasswd, isSecure);
			
			LOG.debug("Building a message..");
			for ( SmtpMsg msg : mails ) {
				sendOneMessage(transport, msg);
			}
						
		} finally {
			LOG.info("Messages are sent and closing the connection..");
			if ( null != transport ) transport.close();
		}
	}

	public static void sendOneMessage(TransportSession transport, SmtpMsg msg) throws MessagingException, AddressException {
		int T = 0;
		
		MimeMessage message = new MimeMessage(transport.session);
		message.setFrom(transport.fromAddress);
		msg.subject = msg.subject.replace('\n', ' ');
		message.setSubject(msg.subject);

		LOG.debug("Create your new message part");
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setContent(msg.body, "text/html");
		
		LOG.debug("Create a related multi-part to combine the parts");
		MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		
		LOG.debug("Fetch the attachment and associate to part");
		if ( null != msg.attachments ) T = msg.attachments.length;
		else T = 0;
		
		for ( int i=0; i< T; i++) {
			if ( null == msg.attachments[i]) continue;
			LOG.debug("Create part for the attachment - " + msg.attachments[i]);
			messageBodyPart = new MimeBodyPart();

			File file = new File(msg.attachments[i]); 		
			DataSource fds = new FileDataSource(file);
			messageBodyPart.setFileName(file.getName());
			messageBodyPart.setDataHandler(new DataHandler(fds));

			LOG.debug("Add part to multi-part");
			multipart.addBodyPart(messageBodyPart);
		}

		//Associate multi-part with message
		message.setContent(multipart);
		
		T = msg.toAddresses.length;
		for ( int i=0; i< T; i++) {
			Address address = new InternetAddress(msg.toAddresses[i]); 		
			message.addRecipient(Message.RecipientType.TO , address);
		}
		
		if ( null != msg.ccAddresses ) T = msg.ccAddresses.length;
		else T = 0;
		for ( int i=0; i< T; i++) {
			Address address = new InternetAddress(msg.ccAddresses[i]); 		
			message.addRecipient(Message.RecipientType.CC , address);
		}

		LOG.debug("Sending the message..");
		transport.connection.sendMessage(message, message.getAllRecipients());
	}
	
	public static class TransportSession {
		Session session;
		Transport connection;
		Address fromAddress;
		
		
		public TransportSession(Session session, Transport transport, Address fromAddress) {
			this.session = session;
			this.connection = transport;
			this.fromAddress = fromAddress;
		}
		
		public void close() throws MessagingException {
			if ( null != connection) connection.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		List<SmtpMsg> msgs = new ArrayList<SmtpMsg>(1);
		SmtpMsg msg = new SmtpMsg(new String[]{"abhinashak@gmail.com"},
				"Hello Mail", "It is perfect.");
		msgs.add(msg);
		
		SMTPAgent.send("smtp.bizosys.com", 25, "XXX", "XXX", false, msgs);
	}
		
}

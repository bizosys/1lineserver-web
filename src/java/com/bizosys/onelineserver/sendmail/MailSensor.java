package com.bizosys.onelineserver.sendmail;

import java.util.List;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.sendmail.SMTPAgent.TransportSession;
import com.oneline.util.StringUtils;
import com.oneline.web.sensor.InvalidRequestException;
import com.oneline.web.sensor.Request;
import com.oneline.web.sensor.Response;
import com.oneline.web.sensor.Sensor;

public class MailSensor implements Sensor 
{
	private static Logger LOG = MailLogger.L;

	@Override
	public void processRequest(Request request, Response response)
	{
		String action = request.action;

		try {
			if ("send".equals(action)) {
				this.send(request, response);
			} else if ("sendbulk".equals(action)) {
				this.sendBulk(request, response);
			} else if ("send.backlog".equals(action)) {
				this.sendBacklog(request, response);
			} else {
				LOG.warn("Invalid Request - " + request.toString());
				throw new InvalidRequestException("INVALID_OPERATION");
			}
		} 
		catch (InvalidRequestException ex) {
			response.error("INVALID_INPUT", ex.getMessage());
			LOG.fatal(request.toString(), ex);
		} catch (Exception ex) {
			response.error("SYSTEM_ERROR", "Please contact System administrator");
			LOG.fatal(request.toString(), ex);
		} 
	}
	
	private void send(Request request, Response response) throws Exception
	{
		if ( ! "127.0.0.1".equals(request.clientIp) ) {
			if ( null == request.getUser() ) {
				response.error("UNKNOWN_USER", "User has not logged in");
			}
		}
		
		String to = request.getString("to", true, true, false);
		String cc = request.getString("cc", false, true, false);
		String subject = request.getString("subject", false, true, false);
		String body = request.getString("body", false, true, false);

		SmtpMsg msg = new SmtpMsg(
				StringUtils.getStrings(to),
				StringUtils.isEmpty(cc) ? null : StringUtils.getStrings(cc),
				subject, body);
		
		boolean isSent = MailSender.getInstance().send(msg);
		if ( isSent ) response.writeTextWithHeaderAndFooter("OK");
		else response.error("MAIL_FAILURE", "Mail could not be sent.");
	}
	
	private void sendBulk(Request request, Response response)
	{
	}	

	private void sendBacklog(Request request, Response response)
	{
		String touchTime = request.getString("touchTime", false, true, false);
		TransportSession transport = null;
		try {
			if ( StringUtils.isEmpty(touchTime)) {
				
			} else {
				List<FailedEmail> femails = 
					FailedEmailTable.selectByTouchTime(touchTime);
				transport = MailSender.getInstance().connect();
				
				LOG.debug("Building a message..");
				for ( FailedEmail femail : femails ) {
					SMTPAgent.sendOneMessage(transport, new SmtpMsg(femail));
				}
				
			}
		} catch (Exception ex) {
			
		} finally {
			LOG.info("Messages are sent and closing the connection..");
			if ( null != transport ) {
				try { transport.close(); } catch (Exception ex) {LOG.warn(ex); };
			}
		}
		
	}		

	@Override
	public void init() {
		MailSender.getInstance();
	}

	@Override
	public String getName() {
		return "sendmail";
	}	
}

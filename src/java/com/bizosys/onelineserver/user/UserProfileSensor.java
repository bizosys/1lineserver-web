package com.bizosys.onelineserver.user;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.sendmail.MailTask;
import com.bizosys.onelineserver.sendmail.SmtpMsg;
import com.bizosys.onelineserver.service.QueueProcessingService;
import com.bizosys.onelineserver.service.ServiceFactory;
import com.oneline.util.Configuration;
import com.oneline.util.FileReaderUtil;
import com.oneline.util.Hash;
import com.oneline.util.StringUtils;
import com.oneline.web.sensor.InvalidRequestException;
import com.oneline.web.sensor.Request;
import com.oneline.web.sensor.Response;
import com.oneline.web.sensor.Sensor;

public class UserProfileSensor implements Sensor 
{
	private final static Logger LOG = Logger.getLogger(UserProfileSensor.class);

	private String key = null;
	private boolean isLoginVerification = false;
	private String welcomeMailTemplateFile = null;
	private String passwordResetMailTemplateFile = null;
	private String welcomeMailUrlPrefix = StringUtils.Empty;
	
	@Override
	public void processRequest(Request request, Response response)
	{
		String action = request.action;

		try
		{
			if ("login".equals(action))
			{
				this.login(request, response);
			}
			else if ("logout".equals(action))
			{
				this.logout(request, response);
			}
			else if ("getloggedinuser".equals(action))
			{
				this.getLoggedInUser(request, response);
			}
			else if ("register".equals(action))
			{
				this.register(request, response);
			}
			else if ("changepassword".equals(action))
			{
				this.changePassword(request, response);
			}
			else if ("resetpassword".equals(action))
			{
				this.resetPassword(request, response);
			}
			else if ("activate".equals(action))
			{
				this.activate(request, response);
			}
			else
			{
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
	
	private void register(Request request, Response response)
	{
		try
		{
			String profile = request.getString("profile", true, false, true);
			String passwd = request.getString("password", true, true, false);
			String loginid = request.getString("loginid", true, true, false);
			
			if (loginid == null) {
				response.error("NO_LOGIN_ID", "Login ID is not sent in the user profile object.");
				return;
			}
			
			UserLogin existingUser = UserLoginTableExt.selectByLoginid(loginid);
	
			if (existingUser != null) {
				response.error("USER_EXISTS", "This user record already exists.");
				return;
			}
	
			String active = (this.isLoginVerification) ? "N" : "Y";
			
			String encodedPasswd = Hash.createHex(this.key, passwd);

			UserLogin newUser = new UserLogin(active, loginid, encodedPasswd, profile);
			UserLoginTableExt.insert(newUser, null);
			request.setUser(new UserProfile(loginid, newUser.id.toString()));
			UserCookieHandler.getInstance().setUser(request.getUser());
			newUser.password = "";
			response.writeObjectWithHeaderAndFooter(newUser);
			
			if ( null == welcomeMailTemplateFile) return;
			
			String token = Hash.createHex(this.key, loginid);
			StringBuilder link = new StringBuilder(512);
			link.append(welcomeMailUrlPrefix).append("loginid=");
			link.append(loginid).append("&token=").append(token);
			
			String welcomeMailTemplate = 
				FileReaderUtil.toString(welcomeMailTemplateFile);
			if ( StringUtils.isEmpty(welcomeMailTemplate)) {
				LOG.warn("No User Welcome mail template in file , " + welcomeMailTemplateFile);
				return;
			}
			String welcomeMessage = welcomeMailTemplate.replaceAll(
				"__returnlink", link.toString());
			String[] subjectAndBody = StringUtils.getStrings(welcomeMessage, '\n');
			
			SmtpMsg msg = null;
			if ( subjectAndBody.length == 1) {
				msg = new SmtpMsg(new String[]{loginid}, null, subjectAndBody[1]);
			} else {
				msg = new SmtpMsg(new String[]{loginid}, subjectAndBody[0], subjectAndBody[1]);
			}
			 
			QueueProcessingService.getInstance().addTask(new MailTask(msg));
		} 
		catch (Exception e)
		{
			response.error("SYSTEM_ERROR", "Sorry, unable to register.", e);
		}

	}

	private void login(Request request, Response response) throws Exception
	{
		String loginId = request.getString("loginid", true, true, false);
		String passwd = request.getString("password", true, true, false);

		UserLogin userlogin = UserLoginTableExt.selectByLoginid(loginId);
		
		if (userlogin == null) {
			response.error("USER_NOT_FOUND", "User id does not exist.");
			return;
		}
		
		String encodedPasswd = Hash.createHex(this.key, passwd);
		if ( !encodedPasswd.equals(userlogin.password)) {
			response.error("INCORRECT_PASSWORD", "Password does not match for this user.");
			return;
		}
		
		if ( ! "Y".equals(userlogin.active) ) {
			response.error("INACTIVE_USER", "Please activate your account.");
			return;
		}
		
		request.setUser(new UserProfile(loginId, userlogin.profile.toString()));
		UserCookieHandler.getInstance().setUser(request.getUser());
		userlogin.password = "";
		response.writeObjectWithHeaderAndFooter(userlogin);
	}

	private void logout(Request req, Response res)
	{
		req.setUser(UserProfile.getAnonymous());
		UserCookieHandler.getInstance().removeUser(req.getUser());
	}

	private void getLoggedInUser(Request request, Response response)
	{
		UserProfile user = request.getUser();
		UserLogin userlogin = null;
		
		if (!user.isGuest())
		{
			try
			{
				userlogin = UserLoginTableExt.selectById(user.loginid);
			} 
			catch (SQLException ex)
			{
				response.error("PROFILE_ERROR", "Error in retrieving user profile", ex);
				return;
			}
		}
		if (userlogin != null)
		{
			userlogin.password = "";
			response.writeObjectWithHeaderAndFooter(userlogin);
		}
		else
		{
			response.error("USER_NOT_LOGGED_IN", "There is no logged in user.");
		}
	}

	private void changePassword(Request req, Response res)
	{
		UserProfile user = req.getUser();
		if ( user.isGuest()) {
			res.error("USER_NOT_LOGGED_IN", "There is no logged in user.");
			return;
		}
		
		req.mapData.put("loginid", user.loginid);
		String newPassword = req.getString("newpassword", true, true, false);
		String password = req.getString("password", true, true, false);
		this.setNewPassword(req, res, password, newPassword, true);
	}

	private void resetPassword(Request req, Response res)
	{
		String loginId = req.getString("loginid", true, true, false);
		String newPassword = new PasswordGenerator().getPassword();
		if (!this.setNewPassword(req, res, null, newPassword, false));
		
		if ( null == passwordResetMailTemplateFile) return;
		
		//Send email with the new mail
		String passwordResetMailTemplate = 
			FileReaderUtil.toString(passwordResetMailTemplateFile);

		String resetMessage = passwordResetMailTemplate.replaceAll(
				"__login", loginId);
		resetMessage = resetMessage.replaceAll("__password", newPassword);
		
		String[] subjectAndBody = StringUtils.getStrings(resetMessage, '\n');
			
		SmtpMsg msg = null;
		if ( subjectAndBody.length == 1) {
			msg = new SmtpMsg(new String[]{loginId}, null, subjectAndBody[1]);
		} else {
			msg = new SmtpMsg(new String[]{loginId}, subjectAndBody[0], subjectAndBody[1]);
		}
		 
		QueueProcessingService.getInstance().addTask(new MailTask(msg));
	}

	private boolean setNewPassword(Request request, Response response, String password, String newPassword, boolean matchOldPassword)
	{
		String loginId = request.getString("loginid", true, true, false);

		UserLogin userlogin;
		try
		{
			userlogin = UserLoginTableExt.selectByLoginid(loginId);
		} 
		catch (SQLException ex)
		{
			response.error("PROFILE_ERROR", "Error in retrieving user profile", ex);
			return false;
		}

		if (userlogin == null) 
		{
			response.error("USER_NOT_FOUND", "This user id does not exist.");
			return false;
		}

		if (matchOldPassword)
		{
			String encodedPasswd = Hash.createHex(this.key, password);
			if (!encodedPasswd.equals(userlogin.password))
			{
				response.error("PASSWORD_MISMATCH", "Old password does not match.");
				return false;
			}
		}
		
		String newpasswordHash = Hash.createHex(this.key, newPassword);
		userlogin.password = newpasswordHash;
		try
		{
			UserLoginTableExt.update(userlogin, null);
		} 
		catch (SQLException ex)
		{
			response.error("UPDATE_FAILED", "Error in updating password.", ex);
			return false;
		}
		userlogin.password = "";
		response.writeObjectWithHeaderAndFooter(userlogin);
		return true;
	}
	
	private void activate(Request req, Response response) {
		String loginId = req.getString("loginid", true, true, false);
		UserLogin userlogin = null;
		
		String incomingToken = req.getString("token", true, true, false);
		String expectingToken = Hash.createHex(this.key, loginId);
		if ( !expectingToken.equals(incomingToken) ) {
			response.error("INVALID_TOKEN", "Token is not valid.");
			return;
		}
		
		try {
			userlogin = UserLoginTableExt.selectByLoginid(loginId);
			if (userlogin == null) {
				response.error("USER_NOT_FOUND", "This user id does not exist.");
				return;
			}
			UserLoginTableExt.activate(loginId, null);
			response.writeHeader();
			response.writeTextWithHeaderAndFooter("<header><menu><alink><ref>" + loginId + "</ref></alink></menu></header>");
			response.writeFooter();
		}  catch (SQLException ex) {
			response.error("SYSTEM_ERROR", "Please contact System administrator", ex);
		}
	}	
	
	@Override
	public void init() 
	{
    	Configuration conf = ServiceFactory.getInstance().getAppConfig();
		this.key = conf.get("passwordkey","Jac!@3n dancias@##@ng the fEER%r haha!!#");
		this.isLoginVerification = conf.getBoolean("login.email.verification", false);
		this.welcomeMailTemplateFile = conf.get("welcome.mail.template", "");
		if ( StringUtils.isEmpty(this.welcomeMailTemplateFile)) 
			this.welcomeMailTemplateFile = null;
		this.passwordResetMailTemplateFile = conf.get("passwordreset.mail.template", "");
		
		this.welcomeMailUrlPrefix = conf.get("welcome.mail.url.prefix", "service.html?service=user&action=activate");
		
		if ( StringUtils.isEmpty(this.passwordResetMailTemplateFile)) 
			this.passwordResetMailTemplateFile = null;

	}

	@Override
	public String getName() {
		return "user";
	}	
}

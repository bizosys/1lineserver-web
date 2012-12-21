package com.bizosys.onelineserver.servlet;

import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.bizosys.onelineserver.sendmail.MailSensor;
import com.bizosys.onelineserver.service.ServiceFactory;
import com.bizosys.onelineserver.sql.SqlSensor;
import com.bizosys.onelineserver.user.UserProfileSensor;
import com.oneline.util.Configuration;
import com.oneline.util.StringUtils;
import com.oneline.web.sensor.Sensor;

public class OnelineServlet extends AbstractOnelineServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void init(ServletConfig config) throws ServletException 
	{
		super.init(config);
		
		LOG.info("OnelineServlet booting services initializing.");

		Sensor user = null;
		Sensor sql = null;
		
		if ( ServiceFactory.getInstance().getAppConfig().get("services.to.start").contains("sql")) {

			user = new UserProfileSensor();
			this.setupSensor(user, user.getName());

			sql = new SqlSensor();
			this.setupSensor(sql, sql.getName());
			
		}
		
		Sensor mail = new MailSensor();
		this.setupSensor(mail, mail.getName());
		
		Configuration conf = ServiceFactory.getInstance().getAppConfig();
		String restServicesLine = conf.get("rest.services");
		LOG.info("Rest Services : " + restServicesLine);
		
		ClassLoader classLoader = OnelineServlet.class.getClassLoader();
		
		if ( ! StringUtils.isEmpty(restServicesLine)) {
			List<String> restClazzes = StringUtils.fastSplit(restServicesLine, ',');
			if ( null != restClazzes) {
				try {
					for (String restClazz : restClazzes) {
						LOG.info("Initiating : " + restClazz);
						Class aClass = classLoader.loadClass(restClazz);
						Sensor aSensor = (Sensor) aClass.newInstance(); 
						this.setupSensor(aSensor, aSensor.getName());
					}
				} catch (RuntimeException e) {
			        e.printStackTrace(System.out);
			        System.exit(1);
				} catch (Exception e) {
			        e.printStackTrace(System.out);
			        System.exit(1);
			    }
			}
		}
		
		LOG.info("OnelineServlet Initialized.");
	}
}

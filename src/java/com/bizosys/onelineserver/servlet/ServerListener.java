package com.bizosys.onelineserver.servlet;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.service.Configuration;
import com.bizosys.onelineserver.service.ServiceFactory;

public class ServerListener
{
	private final static Logger LOG = Logger.getLogger(ServerListener.class);
	
    public void startup()
    {
    	LOG.info("> Starting Oneline Services");
		this.initServices();
    }

    private void initServices()
    {
	      Configuration conf = new Configuration();
	      ServiceFactory serviceFactory = ServiceFactory.getInstance();
	      serviceFactory.serviceStart(conf);
    }
    
    /**
     * Initiate all services and then makes an execution.
     * 
     */
    public static void main(String[] args) throws Exception {
		ServerListener listener = new ServerListener();
		listener.startup();
    }      
}
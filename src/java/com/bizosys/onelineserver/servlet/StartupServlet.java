package com.bizosys.onelineserver.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class StartupServlet extends HttpServlet 
{
    private static final long serialVersionUID = -1212897465095562726L;

    @Override
    public void init(ServletConfig arg0) throws ServletException
    {
		super.init(arg0);
		this.initServer();
    }
    
    private void initServer()
    {
		ServerListener listener = new ServerListener();
		listener.startup();
    }

}

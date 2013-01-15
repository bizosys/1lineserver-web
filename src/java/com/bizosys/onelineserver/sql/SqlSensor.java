package com.bizosys.onelineserver.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.bizosys.onelineserver.model.AppConfig;
import com.bizosys.onelineserver.model.AppConfigTableExt;
import com.oneline.dao.ReadBase;
import com.oneline.dao.ReadXml;
import com.oneline.dao.WriteBase;
import com.oneline.util.StringUtils;
import com.oneline.web.sensor.InvalidRequestException;
import com.oneline.web.sensor.Request;
import com.oneline.web.sensor.Response;
import com.oneline.web.sensor.Sensor;

public class SqlSensor implements Sensor 
{
	private static final Class<Object> OBJECT_CLAZZ = Object.class;
	private static final String QUERY_CONFIGTYPE = "Q";

	private final static Logger LOG = Logger.getLogger(SqlSensor.class);
	private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
	private final static java.util.logging.Logger javaLog = java.util.logging.Logger.getLogger("com.bizosys.onelineserver.sql.SqlSensor");

	private Map<String, AppConfig> queryM;
	
	@Override
	public void init() 
	{
    	this.refreshQueries();    	
	}

	@Override
	public void processRequest(Request request, Response response)
	{
		String action = request.action;

		try
		{
			if ("select".equals(action))
			{
				this.selectQuery(request, response);
			
			}
			else if ("multiselect".equals(action))
			{
				this.multiSelectQuery(request, response);
			
			}
			else if ("insert".equals(action)) {
				this.cudQuery(request, response, true);
				
			} else if ("delete".equals(action)) {
				this.cudQuery(request, response, true);

			} else if ("update".equals(action)) {
				this.cudQuery(request, response, false);

			} else if ("refresh".equals(action)) {
				if (this.refreshQueries()) {
					response.writeTextWithHeaderAndFooter("OK");
				} else {
					response.error("REFRESH_FAILED", "Unable to refresh queries");
				}
			
			} else {
				LOG.warn("Invalid Request - " + request.toString());
				throw new InvalidRequestException("INVALID_OPERATION");
			}
		}
		
		catch (InvalidRequestException ire) {
			response.error("INVALID_REQUEST", "Expected information not found.");
		}
		catch ( SQLException sqlEx)
		{
			if ( sqlEx.getMessage().toLowerCase().contains("duplicate")) {
				response.error("DATA_ISSUES", "A duplicate entry is detected.");
			} else {
				response.error("DATA_ISSUES", sqlEx.getMessage());
			}
		} 
		
		catch (Exception ex)
		{
			LOG.fatal(request.toString(), ex);
			response.error("SYSTEM_ERROR", "Please report to System Administrator");
		} 
	}

	@SuppressWarnings("unchecked")
	private void selectQuery(Request request, Response response) throws SQLException
	{
		javaLog.warning("Proccessing select query request");
		LOG.debug("Proccessing select query request");
		String queryId = request.getString("queryid", true, false, false);
		LOG.debug("queryid:" + queryId);
		String docName = request.getString("docname", false, false, true);
		if ( StringUtils.isEmpty(docName)) docName = "document";
		
		String formatType = request.getString("format", false, false, true);
		if ( StringUtils.isEmpty(formatType)) formatType = "xml";
		
		LOG.debug("QueryId=" + queryId + ", No .Of queries:" + this.queryM.size());
		
		AppConfig queryObject = this.queryM.get(queryId);
		
		if (queryObject == null) {
			refreshQueries();
			queryObject = this.queryM.get(queryId);
		}
		
		if (queryObject == null)
		{
			response.error("QUERY_NOT_FOUND", "Invalid Configuration");
			return;
		}
		
		String loginId = ( null == request.getUser()) ? "GUEST" : request.getUser().loginid;
		String query = queryObject.body;
		int queryLenBefore = query.length(); 
		String replacedQuery = query.replace("__userid", " ? ");
		int queryLenAfter = replacedQuery.length();
		int totalReplacements =  (queryLenBefore - queryLenAfter)/ 5;
		
		// temporarily security has been disabled
		/*if(totalReplacements == 0)
		{
			LOG.debug("SECURITY_COMPROMISED: Query Scope not limited to User");
			response.error("SECURITY_COMPROMISED", "Query Scope not limited to User");
			return;
		}*/
		
		/**
		 * If there is only one relacement, add this to beginning
		 * Helps on making prepared statements
		 * We can not pass it inside parameters due to security risk.
		 */
		List<Object> paramL = request.getList("params", false);
		if ( totalReplacements == 1 ) {
			if ( Collections.EMPTY_LIST == paramL || null == paramL) {  
				paramL = new ArrayList<Object>(1);
			} 
			
			paramL.add(0, loginId);
			query = replacedQuery;

			if ( DEBUG_ENABLED ) {
				LOG.debug("paramL:" + paramL.size() + " , loginId=" + loginId);
				String params = "Params \r\n";
				for (Object param : paramL) {
					params = params + ">>>" + param + "\r\n";
				}
				LOG.debug(params);
			}
		} else {
			//Multiple instanes of __userid. So just replace. No prepared statement.
			query = query.replace("__userid", loginId);
		}
		
		if ( DEBUG_ENABLED ) LOG.debug("Query:" + query);
		
		ReadBase<Object> reader = null;
		
		if ( formatType.equals("xml")) {
			reader = createXmlReader(response, docName);
		} else {
			response.error("UNKNOWN_FORMAT", "Data format is not found. xml,json formats are supported.");
			return;
		}
			
		response.writeHeader();
		reader.execute(query, paramL);
		response.writeFooter();
		
	}
	
	private ReadBase createXmlReader(Response response, String docName) {
		ReadXml<Object> reader = new ReadXml<Object>(response.getWriter(), OBJECT_CLAZZ);
		reader.docName = docName;
		return reader;
	}

	private synchronized boolean refreshQueries()
	{
		if (this.queryM == null) this.queryM = new HashMap<String, AppConfig>();
		else this.queryM.clear();
		LOG.debug("Refreshing queries");
		try 
		{
			javaLog.warning("Refreshing queries");
			List<AppConfig> queryL = AppConfigTableExt.selectByConfigtype(QUERY_CONFIGTYPE);
			for (AppConfig query : queryL)
			{
				this.queryM.put(query.title, query);
			}
			LOG.debug("Refreshing queries done");
			javaLog.warning("Refreshing queries done");
			return true;
		} 
		catch (SQLException e) 
		{
			LOG.error("Error in getting queries.", e);
			javaLog.warning("Error in getting queries." + e.getMessage());
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void cudQuery(Request request, Response response, boolean userIdAtFirst) throws SQLException
	{
		if ( null == request.getUser()) {
			LOG.info("Guest is not authorized to perform this operation.");
			response.error("NOT_AUTHORIZED", "Please Login.");
			return;
		}
		
		if ( request.getUser().isGuest()) {
			LOG.info("Guest is not authorized to perform this operation.");
			response.error("NOT_AUTHORIZED", "Please Login.");
			return;
		}
		
		String loginId = request.getUser().loginid;
		String queryId = request.getString("queryid", true, false, false);
		AppConfig queryObject = this.queryM.get(queryId);
		if (queryObject == null) {
			refreshQueries();
			queryObject = this.queryM.get(queryId);
		}
		if (queryObject == null) {
			response.error("QUERY_NOT_FOUND", "Invalid Configuration");
			return;
		}
		
		String query = queryObject.body;
		int queryLenBefore = query.length(); 
		String replacedQuery = query.replace("__userid", " ? ");
		int queryLenAfter = replacedQuery.length();
		int totalReplacements =  (queryLenBefore - queryLenAfter)/ 5;
		
/*		int queryLenBeforeRole = query.length(); 
		String replacedQueryRole = query.replace("__role", " ? ");
		int queryLenAfterRole = replacedQueryRole.length();
		int totalReplacementsRole =  (queryLenBeforeRole - queryLenAfterRole)/ 3;*/
		
		/*if ( totalReplacements == 0 ) {
			response.error("SECURITY_COMPROMISED", "Query Scope not limited to User");
			return;
		}*/
		
		
		// temporarily security check has been disabled
	  /* if ( totalReplacements == 0  || totalReplacementsRole == 0) {
			response.error("SECURITY_COMPROMISED", "Query Scope not limited to User");
			return;
		}
*/
		/**
		 * If there is only one relacement, add this to beginning
		 * Helps on making prepared statements
		 * We can not pass it inside parameters due to security risk.
		 */
		List<Object> paramL = request.getList("params", false);
		if ( totalReplacements == 1 ) {
			if ( Collections.EMPTY_LIST == paramL || null == paramL) {  
				paramL = new ArrayList<Object>(1);
			} 
			
			if ( userIdAtFirst) paramL.add(0, loginId);
			else paramL.add(loginId);
			
			query = replacedQuery;

			if ( DEBUG_ENABLED ) {
				LOG.debug("paramL:" + paramL.size() + " , loginId=" + loginId);
				String params = "Params \r\n";
				for (Object param : paramL) {
					params = params + ">>>" + param + "\r\n";
				}
				LOG.debug(params);
			}
		} else {
			//Multiple instanes of __userid. So just replace. No prepared statement.
			query = query.replace("__userid", loginId);
		}
		if ( DEBUG_ENABLED ) LOG.debug("Query:" + query);
		
		WriteBase writer = new WriteBase();
		int recordId = writer.insert(query, paramL);;
		response.writeTextWithHeaderAndFooter("<records>" + recordId + "</records>");
	}	
	
	private void multiSelectQuery(Request request, Response response) throws SQLException
	{
		String mergedQuery = request.getString("QUERY_SEQUENCES",true,true,false);
		List<String> sqlIds = StringUtils.fastSplit(mergedQuery, ',');
		
		List<String> sqls = new ArrayList<String>(sqlIds.size());
		for (String sqlId : sqlIds) {
			sqls.add(this.queryM.get(sqlId).body);
		}
		List<Object> paramL = request.getList("params", false);
		MergedRow.prepareReport(response.getWriter(), sqls, paramL);
	}
	
	
	@Override
	public String getName() {
		return "sql";
	}
}

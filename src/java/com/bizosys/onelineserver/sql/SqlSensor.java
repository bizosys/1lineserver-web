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
			if ("select".equals(action))  {
				this.selectQuery(request, response);
			}

			else if ("multi.select".equals(action)) {
				this.multiSelectQuery(request, response);
			
			} else if ("insert".equals(action)) {
				this.insert(request, response);
				
			} else if ("multi.insert".equals(action)) {
				this.multiInsert(request, response);

			} else if ("chained.cud".equals(action)) {
				this.chainedCUDStatements(request, response);

			} else if ("update".equals(action)) {
				this.deleteAndUpdate(request, response);

			} else if ("delete".equals(action)) {
				this.deleteAndUpdate(request, response);

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
		if ( DEBUG_ENABLED ) LOG.debug("Proccessing select query request");
		
		String queryId = request.getString("queryid", true, false, false);
		if ( DEBUG_ENABLED ) LOG.debug("queryid:" + queryId);
		
		String docName = request.getString("docname", false, false, true);
		if ( StringUtils.isEmpty(docName)) docName = "document";
		
		String formatType = request.getString("format", false, false, true);
		if ( StringUtils.isEmpty(formatType)) formatType = "xml";
		
		if ( DEBUG_ENABLED ) LOG.debug("QueryId=" + queryId + ", No .Of queries:" + this.queryM.size());
		
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
		List<Object> paramL = request.getList("params", false);


		
		//Multiple instanes of __userid. So just replace. No prepared statement.
		query = query.replace("__userid", loginId);
		
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
			List<AppConfig> queryL = AppConfigTableExt.selectByConfigtype(QUERY_CONFIGTYPE);
			for (AppConfig query : queryL)
			{
				this.queryM.put(query.title, query);
			}
			LOG.debug("Refreshing queries done");
			return true;
		} 
		catch (SQLException e) 
		{
			LOG.error("Error in getting queries.", e);
			return false;
		}
	}
	
	private void multiSelectQuery(Request request, Response response) throws SQLException
	{
		String mergedQuery = request.getString("QUERY_SEQUENCES",true,true,false);
		List<String> sqlIds = StringUtils.fastSplit(mergedQuery, ',');
		
		List<String> sqls = new ArrayList<String>(sqlIds.size());
		for (String sqlId : sqlIds) {
			sqls.add(this.queryM.get(sqlId).body);
		}
		@SuppressWarnings("unchecked")
		List<Object> paramL = request.getList("params", false);
		MergedRow.prepareReport(response.getWriter(), sqls, paramL);
	}

	
	@SuppressWarnings("unchecked")
	private void insert(Request request, Response response) throws SQLException
	{
		String loginId = isLoggedIn(request, response);
		if ( null == loginId) return;
		
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
		List<Object> paramL = request.getList("params", false);
		if ( Collections.EMPTY_LIST == paramL || null == paramL) {  
			paramL = new ArrayList<Object>(1);
		}
		
		makeQuerySecure(response, query, loginId, paramL, false);
		if ( DEBUG_ENABLED ) LOG.debug("Secured Query:" + query);
		
		WriteBase writer = new WriteBase();
		int recordId = writer.insert(query, paramL);
		
		response.writeTextWithHeaderAndFooter("<records>" + recordId + "</records>");
	}
	
	@SuppressWarnings("unchecked")
	private void deleteAndUpdate(Request request, Response response) throws SQLException
	{
		String loginId = isLoggedIn(request, response);
		if ( null == loginId) return;
		
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
		List<Object> paramL = request.getList("params", false);
		if ( Collections.EMPTY_LIST == paramL || null == paramL) {  
			paramL = new ArrayList<Object>(1);
		}
		
		makeQuerySecure(response, query, loginId, paramL, false);
		if ( DEBUG_ENABLED ) LOG.debug("Secured Query:" + query);
		
		WriteBase writer = new WriteBase();
		int recordsTouched = writer.execute(query, paramL);
		
		response.writeTextWithHeaderAndFooter("<records>" + recordsTouched + "</records>");
	}	


	private void multiInsert(Request request, Response response) throws SQLException
	{
		String loginId = isLoggedIn(request, response);
		if ( null == loginId) return;

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
		int rowCount = request.getInteger("rows", true);
		int colCount = request.getInteger("cols", true);
		query = query.replace("__userid", loginId);
		
		List<Object> paramL = new ArrayList<Object>(colCount);
		
		StringBuilder insertedRecords = new StringBuilder();

		WriteBase writer = new WriteBase();
		
		StringBuilder localLog = new StringBuilder(4096);
		try {
			localLog.append ("Query : " + query);
			writer.beginTransaction();
			
			for(int rowIndex = 1; rowIndex <= rowCount; rowIndex++ ) {
				String key = "c" + rowIndex + "-" ;
				for(int colIndex = 1; colIndex <= colCount; colIndex++ ) {
					
					localLog.append('\n').append(key).append(colIndex).append('=');

					String val = request.getString(key + colIndex, true, true, true);
					paramL.add( val );

					localLog.append(val);
					
				}
				int recordId = writer.insert(query, paramL);
				localLog.append("Record Insered");
				paramL.clear();
				if ( insertedRecords.length() > 0 ) insertedRecords.append(',');
				insertedRecords.append(recordId);
			}
			writer.commitTransaction();
			localLog.append("Transaction Commited");
			writer = null;
						
		} catch (Exception ex) {
			LOG.fatal(localLog, ex);
			ex.printStackTrace(System.out);
			throw new SQLException(ex);
		} finally {
			if ( null != writer) {
				System.out.println("Rolling back Transactions: ");
				try { 
					writer.rollbackTransaction(); 
				} catch(Exception ex) {
					LOG.fatal(ex);
				}
			}
		}
		
		response.writeTextWithHeaderAndFooter("<records>" + insertedRecords.toString() + "</records>");
	}	
	
	
	private void chainedCUDStatements(Request request, Response response) throws SQLException
	{
		String loginId = isLoggedIn(request, response);
		if ( null == loginId) return;

		int totalQueries = request.getInteger("queries", true);
		
		List<String> queries = new ArrayList<String>();
		List<String> crudL = new ArrayList<String>();
		List<List<Object>> queryParams = new ArrayList<List<Object>>();
		
		StringBuilder insertedRecords = new StringBuilder();
		StringBuilder localLog = new StringBuilder();
		
		WriteBase writer = null;
		try {
			for ( int i=1; i <= totalQueries; i++) {
				String queryId = request.getString(i + "queryid" , true, false, false);
				int paramsT = request.getInteger(i + "paramsT" , true);
				String crud = request.getString(i + "sql" , true, true, false);
				
				AppConfig queryObject = this.queryM.get(queryId);
				if (queryObject == null) {
					refreshQueries();
					queryObject = this.queryM.get(queryId);
				}

				if (queryObject == null) {
					response.error("QUERY_NOT_FOUND", "Invalid Configuration");
					return;
				}
				
				List<Object> paramsL = new ArrayList<Object>(paramsT);
				for ( int p=1; p <= paramsT; p++) {
					String aParam = request.getString(i + "params" + p, false, false, true);
					paramsL.add(aParam);
				}
				
				String query = makeQuerySecure(response, queryObject.body, loginId, paramsL, false);
				
				queries.add(query);
				queryParams.add(paramsL);
				crudL.add(crud);
			}
			
			writer = new WriteBase();
			writer.beginTransaction();
			for (int querySeq=0; querySeq<totalQueries; querySeq++) {
				String query = queries.get(querySeq);
				String crud = crudL.get(querySeq);
				localLog.append ("Query : " + query);
				int recordId = 0; 
				if ( crud.equals("insert")) recordId = writer.insert(query, queryParams.get(querySeq));
				else recordId = writer.execute(query, queryParams.get(querySeq));
				
				if ( insertedRecords.length() > 0 ) insertedRecords.append(',');
				insertedRecords.append(recordId);				
				localLog.append("Record Insered");
			}
			writer.commitTransaction();
			localLog.append("Transaction Commited");
			writer = null;

		} catch (Exception ex) {
			LOG.fatal(localLog, ex);
			ex.printStackTrace(System.out);
			throw new SQLException(ex);
		} finally {
			if ( null != writer) {
				LOG.debug("Rolling back Transactions: ");
				try { 
					writer.rollbackTransaction(); 
				} catch(Exception ex) {
					LOG.fatal(ex);
				}
			}
		}
		
		response.writeTextWithHeaderAndFooter("<records>" + insertedRecords.toString() + "</records>");
	}		
	
	private String isLoggedIn(Request request, Response response) {
		if ( null == request.getUser()) {
			LOG.info("Guest is not authorized to perform this operation.");
			response.error("NOT_AUTHORIZED", "Please Login.");
			return null;
		}
		
		if ( request.getUser().isGuest()) {
			LOG.info("Guest is not authorized to perform this operation.");
			response.error("NOT_AUTHORIZED", "Please Login.");
			return null;
		}
		
		String loginId = request.getUser().loginid;
		return loginId;
	}	
	
	private String makeQuerySecure(Response response, String  query, String loginId, List<Object> paramL, boolean allowUserScope) throws SQLException {
		
		int queryLenBefore = query.length(); 
		String replacedQuery = query.replace("__userid", " ? ");
		int queryLenAfter = replacedQuery.length();
		int totalReplacements =  (queryLenBefore - queryLenAfter)/ 5;
		
		if(totalReplacements == 0 && allowUserScope)
		{
			LOG.debug("SECURITY_COMPROMISED: Query Scope not limited to User");
			response.error("SECURITY_COMPROMISED", "Query Scope not limited to User");
			throw new SQLException(query);
		}
		
		/**
		 * If there is only one relacement, add this to beginning
		 * Helps on making prepared statements
		 * We can not pass it inside parameters due to security risk.
		 */
		if ( totalReplacements == 1 ) {
			paramL.add(0, loginId);
			query = replacedQuery;
		} else {
			query = query.replace("__userid", loginId);
		}
		
		if ( DEBUG_ENABLED ) {
			LOG.debug("paramL:" + paramL.size() + " , loginId=" + loginId);
			String params = "Params \r\n";
			for (Object param : paramL) {
				params = params + ">>>" + param + "\r\n";
			}
			LOG.debug(params);
		}
			
		return query;
	}
	
		
	
	@Override
	public String getName() {
		return "sql";
	}
}

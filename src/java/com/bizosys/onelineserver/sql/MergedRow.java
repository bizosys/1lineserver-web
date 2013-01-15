package com.bizosys.onelineserver.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oneline.dao.PoolFactory;
import com.oneline.util.FileReaderUtil;

public class MergedRow {
	
	public static void prepareReport(PrintWriter out, List<String> sqls, List<Object> paramL) throws SQLException {
		
		List<ReadKV> dbReadersList = new ArrayList<ReadKV>();
		
		try {
			for (String aSql : sqls) {
				ReadKV reader = new ReadKV();
				System.out.println(aSql);
				reader.execute(aSql, paramL);	
				dbReadersList.add(reader);
			}
		} catch (SQLException ex) {
			ex.printStackTrace(System.out);
			throw ex;
		}

		List<String> sortedUniqueIds = new ArrayList<String>();
		Set<String> uniqueIds = new HashSet<String>();
		for (ReadKV source : dbReadersList) {
			for (String aId : source.sortedIds) {
				if ( uniqueIds.contains(aId)) continue;
				sortedUniqueIds.add(aId);
			}
			uniqueIds.addAll(source.sortedIds);
		}
		
		out.write("<table>");
		int colI = 0;
		int sourceI = 0;
		StringBuilder sb = new StringBuilder(100);
		for (String uniqueId : sortedUniqueIds) {
			out.append("<row>");
			out.append("<id>").append(uniqueId).append("</id>");

			//All Sources
			sourceI = 0;
			for ( ReadKV source : dbReadersList) {
				colI = 0;
				
				if ( source.foundRecords.containsKey(uniqueId)) {
					for (String aVal : source.foundRecords.get(uniqueId)) {
						sb.append('c').append(sourceI).append('-').append(colI);
						String colName = sb.toString();
						sb.delete(0, 99);
						colI++;

						out.append("<" + colName + ">").append(aVal).append( "</" + colName + ">");

					}
				} else {

					sb.append('c').append(sourceI).append('-').append(colI);
					String colName = sb.toString();
					sb.delete(0, 99);
					colI++;

					out.append("<" + colName + ">0</" + colName + ">");
				}
				
				sourceI++;
			}
			
			out.append("</row>\n");
		}
		out.write("</table>");
	}	
	
	public static void main(String[] args) throws Exception {
		
		PoolFactory.getInstance().setup(FileReaderUtil.toString("jdbc.conf"));
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		
		List<String> sqlQueries = new ArrayList<String>();
		//sqlQueries.put("walkinHourly1",);		
		//sqlQueries.put("walkinHourly2","");		
		//sqlQueries.put("walkinHourly3","");
		//sqlQueries.put("walkinHourly4","");

		sqlQueries.add("select HOUR(walkin_date) as walkinhour, COALESCE(count(id),0) as result from walkins where DATE(walkin_date) = ? group by HOUR(walkin_date) order by HOUR(walkin_date) desc");		
		sqlQueries.add("select HOUR(walkin_date) as walkinhour, COALESCE(count(id),0) as result from walkins where DATE(walkin_date) = ? and walkin_gender = 'male' group by HOUR(walkin_date) ");		
		sqlQueries.add("select HOUR(walkin_date) as walkinhour, COALESCE(count(id),0) as result from walkins where DATE(walkin_date) = ? and walkin_gender = 'female' group by HOUR(walkin_date) ");		
		sqlQueries.add("select HOUR(walkin_date) as walkinhour, sum(accompanied_males) as aMales, sum(accompanied_females) as aFemales from walkins where DATE(walkin_date) = ? and walkin_gender = 'male' group by HOUR(walkin_date) ");		
		
		List<Object> paramL = new ArrayList<Object>();
		paramL.add("2013-01-14");
		prepareReport(out, sqlQueries, paramL);
		
		System.out.println(writer.toString());
		
	}
	
}

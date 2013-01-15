package com.bizosys.onelineserver.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.oneline.dao.ReadBase;
import com.oneline.util.StringUtils;

//import org.apache.log4j.Logger;

public class ReadKV extends ReadBase<Object> {
	
	public Multimap<String, String> foundRecords = ArrayListMultimap.create();
	public List<String> sortedIds = new ArrayList<String>();
	
	protected List<Object> populate() throws SQLException {
		if ( null == this.rs) {
			throw new SQLException("Rs is not initialized.");
		}
		
		String valStr = null;
		int total = rs.getMetaData().getColumnCount();
		while (this.rs.next()) {
			String id = rs.getObject(1).toString();
			sortedIds.add(id);
			if ( null == id) continue;
			
			for ( int i=0; i<total -1; i++) {
				Object val =  rs.getObject(i+2);
				valStr = ( null == val) ? StringUtils.Empty : val.toString();
				foundRecords.put(id, valStr);
			}
		}
		return null;
	}

	@Override
	protected Object getFirstRow() throws SQLException {
		return null;
	}
}
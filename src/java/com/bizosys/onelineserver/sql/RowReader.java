package com.bizosys.onelineserver.sql;

import java.io.Closeable;
import java.io.IOException;

public interface RowReader extends Closeable {
	String[] readNext() throws IOException;
}

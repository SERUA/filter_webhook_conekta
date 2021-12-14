package com.soa.bonitasoft;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class DBConnect {
	public Logger logger = Logger.getLogger(DBConnect.class.getName());
	
	/* The data source. /

	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 * @throws NullPointerException the null pointer exception
	 */
	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 * @throws NullPointerException the null pointer exception
	 */
	public final Connection getConnection() throws Exception {
		Context initContext = new InitialContext();
		//BDM
		DataSource dataSource = (DataSource) initContext.lookup("java:/comp/env/NotManagedBizDataDS");
		return dataSource.getConnection();
	}
	
	public final Connection getConnectionBonita() throws Exception {
		Context initContext = new InitialContext();
		//Bonita instancias etc...
		DataSource dataSource = (DataSource) initContext.lookup("java:/comp/env/bonitaSequenceManagerDS");
		return dataSource.getConnection();
	}
	
	public void closeObj(Connection con, ResultSet rs, PreparedStatement pstm) {
		String exceptionDetails="";
		StringWriter sw = new StringWriter();
		try {
            if (null != rs) {
                rs.close();
            }
        } catch (Exception ers) {
            sw = new StringWriter();
            ers.printStackTrace(new PrintWriter(sw));
            exceptionDetails = sw.toString();
            logger.severe("DBConnect: exception " + ers.getMessage() + " at " + exceptionDetails);
        }
		try {
            if (null != pstm) {
                pstm.close();
            }
        } catch (Exception epstm) {
            sw = new StringWriter();
            epstm.printStackTrace(new PrintWriter(sw));
            exceptionDetails = sw.toString();
            logger.severe("DBConnect: exception " + epstm.getMessage() + " at " + exceptionDetails);
        }
        try {
            if (null != con) {
                con.close();
            }
        } catch (Exception econ) {
            sw = new StringWriter();
            econ.printStackTrace(new PrintWriter(sw));
            exceptionDetails = sw.toString();
            logger.severe("DBConnect: exception " + econ.getMessage() + " at " + exceptionDetails);
        }
        
	}
}

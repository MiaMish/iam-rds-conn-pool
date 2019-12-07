package com.github.miamish.connectors.rds.iam;

import java.sql.SQLException;

import org.apache.tomcat.jdbc.pool.ConnectionPool;

public class IamAuthenticationDataSource extends org.apache.tomcat.jdbc.pool.DataSource {

    @Override
    public ConnectionPool createPool() throws SQLException {
        return pool != null ? pool : new IamAuthenticationConnPool(poolProperties);
    }

}
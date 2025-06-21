package com.ibmzpot.wcaz.jdbctest;

import jakarta.annotation.Resource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Path("/test")
public class TestResource {

    @Resource(name = "jdbc/defaultDataSource")
    private DataSource dataSource;

    @GET
    @Path("/{sqlCount}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDb2Timestamp(@PathParam("sqlCount") int sqlCount) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String responseMessage = "";
        String sql = "SELECT CURRENT TIMESTAMP FROM SYSIBM.SYSDUMMY1";
        int i = 0;

        try {
            // Fallback to manual JNDI lookup if @Resource injection fails
            if (dataSource == null) {
                responseMessage += "@Resource injection failed; attempting manual JNDI lookup.\n";
                InitialContext initialContext = new InitialContext();
                dataSource = (DataSource) initialContext.lookup("java:comp/env/jdbc/defaultDataSource");
                if (dataSource == null) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error: DataSource not found. Check server configuration.")
                            .build();
                } else {
                    responseMessage += "Manual JNDI lookup successful.\n";
                }
            } else {
                responseMessage += "@Resource injection successful.\n";
            }

            connection = dataSource.getConnection();

            if (connection != null) {
                responseMessage += "Successfully connected to DB2!\n";
                responseMessage += "Executing SQL...\n";
                preparedStatement = connection.prepareStatement(sql);
                // Execute the requested number of SQL statements
                while (i < sqlCount) {
                    resultSet = preparedStatement.executeQuery();
                    i++;
                }
                responseMessage += i + " SQL statements executed...\n";

                if (resultSet.next()) {
                    String currentTimestamp = resultSet.getString(1);
                    responseMessage += "Current DB2 Timestamp: " + currentTimestamp;
                } else {
                    responseMessage += "No results found for the sample query.";
                }
                return Response.ok(responseMessage).build();

            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Failed to get a connection from the DataSource.")
                        .build();
            }

        } catch (NamingException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("JNDI Lookup Error: " + e.getMessage())
                    .build();
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Database Error: " + e.getMessage())
                    .build();
        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
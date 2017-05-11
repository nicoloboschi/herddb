/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package org.herddb.ui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

@Path("api")
@Produces(MediaType.APPLICATION_JSON)
public class APIResource {

    @Context
    private UriInfo context;

    @Context
    private HttpServletRequest servletRequest;

    @Context
    private ServletContext servletContext;

    protected Connection getConnection() throws SQLException {
        DataSource ds = (DataSource) servletContext.getAttribute("datasource");
        return ds.getConnection();
    }

    @GET
    @Path("/test")
    public String test() {
        try (Connection con = getConnection()) {
            return "test! " + con;
        } catch (SQLException | IllegalArgumentException err) {
            return "Internal error: " + err;
        }
    }

    @GET
    @Path("/tablespaces")
    public List<List<Object>> tablespaces() {
        try (Connection con = getConnection();
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT * from systablespaces")) {
            List<List<Object>> result = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                row.add(rs.getObject("tablespace_name"));
                row.add(rs.getObject("uuid"));
                row.add(rs.getObject("leader"));
                row.add(rs.getObject("replica"));
                row.add(rs.getObject("expectedreplicacount"));
                row.add(rs.getObject("maxleaderinactivitytime"));
                result.add(row);
            }
            return result;
        } catch (SQLException | IllegalArgumentException err) {
            err.printStackTrace();
            throw new WebApplicationException(err);
        }
    }

    @GET
    @Path("/systables")
    public List<List<Object>> system() {
        try (Connection con = getConnection();
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT * from systables")) {
            List<List<Object>> result = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                row.add(rs.getObject("tablespace"));
                row.add(rs.getObject("table_name"));
                row.add(getBoolean(rs.getString("systemtable")));
                result.add(row);
            }
            return result;
        } catch (SQLException | IllegalArgumentException err) {
            err.printStackTrace();
            throw new WebApplicationException(err);
        }
    }

    @GET
    @Path("/tablespace")
    public Map<String, List<List<Object>>> tablespace(@QueryParam("ts") String ts) {
        Map<String, List<List<Object>>> result = new HashMap<>();

        try (Connection con = getConnection();
            PreparedStatement stats = con.prepareStatement("SELECT * from " + ts + ".systablestats");
            PreparedStatement tables = con.prepareStatement("SELECT * from " + ts + ".systables where systemtable=false");
            PreparedStatement repState = con.prepareStatement("SELECT * from " + ts + ".systablespacereplicastate where tablespace_name=?");
            PreparedStatement transactions = con.prepareStatement("SELECT * from " + ts + ".systransactions")) {
            stats.setString(1, ts);
            tables.setString(1, ts);
            repState.setString(1, ts);
            transactions.setString(1, ts);
            try (ResultSet rs = stats.executeQuery()) {
                List<List<Object>> statsResult = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    row.add(rs.getObject("tablespace"));
                    row.add(rs.getObject("table_name"));
                    row.add(rs.getObject("systemtable"));
                    row.add(rs.getObject("replica"));
                    row.add(rs.getObject("tablesize"));
                    row.add(rs.getObject("loadedpages"));
                    row.add(rs.getObject("loadedpagescount"));
                    row.add(rs.getObject("unloadedpagescount"));
                    row.add(rs.getObject("dirtypages"));
                    row.add(rs.getObject("dirtyrecords"));
                    row.add(rs.getObject("maxlogicalpagesize"));
                    row.add(rs.getObject("keysmemory"));
                    row.add(rs.getObject("buffersmemory"));
                    row.add(rs.getObject("dirtymemory"));
                    statsResult.add(row);
                }
                result.put("stats", statsResult);
            }
            try (ResultSet rs = tables.executeQuery()) {
                List<List<Object>> statsResult = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    row.add(rs.getObject("table_name"));
                    row.add(rs.getObject("systemtable"));
                    statsResult.add(row);
                }
                result.put("tables", statsResult);
            }
            try (ResultSet rs = repState.executeQuery()) {
                List<List<Object>> statsResult = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    row.add(rs.getObject("uuid"));
                    row.add(rs.getObject("nodeid"));
                    row.add(rs.getObject("mode"));
                    row.add(rs.getObject("timestamp"));
                    row.add(rs.getObject("maxleaderinactivitytime"));
                    row.add(rs.getObject("inactivitytime"));
                    statsResult.add(row);
                }
                result.put("replication", statsResult);
            }
            try (ResultSet rs = transactions.executeQuery()) {
                List<List<Object>> statsResult = new ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new ArrayList<>();
                    row.add(rs.getObject("txid"));
                    row.add(rs.getObject("creationtimestamp"));
                    statsResult.add(row);
                }
                result.put("transactions", statsResult);
            }
            return result;
        } catch (SQLException | IllegalArgumentException err) {
            err.printStackTrace();
            throw new WebApplicationException(err);
        }
    }

    @GET
    @Path("/nodes")
    public List<List<Object>> nodes() {
        try (Connection con = getConnection();
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT * from sysnodes")) {
            int count = rs.getMetaData().getColumnCount();
            List<Object> row = new ArrayList<>();
            List<List<Object>> result = new ArrayList<>();
            while (rs.next()) {
                row.add(rs.getObject("nodeid"));
                row.add(rs.getObject("address"));
                row.add(rs.getObject("ssl"));
                result.add(row);
            }
            return result;
        } catch (SQLException | IllegalArgumentException err) {
            err.printStackTrace();
            throw new WebApplicationException(err);
        }
    }

    @GET
    @Path("/query")
    public List<Map<String, Object>> generic(@QueryParam("q") String query) {
        try (Connection con = getConnection();
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery(query)) {
            int count = rs.getMetaData().getColumnCount();

            List<Map<String, Object>> res = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= count; i++) {
                    Map<String, Object> columnDef = new HashMap<>();
                    columnDef.put("value", rs.getObject(i));
                    columnDef.put("class", rs.getObject(i).getClass().getName());
                    row.put(rs.getMetaData().getColumnName(i), columnDef);
                }
                res.add(row);
            }
            return res;
        } catch (SQLException | IllegalArgumentException err) {
            err.printStackTrace();
            throw new WebApplicationException(err);
        }
    }

    private String getBoolean(String value) {
        if (value == null) {
            return "<div class='icon-container'><span class='ti-hand-point-down'></span><span class='icon-name'> </span></div>";
        } else {
            return "<div class='icon-container'><span class='ti-hand-point-down'></span><span class='icon-name'> </span></div>";
        }
    }

}

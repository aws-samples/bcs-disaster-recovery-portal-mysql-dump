// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

public class DbParameter {
    private String[] databases;
    private String username;
    private String passwordId;
    private String host;
    private int port;

    String getConnectionString() {
        return "jdbc:mysql://" + host + ":" + port + "?useSSL=false";
    }

    public String[] getDatabases() {
        return databases;
    }

    public void setDatabases(String[] databases) {
        this.databases = databases;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordId() {
        return passwordId;
    }

    public void setPasswordId(String passwordId) {
        this.passwordId = passwordId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

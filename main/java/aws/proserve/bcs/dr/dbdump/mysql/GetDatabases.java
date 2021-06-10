// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.lambda.StringsHandler;
import aws.proserve.bcs.dr.secret.SecretManager;
import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class GetDatabases implements StringsHandler<DbParameter> {

    @Override
    public String[] handleRequest(DbParameter parameter, Context context) {
        return MySqlComponent.build().getDatabases().getDatabases(parameter);
    }

    @Singleton
    static class Worker {
        private final Logger log = LoggerFactory.getLogger(getClass());

        private final SecretManager secretManager;

        @Inject
        Worker(SecretManager secretManager) {
            this.secretManager = secretManager;
        }

        String[] getDatabases(DbParameter parameter) {
            Connection connection = null;
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(parameter.getConnectionString(),
                        parameter.getUsername(),
                        secretManager.getSecret(parameter.getPasswordId()));
                final var statement = connection.createStatement();
                final var rs = statement.executeQuery("show databases");
                final var databases = new ArrayList<String>();
                while (rs.next()) {
                    databases.add(rs.getString(1));
                }
                return databases.toArray(new String[0]);
            } catch (ClassNotFoundException | SQLException e) {
                log.warn("Unable to get databases: " + parameter.getConnectionString(), e);
                return null;
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        log.warn("Unable to close connection.", e);
                    }
                }
            }
        }
    }
}


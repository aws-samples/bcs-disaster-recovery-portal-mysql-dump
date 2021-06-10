// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.dbdump.DbDumpConstants;
import aws.proserve.bcs.dr.lambda.StringsHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.project.Side;
import aws.proserve.bcs.dr.secret.SecretManager;
import aws.proserve.bcs.dr.secret.Secrets;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CallGetDatabases implements StringsHandler<CallGetDatabases.Request> {

    @Override
    public String[] handleRequest(Request request, Context context) {
        return MySqlComponent.build(request.getRegion(), request.getProjectId())
                .callGetDatabases()
                .call(request);
    }

    @Singleton
    static class Worker {
        private final Logger log = LoggerFactory.getLogger(getClass());

        private final ObjectMapper mapper;
        private final AWSLambda sourceLambda;
        private final SecretManager secretManager;

        @Inject
        Worker(ObjectMapper mapper,
               @Source AWSLambda sourceLambda,
               SecretManager secretManager) {
            this.mapper = mapper;
            this.sourceLambda = sourceLambda;
            this.secretManager = secretManager;
        }

        String[] call(Request request) {
            try {
                request.getDbParameter().setPasswordId(Secrets.idOfDb(request.getProjectId(), Side.source, request.getDbId()));
                final var input = mapper.writeValueAsString(request.getDbParameter());
                final var result = sourceLambda.invoke(new InvokeRequest()
                        .withFunctionName(DbDumpConstants.MYSQL_GET_DATABASES)
                        .withPayload(StandardCharsets.UTF_8.encode(input)));
                final var output = StandardCharsets.UTF_8.decode(result.getPayload()).toString();
                log.debug("GetDatabases ({}) region {}, output {}", result.getStatusCode(), request.getRegion(), output);

                return mapper.readValue(output, String[].class);
            } catch (IOException e) {
                log.warn("Unable to invoke " + DbDumpConstants.MYSQL_GET_DATABASES, e);
                return null;
            }
        }
    }

    static class Request {
        private String region;
        private String dbId;
        private String projectId;
        private DbParameter dbParameter;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getDbId() {
            return dbId;
        }

        public void setDbId(String dbId) {
            this.dbId = dbId;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public DbParameter getDbParameter() {
            return dbParameter;
        }

        public void setDbParameter(DbParameter dbParameter) {
            this.dbParameter = dbParameter;
        }
    }
}


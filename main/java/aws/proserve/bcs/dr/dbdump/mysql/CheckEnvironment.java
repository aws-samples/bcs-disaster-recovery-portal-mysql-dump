// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.dbdump.DbDumpConstants;
import aws.proserve.bcs.dr.lambda.BoolHandler;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.runtime.Context;

import javax.inject.Inject;
import javax.inject.Singleton;

public class CheckEnvironment implements BoolHandler<CheckEnvironment.Request> {

    @Override
    public boolean handleRequest(Request request, Context context) {
        return MySqlComponent.build(request.getRegion(), request.getProjectId())
                .checkEnvironment()
                .checkLambda();
    }

    @Singleton
    static class Worker {
        private final AWSLambda sourceLambda;

        @Inject
        Worker(@Source AWSLambda sourceLambda) {
            this.sourceLambda = sourceLambda;
        }

        boolean checkLambda() {
            try {
                sourceLambda.getFunction(new GetFunctionRequest()
                        .withFunctionName(DbDumpConstants.MYSQL_GET_DATABASES));
                return true;
            } catch (ResourceNotFoundException e) {
                return false;
            }
        }
    }

    static class Request {
        private String region;
        private String projectId;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }
    }
}

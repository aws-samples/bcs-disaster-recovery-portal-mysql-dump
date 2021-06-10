// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.dbdump.DbDumpConstants;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.lambda.VoidHandler;
import aws.proserve.bcs.dr.lambda.annotation.Default;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.lambda.util.StackUpdater;
import aws.proserve.bcs.dr.s3.S3Constants;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.Runtime;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.VpcConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

public class PrepareEnvironment implements VoidHandler<PrepareEnvironment.Request> {
    private static final String LAMBDA_ROLE = "DRPortal-DbDump-MySql-Lambda";

    @Override
    public void handleRequest(Request request, Context context) {
        MySqlComponent.build(request.getRegion(), request.getProjectId())
                .prepareEnvironment()
                .prepare(request);
    }

    @Singleton
    static class Worker extends BaseWorker {
        private final Logger log = LoggerFactory.getLogger(getClass());

        private final AmazonS3 s3;
        private final AmazonS3 sourceS3;
        private final AWSSimpleSystemsManagement ssm;
        private final AWSSimpleSystemsManagement sourceSsm;
        private final AmazonCloudFormation sourceCfn;
        private final AmazonIdentityManagement sourceIam;
        private final AWSLambda sourceLambda;
        private final CheckEnvironment.Worker checkEnvironment;

        @Inject
        Worker(@Default AmazonS3 s3,
               @Source AmazonS3 sourceS3,
               @Default AWSSimpleSystemsManagement ssm,
               @Source AWSSimpleSystemsManagement sourceSsm,
               @Source AmazonCloudFormation sourceCfn,
               @Source AmazonIdentityManagement sourceIam,
               @Source AWSLambda sourceLambda,
               CheckEnvironment.Worker checkEnvironment) {
            this.s3 = s3;
            this.sourceS3 = sourceS3;
            this.ssm = ssm;
            this.sourceSsm = sourceSsm;
            this.sourceCfn = sourceCfn;
            this.sourceIam = sourceIam;
            this.sourceLambda = sourceLambda;
            this.checkEnvironment = checkEnvironment;
        }

        void prepare(Request request) {
            log.info("Prepare environment at region {}", request.getRegion());
            deployBucket();
            createLambda();
            configureLambda(request);
        }

        private void deployBucket() {
            final var updater = new StackUpdater(sourceCfn, S3Constants.COMMON_BUCKET_STACK_NAME);
            if (updater.isValid()) {
                log.info("Stack [{}] already exists.", S3Constants.COMMON_BUCKET_STACK_NAME);
                return;
            }

            final var stream = s3.getObject(getBucket(ssm), S3Constants.COMMON_BUCKET_JSON).getObjectContent();
            final var body = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining(System.lineSeparator()));
            updater.update(body);
        }

        private void copyLambdaAsset() {
            final var source = s3.getObject(getBucket(ssm), S3Constants.LAMBDA_DBDUMP_MYSQL);
            sourceS3.putObject(getBucket(sourceSsm), S3Constants.LAMBDA_DBDUMP_MYSQL,
                    source.getObjectContent(), source.getObjectMetadata());
        }

        private void createLambda() {
            if (checkEnvironment.checkLambda()) {
                log.info("Lambda {} already exists.", DbDumpConstants.MYSQL_GET_DATABASES);
                return;
            }

            final var request = new ListRolesRequest();
            ListRolesResult result;
            Optional<Role> lambdaRole;
            do {
                result = sourceIam.listRoles(request);
                request.setMarker(result.getMarker());

                lambdaRole = result.getRoles().stream()
                        .filter(r -> r.getRoleName().startsWith(LAMBDA_ROLE))
                        .findFirst();
            } while (lambdaRole.isEmpty() && result.getMarker() != null);

            if (lambdaRole.isEmpty()) {
                throw new PortalException("Expected role is missing: " + LAMBDA_ROLE);
            }

            copyLambdaAsset();
            sourceLambda.createFunction(new CreateFunctionRequest()
                    .withFunctionName(DbDumpConstants.MYSQL_GET_DATABASES)
                    .withRuntime(Runtime.Java11)
                    .withHandler("aws.proserve.bcs.dr.dbdump.mysql.GetDatabases::handleRequest")
                    .withRole(lambdaRole.get().getArn())
                    .withMemorySize(1024)
                    .withTimeout(10 * 60)
                    .withCode(new FunctionCode()
                            .withS3Bucket(getBucket(sourceSsm))
                            .withS3Key(S3Constants.LAMBDA_DBDUMP_MYSQL)));
        }

        private void configureLambda(Request request) {
            sourceLambda.updateFunctionConfiguration(new UpdateFunctionConfigurationRequest()
                    .withFunctionName(DbDumpConstants.MYSQL_GET_DATABASES)
                    .withVpcConfig(new VpcConfig()
                            .withSubnetIds(request.getSubnetIds())
                            .withSecurityGroupIds(request.getSecurityGroupIds())));
        }
    }

    static class Request {
        private String region;
        private String projectId;
        private String[] subnetIds;
        private String[] securityGroupIds;

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

        public String[] getSubnetIds() {
            return subnetIds;
        }

        public void setSubnetIds(String[] subnetIds) {
            this.subnetIds = subnetIds;
        }

        public String[] getSecurityGroupIds() {
            return securityGroupIds;
        }

        public void setSecurityGroupIds(String[] securityGroupIds) {
            this.securityGroupIds = securityGroupIds;
        }
    }
}

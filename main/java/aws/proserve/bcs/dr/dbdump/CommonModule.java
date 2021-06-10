// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump;


import aws.proserve.bcs.dr.lambda.annotation.Default;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.secret.Credential;
import com.amazonaws.jmespath.ObjectMapperSingleton;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;

import javax.annotation.Nullable;
import javax.inject.Singleton;

@Module
@Singleton
public class CommonModule {

    @Provides
    @Default
    AmazonS3 s3() {
        return AmazonS3ClientBuilder.standard().enableForceGlobalBucketAccess().build();
    }

    @Provides
    @Source
    AmazonS3 sourceS3(@Nullable @Source String region, @Nullable Credential credential) {
        return AmazonS3ClientBuilder.standard().enableForceGlobalBucketAccess()
                .withRegion(region)
                .withCredentials(Credential.toProvider(credential))
                .build();
    }

    @Provides
    @Singleton
    ObjectMapper objectMapper() {
        return ObjectMapperSingleton.getObjectMapper();
    }

    @Provides
    @Singleton
    AWSSecretsManager secretsManager() {
        return AWSSecretsManagerClientBuilder.defaultClient();
    }

    /**
     * @apiNote SSM at the default region stores the values in the parameter store.
     */
    @Provides
    @Default
    AWSSimpleSystemsManagement ssm() {
        return AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }

    @Provides
    @Source
    AWSSimpleSystemsManagement sourceSsm(@Nullable @Source String region, @Nullable Credential credential) {
        return AWSSimpleSystemsManagementClientBuilder.standard()
                .withRegion(region)
                .withCredentials(Credential.toProvider(credential))
                .build();
    }

    @Provides
    @Source
    AmazonCloudFormation sourceCfn(@Nullable @Source String region, @Nullable Credential credential) {
        return AmazonCloudFormationClientBuilder.standard()
                .withRegion(region)
                .withCredentials(Credential.toProvider(credential))
                .build();
    }

    @Provides
    @Source
    AmazonIdentityManagement sourceIam(@Nullable @Source String region, @Nullable Credential credential) {
        return AmazonIdentityManagementClientBuilder.standard()
                .withRegion(region)
                .withCredentials(Credential.toProvider(credential))
                .build();
    }

    @Provides
    @Source
    AWSLambda sourceLambda(@Nullable @Source String region, @Nullable Credential credential) {
        return AWSLambdaClientBuilder.standard()
                .withRegion(region)
                .withCredentials(Credential.toProvider(credential))
                .build();
    }
}

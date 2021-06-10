// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.dbdump.CommonModule;
import aws.proserve.bcs.dr.dbdump.DbDumpConstants;
import aws.proserve.bcs.dr.lambda.annotation.Source;
import aws.proserve.bcs.dr.secret.Credential;
import aws.proserve.bcs.dr.secret.SecretManager;
import dagger.BindsInstance;
import dagger.Component;

import javax.annotation.Nullable;
import javax.inject.Singleton;

@Singleton
@Component(modules = CommonModule.class)
public interface MySqlComponent {

    static MySqlComponent build() {
        return DaggerMySqlComponent.builder().build();
    }

    static MySqlComponent build(String source, String projectId) {
        return DaggerMySqlComponent.builder()
                .sourceRegion(source)
                .credential(DaggerMySqlComponent.builder()
                        .build()
                        .secretManager()
                        .getCredentialByProject(projectId))
                .build();
    }

    SecretManager secretManager();

    CallGetDatabases.Worker callGetDatabases();

    DumpMySql.Worker dumpMySql();

    GetDatabases.Worker getDatabases();

    PrepareEnvironment.Worker prepareEnvironment();

    CheckEnvironment.Worker checkEnvironment();

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder sourceRegion(@Nullable @Source String region);

        @BindsInstance
        Builder credential(@Nullable Credential credential);

        MySqlComponent build();
    }
}

// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.dbdump.DbDumpConstants;
import aws.proserve.bcs.dr.exception.PortalException;
import aws.proserve.bcs.dr.lambda.StringHandler;
import aws.proserve.bcs.dr.lambda.annotation.Default;
import aws.proserve.bcs.dr.lambda.cmd.CommandExecutor;
import aws.proserve.bcs.dr.lambda.cmd.SysCommands;
import aws.proserve.bcs.dr.secret.SecretManager;
import aws.proserve.bcs.dr.util.Preconditions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class DumpMySql implements StringHandler<DbParameter> {

    public static void main(String[] args) {
        MySqlComponent.build().dumpMySql().dump(getRequest());
        System.exit(0);
    }

    private static DbParameter getRequest() {
        final var parameter = new DbParameter();
        parameter.setHost(env("host"));
        parameter.setPort(Integer.parseInt(env("port")));
        parameter.setUsername(env("username"));
        parameter.setPasswordId(env("password_id"));
        parameter.setDatabases(env("databases").split(","));
        return parameter;
    }

    private static String env(String key) {
        return Objects.requireNonNull(System.getenv(key), key + " cannot be null.");
    }

    @Override
    public String handleRequest(DbParameter parameter, Context context) {
        return MySqlComponent.build().dumpMySql().dump(parameter);
    }

    @Singleton
    static class Worker extends BaseWorker {
        private final Logger log = LoggerFactory.getLogger(getClass());

        private final AmazonS3 s3;
        private final AWSSimpleSystemsManagement ssm;
        private final SecretManager secretManager;
        private final GetDatabases.Worker getDatabases;

        @Inject
        Worker(@Default AmazonS3 s3,
               @Default AWSSimpleSystemsManagement ssm,
               SecretManager secretManager,
               GetDatabases.Worker getDatabases) {
            this.s3 = s3;
            this.ssm = ssm;
            this.secretManager = secretManager;
            this.getDatabases = getDatabases;
        }

        private void checkDisk() {
            try (final var executor = CommandExecutor.create("df")) {
                final var result = executor.execute(SysCommands.df().humanReadable());
                if (result.isSuccessful()) {
                    log.info("checkDisk is ok: {}", result.getOutput());
                } else {
                    log.warn("Unable to check disk: {}", result);
                    throw new PortalException("Unable to check disk: " + result);
                }
            }
        }

        private void checkVersion() {
            try (final var executor = CommandExecutor.create("MySqlDump")) {
                final var result = executor.execute(MySqlCommands.mysqldump().version());
                if (result.isSuccessful()) {
                    log.info("checkVersion is ok: mysqldump version is {}", result.getOutput());
                } else {
                    log.warn("Unable to check version of mysqldump: {}", result);
                    throw new PortalException("Unable to check version of mysqldump: " + result);
                }
            }
        }

        private void checkDatabases(DbParameter parameter) {
            final var set = Set.of(getDatabases.getDatabases(parameter));
            Preconditions.checkArgument(Stream.of(parameter.getDatabases()).allMatch(set::contains),
                    "Unable to find some databases: " + Arrays.toString(parameter.getDatabases()));
            log.info("checkDatabase is ok: queried {} databases.", set.size());
        }

        private File dumpToLocalDisk(DbParameter parameter) {
            try (final var executor = CommandExecutor.create("MySqlDump")) {
                final var file = File.createTempFile("drportal-dbdump-mysql-", ".sql", new File(DbDumpConstants.DBDUMP_FOLDER));
                final var result = executor.execute(MySqlCommands.mysqldump()
                        .user(parameter.getUsername())
                        .password(secretManager.getSecret(parameter.getPasswordId()))
                        .host(parameter.getHost())
                        .port(parameter.getPort())
                        .databases(parameter.getDatabases())
                        .resultFile(file.getAbsolutePath())
                        .events()
                        .routines()
                        .triggers()
                        .compress()
                        .orderByPrimary()
                        .singleTransaction());
                if (result.isSuccessful()) {
                    log.info("dumpToLocalDisk is ok, to {}", file);
                    return file;
                } else {
                    log.warn("Unable to dump mysql: {}", result);
                    throw new PortalException("Unable to dump mysql: " + result);
                }
            } catch (IOException e) {
                log.warn("Unable to run mysqldump", e);
                throw new PortalException("Unable to run mysqldump", e);
            }
        }

        private File compress(File file) {
            try (final var executor = CommandExecutor.create("Tar")) {
                final var target = File.createTempFile("drportal-dbdump-mysql-", ".tar.gz", new File(DbDumpConstants.DBDUMP_FOLDER));
                final var result = executor.execute(SysCommands.tar()
                        .compressFile(target.getAbsolutePath(), file.getAbsolutePath()));
                if (result.isSuccessful()) {
                    log.info("compress is ok, to {}", target);
                    return new File(target.getAbsolutePath());
                } else {
                    log.warn("Unable to compress: {}", result);
                    throw new PortalException("Unable to compress: " + result);
                }
            } catch (IOException e) {
                log.warn("Unable to compress", e);
                throw new PortalException("Unable to compress", e);
            }
        }

        private void copyToS3(File file) {
            s3.putObject(getBucket(ssm), DbDumpConstants.DBDUMP_FOLDER.substring(1) + "/" + file.getName(), file);
            log.info("copyToS3 is ok: {}", file.getName());
        }

        String dump(DbParameter parameter) {
            log.info("Dump databases: {}", String.join(", ", parameter.getDatabases()));

            checkDisk();
            checkVersion();
            checkDatabases(parameter);
            final var file = compress(dumpToLocalDisk(parameter));
            copyToS3(file);

            log.info("Dumped and compressed to file {}", file.getName());
            return file.getName();
        }
    }
}

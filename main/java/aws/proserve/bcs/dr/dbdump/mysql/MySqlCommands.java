// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.lambda.cmd.CommandBase;

public final class MySqlCommands {

    private MySqlCommands() {
    }

    public static MySqlDump mysqldump() {
        return new MySqlDump();
    }

    public static final class MySqlDump extends CommandBase<MySqlDump> {
        private MySqlDump() {
            add("mysqldump");
        }

        public MySqlDump user(String user) {
            return addWithEqual("--user", user);
        }

        public MySqlDump password(String password) {
            return addWithEqual("--password", password);
        }

        public MySqlDump host(String host) {
            return addWithEqual("--host", host);
        }

        public MySqlDump defaultPort() {
            return port(3306);
        }

        public MySqlDump port(int port) {
            return addWithEqual("--port", port);
        }

        public MySqlDump databases(String[] databases) {
            return add("--databases", String.join(" ", databases));
        }

        public MySqlDump compress() {
            return add("--compress");
        }

        public MySqlDump events() {
            return add("--events");
        }

        public MySqlDump orderByPrimary() {
            return add("--order-by-primary");
        }

        public MySqlDump resultFile(String file) {
            return addWithEqual("--result-file", file);
        }

        public MySqlDump routines() {
            return add("--routines");
        }

        public MySqlDump singleTransaction() {
            return add("--single-transaction");
        }

        public MySqlDump triggers() {
            return add("--triggers");
        }

        public MySqlDump version() {
            return add("--version");
        }
    }
}

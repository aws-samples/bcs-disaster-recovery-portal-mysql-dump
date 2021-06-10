// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package aws.proserve.bcs.dr.dbdump.mysql;

import aws.proserve.bcs.dr.s3.S3Constants;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;

class BaseWorker {

    String getBucket(AWSSimpleSystemsManagement ssm) {
        final var parameters = ssm.getParameters(new GetParametersRequest()
                .withNames(S3Constants.PARAM_BUCKET)).getParameters();
        if (parameters.isEmpty()) {
            throw new IllegalStateException("Unable to find bucket at " + S3Constants.PARAM_BUCKET);
        }
        return parameters.get(0).getValue();
    }
}

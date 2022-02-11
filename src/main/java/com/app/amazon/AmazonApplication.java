package com.app.amazon;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;
import com.amazonaws.services.securitytoken.model.GetFederationTokenResult;
import com.amazonaws.util.IOUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.util.List;

@SpringBootApplication
public class AmazonApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmazonApplication.class, args);

        Regions clientRegion = Regions.EU_CENTRAL_1;
        String bid = "dir2";
        String bucketName = "bucket-dacian";
        String federatedUser = "user2";
        String resourceARN = "arn:aws:s3:::"+bucketName;
//        String resourceARN="arn:aws:s3:::bucket-dacian/dir1/*";
        try {
            AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder
                    .standard()
                    .withCredentials(new ProfileCredentialsProvider())
                    .withRegion(clientRegion)
                    .build();

            GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest();
            getFederationTokenRequest.setDurationSeconds(7200);
            getFederationTokenRequest.setName(federatedUser);

            // Define the policy and add it to the request.
            Policy policy = new Policy();
            policy.withStatements(new Statement(Statement.Effect.Allow)
                    .withActions(S3Actions.AllS3Actions)
                            .withConditions(new Condition().withType("StringLike").withConditionKey("s3:prefix").withValues(bid))
                    .withResources(new Resource(resourceARN)));
            getFederationTokenRequest.setPolicy(policy.toJson());

            // Get the temporary security credentials.
            GetFederationTokenResult federationTokenResult = stsClient.getFederationToken(getFederationTokenRequest);
            Credentials sessionCredentials = federationTokenResult.getCredentials();

            // Package the session credentials as a BasicSessionCredentials
            // object for an Amazon S3 client object to use.
            BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(
                    sessionCredentials.getAccessKeyId(),
                    sessionCredentials.getSecretAccessKey(),
                    sessionCredentials.getSessionToken());
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(basicSessionCredentials))
                    .withRegion(clientRegion)
                    .build();

            // To verify that the client works, send a listObjects request using
            // the temporary security credentials.
            File file = new File("/Users/kp/Downloads/bread-2.png");

            // PUT
            //final PutObjectResult putObjectResult = s3Client.putObject(bucketName, bid + "/users/"+file.getName(), file);

            //DELETE
            for (S3ObjectSummary input : s3Client.listObjects(bucketName, bid).getObjectSummaries()){
                s3Client.deleteObject(bucketName, input.getKey());
            }

            // GET
//            final S3Object object = s3Client.getObject(bucketName, "dir1" + "/users/bread-2.png");
//            InputStream stream = object.getObjectContent().getDelegateStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//
//            IOUtils.copy(stream, new FileOutputStream("/Users/kp/Downloads/bread-pit.png"));
//            System.out.println(s3Client.listObjects(bucketName,bid).getObjectSummaries());

//            System.out.println("No. of Objects = " + objects);
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }


    }

}

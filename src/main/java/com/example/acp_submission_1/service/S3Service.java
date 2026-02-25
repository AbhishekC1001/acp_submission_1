package com.example.acp_submission_1.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3Service {
    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public List<String> getAllObjectContents(String bucket) {
        List<String> contents = new ArrayList();
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder().bucket(bucket);

        ListObjectsV2Response response;
        do {
            response = this.s3Client.listObjectsV2((ListObjectsV2Request)requestBuilder.build());
            Iterator it = response.contents().iterator();

            while(it.hasNext()) {
                S3Object obj = (S3Object)it.next();
                contents.add(this.getObjectContent(bucket, obj.key()));
            }

            requestBuilder.continuationToken(response.nextContinuationToken());
        } while(Boolean.TRUE.equals(response.isTruncated()));

        return contents;
    }

    public String getObjectContent(String bucket, String key) {
        return this.s3Client.getObjectAsBytes((GetObjectRequest)GetObjectRequest.builder().bucket(bucket).key(key).build()).asUtf8String();
    }

    public void putObject(String bucket, String key, String content) {
        this.ensureBucketExists(bucket);
        this.s3Client.putObject((PutObjectRequest)PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromString(content));
    }

    private void ensureBucketExists(String bucket) {
        try {
            this.s3Client.headBucket((HeadBucketRequest)HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException var3) {
            this.s3Client.createBucket((CreateBucketRequest)CreateBucketRequest.builder().bucket(bucket).build());
        }

    }
}

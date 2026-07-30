package de.thm.mni.backend.storage.s3

import de.thm.mni.backend.storage.FileStorageException
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.net.URI

/**
 * Builds the S3-compatible client used for attachment storage.
 */
@Configuration
@EnableConfigurationProperties(S3StorageProperties::class)
class S3ClientConfig {
    @Bean
    fun s3Client(properties: S3StorageProperties): S3Client {
        validate(properties)
        return S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint))
            .region(Region.of(properties.region))
            .credentialsProvider(credentialsProvider(properties))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(properties.pathStyleAccess)
                    .build()
            )
            .build()
    }

    @Bean
    fun s3BucketInitializer(s3Client: S3Client, properties: S3StorageProperties): ApplicationRunner {
        return ApplicationRunner {
            if (!properties.initializeBucket) {
                return@ApplicationRunner
            }
            ensureBucketWithRetry(s3Client, properties)
        }
    }

    private fun validate(properties: S3StorageProperties) {
        val missingValues = mutableListOf<String>()
        if (properties.endpoint.isBlank()) {
            missingValues.add("endpoint")
        }
        if (properties.bucket.isBlank()) {
            missingValues.add("bucket")
        }
        if (properties.initializeBucket && properties.credentialsAreIncomplete()) {
            missingValues.add("credentials")
        }
        if (missingValues.isNotEmpty()) {
            throw FileStorageException("S3 configuration is incomplete: ${missingValues.joinToString()}.")
        }
    }

    private fun credentialsProvider(properties: S3StorageProperties): AwsCredentialsProvider {
        if (properties.credentialsAreIncomplete()) {
            return AnonymousCredentialsProvider.create()
        }

        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(properties.accessKey, properties.secretKey)
        )
    }

    private fun ensureBucketWithRetry(s3Client: S3Client, properties: S3StorageProperties) {
        var lastFailure: Exception? = null
        repeat(BUCKET_INIT_ATTEMPTS) { attempt ->
            try {
                ensureBucket(s3Client, properties.bucket)
                return
            } catch (ex: SdkException) {
                lastFailure = ex
                if (attempt < BUCKET_INIT_ATTEMPTS - 1) {
                    Thread.sleep(BUCKET_INIT_RETRY_DELAY_MS)
                }
            }
        }

        throw FileStorageException("Could not initialize S3 bucket '${properties.bucket}'.", lastFailure)
    }

    private fun ensureBucket(s3Client: S3Client, bucket: String) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
        } catch (ex: S3Exception) {
            if (ex.statusCode() != HTTP_NOT_FOUND) {
                throw ex
            }
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val BUCKET_INIT_ATTEMPTS = 30
        const val BUCKET_INIT_RETRY_DELAY_MS = 1_000L
    }
}

private fun S3StorageProperties.credentialsAreIncomplete(): Boolean {
    return accessKey.isBlank() || secretKey.isBlank()
}

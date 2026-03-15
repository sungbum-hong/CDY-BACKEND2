package com.cdy.cdy.common.r2;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.List;

@Configuration
@Profile("!test")
public class R2Config {

    private final StorageProps p;

    public R2Config(StorageProps p) {
        this.p = p;
    }

    /** R2에 실제 요청(업로드/다운로드/삭제)을 날리는 클라이언트 */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.getAccessKey(), p.getSecretKey())))
                .region(Region.of(p.getRegion()))
                .endpointOverride(URI.create(p.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    /** 프리사인 URL 발급용 */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.getAccessKey(), p.getSecretKey())))
                .region(Region.of(p.getRegion()))
                .endpointOverride(URI.create(p.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /** 서버 시작 시 R2 버킷 CORS 정책 자동 적용 */
    @PostConstruct
    public void applyBucketCors() {
        try (S3Client client = s3Client()) {
            CORSRule corsRule = CORSRule.builder()
                    .allowedOrigins("https://www.codiyoung.com")
                    .allowedMethods("GET", "PUT", "POST", "DELETE")
                    .allowedHeaders("*")
                    .exposeHeaders("ETag")
                    .maxAgeSeconds(3000)
                    .build();

            client.putBucketCors(PutBucketCorsRequest.builder()
                    .bucket(p.getBucket())
                    .corsConfiguration(CORSConfiguration.builder()
                            .corsRules(List.of(corsRule))
                            .build())
                    .build());

            System.out.println("[R2] CORS 정책 적용 완료: " + p.getBucket());
        } catch (Exception e) {
            System.err.println("[R2] CORS 적용 실패: " + e.getMessage());
        }
    }
}

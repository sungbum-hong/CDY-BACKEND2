package com.cdy.cdy.common.r2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final StorageProps props;
    private final S3Presigner presigner;
    private final S3Client s3Client;



    // ---------------------------
    // 1) 일반 업로드 키 생성 규칙
    // ---------------------------

    // 원본 파일명에서 확장자를 뽑아낸 뒤, uploads/ 폴더 아래에 UUID를 붙여 키를 만든다.
    public String buildKey(String originalFilename) {
        // originalFilename이 존재하고 점(.)이 있으면 마지막 점 뒤를 확장자로 사용한다.
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                // 확장자를 도저히 알 수 없는 경우 기본값을 "bin"으로 둔다.
                : "bin";
        // 예: uploads/550e8400-e29b-41d4-a716-446655440000.jpg
        return "uploads/" + UUID.randomUUID() + "." + ext;
    }

    // ---------------------------
    // 2) 아바타(프로필 이미지) 키 규칙
    // ---------------------------

    // 사용자별로 구분되는 경로(avatars/{userId}/) 아래에 UUID.확장자 형태로 생성한다.
    public String buildAvatarKey(Long userId, String filename) {
        // filename에서 확장자를 추출(없으면 jpg로 가정: 대부분 이미지이기 때문).
        String ext = (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf('.') + 1)
                : "jpg";
        // 예: avatars/42/a1b2c3d4-....-ffff.jpg
        return "avatars/" + userId + "/" + UUID.randomUUID() + "." + ext;
    }

    // --------------------------------
    // 3) PUT(업로드) 프리사인 URL 생성
    // --------------------------------

    // 프론트가 파일을 R2로 직접 업로드할 때 사용할 서명된 URL을 생성한다.
    //  - key: 업로드할 객체 키(위의 buildKey/buildAvatarKey로 만든 값)
    //  - contentType: 업로드 파일의 MIME 타입(예: image/jpeg)
    public URL presignPut(String key, String contentType) {
        // 실제 S3 PUT 요청 스펙을 정의한다(어느 버킷의 어떤 키에 어떤 타입으로 올릴지).
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();

        // 위의 PUT 스펙에 "서명 유효시간" 같은 프리사인 메타를 덧붙여 최종 서명 요청을 만든다.
        PutObjectPresignRequest preq = PutObjectPresignRequest.builder()
                .signatureDuration(             // presign URL이 살아있는 시간
                        Duration.ofSeconds(props.getPresignSecond()))
                .putObjectRequest(put)          // 실제 PUT 스펙
                .build();

        // 프리사이너가 서명된 요청을 만들어 준다(내부적으로 키/시크릿/엔드포인트로 서명).
        PresignedPutObjectRequest presigned = presigner.presignPutObject(preq);

        // 최종적으로 브라우저가 그대로 호출할 수 있는 URL을 반환한다.
        return presigned.url();
    }

    // --------------------------------
    // 4) GET(조회) 프리사인 URL 생성
    // --------------------------------

    // 버킷이 private일 때, 이미지를 보여주기 위해 일정 시간만 유효한 조회 URL을 만든다.
    //  - key: 조회할 객체 키
    //  - seconds: URL 유효시간(초)
    public URL presignGet(String key, int seconds) {
        // 어떤 버킷의 어떤 키를 가져올지 정의한다.
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(props.getBucket())      // 대상 버킷명
                .key(key)                       // 객체 키
                .build();

        // 유효시간과 함께 GET 스펙을 싸서 서명 요청을 만든다.
        GetObjectPresignRequest greq = GetObjectPresignRequest.builder()
                .signatureDuration(             // presign URL이 살아있는 시간
                        Duration.ofSeconds(seconds))
                .getObjectRequest(get)          // 실제 GET 스펙
                .build();

        // 서명된 GET 요청을 만들고, 그 안의 URL을 꺼내 반환한다.
        return presigner.presignGetObject(greq).url();
    }

    // --------------------------------
    // 5) 서버 사이드 직접 업로드
    // --------------------------------

    public String upload(MultipartFile file) {
        try {
            String key = buildKey(file.getOriginalFilename());
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return publicUrl(key);
        } catch (Exception e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage(), e);
        }
    }

    // --------------------------------
    // 6) 퍼블릭 버킷용 정적 접근 URL
    // --------------------------------

    // 버킷을 퍼블릭으로 열어두었다면, 굳이 presign 없이
    // "엔드포인트/버킷/키" 형태의 고정 URL로 접근할 수 있다.
    public String publicUrl(String key) {
        // 예: https://<ACCOUNT>.r2.cloudflarestorage.com/<bucket>/<key>
        return props.getEndpoint() + "/" + props.getBucket() + "/" + key;
    }
}

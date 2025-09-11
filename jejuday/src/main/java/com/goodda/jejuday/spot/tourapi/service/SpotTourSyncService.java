package com.goodda.jejuday.spot.tourapi.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.goodda.jejuday.spot.entity.Spot;
import com.goodda.jejuday.spot.tourapi.SpotTourRepository;
import com.goodda.jejuday.spot.tourapi.TourApiClient;
import com.goodda.jejuday.spot.tourapi.TourApiProperties;
import com.goodda.jejuday.spot.tourapi.dto.TourApiPage;
import com.goodda.jejuday.spot.tourapi.dto.TourItem;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotTourSyncService {

    private final TourApiClient client;
    private final SpotTourRepository repo;
    private final TourApiProperties props;
    private final com.goodda.jejuday.auth.repository.UserRepository userRepository;
    private final TransactionTemplate tx;
    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    /** 이미지 다운로드 전용 WebClient (baseUrl 없음, buffer 제한 해제) */
    @Qualifier("downloadWebClient")
    private final WebClient downloadWebClient;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PROVIDER_PREFIX = "KTO:"; // externalPlaceId = "KTO:<contentid>"

    @PostConstruct
    void checkProps() {
        log.info("TourAPI baseUrl={}, korServicePath={}, systemUserId={}",
                props.getBaseUrl(), props.getKorServicePath(), props.getSystemUserId());
    }

    // =========================================================
    // 공개 메소드
    // =========================================================
    public Result initialImport(String arrange, String areaCode, String lDongRegnCd, String lDongSignguCd, int rows) {
        int page = 1, imported = 0, updated = 0, skipped = 0, pages = 0, total = 0;
        for (;;) {
            TourApiPage p = client.areaBasedList(page, rows, arrange, areaCode, lDongRegnCd, lDongSignguCd);
            pages++; total = p.getTotalCount();
            if (p.getItems().isEmpty()) break;

            for (TourItem it : p.getItems()) {
                Upsert u = tx.execute(status -> upsert(it)); // 아이템 단위 트랜잭션
                imported += u.i; updated += u.u; skipped += u.s;
            }
            if (page * rows >= total) break;
            page++;
        }
        return new Result(imported, updated, skipped, pages, total, null);
    }

    @Transactional
    public Result syncSince(String sinceYmd, String arrange, String areaCode, String lDongRegnCd, String lDongSignguCd, int rows) {
        int page = 1, imported = 0, updated = 0, skipped = 0, pages = 0, total = 0;
        String last = null;
        for (;;) {
            TourApiPage p = client.areaBasedSyncList(sinceYmd, page, rows, arrange, areaCode, lDongRegnCd, lDongSignguCd, last);
            pages++; total = p.getTotalCount();
            if (p.getItems().isEmpty()) break;

            for (TourItem it : p.getItems()) {
                last = it.getContentid();
                Upsert u = upsert(it);
                imported += u.i; updated += u.u; skipped += u.s;
            }
            if (page * rows >= total) break;
            page++;
        }
        return new Result(imported, updated, skipped, pages, total, sinceYmd);
    }

    // =========================================================
    // 핵심 업서트(이미지 포함)
    // =========================================================
    private Upsert upsert(TourItem it) {
        String externalId = PROVIDER_PREFIX + it.getContentid();

        Spot s = repo.findByExternalPlaceId(externalId).orElse(null);
        if (s == null) {
            s = new Spot();
            mapSpotBasics(s, it, externalId);
            s = repo.save(s);                  // 1) 먼저 저장해서 ID 확보

            mapSpotImages(s, it);              // 2) 이미지 복사 + 필드 세팅
            repo.save(s);                      // 3) 이미지 반영 저장
            return Upsert.insert();
        } else {
            mapSpotBasics(s, it, externalId);  // 기본 필드 갱신
            mapSpotImages(s, it);              // 필요 시 이미지 갱신(비어있으면 채움)
            return Upsert.update();
        }
    }

    /** Spot 엔티티에 '있는' 기본 필드만 안전하게 반영 (이미지 제외) */
    private void mapSpotBasics(Spot s, TourItem t, String externalId) {
        // 타입/작성자/외부ID
        s.setType(Spot.SpotType.SPOT);   // 외부 콘텐츠 → 기본은 SPOT
        s.setUserCreated(false);
        s.setExternalPlaceId(externalId);

        Long sysUserId = props.getSystemUserId();
        if (sysUserId == null) throw new IllegalStateException("tourapi.system-user-id 가 설정되어야 합니다.");
        s.setUser(userRepository.getReferenceById(sysUserId));

        // 이름
        if (t.getTitle() != null) s.setName(t.getTitle());

        // 좌표: mapy=위도, mapx=경도
        BigDecimal lat = toBigDecimal(t.getMapy());
        BigDecimal lon = toBigDecimal(t.getMapx());
        if (lat != null) s.setLatitude(lat);
        if (lon != null) s.setLongitude(lon);

        // 카테고리 메타(코드 위주)
        if (t.getCat1() != null) s.setCategoryGroupCode(t.getCat1());
        else if (t.getLclsSystm1() != null) s.setCategoryGroupCode(t.getLclsSystm1());
        if (t.getCat3() != null) s.setCategoryName(t.getCat3());
    }

    /**
     * 외부 이미지 URL(firstimage, firstimage2)을 다운로드→S3 업로드→Spot에 부착.
     * - Spot에 setImagesOrdered(List) 메소드가 있으면 우선 사용.
     * - 없으면 img1/img2/img3(혹은 image1/image2/thumbnailUrl 등) 필드에 세팅.
     * - 기존에 이미지가 비어있으면 채우고, 있으면 유지(중복 업로드 방지).
     */
    private void mapSpotImages(Spot s, TourItem t) {
        List<String> current = readCurrentImageUrls(s);
        boolean alreadyHas = current.stream().anyMatch(u -> u != null && !u.isBlank());
        if (alreadyHas) return; // 기존 이미지 있으면 그대로 둡니다.

        List<String> candidates = new ArrayList<>(2);
        if (t.getFirstimage() != null && !t.getFirstimage().isBlank()) candidates.add(t.getFirstimage());
        if (t.getFirstimage2() != null && !t.getFirstimage2().isBlank()) candidates.add(t.getFirstimage2());
        if (candidates.isEmpty()) return;

        List<String> uploaded = new ArrayList<>();
        for (int i = 0; i < candidates.size() && uploaded.size() < 3; i++) {
            String src = candidates.get(i);
            String hint = (i == 0 ? "first" : "second") + extFromUrl(src);
            String s3 = copyExternalImageToS3(src, s.getId(), hint);
            if (s3 != null) uploaded.add(s3);
        }
        if (uploaded.isEmpty()) return;

        setImagesOrderedOrFirst3(s, uploaded);
    }

    // =========================================================
    // 이미지 유틸
    // =========================================================

    /** 트랜잭션 밖에서 실행(다운로드 느림 대비) */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String copyExternalImageToS3(String imageUrl, Long spotId, String filenameHint) {
        try {
            ByteArrayResource res = downloadWebClient.get()
                    .uri(imageUrl)
                    .retrieve()
                    .bodyToMono(ByteArrayResource.class)
                    .block();

            if (res == null || res.contentLength() <= 0) {
                log.warn("Empty body from {}", imageUrl);
                return null;
            }

            // Content-Type 추정 (없으면 jpeg)
            String contentType = guessContentType(imageUrl);

            String key = "spot-images/" + spotId + "/" + UUID.randomUUID() + "-" +
                    (filenameHint == null ? "image" : filenameHint);

            ObjectMetadata md = new ObjectMetadata();
            md.setContentLength(res.contentLength());
            md.setContentType(contentType);

            amazonS3.putObject(bucketName, key, res.getInputStream(), md);
            return amazonS3.getUrl(bucketName, key).toString();

        } catch (Exception e) {
            log.warn("Failed to copy image to S3: {} (reason: {})", imageUrl, e.getMessage());
            return null;
        }
    }

    /** 현재 Spot의 이미지 URL들을 읽는다. 우선 getImageUrls(), 없으면 img1/img2/img3 계열을 탐색 */
    @SuppressWarnings("unchecked")
    private List<String> readCurrentImageUrls(Spot s) {
        try {
            var m = Spot.class.getMethod("getImageUrls");
            Object v = m.invoke(s);
            if (v instanceof List<?>) {
                List<?> raw = (List<?>) v;
                List<String> list = new ArrayList<>();
                for (Object o : raw) list.add(Objects.toString(o, null));
                return list;
            }
        } catch (Exception ignore) { /* fallback 아래로 */ }

        BeanWrapper w = new BeanWrapperImpl(s);
        List<String> out = new ArrayList<>(3);
        out.add(readIfReadable(w, "img1", "image1", "thumbnailUrl"));
        out.add(readIfReadable(w, "img2", "image2"));
        out.add(readIfReadable(w, "img3", "image3"));
        return out;
    }

    /** setImagesOrdered(List) 있으면 사용, 없으면 img1/img2/img3로 세팅 */
    private void setImagesOrderedOrFirst3(Spot s, List<String> urls) {
        List<String> top3 = urls.size() > 3 ? urls.subList(0, 3) : urls;
        try {
            var m = Spot.class.getMethod("setImagesOrdered", List.class);
            m.invoke(s, top3);
            return;
        } catch (Exception ignore) { /* fallback 아래로 */ }

        BeanWrapper w = new BeanWrapperImpl(s);
        writeIfPresent(w, top3.size() > 0 ? top3.get(0) : null, "img1", "image1", "thumbnailUrl");
        writeIfPresent(w, top3.size() > 1 ? top3.get(1) : null, "img2", "image2");
        writeIfPresent(w, top3.size() > 2 ? top3.get(2) : null, "img3", "image3");
    }

    private static String guessContentType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    private static String extFromUrl(String url) {
        try {
            String path = new java.net.URI(url).getPath();
            int dot = path.lastIndexOf('.');
            if (dot >= 0 && dot < path.length() - 1) {
                String ext = path.substring(dot);
                if (ext.length() <= 5) return ext;
            }
        } catch (Exception ignore) {}
        return ".jpg";
    }

    private static String readIfReadable(BeanWrapper w, String... names) {
        for (String n : names) {
            if (w.isReadableProperty(n)) {
                try {
                    Object v = w.getPropertyValue(n);
                    return (v == null) ? null : v.toString();
                } catch (Exception ignore) {}
            }
        }
        return null;
    }

    private static void writeIfPresent(BeanWrapper w, Object value, String... candidates) {
        if (value == null) return;
        for (String n : candidates) {
            if (w.isWritableProperty(n)) {
                try { w.setPropertyValue(n, value); return; } catch (Exception ignore) {}
            }
        }
    }

    // =========================================================
    // 공통 유틸
    // =========================================================
    private static BigDecimal toBigDecimal(String s) {
        try { return (s == null || s.isBlank()) ? null : new BigDecimal(s); }
        catch (Exception e) { return null; }
    }
    @SuppressWarnings("unused")
    private static LocalDateTime parseDateTime(String s) {
        try { return (s == null) ? null : LocalDateTime.parse(s, DT); }
        catch (Exception e) { return null; }
    }

    // =========================================================
    // 결과 타입
    // =========================================================
    public record Result(int imported, int updated, int skipped, int pages, int total, String since) {}
    private record Upsert(int i, int u, int s) {
        static Upsert insert() { return new Upsert(1, 0, 0); }
        static Upsert update() { return new Upsert(0, 1, 0); }
        @SuppressWarnings("unused")
        static Upsert skip()   { return new Upsert(0, 0, 1); }
    }
}

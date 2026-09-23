package com.cdy.cdy.domain.shop.service;

import com.cdy.cdy.common.r2.ImageUrlResolver;
import com.cdy.cdy.domain.partner.entity.Partner;
import com.cdy.cdy.domain.partner.entity.PartnerCategory;
import com.cdy.cdy.domain.partner.entity.PartnerStatus;
import com.cdy.cdy.domain.partner.repository.PartnerRepository;
import com.cdy.cdy.domain.shop.dto.ProductDtos;
import com.cdy.cdy.domain.shop.entity.Product;
import com.cdy.cdy.domain.shop.entity.ProductImage;
import com.cdy.cdy.domain.shop.entity.ProductStatus;
import com.cdy.cdy.domain.shop.repository.CartItemRepository;
import com.cdy.cdy.domain.shop.repository.OrderItemRepository;
import com.cdy.cdy.domain.shop.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    /** 오늘의 특가에 노출할 개수 */
    private static final int TODAY_DEAL_LIMIT = 6;

    private final ProductRepository productRepository;
    private final PartnerRepository partnerRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ImageUrlResolver imageUrlResolver;

    // ================= 크루용 =================

    /**
     * 크루 목록. category 는 업체 카테고리, keyword 는 상품명+업체명을 함께 검색한다.
     * maxDiscountRate 는 필터와 무관한 전체 기준값이라 별도로 구한다.
     */
    @Transactional(readOnly = true)
    public ProductDtos.ListResponse findForCrew(PartnerCategory category, Long partnerId, String keyword) {

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        List<ProductDtos.ListItem> items = productRepository
                .findForCrew(ProductStatus.HIDDEN, PartnerStatus.HIDDEN, category, partnerId, kw)
                .stream()
                .map(this::toListItem)
                .toList();

        return new ProductDtos.ListResponse(items, maxDiscountRate());
    }

    /** 배너 "최대 N%" 용. 판매중 상품이 없으면 0 */
    @Transactional(readOnly = true)
    public int maxDiscountRate() {
        return onSale().stream()
                .mapToInt(Product::discountRate)
                .max()
                .orElse(0);
    }

    /** 판매중 상품이 1개 이상인 제휴 업체만. 업체 정렬은 sortOrder → id */
    @Transactional(readOnly = true)
    public List<ProductDtos.PartnerItem> findPartnersWithProducts() {

        // 업체별 판매중 상품 수 집계. onSale() 이 이미 sortOrder/id 순이라 삽입 순서를 유지한다
        Map<Long, Partner> partners = new LinkedHashMap<>();
        Map<Long, Long> counts = new LinkedHashMap<>();

        for (Product p : onSale()) {
            Partner pt = p.getPartner();
            partners.putIfAbsent(pt.getId(), pt);
            counts.merge(pt.getId(), 1L, Long::sum);
        }

        return partners.values().stream()
                .sorted(Comparator
                        .comparing((Partner pt) -> pt.getSortOrder() == null ? 0 : pt.getSortOrder())
                        .thenComparing(Partner::getId))
                .map(pt -> new ProductDtos.PartnerItem(
                        pt.getId(),
                        pt.getName(),
                        pt.getCategory(),
                        imageUrlResolver.toPresignedUrl(pt.getLogoImageKey()),
                        counts.getOrDefault(pt.getId(), 0L)))
                .toList();
    }

    /** 오늘의 특가 — 할인율 내림차순 상위 N개, 동률이면 최신(id desc) */
    @Transactional(readOnly = true)
    public List<ProductDtos.ListItem> findTodayDeals() {
        return onSale().stream()
                .sorted(Comparator
                        .comparingInt(Product::discountRate).reversed()
                        .thenComparing(Comparator.comparing(Product::getId).reversed()))
                .limit(TODAY_DEAL_LIMIT)
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDtos.Detail findDetail(Long id) {

        Product p = productRepository.findWithImagesById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품"));

        Partner pt = p.getPartner();

        // 숨긴 상품, 그리고 숨긴 업체의 상품은 크루에게 존재하지 않는 것으로 취급한다
        if (p.getStatus() == ProductStatus.HIDDEN || pt.getStatus() == PartnerStatus.HIDDEN) {
            throw new EntityNotFoundException("존재하지 않는 상품");
        }

        return new ProductDtos.Detail(
                p.getId(),
                p.getName(),
                pt.getId(),
                pt.getName(),
                pt.getCategory(),
                imageUrlResolver.toPresignedUrl(pt.getLogoImageKey()),
                p.getOriginalPrice(),
                p.getCrewPrice(),
                p.discountRate(),
                p.getStock(),
                p.getStatus(),
                imageUrlResolver.toPresignedUrl(p.getThumbnailKey()),
                p.getImages().stream()
                        .map(img -> imageUrlResolver.toPresignedUrl(img.getImageKey()))
                        .toList(),
                p.getDescription());
    }

    // ================= 어드민용 =================

    @Transactional(readOnly = true)
    public List<ProductDtos.AdminItem> findAllForAdmin() {
        return productRepository.findAllForAdmin().stream().map(this::toAdminItem).toList();
    }

    @Transactional
    public void create(ProductDtos.Request dto) {
        productRepository.save(buildProduct(dto));
    }

    @Transactional
    public void update(Long id, ProductDtos.Request dto) {

        Product p = productRepository.findWithImagesById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품"));

        validate(dto);

        p.update(dto.name(),
                resolvePartner(dto.partnerId()),
                dto.originalPrice(),
                dto.crewPrice(),
                dto.stock(),
                dto.supplierName(),
                dto.productCode(),
                dto.thumbnailKey(),
                dto.description(),
                dto.status(),
                dto.sortOrder());

        // imageKeys 를 아예 안 보내면(null) 기존 이미지 유지, 빈 배열이면 전체 삭제
        if (dto.imageKeys() != null) p.replaceImages(dto.imageKeys());
    }

    /**
     * 하드 삭제. 단 주문된 적 있는 상품은 지우지 않는다 —
     * shop_order_items 가 product_id FK 를 들고 있어서 지우면 주문 내역이 깨진다.
     * 그 경우는 숨김(PUT 으로 status=HIDDEN)을 쓰라고 안내한다.
     *
     * 장바구니는 주문 이력이 아니라 임시 상태라 그냥 비우고 진행한다.
     * 상세 이미지(product_images)는 Product.images 의 cascade=ALL + orphanRemoval 로 같이 지워진다.
     */
    @Transactional
    public void delete(Long id) {

        Product p = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품"));

        if (orderItemRepository.existsByProductId(id)) {
            throw new IllegalArgumentException("주문 이력이 있는 상품은 삭제할 수 없어요. 숨김 처리를 사용해주세요.");
        }

        // 상품보다 먼저 지워야 FK 제약에 걸리지 않는다. flush 로 순서를 확정한다.
        cartItemRepository.deleteByProductId(id);
        cartItemRepository.flush();

        productRepository.delete(p);
    }

    /**
     * JSON 배열 일괄 등록. 한 건이 실패해도 나머지는 등록하고
     * 실패 건은 사유를 모아서 돌려준다.
     */
    @Transactional
    public ProductDtos.BulkResult createBulk(List<ProductDtos.Request> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("등록할 상품이 없습니다.");
        }

        int created = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            ProductDtos.Request dto = requests.get(i);
            try {
                productRepository.save(buildProduct(dto));
                created++;
            } catch (RuntimeException e) {
                errors.add("[%d번째 %s] %s".formatted(
                        i + 1,
                        dto == null || dto.name() == null ? "이름없음" : dto.name(),
                        e.getMessage()));
            }
        }

        log.info("[ProductService] 일괄 등록 - 성공 {}건, 실패 {}건", created, errors.size());
        return new ProductDtos.BulkResult(created, errors);
    }

    // ================= 내부 =================

    private List<Product> onSale() {
        return productRepository.findOnSale(ProductStatus.ACTIVE, PartnerStatus.HIDDEN);
    }

    private Product buildProduct(ProductDtos.Request dto) {

        validate(dto);

        Product p = Product.builder()
                .name(dto.name().trim())
                .partner(resolvePartner(dto.partnerId()))
                .originalPrice(dto.originalPrice())
                .crewPrice(dto.crewPrice())
                .stock(dto.stock() != null ? dto.stock() : 0)
                .supplierName(dto.supplierName())
                .productCode(dto.productCode())
                .thumbnailKey(dto.thumbnailKey())
                .description(dto.description())
                .status(dto.status() != null ? dto.status() : ProductStatus.ACTIVE)
                .sortOrder(dto.sortOrder() != null ? dto.sortOrder() : 0)
                .build();

        p.replaceImages(dto.imageKeys());
        return p;
    }

    /**
     * 상품은 반드시 살아 있는 제휴 업체에 붙어야 한다.
     * 숨긴 업체에 새 상품을 달면 등록되자마자 목록에서 사라져 어드민이 원인을 못 찾는다.
     */
    private Partner resolvePartner(Long partnerId) {

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 제휴 업체입니다: " + partnerId));

        if (partner.getStatus() == PartnerStatus.HIDDEN) {
            throw new IllegalArgumentException("숨김 처리된 제휴 업체에는 상품을 등록할 수 없습니다: " + partner.getName());
        }
        return partner;
    }

    private void validate(ProductDtos.Request dto) {
        if (dto == null) throw new IllegalArgumentException("상품 정보가 없습니다.");
        if (dto.name() == null || dto.name().isBlank()) throw new IllegalArgumentException("상품명은 필수입니다.");
        if (dto.partnerId() == null) throw new IllegalArgumentException("제휴 업체는 필수입니다.");
        if (dto.originalPrice() == null || dto.originalPrice() < 0) throw new IllegalArgumentException("정가가 올바르지 않습니다.");
        if (dto.crewPrice() == null || dto.crewPrice() < 0) throw new IllegalArgumentException("크루가가 올바르지 않습니다.");
        if (dto.stock() != null && dto.stock() < 0) throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
    }

    private ProductDtos.ListItem toListItem(Product p) {
        Partner pt = p.getPartner();
        return new ProductDtos.ListItem(
                p.getId(),
                p.getName(),
                pt.getId(),
                pt.getName(),
                pt.getCategory(),
                p.getOriginalPrice(),
                p.getCrewPrice(),
                p.discountRate(),
                p.getStock(),
                p.getStatus(),
                imageUrlResolver.toPresignedUrl(p.getThumbnailKey()));
    }

    private ProductDtos.AdminItem toAdminItem(Product p) {
        List<String> keys = p.getImages().stream().map(ProductImage::getImageKey).toList();
        Partner pt = p.getPartner();
        return new ProductDtos.AdminItem(
                p.getId(),
                p.getName(),
                pt.getId(),
                pt.getName(),
                pt.getCategory(),
                p.getOriginalPrice(),
                p.getCrewPrice(),
                p.discountRate(),
                p.getStock(),
                p.getSupplierName(),
                p.getProductCode(),
                p.getThumbnailKey(),
                imageUrlResolver.toPresignedUrl(p.getThumbnailKey()),
                keys,
                keys.stream().map(imageUrlResolver::toPresignedUrl).toList(),
                p.getDescription(),
                p.getStatus(),
                p.getSortOrder(),
                p.getCreatedAt());
    }
}

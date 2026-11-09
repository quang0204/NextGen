package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.common.PageInfoResponse;
import com.wedbackendservice.nextgen.dto.portal.response.admin.AuditLogListResponse;
import com.wedbackendservice.nextgen.dto.portal.response.admin.AuditLogResponse;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogsService {

    private final com.wedbackendservice.nextgen.repository.AuditLogRepository auditLogRepository;
    private final SecurityHelper securityHelper;

    /**
     * API 13.5: nhật ký kiểm toán (chỉ đọc, ADMIN).
     */
    @Transactional(readOnly = true)
    public AuditLogListResponse getLogs(int page, int size, Long userId, String action,
                                        String entityName, LocalDate fromDate, LocalDate toDate) {
        securityHelper.authenticateAdmin();

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AuditLog> result = auditLogRepository.findAll(
                buildSpecification(userId, action, entityName, fromDate, toDate), pageable);

        return AuditLogListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .pageInfo(PageInfoResponse.of(safePage, safeSize, result))
                .build();
    }

    private Specification<AuditLog> buildSpecification(Long userId, String action, String entityName,
                                                       LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                Join<AuditLog, com.wedbackendservice.nextgen.entity.User> userJoin =
                        root.join("user", JoinType.LEFT);
                predicates.add(cb.equal(userJoin.get("userId"), userId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("action")),
                        "%" + action.trim().toLowerCase() + "%"));
            }
            if (entityName != null && !entityName.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("entityName")),
                        entityName.trim().toLowerCase()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<LocalDateTime>get("createdAt"), toDate.atTime(LocalTime.MAX)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .userId(log.getUser() != null ? log.getUser().getUserId() : null)
                .userName(log.getUser() != null ? log.getUser().getFullName() : null)
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

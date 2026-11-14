package com.wedbackendservice.nextgen.service.client;

import com.wedbackendservice.nextgen.dto.client.request.ticket.SupportTicketCreateRequest;
import com.wedbackendservice.nextgen.dto.portal.request.ticket.SupportTicketResolveRequest;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.portal.response.ticket.AdminSupportTicketListResponse;
import com.wedbackendservice.nextgen.dto.client.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.entity.SupportTicket;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.TicketStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.SupportTicketRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketsService {

    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private final SupportTicketRepository supportTicketRepository;
    private final SecurityHelper securityHelper;

    /**
     * API 12.4: gửi phiếu hỗ trợ.
     */
    @Transactional
    public SupportTicketResponse createTicket(SupportTicketCreateRequest request) {
        User user = securityHelper.getCurrentUser();

        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.getSubject().trim())
                .description(request.getDescription().trim())
                .priority(normalizePriority(request.getPriority()))
                .status(TicketStatus.OPEN.name())
                .build();
        return toResponse(supportTicketRepository.save(ticket));
    }

    /**
     * Lấy danh sách phiếu hỗ trợ của khách hàng hiện tại hoặc theo targetUserId.
     */
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getMyTickets(Long targetUserId) {
        User currentUser = securityHelper.getCurrentUser();
        List<SupportTicket> tickets;
        if (targetUserId != null && (currentUser.getRole() == com.wedbackendservice.nextgen.enums.Role.ADMIN 
                || currentUser.getRole() == com.wedbackendservice.nextgen.enums.Role.STAFF)) {
            tickets = supportTicketRepository.findByUser_UserIdOrderByCreatedAtDesc(targetUserId);
        } else {
            tickets = supportTicketRepository.findByUserOrderByCreatedAtDesc(currentUser);
        }
        return tickets.stream().map(this::toResponse).toList();
    }

    /**
     * API 12.5: quản lý phiếu hỗ trợ (STAFF / ADMIN).
     */
    @Transactional(readOnly = true)
    public AdminSupportTicketListResponse getAdminTickets(int page, int size, String status,
                                                          String priority, String search) {
        securityHelper.authenticateStaffOrAdmin();

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SupportTicket> result = supportTicketRepository.findAll(
                buildSpecification(status, priority, search), pageable);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (TicketStatus ticketStatus : TicketStatus.values()) {
            statusCounts.put(ticketStatus.name(), 0L);
        }
        for (Object[] row : supportTicketRepository.countByStatusGrouped()) {
            if (row[0] != null && row[1] != null) {
                statusCounts.put((String) row[0], ((Number) row[1]).longValue());
            }
        }

        return AdminSupportTicketListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .pageInfo(PageResponse.of(safePage, safeSize, result))
                .statusCounts(statusCounts)
                .build();
    }

    /**
     * API 12.5 phụ: cập nhật và phản hồi phiếu hỗ trợ.
     */
    @Transactional
    public SupportTicketResponse updateTicket(Long ticketId, SupportTicketResolveRequest request) {
        User staff = securityHelper.authenticateStaffOrAdmin();
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_TICKET_NOT_FOUND));

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus().name());
            if (request.getStatus().isFinal()) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }
        ticket.setHandledBy(staff);
        if (request.getResolutionNote() != null && !request.getResolutionNote().isBlank()) {
            ticket.setResolutionNote(request.getResolutionNote().trim());
        }
        log.info("Staff {} updated ticket {}", staff.getUserId(), ticketId);
        return toResponse(supportTicketRepository.save(ticket));
    }

    private Specification<SupportTicket> buildSpecification(String status, String priority, String search) {
        validateStatusFilter(status);
        validatePriorityFilter(priority);

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (priority != null && !priority.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("priority")), priority.trim().toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                query.distinct(true);
                Join<SupportTicket, User> userJoin = root.join("user", JoinType.LEFT);
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("subject")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(userJoin.get("fullName")), like),
                        cb.like(cb.lower(userJoin.get("email")), like),
                        cb.like(cb.lower(userJoin.get("phone")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "MEDIUM";
        }
        String value = priority.trim().toUpperCase();
        if (!PRIORITIES.contains(value)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Mức ưu tiên không hợp lệ (LOW | MEDIUM | HIGH | URGENT).");
        }
        return value;
    }

    private static void validateStatusFilter(String status) {
        if (status != null && !status.isBlank() && TicketStatus.from(status) == null) {
            throw new AppException(ErrorCode.ERR_TICKET_INVALID_STATUS);
        }
    }

    private static void validatePriorityFilter(String priority) {
        if (priority != null && !priority.isBlank()
                && !PRIORITIES.contains(priority.trim().toUpperCase())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Mức ưu tiên không hợp lệ (LOW | MEDIUM | HIGH | URGENT).");
        }
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        User customer = ticket.getUser();
        User handledBy = ticket.getHandledBy();
        return SupportTicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .userId(customer != null ? customer.getUserId() : null)
                .customerName(customer != null ? customer.getFullName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .customerPhone(customer != null ? customer.getPhone() : null)
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .handledById(handledBy != null ? handledBy.getUserId() : null)
                .handledByName(handledBy != null ? handledBy.getFullName() : null)
                .resolutionNote(ticket.getResolutionNote())
                .resolvedAt(ticket.getResolvedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}

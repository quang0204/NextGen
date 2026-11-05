package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.ticket.SupportTicketCreateRequest;
import com.wedbackendservice.nextgen.dto.request.ticket.SupportTicketResolveRequest;
import com.wedbackendservice.nextgen.dto.response.ticket.AdminSupportTicketListResponse;
import com.wedbackendservice.nextgen.dto.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.entity.SupportTicket;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
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
    private static final String DEFAULT_PRIORITY = "MEDIUM";

    private final SupportTicketRepository supportTicketRepository;
    private final SecurityHelper securityHelper;

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

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getMyTickets(int page, int size) {
        User user = securityHelper.getCurrentUser();
        Pageable pageable = pageRequest(page, size);
        return supportTicketRepository.findByUser_UserId(user.getUserId(), pageable)
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketDetail(Long ticketId) {
        User user = securityHelper.getCurrentUser();
        SupportTicket ticket = requireTicket(ticketId);
        if (user.getRole() != Role.STAFF && user.getRole() != Role.ADMIN
                && (ticket.getUser() == null || !user.getUserId().equals(ticket.getUser().getUserId()))) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return toResponse(ticket);
    }

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
            statusCounts.put(ticketStatus.name(), supportTicketRepository.countByStatus(ticketStatus.name()));
        }

        return AdminSupportTicketListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(safePage)
                .size(safeSize)
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .hasNext(result.hasNext())
                .statusCounts(statusCounts)
                .build();
    }

    @Transactional
    public SupportTicketResponse resolveTicket(Long ticketId, SupportTicketResolveRequest request) {
        User staff = securityHelper.authenticateStaffOrAdmin();
        SupportTicket ticket = requireTicket(ticketId);
        TicketStatus newStatus = request.getStatus();

        ticket.setStatus(newStatus.name());
        ticket.setHandledBy(staff);
        if (newStatus.isFinal()) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (request.getResolutionNote() != null && !request.getResolutionNote().isBlank()) {
            ticket.setResolutionNote(request.getResolutionNote().trim());
        }
        log.info("Staff {} updated ticket {} to {}", staff.getUserId(), ticketId, newStatus);
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

    private static Pageable pageRequest(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private SupportTicket requireTicket(Long ticketId) {
        return supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_TICKET_NOT_FOUND));
    }

    private static String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return DEFAULT_PRIORITY;
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

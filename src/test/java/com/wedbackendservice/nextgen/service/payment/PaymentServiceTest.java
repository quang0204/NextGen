package com.wedbackendservice.nextgen.service.payment;

import com.wedbackendservice.nextgen.configuration.BankTransferConfig;
import com.wedbackendservice.nextgen.dto.client.request.payment.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.dto.client.request.payment.SimulateCallbackRequest;
import com.wedbackendservice.nextgen.dto.client.response.payment.PaymentIntentResponse;
import com.wedbackendservice.nextgen.dto.client.response.payment.SimulateCallbackResponse;
import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.Payment;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.PaymentMethod;
import com.wedbackendservice.nextgen.enums.PaymentStatus;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.PaymentRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private BankTransferConfig bankTransferConfig;

    @InjectMocks
    private PaymentService paymentService;

    private User adminUser;
    private User customerUser;
    private Order sampleOrder;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().userId(1L).role(Role.ADMIN).build();
        customerUser = User.builder().userId(2L).role(Role.CUSTOMER).build();
        sampleOrder = Order.builder()
                .orderId(10L)
                .orderCode("ORD100")
                .user(customerUser)
                .totalAmount(BigDecimal.valueOf(250000))
                .finalAmount(BigDecimal.valueOf(250000))
                .status("PENDING")
                .paymentStatus("PENDING")
                .build();
        samplePayment = Payment.builder()
                .paymentId(100L)
                .order(sampleOrder)
                .paymentMethod("COD")
                .amount(BigDecimal.valueOf(250000))
                .transactionRef("PAY-TEST-001")
                .status(PaymentStatus.PENDING.name())
                .build();
    }

    @Test
    void simulateCallback_asAdmin_updatesPaymentAndOrderStatusToSuccess() {
        when(securityHelper.authenticateAdmin()).thenReturn(adminUser);
        when(paymentRepository.findByTransactionRef("PAY-TEST-001")).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        SimulateCallbackRequest request = SimulateCallbackRequest.builder()
                .transactionRef("PAY-TEST-001")
                .amount(BigDecimal.valueOf(250000))
                .success(true)
                .build();

        SimulateCallbackResponse response = paymentService.simulateCallback(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS.name());
        assertThat(response.getOrderPaymentStatus()).isEqualTo("PAID");
        verify(securityHelper).authenticateAdmin();
        verify(paymentRepository).save(samplePayment);
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    void simulateCallback_notAdmin_throwsUnauthorized() {
        when(securityHelper.authenticateAdmin()).thenThrow(new AppException(ErrorCode.UNAUTHORIZED));

        SimulateCallbackRequest request = SimulateCallbackRequest.builder()
                .transactionRef("PAY-TEST-001")
                .success(true)
                .build();

        assertThatThrownBy(() -> paymentService.simulateCallback(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(paymentRepository, never()).findByTransactionRef(any());
    }

    @Test
    void createPaymentIntent_cod_success() {
        when(securityHelper.getCurrentUser()).thenReturn(customerUser);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findFirstByOrder_OrderIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId(101L);
            return p;
        });

        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .orderId(10L)
                .paymentMethod(PaymentMethod.COD)
                .build();

        PaymentIntentResponse response = paymentService.createPaymentIntent(request);

        assertThat(response.getPaymentId()).isEqualTo(101L);
        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getPaymentMethod()).isEqualTo("COD");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING.name());
    }
}

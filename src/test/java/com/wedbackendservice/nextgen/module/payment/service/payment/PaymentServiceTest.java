package com.wedbackendservice.nextgen.module.payment.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.config.BankTransferConfig;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderStatusHistoryRepository;
import com.wedbackendservice.nextgen.module.order.service.OrderStateService;
import com.wedbackendservice.nextgen.module.payment.dto.client.request.CreatePaymentIntentRequest;
import com.wedbackendservice.nextgen.module.payment.dto.client.request.SimulateCallbackRequest;
import com.wedbackendservice.nextgen.module.payment.dto.client.response.SimulateCallbackResponse;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentMethod;
import com.wedbackendservice.nextgen.module.payment.enums.PaymentStatus;
import com.wedbackendservice.nextgen.module.payment.repository.PaymentRepository;
import com.wedbackendservice.nextgen.module.payment.service.client.PaymentService;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;

  @Mock private OrderRepository orderRepository;

  @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;

  @Mock private SecurityHelper securityHelper;

  @Mock private BankTransferConfig bankTransferConfig;

  @Mock private OrderStateService orderStateService;

  @InjectMocks private PaymentService paymentService;

  private User adminUser;
  private User customerUser;
  private Order sampleOrder;
  private Payment samplePayment;

  @BeforeEach
  void setUp() {
    adminUser = User.builder().userId(1L).role(Role.ADMIN).build();
    customerUser = User.builder().userId(2L).role(Role.CUSTOMER).build();
    sampleOrder =
        Order.builder()
            .orderId(10L)
            .orderCode("ORD100")
            .user(customerUser)
            .totalAmount(BigDecimal.valueOf(250000))
            .finalAmount(BigDecimal.valueOf(250000))
            .status("PENDING")
            .paymentStatus("PENDING")
            .build();
    samplePayment =
        Payment.builder()
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
    when(paymentRepository.findByTransactionRef("PAY-TEST-001"))
        .thenReturn(Optional.of(samplePayment));
    when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    SimulateCallbackRequest request =
        SimulateCallbackRequest.builder()
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

    SimulateCallbackRequest request =
        SimulateCallbackRequest.builder().transactionRef("PAY-TEST-001").success(true).build();

    assertThatThrownBy(() -> paymentService.simulateCallback(request))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

    verify(paymentRepository, never()).findByTransactionRef(any());
  }

  @Test
  void createPaymentIntent_cod_rejected() {
    when(securityHelper.getCurrentUser()).thenReturn(customerUser);
    when(orderRepository.findById(10L)).thenReturn(Optional.of(sampleOrder));

    CreatePaymentIntentRequest request =
        CreatePaymentIntentRequest.builder().orderId(10L).paymentMethod(PaymentMethod.COD).build();

    assertThatThrownBy(() -> paymentService.createPaymentIntent(request))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("COD");
  }
}

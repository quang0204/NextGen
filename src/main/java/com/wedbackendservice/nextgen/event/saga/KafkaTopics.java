package com.wedbackendservice.nextgen.event.saga;

public final class KafkaTopics {
  private KafkaTopics() {}

  public static final String ORDER_CREATED = "order-created-topic";
  public static final String STOCK_RESERVED = "stock-reserved-topic";
  public static final String PAYMENT_EVENTS = "payment-events-topic";
  public static final String RELEASE_STOCK = "release-stock-topic";
  public static final String STOCK_RELEASED = "stock-released-topic";
  public static final String ORDER_CANCELLED = "order-cancelled-topic";
  public static final String DEAD_LETTER_TOPIC = "order-events-dlt";
}

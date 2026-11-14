package com.ruralmarketplace.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CartCheckoutRequest {
    @NotNull(message = "Fulfillment type is required")
    private CreateOrderRequest.FulfillmentType fulfillmentType;
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String deliveryAddress;
    public CreateOrderRequest.FulfillmentType getFulfillmentType() { return fulfillmentType; }
    public void setFulfillmentType(CreateOrderRequest.FulfillmentType fulfillmentType) { this.fulfillmentType = fulfillmentType; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}

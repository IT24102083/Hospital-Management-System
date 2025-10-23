package com.hospital.hospitalmanagementsystem.service.external;//package com.hospital.hospitalmanagementsystem.service.external;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//@Service
//public class PaymentGatewayService {
//
//    @Value("${payment.gateway.url:https://api.stripe.com/v1}")
//    private String gatewayUrl;
//
//    @Value("${payment.gateway.secret-key}")
//    private String secretKey;
//
//    @Value("${payment.gateway.enabled:true}")
//    private boolean gatewayEnabled;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    // Process credit/debit card payment
//    public PaymentResult processCardPayment(BigDecimal amount, String cardToken, String transactionId) {
//        try {
//            if (!gatewayEnabled) {
//                // Mock successful payment for testing
//                return new PaymentResult(true, generateAuthCode(), null, transactionId);
//            }
//
//            // Prepare payment request
//            Map<String, Object> paymentRequest = new HashMap<>();
//            paymentRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // Convert to cents
//            paymentRequest.put("currency", "usd");
//            paymentRequest.put("source", cardToken);
//            paymentRequest.put("description", "HealthFirst Medical Payment");
//            paymentRequest.put("metadata", Map.of("transaction_id", transactionId));
//
//            // Make API call to payment gateway
//            // This is a simplified example - actual implementation would depend on your payment provider
//            Map<String, Object> response = makeGatewayRequest("/charges", paymentRequest);
//
//            if ("succeeded".equals(response.get("status"))) {
//                return new PaymentResult(true, (String) response.get("id"), null, transactionId);
//            } else {
//                return new PaymentResult(false, null, "Payment declined by gateway", transactionId);
//            }
//
//        } catch (Exception e) {
//            return new PaymentResult(false, null, "Payment processing error: " + e.getMessage(), transactionId);
//        }
//    }
//
//    // Process refund
//    public RefundResult processRefund(String originalTransactionId, BigDecimal refundAmount) {
//        try {
//            if (!gatewayEnabled) {
//                // Mock successful refund for testing
//                return new RefundResult(true, generateRefundId(), null);
//            }
//
//            Map<String, Object> refundRequest = new HashMap<>();
//            refundRequest.put("charge", originalTransactionId);
//            refundRequest.put("amount", refundAmount.multiply(BigDecimal.valueOf(100)).intValue());
//
//            Map<String, Object> response = makeGatewayRequest("/refunds", refundRequest);
//
//            if ("succeeded".equals(response.get("status"))) {
//                return new RefundResult(true, (String) response.get("id"), null);
//            } else {
//                return new RefundResult(false, null, "Refund declined by gateway");
//            }
//
//        } catch (Exception e) {
//            return new RefundResult(false, null, "Refund processing error: " + e.getMessage());
//        }
//    }
//
//    // Verify card token
//    public boolean verifyCardToken(String cardToken) {
//        try {
//            if (!gatewayEnabled) {
//                return true; // Mock verification for testing
//            }
//
//            // Implementation would verify token with payment gateway
//            return cardToken != null && cardToken.startsWith("tok_");
//
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    // Create customer for recurring payments
//    public String createCustomer(String email, String cardToken) {
//        try {
//            if (!gatewayEnabled) {
//                return "cus_" + UUID.randomUUID().toString().substring(0, 8);
//            }
//
//            Map<String, Object> customerRequest = new HashMap<>();
//            customerRequest.put("email", email);
//            customerRequest.put("source", cardToken);
//
//            Map<String, Object> response = makeGatewayRequest("/customers", customerRequest);
//            return (String) response.get("id");
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to create customer: " + e.getMessage());
//        }
//    }
//
//    // Charge existing customer
//    public PaymentResult chargeCustomer(String customerId, BigDecimal amount, String transactionId) {
//        try {
//            if (!gatewayEnabled) {
//                return new PaymentResult(true, generateAuthCode(), null, transactionId);
//            }
//
//            Map<String, Object> chargeRequest = new HashMap<>();
//            chargeRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
//            chargeRequest.put("currency", "usd");
//            chargeRequest.put("customer", customerId);
//            chargeRequest.put("description", "HealthFirst Payment Plan Payment");
//
//            Map<String, Object> response = makeGatewayRequest("/charges", chargeRequest);
//
//            if ("succeeded".equals(response.get("status"))) {
//                return new PaymentResult(true, (String) response.get("id"), null, transactionId);
//            } else {
//                return new PaymentResult(false, null, "Payment declined", transactionId);
//            }
//
//        } catch (Exception e) {
//            return new PaymentResult(false, null, "Payment error: " + e.getMessage(), transactionId);
//        }
//    }
//
//    // Private helper methods
//    private Map<String, Object> makeGatewayRequest(String endpoint, Map<String, Object> data) {
//        // This would make actual HTTP request to payment gateway
//        // Simplified mock response for demonstration
//        Map<String, Object> mockResponse = new HashMap<>();
//        mockResponse.put("status", "succeeded");
//        mockResponse.put("id", generateAuthCode());
//        return mockResponse;
//    }
//
//    private String generateAuthCode() {
//        return "AUTH" + System.currentTimeMillis();
//    }
//
//    private String generateRefundId() {
//        return "REF" + System.currentTimeMillis();
//    }
//
//    // Result classes
//    public static class PaymentResult {
//        private final boolean successful;
//        private final String authorizationCode;
//        private final String errorMessage;
//        private final String transactionId;
//
//        public PaymentResult(boolean successful, String authorizationCode, String errorMessage, String transactionId) {
//            this.successful = successful;
//            this.authorizationCode = authorizationCode;
//            this.errorMessage = errorMessage;
//            this.transactionId = transactionId;
//        }
//
//        public boolean isSuccessful() { return successful; }
//        public String getAuthorizationCode() { return authorizationCode; }
//        public String getErrorMessage() { return errorMessage; }
//        public String getTransactionId() { return transactionId; }
//    }
//
//    public static class RefundResult {
//        private final boolean successful;
//        private final String refundId;
//        private final String errorMessage;
//
//        public RefundResult(boolean successful, String refundId, String errorMessage) {
//            this.successful = successful;
//            this.refundId = refundId;
//            this.errorMessage = errorMessage;
//        }
//
//        public boolean isSuccessful() { return successful; }
//        public String getRefundId() { return refundId; }
//        public String getErrorMessage() { return errorMessage; }
//    }
//}
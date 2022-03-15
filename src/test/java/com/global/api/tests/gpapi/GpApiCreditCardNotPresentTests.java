package com.global.api.tests.gpapi;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.Transaction;
import com.global.api.entities.enums.Channel;
import com.global.api.entities.enums.TransactionStatus;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.GatewayException;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.serviceConfigs.GpApiConfig;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.*;

public class GpApiCreditCardNotPresentTests extends BaseGpApiTest {

    private final CreditCardData card;
    private final BigDecimal amount = new BigDecimal("2.02");
    private final String currency = "USD";

    public GpApiCreditCardNotPresentTests() throws ApiException {

        GpApiConfig config = new GpApiConfig();

        // GP-API settings
        config
                .setAppId(APP_ID)
                .setAppKey(APP_KEY)
                .setChannel(Channel.CardNotPresent.getValue());

        //DO NOT DELETE - usage example for some settings
//        HashMap<String, String> dynamicHeaders = new HashMap<String, String>() {{
//            put("x-gp-platform", "prestashop;version=1.7.2");
//            put("x-gp-extension", "coccinet;version=2.4.1");
//        }};
//
//        config.setDynamicHeaders(dynamicHeaders);

        config.setEnableLogging(true);

        ServicesContainer.configureService(config, GP_API_CONFIG_NAME);

        card = new CreditCardData();
        card.setNumber("4263970000005262");
        card.setExpMonth(expMonth);
        card.setExpYear(expYear);
        card.setCvn("123");
        card.setCardPresent(true);
    }

    @Test
    public void CreditAuthorization() throws ApiException {
        Transaction transaction =
                card
                        .authorize(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);
    }

    @Test
    public void CreditAuthorizationWithPaymentLinkId() throws ApiException {
        Transaction transaction =
                card
                        .authorize(amount)
                        .withCurrency(currency)
                        .withPaymentLinkId("LNK_W1xgWehivDP8P779cFDDTZwzL01EEw4")
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);
    }

    @Test
    public void CreditAuthorization_CaptureLowerAmount() throws ApiException {
        Transaction transaction =
                card
                        .authorize(5)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);

        Transaction capture =
                transaction
                        .capture(2.99)
                        .withGratuity(new BigDecimal(2))
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(capture, TransactionStatus.Captured);
    }

    @Test
    public void CreditAuthorization_CaptureHigherAmount() throws ApiException {
        Transaction transaction =
                card
                        .authorize(amount)
                        .withCurrency(currency)
                        .withAllowDuplicates(true)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);

        Transaction capture =
                transaction
                        .capture(amount.doubleValue() * 1.15)
                        .withGratuity(new BigDecimal(2))
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(capture, TransactionStatus.Captured);
    }

    @Test
    public void CreditAuthorization_CaptureHigherAmount_WithError() throws ApiException {
        Transaction transaction =
                card
                        .authorize(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);

        boolean exceptionCaught = false;
        try {
            transaction
                    .capture(amount.doubleValue() * 1.16)
                    .withGratuity(new BigDecimal(2))
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("50020", ex.getResponseText());
            assertEquals("INVALID_REQUEST_DATA", ex.getResponseCode());
            assertEquals("Status Code: 400 - Can't settle for more than 115% of that which you authorised ", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditAuthorizationAndCapture() throws ApiException {
        Transaction transaction =
                card
                        .authorize(new BigDecimal(14))
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);

        Transaction capture =
                transaction
                        .capture(new BigDecimal(16))
                        .withGratuity(new BigDecimal(2))
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(capture, TransactionStatus.Captured);
    }

    @Test
    public void CreditAuthorizationAndCapture_WithIdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        Transaction transaction =
                card
                        .authorize(amount)
                        .withCurrency(currency)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);

        boolean exceptionCaught = false;
        try {
            transaction
                    .capture(new BigDecimal(16))
                    .withIdempotencyKey(idempotencyKey)
                    .withGratuity(new BigDecimal(2))
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditAuthorization_WithIdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        Transaction transaction =
                card
                        .authorize(amount)
                        .withCurrency(currency)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Preauthorized);

        boolean exceptionCaught = false;
        try {
            card
                    .authorize(amount)
                    .withCurrency(currency)
                    .withIdempotencyKey(idempotencyKey)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditCaptureWrongId() throws ApiException {
        Transaction authorization = new Transaction();
        authorization.setTransactionId(UUID.randomUUID().toString());

        boolean exceptionCaught = false;
        try {
            authorization
                    .capture(amount)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("RESOURCE_NOT_FOUND", ex.getResponseCode());
            assertEquals("40008", ex.getResponseText());
            assertEquals(String.format("Status Code: 404 - Transaction %s not found at this location.", authorization.getTransactionId()), ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditSale() throws ApiException {
        Address address = new Address();
        address.setStreetAddress1("123 Main St.");
        address.setCity("Downtown");
        address.setState("NJ");
        address.setPostalCode("12345");

        Transaction response =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withAddress(address)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
        assertEquals(amount, response.getBalanceAmount());
    }

    @Test
    public void CreditSale_ONSuccess() throws ApiException {
        Transaction response =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withRequestMultiUseToken(true)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
        assertEquals(amount, response.getBalanceAmount());
        assertTrue(response.getToken().startsWith("PMT_"));

        String generatedToken = response.getToken();

        CreditCardData creditCardData = new CreditCardData();
        creditCardData.setToken(generatedToken);
        Transaction transaction =
                creditCardData
                        .charge(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);
    }

    @SneakyThrows
    @Test
    public void CreditSale_WithoutPermissions() {
        String[] permissions = new String[]{"TRN_POST_Capture"};

        GpApiConfig config = new GpApiConfig();

        // GP-API settings
        config
                .setAppId(APP_ID)
                .setAppKey(APP_KEY)
                .setPermissions(permissions)
                .setChannel(Channel.CardNotPresent.getValue());

        final String GP_API_CONFIG_NAME_WITHOUT_PERMISSIONS = "GpApiConfig_WithoutPermissions";

        ServicesContainer.configureService(config, GP_API_CONFIG_NAME_WITHOUT_PERMISSIONS);

        boolean exceptionCaught = false;
        try {
            card
                    .charge(new BigDecimal(19.99))
                    .withCurrency("USD")
                    .execute(GP_API_CONFIG_NAME_WITHOUT_PERMISSIONS);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("40212", ex.getResponseText());
            assertEquals("ACTION_NOT_AUTHORIZED", ex.getResponseCode());
            assertEquals("Status Code: 403 - Permission not enabled to execute action", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditSale_WithRequestMultiUseToken() throws ApiException {
        Transaction response =
                card.charge(amount)
                        .withCurrency(currency)
                        .withRequestMultiUseToken(true)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
        assertNotNull(response.getToken());
    }

    @Test
    public void CreditRefund() throws ApiException {
        Transaction response =
                card
                        .refund(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
    }

    @Test
    public void CreditRefundTransaction() throws ApiException {
        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        Transaction response =
                transaction
                        .refund(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
    }

    @Test
    public void CreditRefundTransaction_RefundLowerAmount() throws ApiException {
        Transaction transaction =
                card
                        .charge(new BigDecimal(5.95))
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        Transaction response =
                transaction
                        .refund(new BigDecimal(3.25))
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
    }

    @Test
    public void CreditRefundTransaction_Refund_AcceptedHigherAmount() throws ApiException {
        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        Transaction response =
                transaction
                        .refund(amount.doubleValue() * 1.1)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
    }

    @Test
    public void CreditRefundTransaction_RefundHigherAmount() throws ApiException {
        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withAllowDuplicates(true)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        boolean exceptionCaught = false;
        try {
            transaction
                    .refund(amount.doubleValue() * 1.2)
                    .withCurrency("USD")
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("40087", ex.getResponseText());
            assertEquals("INVALID_REQUEST_DATA", ex.getResponseCode());
            assertEquals("Status Code: 400 - You may only refund up to 115% of the original amount ", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditRefundTransaction_WithIdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        boolean exceptionCaught = false;
        try {
            transaction
                    .refund(amount)
                    .withIdempotencyKey(idempotencyKey)
                    .withCurrency(currency)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditRefundTransactionWrongId() throws ApiException {
        Transaction charge = new Transaction();
        charge.setTransactionId(UUID.randomUUID().toString());

        boolean exceptionCaught = false;
        try {
            charge
                    .refund(amount)
                    .withCurrency(currency)
                    .execute(GP_API_CONFIG_NAME);

        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("RESOURCE_NOT_FOUND", ex.getResponseCode());
            assertEquals("40008", ex.getResponseText());
            assertEquals(String.format("Status Code: 404 - Transaction %s not found at this location.", charge.getTransactionId()), ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditReverseTransaction() throws ApiException {
        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        Transaction reverse =
                transaction
                        .reverse(amount)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(reverse, TransactionStatus.Reversed);
    }

    @Test
    public void CreditReverseTransaction_WithIdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        boolean exceptionCaught = false;
        try {
            transaction
                    .reverse(amount)
                    .withIdempotencyKey(idempotencyKey)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditReverseTransactionWrongId() throws ApiException {
        Transaction charge = new Transaction();
        charge.setTransactionId(UUID.randomUUID().toString());

        boolean exceptionCaught = false;
        try {
            charge
                    .reverse(amount)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("RESOURCE_NOT_FOUND", ex.getResponseCode());
            assertEquals("40008", ex.getResponseText());
            assertEquals(String.format("Status Code: 404 - Transaction %s not found at this location.", charge.getTransactionId()), ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditPartialReverseTransaction() throws ApiException {
        Transaction transaction =
                card
                        .charge(new BigDecimal(3.99))
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        boolean exceptionCaught = false;
        try {
            transaction
                    .reverse(new BigDecimal(1.29))
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("INVALID_REQUEST_DATA", ex.getResponseCode());
            assertEquals("40214", ex.getResponseText());
            assertEquals("Status Code: 400 - partial reversal not supported", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditAuthorizationForMultiCapture() throws ApiException {
        Transaction authorization =
                card
                        .authorize(new BigDecimal(14))
                        .withCurrency(currency)
                        .withMultiCapture(true)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(authorization, TransactionStatus.Preauthorized);
        assertTrue(authorization.isMultiCapture());

        Transaction capture1 =
                authorization
                        .capture(new BigDecimal(3))
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(capture1, TransactionStatus.Captured);

        Transaction capture2 =
                authorization.capture(new BigDecimal(5))
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(capture2, TransactionStatus.Captured);

        Transaction capture3 =
                authorization
                        .capture(new BigDecimal(7))
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(capture3, TransactionStatus.Captured);
    }

    @Test
    public void CreditVerify() throws ApiException {
        Transaction response =
                card
                        .verify()
                        .withCurrency(currency)
                        .execute(GP_API_CONFIG_NAME);

        assertNotNull(response);
        assertEquals(SUCCESS, response.getResponseCode());
        assertEquals(VERIFIED, response.getResponseMessage());
    }

    @Test
    public void CreditVerify_With_Address() throws ApiException {
        Address address = new Address();
        address.setPostalCode("WB3 A21");
        address.setStreetAddress1("Flat 456");

        Transaction response =
                card
                        .verify()
                        .withCurrency(currency)
                        .withAddress(address)
                        .execute(GP_API_CONFIG_NAME);

        assertNotNull(response);
        assertEquals(SUCCESS, response.getResponseCode());
        assertEquals(VERIFIED, response.getResponseMessage());
        assertNotNull(response.getTransactionId());
    }

    @Test
    public void CreditVerify_WithIdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        Transaction response =
                card
                        .verify()
                        .withCurrency(currency)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(GP_API_CONFIG_NAME);

        assertNotNull(response);
        assertEquals(SUCCESS, response.getResponseCode());
        assertEquals(VERIFIED, response.getResponseMessage());

        boolean exceptionCaught = false;
        try {
            card
                    .verify()
                    .withCurrency(currency)
                    .withIdempotencyKey(idempotencyKey)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditVerify_Without_Currency() throws ApiException {
        boolean exceptionCaught = false;
        try {
            card
                    .verify()
                    .execute(GP_API_CONFIG_NAME);

        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("40005", ex.getResponseText());
            assertEquals("MANDATORY_DATA_MISSING", ex.getResponseCode());
            assertEquals("Status Code: 400 - Request expects the following fields currency", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditVerify_InvalidCVV() throws ApiException {
        card.setCvn("1234");
        boolean exceptionCaught = false;
        try {
            card
                    .verify()
                    .withCurrency(currency)
                    .execute(GP_API_CONFIG_NAME);

        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("40085", ex.getResponseText());
            assertEquals("INVALID_REQUEST_DATA", ex.getResponseCode());
            assertEquals("Status Code: 400 - Security Code/CVV2/CVC must be 3 digits", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditVerify_NotNumericCVV() throws ApiException {
        card.setCvn("SMA");
        boolean exceptionCaught = false;
        try {
            card
                    .verify()
                    .withCurrency(currency)
                    .execute(GP_API_CONFIG_NAME);

        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("50018", ex.getResponseText());
            assertEquals("SYSTEM_ERROR_DOWNSTREAM", ex.getResponseCode());
            assertEquals("Status Code: 502 - The line number 12 which contains '         [number] XXX [/number] ' does not conform to the schema", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditChargeTransactions_WithSameIdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        boolean exceptionCaught = false;
        try {
            card
                    .charge(amount)
                    .withCurrency(currency)
                    .withIdempotencyKey(idempotencyKey)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CreditChargeTransactions_WithDifferentIdempotencyKey() throws ApiException {
        Transaction transaction =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withIdempotencyKey(UUID.randomUUID().toString())
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(transaction, TransactionStatus.Captured);

        Transaction response =
                card
                        .charge(amount)
                        .withCurrency(currency)
                        .withIdempotencyKey(UUID.randomUUID().toString())
                        .execute(GP_API_CONFIG_NAME);
        assertTransactionResponse(response, TransactionStatus.Captured);
    }

    @Test
    public void CreditSale_ExpiryCard() throws ApiException {
        card.setExpYear(DateTime.now().getYear() - 1);

        boolean exceptionCaught = false;
        try {
            card
                    .charge(new BigDecimal(14))
                    .withCurrency("USD")
                    .withAllowDuplicates(true)
                    .execute(GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("INVALID_REQUEST_DATA", ex.getResponseCode());
            assertEquals("40085", ex.getResponseText());
            assertEquals("Status Code: 400 - Expiry date invalid", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    private void assertTransactionResponse(Transaction transaction, TransactionStatus transactionStatus) {
        assertNotNull(transaction);
        assertEquals(SUCCESS, transaction.getResponseCode());
        assertEquals(transactionStatus.getValue(), transaction.getResponseMessage());
    }

}
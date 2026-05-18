package com.mysawit.pembayaran.client;

public interface XenditClient {

    PaymentInvoice createInvoice(String externalId,
                                 double amountRupiah,
                                 String description,
                                 String successRedirectUrl,
                                 String failureRedirectUrl);
}

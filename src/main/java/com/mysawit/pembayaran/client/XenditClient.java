package com.mysawit.pembayaran.client;

import java.util.Map;

public interface XenditClient {

    Map<String, Object> createInvoice(String externalId,
                                      double amountRupiah,
                                      String description,
                                      String successRedirectUrl,
                                      String failureRedirectUrl);
}

package com.mysawit.pembayaran.client;

import java.math.BigDecimal;
import java.util.Map;

public interface XenditClient {

    Map<String, Object> createInvoice(String externalId,
                                      BigDecimal amountRupiah,
                                      String description,
                                      String successRedirectUrl,
                                      String failureRedirectUrl);
}

package com.fix.gmall.service;

import com.fix.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    public void savePaymentInfo(PaymentInfo paymentInfo);

    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfo);

    public boolean refund(String orderId) ;

    public Map createNative(String orderId, String money);
}

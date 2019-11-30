package com.fix.gmall.service;

import com.fix.gmall.bean.OrderInfo;

public interface OrderService {
    String saveOrder(OrderInfo orderInfo);

    String getTradeNo(String userId);

    boolean checkTradeCode(String userId, String tradeNo);

    void delTradeCode(String userId);

    boolean checkStock(String skuId, Integer skuNum);

    OrderInfo getOrderInfo(String orderId);
}

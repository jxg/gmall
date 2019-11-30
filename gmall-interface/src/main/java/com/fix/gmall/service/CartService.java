package com.fix.gmall.service;

import com.fix.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    void addToCart(String skuId, String userId, int parseInt);

    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    void checkCart(String skuId, String isChecked, String userId);

    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 从数据库重新加载用户购物车的数据到redis缓存中
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}

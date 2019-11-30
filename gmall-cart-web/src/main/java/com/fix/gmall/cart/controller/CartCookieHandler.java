package com.fix.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fix.gmall.bean.CartInfo;
import com.fix.gmall.bean.SkuInfo;
import com.fix.gmall.config.CookieUtil;
import com.fix.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {
    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE = 7 * 24 * 3600;

    @Reference
    private ManageService manageService;


    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        // 声明一个集合
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 如果没有，则直接添加到集合！记住一个boolean 类型变量处理！
        boolean ifExist = false;
        // 判断cookieValue不能为空！
        if (StringUtils.isNotEmpty(cookieValue)) {
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            for (CartInfo cartInfo : cartInfoList) {
                if (cartInfo.getSkuId().equals(skuId)) {
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExist = true;
                }
            }

        }
        if (!ifExist) {
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            // 添加集合
            cartInfoList.add(cartInfo);
        }

        CookieUtil.setCookie(request, response, cookieCartName, JSON.toJSONString(cartInfoList), COOKIE_CART_MAXAGE, true);

    }


    public List<CartInfo> getCartList(HttpServletRequest request) {
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        // 声明一个集合

        if (StringUtils.isNotEmpty(cookieValue)){
            List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            return cartInfoList;
        }

        return null;
    }

    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {

        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {

        List<CartInfo> cartList = getCartList(request);
        for(CartInfo cartInfo : cartList) {
            if(skuId.equals(cartInfo.getSkuId())){
                cartInfo.setIsChecked(isChecked);
            }
        }

        CookieUtil.setCookie(request,response,cookieCartName, JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);

    }
}

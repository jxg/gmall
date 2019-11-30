package com.fix.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fix.gmall.bean.*;
import com.fix.gmall.config.LoginRequire;
import com.fix.gmall.service.CartService;
import com.fix.gmall.service.ManageService;
import com.fix.gmall.service.OrderService;
import com.fix.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserService userService;
    @Reference
    CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;


    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);
        request.setAttribute("userAddressList",userAddressList);


        // 展示送货清单：
        // 数据来源：勾选的购物车！user:userId:checked！
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        // 声明一个集合来存储订单明细
        ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();
        // 将集合数据赋值OrderDetail
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());

            orderDetailArrayList.add(orderDetail);
        }

        // 总金额：
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailArrayList);
        // 调用计算总金额的方法  {totalAmount}
        orderInfo.sumTotalAmount();

        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        // 保存送货清单集合
        request.setAttribute("orderDetailArrayList",orderDetailArrayList);
        //防止表单重复提交
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);
        // 返回一个视图名称叫index.html
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo) {
        String userId = (String) request.getAttribute("userId");
        // orderInfo 中还缺少一个userId
        orderInfo.setUserId(userId);
        // 判断是否是重复提交
        // 先获取页面的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 调用比较方法
        boolean result = orderService.checkTradeCode(userId, tradeNo);
        if(!result){
            request.setAttribute("errMsg","订单已提交，不能重复提交！");
            return "tradeFail";
        }
        // 验证库存！ /hasStock?skuId=10221&num=2
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean flag = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!flag){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"商品库存不足！");
                return "tradeFail";
            }

            // 获取skuInfo 对象b
            SkuInfo skuInfo =	manageService.getSkuInfo(orderDetail.getSkuId());
            //
            int res = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if (res!=0){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"价格不匹配");
                cartService.loadCartCache(userId);
                return "tradeFail";
            }
        }

            // 调用服务层
        String orderId = orderService.saveOrder(orderInfo);
        // 删除流水号
        orderService.delTradeCode(userId);
        // 支付
        return "redirect://payment.gmall.com/index?orderId="+orderId;
       }




    }

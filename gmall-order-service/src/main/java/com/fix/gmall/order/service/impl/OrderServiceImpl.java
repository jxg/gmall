package com.fix.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.fix.gmall.bean.OrderDetail;
import com.fix.gmall.bean.OrderInfo;
import com.fix.gmall.bean.enums.OrderStatus;
import com.fix.gmall.bean.enums.ProcessStatus;
import com.fix.gmall.config.RedisUtil;
import com.fix.gmall.order.mapper.OrderDetailMapper;
import com.fix.gmall.order.mapper.OrderInfoMapper;
import com.fix.gmall.service.OrderService;
import com.fix.gmall.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class OrderServiceImpl  implements OrderService {
    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        String outTradeNo = "FIX"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 创建时间
        orderInfo.setCreateTime(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfoMapper.insertSelective(orderInfo);

        List<OrderDetail> orderDetailList  = orderInfo.getOrderDetailList();
        for(OrderDetail orderDetail : orderDetailList){
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }


        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNO = UUID.randomUUID().toString();
        jedis.set("userID:"+userId+":tradeNo",tradeNO);
        jedis.close();
        return tradeNO;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeNo) {
        Jedis jedis = redisUtil.getJedis();
        String tradeNoCache = jedis.get("userID:" + userId + ":tradeNo");
        if(tradeNo.equals(tradeNoCache)){
            return true;
        }
        jedis.close();
        return false;
    }

    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        jedis.del("userID:" + userId + ":tradeNo");
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        String s = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(s);
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        // select * from orderInf where id = orderId
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));

        return orderInfo;
    }
}

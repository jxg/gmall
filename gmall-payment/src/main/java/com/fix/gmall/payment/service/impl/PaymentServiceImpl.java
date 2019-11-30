package com.fix.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.fix.gmall.bean.OrderInfo;
import com.fix.gmall.bean.PaymentInfo;
import com.fix.gmall.payment.mapper.PaymentInfoMapper;
import com.fix.gmall.service.OrderService;
import com.fix.gmall.service.PaymentService;
import com.fix.gmall.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl  implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Reference
    OrderService orderService;
    @Autowired
    AlipayClient alipayClient;
    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;



    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        // select * from paymentInfo where out_trade_no =?
        return   paymentInfoMapper.selectOne(paymentInfoQuery);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);

    }

    @Override
    public boolean refund(String orderId) {
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("out_trade_no",orderInfo.getOutTradeNo());
        paramMap.put("refund_amount",orderInfo.getTotalAmount());
        paramMap.put("refund_reason","正常退款");
        request.setBizContent(JSON.toJSONString(paramMap));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    @Override
    public Map createNative(String orderId, String money){
        HashMap<String, String> map = new HashMap<>();
        map.put("appid",appid);
        map.put("mch_id",partner);
        map.put("nonce_str", WXPayUtil.generateNonceStr());
        map.put("out_trade_no",orderId);
        map.put("spbill_create_ip","127.0.0.1");
        map.put("total_fee",money);
        map.put("notify_url","http://www.weixin.qq.com/wxpay/pay.php");
        map.put("trade_type","NATIVE");

        try {
            String xmlParam = WXPayUtil.generateSignature(map, partnerkey);
            // 导入工具类：项目中
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(xmlParam);
            httpClient.post();
            // 获取结果：将结果集放入map 中！
            Map<String, String> resultMap=new HashMap<>();
            // 将结果集转换为map

            String result = httpClient.getContent();
            Map<String, String> xmlToMap = WXPayUtil.xmlToMap(result);
            resultMap.put("code_url",xmlToMap.get("code_url"));
            resultMap.put("total_fee",money);
            resultMap.put("out_trade_no",orderId);
            // 将结果返回控制器
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

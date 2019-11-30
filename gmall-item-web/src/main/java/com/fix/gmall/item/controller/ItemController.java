package com.fix.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fix.gmall.bean.SkuInfo;
import com.fix.gmall.bean.SkuSaleAttrValue;
import com.fix.gmall.bean.SpuSaleAttr;
import com.fix.gmall.config.LoginRequire;
import com.fix.gmall.service.ListService;
import com.fix.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {
    @Reference
    ManageService manageService;
    @Reference
    ListService listService;

    //@LoginRequire
    @RequestMapping("{skuId}.html")
    public String getSkuInfo(@PathVariable String skuId, HttpServletRequest request){

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);

        // 查询销售属性，销售属性值集合 spuId，skuId
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);



        // 获取spu下所有的sku销售属性值Id
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        String key = "";
        HashMap<String, Object> map = new HashMap<>();
        // 普通循环

        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
            // 什么时候停止拼接 当本次循环的skuId 与 下次循环的skuId 不一致的时候！停止拼接。拼接到最后则停止拼接！？
            // 什么时候加|

            // 第一次拼接 key=118
            // 第二次拼接 key=118|
            // 第三次拼接 key=118|120 放入map 中 ，并清空key
            // 第四次拼接 key=119
            if (key.length()>0){
                key+="|";
            }
            key+= skuSaleAttrValue.getSaleAttrValueId();
            if ((i+1)== skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals( skuSaleAttrValueList.get(i+1).getSkuId())){
                // 放入map集合
                map.put(key,skuSaleAttrValue.getSkuId());
                // 并且清空key
                key="";
            }
        }
        // 将map 转换为json 字符串
        String valuesSkuJson  = JSON.toJSONString(map);
        System.out.println("拼接Json：="+valuesSkuJson );

        // 保存json
        request.setAttribute("valuesSkuJson",valuesSkuJson);
        listService.incrHotScore(skuId);
        return "item";

    }
}

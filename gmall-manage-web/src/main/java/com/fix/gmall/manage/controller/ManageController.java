package com.fix.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fix.gmall.bean.*;
import com.fix.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {
    @Reference
    ManageService manageService;


    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
       // System.out.println(manageService.getCatalog1());
        return manageService.getCatalog1();
    }
    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        return manageService.getCatalog2(catalog1Id);
    }

    // http://localhost:8082/getCatalog3?catalog2Id=37
    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }


    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        System.out.println("ni");
        return manageService.getAttrList(catalog3Id);
    }

    // 将前台页面传递过来的json 数据转换为对象
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        // 传递的什么
        manageService.saveAttrInfo(baseAttrInfo);
    }
    @RequestMapping("getAttrValueList")
    public  List<BaseAttrValue> getAttrValueList(String attrId){
        // 先通过attrId 查询平台属性 select * from baseAttrInfo where id = attrId
        BaseAttrInfo baseAttrInfo =  manageService.getAttrInfo(attrId);
        // 返回平台属性中的平台属性值集合baseAttrInfo.getAttrValueList();
        return baseAttrInfo.getAttrValueList();

    }

    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }



}

package com.fix.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;

import com.fix.gmall.bean.SkuInfo;
import com.fix.gmall.bean.SkuLsInfo;
import com.fix.gmall.bean.SpuImage;
import com.fix.gmall.bean.SpuSaleAttr;
import com.fix.gmall.service.ListService;
import com.fix.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController  //    @ResponseBody+@Controller
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

//    @RequestMapping("spuImageList")
//    public List<SpuImage> spuImageList(String spuId){
//
//    }
//    http://localhost:8082/spuImageList?spuId=58
    @RequestMapping("spuImageList")
    public List<SpuImage> spuImageList(SpuImage spuImage){
        // 调用service 层
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuImage);
        return spuImageList;
    }

    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        // 调用service 层
        return manageService.getSpuSaleAttrList(spuId);
    }
    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        if (skuInfo!=null){
            manageService.saveSkuInfo(skuInfo);
        }

    }

    @RequestMapping("onSale")
    public  void onSale(String skuId){
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        BeanUtils.copyProperties(skuInfo,skuLsInfo);


       listService.saveSkuLsInfo(skuLsInfo);

    }
}

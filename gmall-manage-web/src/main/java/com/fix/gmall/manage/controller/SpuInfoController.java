package com.fix.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fix.gmall.bean.SpuInfo;
import com.fix.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class SpuInfoController {
    @Reference
     ManageService manageService;
    @RequestMapping("spuList")
    public List<SpuInfo> spuList(SpuInfo spuInfo){
        return manageService.getSpuList(spuInfo);
    }

    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){

        if (spuInfo!=null){
            // 调用保存
            manageService.saveSpuInfo(spuInfo);
        }


    }
}

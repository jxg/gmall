package com.fix.gmall.service;


import com.fix.gmall.bean.SkuLsInfo;
import com.fix.gmall.bean.SkuLsParams;
import com.fix.gmall.bean.SkuLsResult;

public interface ListService {

    /**
     * 保存数据到es 中！
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 检索数据
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);


    public void incrHotScore(String skuId);
}

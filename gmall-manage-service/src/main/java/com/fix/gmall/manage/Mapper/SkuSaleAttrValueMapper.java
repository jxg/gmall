package com.fix.gmall.manage.Mapper;

import com.fix.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue>{
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}

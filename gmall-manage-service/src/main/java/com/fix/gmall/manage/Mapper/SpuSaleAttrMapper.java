package com.fix.gmall.manage.Mapper;

import com.fix.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    public List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String supId,String skuId);
}

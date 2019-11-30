package com.fix.gmall.cart.mapper;

import com.fix.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.BaseMapper;

import java.util.List;

public interface CartInfoMapper extends BaseMapper<CartInfo> {
    List<CartInfo> selectCartListWithCurPrice(String userId);
}

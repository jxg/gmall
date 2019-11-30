package com.fix.gmall.service;



import com.fix.gmall.bean.UserAddress;
import com.fix.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {
    /**
     * 查询所有数据
     * @return
     */
    List<UserInfo> findAll();

    /**
     * 根据userId 查询用户地址列表
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressList(String userId);


    UserInfo login(UserInfo userInfo);

    UserInfo verify(String userId);
}

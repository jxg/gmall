package com.fix.gmall.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.fix.gmall.bean.UserAddress;
import com.fix.gmall.bean.UserInfo;
import com.fix.gmall.config.RedisUtil;
import com.fix.gmall.service.UserService;
import com.fix.gmall.user.mapper.UserAddressMapper;
import com.fix.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.List;
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;


    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        // 调用mapper
        // select * from userAddress where userId=?
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return    userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String passwd = userInfo.getPasswd();
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPwd);

        UserInfo info = userInfoMapper.selectOne(userInfo);
        if(info != null){
            Jedis jedis = redisUtil.getJedis();
            String userKey = userKey_prefix+info.getId()+userinfoKey_suffix;

            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));
             jedis.close();
             return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            // 定义key
            String userKey = userKey_prefix+userId+userinfoKey_suffix;

            String userJson = jedis.get(userKey);
            if(!StringUtils.isEmpty(userJson)){
                UserInfo userInfo = JSON.parseObject(userJson,UserInfo.class);
                return userInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
        }
        return null;
    }
}

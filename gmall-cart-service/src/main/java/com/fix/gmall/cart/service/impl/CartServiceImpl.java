package com.fix.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.fix.gmall.bean.CartInfo;
import com.fix.gmall.bean.SkuInfo;
import com.fix.gmall.cart.constant.CartConst;
import com.fix.gmall.cart.mapper.CartInfoMapper;
import com.fix.gmall.config.RedisUtil;
import com.fix.gmall.service.CartService;
import com.fix.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {
    @Reference
    ManageService manageService;
    @Autowired
    CartInfoMapper cartInfoMapper;
    @Autowired
    RedisUtil redisUtil;
    //登录用户的购物车信息存放在redis中
    @Override
    public void addToCart(String skuId, String userId, int num) {
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        CartInfo cartInfo  = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuId(skuId);

        CartInfo selectOne = cartInfoMapper.selectOne(cartInfo);
        if(selectOne != null){
            selectOne.setSkuNum(selectOne.getSkuNum()+ num);
            selectOne.setSkuPrice(selectOne.getCartPrice());
            cartInfoMapper.updateByPrimaryKeySelective(selectOne);
        }else{
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            // 属性赋值
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(num);
            cartInfoMapper.insertSelective(cartInfo1);
            selectOne = cartInfo1;

        }
        jedis.hset(cartKey,skuId, JSON.toJSONString(selectOne));
        // 得到用户的key key=user:userId:info
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userKey);
        jedis.expire(cartKey,ttl.intValue());
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> hvals = jedis.hvals(cartKey);
        List<CartInfo> list = new ArrayList<>();
        if (hvals!=null && hvals.size()>0) {
            for (String cartInfoStr : hvals) {

                CartInfo cartInfo1 = JSON.parseObject(cartInfoStr, CartInfo.class);
                list.add(cartInfo1);
            }
            list.sort((o1,o2) ->{
                return o1.getId().compareTo(o2.getId());
            });

            return list;
        }else{
            list = loadCartCache(userId);
              return list;
        }

    }

     @Override
    public List<CartInfo> loadCartCache(String userId) {
        // select * from cartInfo where userId = ? 不可取！查询不到实时价格！
        // cartInfo , skuInfo 从这两张表中查询！
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoList==null || cartInfoList.size()==0){
            return  null;
        }
        Map<String,String>  jsonMap = new HashMap<>();
        for(CartInfo cartInfo : cartInfoList){
            jsonMap.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }

        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        jedis.hmset(cartKey,jsonMap);
        jedis.close();


        return cartInfoList;

    }

    /**
     * 合并购物车，把cookie 里面的商品廷加到数据库里，然后数据库最新购物车数据，加载到redis中
     * 合并商品的数量以及选中状态，状态的合并以cookie为准
     * @param cartListCK
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        for(CartInfo cartInfoCK : cartListCK){
            boolean isMatched = false;
            for(CartInfo cartInfoDB : cartInfoList){
                if(cartInfoDB.getSkuId().equals( cartInfoCK.getSkuId())){
                    cartInfoDB.setSkuNum(cartInfoCK.getSkuNum()+cartInfoDB.getSkuNum());
                    cartInfoDB.setIsChecked(cartInfoCK.getIsChecked());
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatched  = true;
                }
            }

            if(isMatched == false){
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }

        List<CartInfo> cartInfoAllList = loadCartCache(userId);
        return cartInfoAllList;
    }

    /**
     * 购物车被选中时候，固定
     * 以及新建一个选中的购物车，用于结算的时候需要
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        System.out.println("checked.......");
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //用户登录时候
        String cartInfoJson = jedis.hget(cartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartInfoJson,CartInfo.class);
        cartInfo.setIsChecked(isChecked);

        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
        String cartKeyChecked = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if(isChecked.equals("1")){
            jedis.hset(cartKeyChecked,skuId,JSON.toJSONString(cartInfo));
        }else{
            jedis.hdel(cartKeyChecked,skuId);
        }
         jedis.close();
    }


    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String cartKeyChecked = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> hvals = jedis.hvals(cartKeyChecked);
        List<CartInfo> cartInfoList = new ArrayList<>();
        for(String cartJson : hvals){
            cartInfoList.add(JSON.parseObject(cartJson,CartInfo.class));
        }
        jedis.close();
        return cartInfoList;
    }
}

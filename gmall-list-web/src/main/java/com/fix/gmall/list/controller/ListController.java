package com.fix.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fix.gmall.bean.BaseAttrInfo;
import com.fix.gmall.bean.BaseAttrValue;
import com.fix.gmall.bean.SkuLsParams;
import com.fix.gmall.bean.SkuLsResult;
import com.fix.gmall.service.ListService;
import com.fix.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    // http://list.gmall.com/list.html?catalog3Id=61
    @RequestMapping("list.html")

    public String listData(SkuLsParams skuLsParams, HttpServletRequest request) {

        // 设置每页显示的数据条数
        skuLsParams.setPageSize(1);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
        request.setAttribute("skuLsInfoList",skuLsResult.getSkuLsInfoList());

        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> baseAttrInfoList = null;
        // 定义一个面包屑集合
        List<BaseAttrValue> baseAttrValueList = new ArrayList<>();
        if(attrValueIdList != null && attrValueIdList.size() > 0) {
            baseAttrInfoList = manageService.getAttrList(attrValueIdList);

        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            // 平台属性
            BaseAttrInfo baseAttrInfo = iterator.next();
            // 获取平台属性值集合对象
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 获取skuLsParams.getValueId() 循环对比
                if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
                    for (String valueId : skuLsParams.getValueId()) {
                        if (valueId.equals(baseAttrValue.getId())) {
                            // 如果平台属性值id 相同，则将数据移除！
                            iterator.remove();
                            BaseAttrValue baseAttrValueed = new BaseAttrValue();
                            // 将平台属性值的名称改为了面包屑
                            baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            // 将用户点击的平台属性值Id 传递到makeUrlParam 方法中，
                            // 重新制作返回的url 参数！ 组合成面包屑的urL
                            String newUrlparam = makeUrlParam(skuLsParams, valueId);
                            // 重新制作返回的url 参数！
                            baseAttrValueed.setUrlParam(newUrlparam);
                            // 将baseAttrValueed 放入集合中
                            baseAttrValueList.add(baseAttrValueed);
                        }
                    }

                }
            }
        }
        }

        // 保存分页的数据：
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        request.setAttribute("totalPages",skuLsResult.getTotalPages());

        request.setAttribute("baseAttrInfoList",baseAttrInfoList);

        String urlParam = makeUrlParam(skuLsParams);
                // 保存到作用域
        request.setAttribute("urlParam",urlParam);

        // 保存一个检索的关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());

        // 保存一个面包屑：
        request.setAttribute("baseAttrValueList",baseAttrValueList);

        return "list";

    }

    private String makeUrlParam(SkuLsParams skuLsParams,String ... excludeValueIds) {
        String urlParam = "";
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam +="keyword="+skuLsParams.getKeyword();
        }
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0) {
            if(urlParam.length() > 0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();

        }

        if (skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (String valueId : skuLsParams.getValueId()) {

                if (excludeValueIds!=null && excludeValueIds.length>0){
                    // 获取点击面包屑时的平台属性值Id
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)){
                        // break;
                        // continue;
                        continue;
                    }
                }
                // 如果有多个参数则拼接&符号
                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }


        return urlParam;


        }
}

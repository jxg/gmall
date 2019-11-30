package com.fix.gmall.config;

import com.alibaba.fastjson.JSON;
import com.fix.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getParameter("newToken");
        if(token != null){
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }

        if(token == null){
            token = CookieUtil.getCookieValue(request,"token",false);
        }

        if(token != null){
            Map map = getUserMapByToken(token);
            // 取出用户昵称
            String nickName = (String) map.get("nickName");
            // 保存到作用域
            request.setAttribute("nickName",nickName);
        }

       HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire loginRequire = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if(loginRequire !=null ){

           String salt = request.getHeader("X-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if ("success".equals(result)){
                // 登录，认证成功！
                // 保存一下userId
                // 开始解密token 获取nickName
                Map map = getUserMapByToken(token);
                // 取出userId
                String userId = (String) map.get("userId");
                // 保存到作用域
                request.setAttribute("userId",userId);
                return true;
            }else {
                // 认证失败！并且 methodAnnotation.autoRedirect()=true; 必须登录
                if (loginRequire.autoRedirect()) {
                      String requestURL = request.getRequestURL().toString();
                    System.out.println("requestURL: "+requestURL);
                    String encodeURL = URLEncoder.encode(requestURL,"UTF-8");
                    System.out.println("encodeURL："+encodeURL); //  http%3A%2F%2Fitem.gmall.com%2F36.html

                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }
            }


        }

        return true;
    }

    private Map getUserMapByToken(String token) {
        String tokenUserInfo = StringUtils.substringBetween(token,".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] decode = base64UrlCodec.decode(tokenUserInfo);
        String mapJson = null;
        try {
             mapJson = new String(decode, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return JSON.parseObject(mapJson,Map.class);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
    }
}

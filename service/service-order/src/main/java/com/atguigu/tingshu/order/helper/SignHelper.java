package com.atguigu.tingshu.order.helper;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class SignHelper {

    private static String signKey = "atguigu123";
    /**
     * 验签方法
     * @param parameterMap
     */
    public static void checkSign(Map<String, Object> parameterMap){
        //校验签名时间
        //获取签名生成的时间戳
        Long remoteTimestamp = (Long)parameterMap.get("timestamp");
        //判断是否为空
        if(StringUtils.isEmpty(remoteTimestamp)){
            throw new GuiguException(ResultCodeEnum.SIGN_ERROR);
        }
        //获取当前时间戳
        long currentTimestamp = getTimestamp();
        //超时判断
        if (Math.abs(currentTimestamp - remoteTimestamp) > 500000) {
            log.error("签名已过期，服务器当前时间:{}", currentTimestamp);
            throw new GuiguException(ResultCodeEnum.SIGN_OVERDUE);
        }

        //校验签名
        //获取原来的签名
        String signRemote = (String)parameterMap.get("sign");

        //获取当前加密后的签名
        String signLocal = getSign(parameterMap);
        //如果之前为空，抛出异常
        if(StringUtils.isEmpty(signRemote)){
            throw new GuiguException(ResultCodeEnum.SIGN_ERROR);
        }
        //校验两个签名是否一致
        if(!signRemote.equals(signLocal)){
            throw new GuiguException(ResultCodeEnum.SIGN_ERROR);
        }
    }

    /**
     * 请求数据获取签名
     * @param parameterMap
     * @return
     */
    public static String getSign(Map<String, Object> parameterMap) {
        //去掉sign参数
        if(parameterMap.containsKey("sign")) {
            parameterMap.remove("sign");
        }

        //有序
        TreeMap<String, Object> sorted = new TreeMap<>(parameterMap);
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, Object> param : sorted.entrySet()) {
            //获取键值对中的值
            str.append(param.getValue()).append("|");
        }
        //最后连接signKey
        str.append(signKey);
        log.info("加密前：" + str.toString());
        String md5Str = MD5.encrypt(str.toString());//不可逆加密算法
        log.info("加密后：" + md5Str);
        return md5Str;
    }

    /**
     * 获取时间戳
     * @return
     */
    public static long getTimestamp() {
        return new Date().getTime();
    }

}

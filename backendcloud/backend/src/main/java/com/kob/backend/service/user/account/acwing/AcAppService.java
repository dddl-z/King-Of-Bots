package com.kob.backend.service.user.account.acwing;

import com.alibaba.fastjson.JSONObject;

public interface AcAppService {
    JSONObject applyCode(); // 返回需要用的参数
    JSONObject receiveCode(String code, String state); // 临时密钥，防止攻击的随机数字，接收第三方发送的结果
}

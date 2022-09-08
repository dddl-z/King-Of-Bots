package com.kob.backend.service.impl.user.account.acwing;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.user.account.acwing.utils.HttpClientUtil;
import com.kob.backend.service.user.account.acwing.WebService;
import com.kob.backend.utils.JwtUtil;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@Service
public class WebServiceImpl implements WebService {

    private final static String appId = "2883";
    private final static String appSecret = "9edca85d87cb486fa2adbc447f5219e9";
    private final static String redirectUrl = "https://app2883.acapp.acwing.com.cn/user/account/acwing/web/receive_code/"; // 回调函数的URL
    private final static String applyAccessTokenUrl = "https://www.acwing.com/third_party/api/oauth2/access_token/"; // 向acwing申请授权令牌和用户openid的Url
    private final static String getUserInfoUrl = "https://www.acwing.com/third_party/api/meta/identity/getinfo/"; // 向acwing申请用户信息的Url
    private final static Random random = new Random();

    @Autowired
    private UserMapper userMapper; // 需要查询数据库里的openid

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // 注入redis的接口

    @Override
    public JSONObject applyCode() {
        JSONObject resp = new JSONObject();

        String encodeUrl = ""; // 将redirectUrl编码
        try {
            encodeUrl = URLEncoder.encode(redirectUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            resp.put("result", "failed");
            return resp;
        }

        // 为state赋予随机串，并加到redis中，且设置过期时间
        StringBuilder state = new StringBuilder();
        for (int i = 0; i < 10; i ++) {
            state.append((char) (random.nextInt(10) + '0'));
        }
        resp.put("result", "success");
        redisTemplate.opsForValue().set(state.toString(), "true"); // 将state存入redis
        redisTemplate.expire(state.toString(), Duration.ofMinutes(10)); // 设置过期时间，这里是10分钟

        // Url是向acwing申请授权码，在这里添加参数
        String applyCodeUrl = "https://www.acwing.com/third_party/api/oauth2/web/authorize/?appid=" + appId
                + "&redirect_uri=" + encodeUrl
                + "&scope=userinfo"
                + "&state=" + state;
        resp.put("apply_code_url", applyCodeUrl); // 向acwing申请授权码

        return resp;
    }

    @Override
    public JSONObject receiveCode(String code, String state) {
        JSONObject resp = new JSONObject();
        resp.put("result", "failed");
        if (code == null || state == null) {
            return resp;
        }

        // 验证state是否在redis里存在，不存在就失败，成功就删掉state保证安全
        if (Boolean.FALSE.equals(redisTemplate.hasKey(state))) {
            return resp;
        }
        redisTemplate.delete(state);

        // 成功拿到授权码之后，向acwing申请授权令牌和用户openid
        List<NameValuePair> nameValuePairs = new LinkedList<>(); // 申请授权令牌和用户openid的Url需要的参数列表
        nameValuePairs.add(new BasicNameValuePair("appid", appId));
        nameValuePairs.add(new BasicNameValuePair("secret", appSecret));
        nameValuePairs.add(new BasicNameValuePair("code", code));

        // 通过向acwing申请授权令牌和用户openid的Url获得结果
        String getString = HttpClientUtil.get(applyAccessTokenUrl, nameValuePairs);
        if (getString == null) {
            return resp;
        }
        JSONObject getResp = JSONObject.parseObject(getString); // 将结果解析成JSON

        // 从JSON里获取授权令牌和用户的openid
        String accessToken = getResp.getString("access_token");
        String openid = getResp.getString("openid");
        if (accessToken == null || openid == null) {
            return resp;
        }

        // 在数据库里判断获取到的openid是否已经存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(); // 定义查询条件
        queryWrapper.eq("openid", openid);
        List<User> users = userMapper.selectList(queryWrapper);
        if (!users.isEmpty()) { // 如果存在，就取出，并获取该用户的jwt-token
            User user = users.get(0);
            String jwt = JwtUtil.createJWT(user.getId().toString()); // 将用户的 userID 封装成 JWT-token
            resp.put("result", "success");
            resp.put("jwt_token", jwt);
            return resp;
        }

        // 如果不存在通过令牌和openid就向acwing申请获取用户信息
        nameValuePairs = new LinkedList<>(); // 向acwing申请获取用户信息需要的参数列表
        nameValuePairs.add(new BasicNameValuePair("access_token", accessToken));
        nameValuePairs.add(new BasicNameValuePair("openid", openid));

        // 通过向acwing申请用户信息的url获得结果
        getString = HttpClientUtil.get(getUserInfoUrl, nameValuePairs);
        if (getString == null) {
            return resp;
        }
        getResp = JSONObject.parseObject(getString); // 将结果解析成JSON

        // 从JSON里解析用户的用户名和头像
        String username = getResp.getString("username");
        String photo = getResp.getString("photo");

        // 判断用户名或头像是否为空
        if (username == null || photo == null) {
            return resp;
        }

        // 判断是否重名
        for (int i = 0; i < 100; i ++) {
            QueryWrapper<User> usernameQueryWrapper = new QueryWrapper<>();
            usernameQueryWrapper.eq("username", username);
            if (userMapper.selectList(usernameQueryWrapper).isEmpty()) {
                break;
            }
            username += (char) (random.nextInt(10) + '0');
            if (i == 99) {
                return resp;
            }
        }

        // 通过用户名和头像注册一个User
        User user = new User(
                null,
                username,
                null,
                photo,
                1500,
                openid
        );
        userMapper.insert(user);
        String jwt = JwtUtil.createJWT(user.getId().toString()); // 将用户的 userID 封装成 JWT-token
        resp.put("result", "success");
        resp.put("jwt_token", jwt);
        return resp;
    }
}

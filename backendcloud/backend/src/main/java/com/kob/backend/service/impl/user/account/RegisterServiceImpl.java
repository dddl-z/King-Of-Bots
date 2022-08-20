package com.kob.backend.service.impl.user.account;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.User;
import com.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Autowired // 数据库查询，需要注入该接口
    private UserMapper userMapper;

    @Autowired // 存密码的时候，需要加密
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, String> register(String username, String password, String confirmedPassword) {
        Map<String, String> map = new HashMap<>();
        if (username == null) { // 判断参数
            map.put("error_message", "用户名不能为空！");
            return map;
        }
        if (password == null || confirmedPassword == null) {
            map.put("error_message", "密码不能为空！");
            return map;
        }

        username = username.trim(); // 删掉首尾的空白字符
        if (username.length() == 0) { // 判断长度
            map.put("error_message", "用户名不能为空！");
            return map;
        }
        if (password.length() == 0 || confirmedPassword.length() == 0) {
            map.put("error_message", "密码不能为空！");
            return map;
        }
        if(username.length() > 100) {
            map.put("error_message", "用户名长度不能大于100！");
            return map;
        }
        if (password.length() > 100 || confirmedPassword.length() > 100) {
            map.put("error_message", "密码长度不能大于100！");
            return map;
        }

        if (!password.equals(confirmedPassword)) {
            map.put("error_message", "两次输入的密码不一致");
            return map;
        }

        // 数据库查询判断是否存在输入的用户名
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username); // 在数据库里查询输入的用户名
        List<User> users = userMapper.selectList(queryWrapper); // 将结果存到 List 里
        if (!users.isEmpty()) { // 如果在数据库里查到了输入的用户名，说明有重复
            map.put("error_message", "用户名已存在");
            return map;
        }

        // 加到数据库里
        String encodedPassword = passwordEncoder.encode(password);
        String photo = "https://cdn.acwing.com/media/user/profile/photo/191859_lg_eb85e6a4c4.jpg";
        User user = new User(null, username, encodedPassword, photo, 1500);
        userMapper.insert(user);

        // 成功
        map.put("error_message", "success");
        return map;
    }
}

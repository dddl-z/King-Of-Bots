package com.kob.backend.service.impl.user.bot;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.bot.AddService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AddServiceImpl implements AddService {

    @Autowired
    private BotMapper botMapper;

    @Override
    public Map<String, String> add(Map<String, String> data) {

        // 根据token得知当前user是谁
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        User user = loginUser.getUser();

        // 需要传入的数据
        String title = data.get("title");
        String description = data.get("description");
        String content = data.get("content");

        // 定义答案
        Map<String, String> map = new HashMap<>();

        // 业务逻辑
        if (title == null || title.length() == 0) {
            map.put("error_message", "标题不能为空！");
            return map;
        }
        if (title.length() > 100) {
            map.put("error_message", "标题长度不能大于100！");
            return map;
        }
        if (description == null || description.length() == 0) {
            description = "这个用户很懒，什么也没留下~";
        }
        if (description.length() > 300) {
            map.put("error_message", "Bot描述的长度不能大于300！");
            return map;
        }
        if (content == null || content.length() == 0) {
            map.put("error_message", "代码不能为空！");
            return map;
        }
        if (content.length() > 10000) {
            map.put("error_message", "代码长度不能大于10000！");
            return map;
        }

        QueryWrapper<Bot> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());
        if (botMapper.selectCount(queryWrapper) >= 5) {
            map.put("error_message", "每个用户最多只能创建5个Bot！");
            return map;
        }

        // 创建一个bot
        Date now = new Date();
        Bot bot = new Bot(null, user.getId(), title, description, content, now, now);

        // 加入数据库
        botMapper.insert(bot);
        map.put("error_message", "success");

        return map;
    }
}

package com.kob.backend.service.user.bot;

import com.kob.backend.pojo.Bot;

import java.util.List;

public interface GetListService { // 查询自己的 Bot，user_id 可以在 token 里获取，不需要传参数
    List<Bot> getList();
}

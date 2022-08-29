package com.kob.backend.service.impl.record;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import com.kob.backend.service.record.GetRecordListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class GetRecordListServiceImpl implements GetRecordListService {
    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public JSONObject getList(Integer page) {
        IPage<Record> recordIPage = new Page<>(page, 10); // 参数：页数，每一页里有多少个数据
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>(); // 排序
        queryWrapper.orderByDesc("id"); // 用表中的某一列排序

        // 先将所有的对局记录按照id降序排序，然后返回其中第page页的内容，内容里的项目个数也是人为定义的
        List<Record> records = recordMapper.selectPage(recordIPage, queryWrapper).getRecords();

        // 将对局记录的信息存下来，包括对战双方的用户名和头像
        List<JSONObject> items = new LinkedList<>();
        for (Record record : records) {
            User userA = userMapper.selectById(record.getAId());
            User userB = userMapper.selectById(record.getBId());

            JSONObject item = new JSONObject();
            item.put("a_photo", userA.getPhoto());
            item.put("a_username", userA.getUsername());
            item.put("b_photo", userB.getPhoto());
            item.put("b_username", userB.getUsername());

            String result = "平局";
            if ("A".equals(record.getLoser())) {
                result = "B胜";
            }
            if ("B".equals(record.getLoser())) {
                result = "A胜";
            }
            item.put("result", result);

            item.put("record", record);

            items.add(item);
        }

        // 将查询到的数据返回前端
        JSONObject resp = new JSONObject();
        resp.put("records", items);
        resp.put("records_count", recordMapper.selectCount(null)); // 返回总数
        return resp;
    }
}

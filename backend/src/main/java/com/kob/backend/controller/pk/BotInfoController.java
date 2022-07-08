package com.kob.backend.controller.pk;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/pk/")
public class BotInfoController {

    @RequestMapping("getBotInfo/")
    public List<String> getBotInfo() {
        List<String> list = new ArrayList<>();
        list.add("yxc");
        list.add("acwing");
        list.add("nb");

        return list;
    }
}

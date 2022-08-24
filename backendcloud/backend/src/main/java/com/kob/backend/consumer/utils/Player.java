package com.kob.backend.consumer.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private Integer id; // 对应的玩家id
    private Integer botId; // 如果是 -1，表示亲自出马，否则表示用对应的bot
    private String botCode; // 出战的bot代码
    private Integer sx; // 起始位置
    private Integer sy;
    private List<Integer> steps; // 每一回合的指令方向

    private boolean check_tail_increasing(int step) { // 检验当前回合蛇的长度是否增加
        if (step <= 10) {
            return true;
        }
        return step % 3 == 1;
    }

    public List<Cell> getCells() { // 获取蛇的身体
        List<Cell> res = new ArrayList<>();

        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        int x = sx, y = sy;
        int step = 0; // 回合数
        res.add(new Cell(x, y));
        for (int d : steps) { // 枚举走过的每个回合的指令
            x += dx[d];
            y += dy[d];

            res.add(new Cell(x, y));
            if (!check_tail_increasing(++ step)) { // 当前回合没有增加长度，删掉蛇尾
                res.remove(0);
            }
        }
        return res;
    }

    public String getStepsString() { // 将每一步的操作转化为String存到数据库里
        StringBuilder res = new StringBuilder();
        for (int d : steps) {
            res.append(d);
        }
        return res.toString();
    }
}

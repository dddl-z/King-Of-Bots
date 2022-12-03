package com.kob.botrunningsystem.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// 从前端传来的代码，为了方便在idea里写
public class Bot implements java.util.function.Supplier<Integer> {
    static class Cell {
        public int x;
        public int y;

        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private boolean check_tail_increasing(int step) { // 检验当前回合蛇的长度是否增加
        if (step <= 10) {
            return true;
        }
        return step % 3 == 1;
    }

    public List<Cell> getCells(int sx, int sy, String steps) { // 获取蛇的身体
        steps = steps.substring(1, steps.length() - 1);
        List<Cell> res = new ArrayList<>();

        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        int x = sx, y = sy;
        int step = 0; // 回合数
        res.add(new Cell(x, y));
        for (int i = 0; i < steps.length(); i ++) { // 枚举走过的每个回合的指令
            int d = steps.charAt(i) - '0';
            x += dx[d];
            y += dy[d];

            res.add(new Cell(x, y));
            if (!check_tail_increasing(++ step)) { // 当前回合没有增加长度，删掉蛇尾
                res.remove(0);
            }
        }
        return res;
    }

    public Integer nextMove(String input) {
        // 分割对应信息
        String[] strs = input.split("#");

        // 取出地图信息
        int[][] g = new int[13][14];
        for (int i = 0, k = 0; i < 13; i ++) {
            for (int j = 0; j < 14; j ++, k ++) {
                if (strs[0].charAt(k) == '1') {
                    g[i][j] = 1;
                }
            }
        }

        // 获取双方的蛇身体
        int aSx = Integer.parseInt(strs[1]);
        int aSy = Integer.parseInt(strs[2]);
        int bSx = Integer.parseInt(strs[4]);
        int bSy = Integer.parseInt(strs[5]);
        List<Cell> aCells = getCells(aSx, aSy, strs[3]);
        List<Cell> bCells = getCells(bSx, bSy, strs[6]);

        // 在地图里标注一下双方蛇的身体
        for (Cell c : aCells) {
            g[c.x][c.y] = 1;
        }
        for (Cell c : bCells) {
            g[c.x][c.y] = 1;
        }

        // 判断4个方向哪个方向不走
        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        for (int i = 0; i < 4; i ++) {
            int x = aCells.get(aCells.size() - 1).x + dx[i];
            int y = aCells.get(aCells.size() - 1).y + dy[i];
            if (x >= 0 && x < 13 && y >= 0 && y < 14 && g[x][y] == 0) {
                return i;
            }
        }

        return 0;
    }

    @Override
    public Integer get() {
        File file = new File("input.txt");
        try {
            Scanner sc = new Scanner(file); // 从文件里获取参数
            return nextMove(sc.next());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
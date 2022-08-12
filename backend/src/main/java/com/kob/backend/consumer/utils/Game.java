package com.kob.backend.consumer.utils;

import java.util.Random;

public class Game {
    private final Integer rows;
    private final Integer cols;
    private final Integer inner_walls_count;
    private final int[][] g;
    private final int[] dx = {-1, 0, 1, 0};
    private final int[] dy = {0, 1, 0, -1};

    public Game(Integer rows, Integer cols, Integer inner_walls_count) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
    }

    public int[][] getG() {
        return g;
    }

    // 检查双方是否连通，判断连通性使用 Floyd-fill 算法
    private boolean check_connectivity(int sx, int sy, int tx, int ty) {
        if (sx == tx && sy == ty) {
            return true;
        }

        g[sx][sy] = 1;

        for (int i = 0; i < 4; i ++) {
            int x = sx + dx[i], y = sy + dy[i];
            if (x >= 0 && x < this.rows && y >= 0 && y < this.cols && g[x][y] == 0) {
                if (check_connectivity(x, y, tx, ty)) {
                    g[sx][sy] = 0;
                    return true;
                }
            }
        }

        g[sx][sy] = 0;
        return false;
    }

    // 画地图
    private boolean draw() {
        // 初始化
        for (int i = 0; i < this.rows; i ++) {
            for (int j = 0; j < this.cols; j ++) {
                g[i][j] = 0; // 0 表示空地，1 表示墙
            }
        }

        // 给四周加上障碍物
        for (int r = 0; r < this.rows; r ++) {
            g[r][0] = g[r][this.cols - 1] = 1;
        }
        for (int c = 0; c < this.cols; c ++) {
            g[0][c] = g[this.rows - 1][c] = 1;
        }

        // 创建随机障碍物
        Random random = new Random();
        for (int i = 0; i < this.inner_walls_count / 2; i ++) {
            for (int j = 0; j < 1000; j ++) {
                int r = random.nextInt(this.rows); // 返回 0 ~ this.rows - 1 之间的任何一个随机值
                int c = random.nextInt(this.cols);

                if (g[r][c] == 1 || g[this.rows - 1 - r][this.cols - 1 - c] == 1) { // 中心对称
                    continue;
                }

                if (r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2) { // 使左下角和右上角没有障碍物吗，因为这两个格子是两方的起点
                    continue;
                }

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = 1; // 关于左斜线对称放障碍物
                break;
            }
        }

        return check_connectivity(this.rows - 2, 1, 1, this.cols - 2 );
    }

    public void createMap() { // 随机画地图
        for (int i = 0; i < 1000; i ++) {
            if (draw()) {
                break;
            }
        }
    }
}

package com.kob.backend.consumer.utils;

import com.alibaba.fastjson.JSONObject;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.Record;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread { // 继承Thread，变成支持多线程的类
    private final Integer rows; // 行
    private final Integer cols; // 列
    private final Integer inner_walls_count; // 墙的数量
    private final int[][] g; // 地图，其中 0 表示空地，1 表示墙
    private final int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1}; // 上右下左四个方向的偏移量
    private final Player playerA, playerB;
    private Integer nextStepA = null; // 玩家A的下一步操作，0123 表示上下左右四个方向，null 表示没有获取到下一步操作
    private Integer nextStepB = null; // 玩家B的下一步操作
    private ReentrantLock lock = new ReentrantLock(); // 加锁解决读写冲突
    private String status = "playing"; // 游戏状态：playing 表示正在进行游戏，finished 表示有一方操作不合法，游戏结束
    private String loser = ""; // 败者：all 表示平局，A 表示 A 输了，B 表示 B 输了
    private final static String addBotUrl = "http://127.0.0.1:3002/bot/add/";
    public Game(
            Integer rows,
            Integer cols,
            Integer inner_walls_count,
            Integer idA,
            Bot botA,
            Integer idB,
            Bot botB
    ) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];

        // 取出对应的bot，亲自出马是-1
        Integer botIdA = -1;
        Integer botIdB = -1;
        String botCodeA = "";
        String botCodeB = "";
        if (botA != null) {
            botIdA = botA.getId();
            botCodeA = botA.getContent();
        }
        if (botB != null) {
            botIdB = botB.getId();
            botCodeB = botB.getContent();
        }

        playerA = new Player(idA, botIdA, botCodeA,rows - 2, 1, new ArrayList<>());
        playerB = new Player(idB, botIdB, botCodeB, 1, cols - 2, new ArrayList<>());
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public void setNextStepA(Integer nextStepA) { // 设置玩家A的下一步操作
        lock.lock(); // 设置下一步操作时先把锁锁上
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock(); // 设置完成后不管有没有报异常，都会将锁打开
        }
    }

    public void setNextStepB(Integer nextStepB) { // 设置玩家B的下一步操作
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
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

    private String getInput(Player player) { // 将当前的局面信息，编码成一个字符串
        // 判断谁是谁
        Player me;
        Player you;
        if (playerA.getId().equals(player.getId())) {
            me = playerA;
            you = playerB;
        } else {
            me = playerB;
            you = playerA;
        }

        // 地图信息 # 我的起始坐标x # 我的起始坐标y # 我的操作 # 你的起始坐标x # 你的起始坐标y # 你的操作
        return getMapString() + "#" +
                me.getSx() + "#" +
                me.getSy() + "#(" +
                me.getStepsString() + ")#" + // 防止操作序列为空，用括号括起来
                you.getSx() + "#" +
                you.getSy() + "#(" +
                you.getStepsString() + ")#";
    }

    private void sendBotCode(Player player) { // 判断该玩家是人还是bot
        if (player.getBotId().equals(-1)) { // 亲自出马，不需要执行代码
            return;
        }

        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", player.getId().toString());
        data.add("bot_code", player.getBotCode());
        data.add("input", getInput(player));
        WebSocketServer.restTemplate.postForObject(addBotUrl, data, String.class); // 向BotRunningSystem发送代码
    }

    private boolean nextStep() { // 等待两名玩家的下一步操作
        try {
            Thread.sleep(200); // 防止过快读入操作，导致前端渲染时漏掉某些操作
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 判断两名玩家是人还是bot操作
        sendBotCode(playerA);
        sendBotCode(playerB);

        for (int i = 0; i < 50; i++) {
            try {
                Thread.sleep(100);
                lock.lock();
                try {
                    if (nextStepA != null && nextStepB != null) { // 如果两名玩家的操作都读到了
                        playerA.getSteps().add(nextStepA); // 将操作存下来
                        playerB.getSteps().add(nextStepB);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private boolean check_valid(List<Cell> cellsA, List<Cell> cellsB) { // 判断双方最后一个单元（最新一次操作）是否合法
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);
        if (g[cell.x][cell.y] == 1) { // 与墙重合了，不合法
            return false;
        }

        for (int i = 0; i < n - 1; i ++) {
            if (cellsA.get(i).x == cell.x && cellsA.get(i).y == cell.y) { // 和自己的身体重合了
                return false;
            }
        }

        for (int i = 0; i < n - 1; i ++) {
            if (cellsB.get(i).x == cell.x && cellsB.get(i).y == cell.y) { // 和对方的身体重合了
                return false;
            }
        }

        return true;
    }

    private void judge() { // 判断两名玩家下一步操作是否合法
        List<Cell> cellsA = playerA.getCells(); // 获取两条蛇的身体
        List<Cell> cellsB = playerB.getCells();

        boolean validA = check_valid(cellsA, cellsB);
        boolean validB = check_valid(cellsB, cellsA);
        if (!validA || !validB) { // 如果任意一方有非法操作，游戏结束
            status = "finished";

            if (!validA && !validB) {
                loser = "all";
            } else if (!validA) {
                loser = "A";
            } else if (!validB) {
                loser = "B";
            }
        }
    }

    private void sendAllMessage(String message) { // 向两名玩家广播信息
        if (WebSocketServer.users.get(playerA.getId()) != null) {
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        }
        if (WebSocketServer.users.get(playerB.getId()) != null) {
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
        }
    }

    private void sendMove() { // judge 评判双反都合法后，向双方Client广播移动信息，以达到同步
        lock.lock();
        try {
            JSONObject resp = new JSONObject();
            resp.put("event", "move");
            resp.put("a_direction", nextStepA);
            resp.put("b_direction", nextStepB);
            sendAllMessage(resp.toJSONString());
            nextStepA = nextStepB = null; // 前端接收到广播的操作之后，后端将操作清空，以备下一次输入的读取
        } finally {
            lock.unlock();
        }
    }

    private String getMapString() { // 将地图信息转化为String存到数据库里
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < rows; i ++) {
            for (int j = 0; j < cols; j ++) {
                res.append(g[i][j]);
            }
        }
        return res.toString();
    }

    private void saveToDatabase() { // 将该剧的对局记录信息存到数据库里
        Record record = new Record(
                null,
                playerA.getId(),
                playerA.getSx(),
                playerA.getSy(),
                playerB.getId(),
                playerB.getSx(),
                playerB.getSy(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                getMapString(),
                loser,
                new Date()
        );

        WebSocketServer.recordMapper.insert(record);
    }

    private void sendResult() { // 游戏结束后，给双方Client公布游戏结果
        JSONObject resp = new JSONObject();
        resp.put("event", "result");
        resp.put("loser", loser);
        saveToDatabase();
        sendAllMessage(resp.toJSONString());
    }

    @Override
    public void run() { // 新线程的入口
        for (int i = 0; i < 1000; i ++) {
            if (nextStep()) { // 是否获取到了两条蛇的下一步操作
                judge();
                if (status.equals("playing")) { // 双方操作都合法
                    sendMove();
                } else { // 双方有不合法操作，游戏结束，向前端公布游戏结果
                    sendResult();
                    break;
                }
            } else {
                status = "finished";
                lock.lock();
                try {
                    if (nextStepA == null && nextStepB == null) { // 平局
                        loser = "all";
                    } else if (nextStepA == null) {
                        loser = "A";
                    } else {
                        loser = "B";
                    }
                } finally {
                    lock.unlock();
                }
                sendResult();
                break;
            }
        }
    }
}

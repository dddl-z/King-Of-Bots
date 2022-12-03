package com.kob.botrunningsystem.service.impl.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread {
    private static final ReentrantLock lock = new ReentrantLock(); // 锁
    private final Condition condition = lock.newCondition(); // 条件变量，实现线程阻塞和唤醒
    private final Queue<Bot> bots = new LinkedList<>();

    public void addBot(Integer userId, String botCode, String input) { // 在队列里将一个新的bot
        lock.lock();
        try {
            bots.add(new Bot(userId, botCode, input));
            condition.signalAll(); // 唤醒线程
        } finally {
            lock.unlock();
        }
    }

    private void consume(Bot bot) { // 消耗任务，比较耗时
        Consumer consumer = new Consumer();
        consumer.startTimeout(2000, bot);
    }

    @Override
    public void run() {
        while (true) {
            lock.lock();
            if (bots.isEmpty()) { // 如果队列为空，将当前线程（消费者线程）阻塞
                try {
                    condition.await(); // 阻塞，锁释放
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    lock.unlock();
                    break;
                }
            } else {
                Bot bot = bots.remove(); // 返回并删除队头
                lock.unlock();
                consume(bot);
            }
        }
    }
}

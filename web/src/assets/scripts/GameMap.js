import { AcGameObject } from "./AcGameObject";
import { Snake } from "./Snake";
import { Wall } from "./Wall";

export class GameMap extends AcGameObject {
    constructor(ctx, parent) { // ctx 是画布，前端游戏优先在画布里画，parent 是画布的父元素，用来动态修改画布的长宽
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.L = 0; // L 表示一个单位的长度

        this.rows = 13;
        this.cols = 14;

        this.inner_walls_count = 20;
        this.walls = []; // 存储所有的墙

        this.snakes = [
            new Snake({ id: 0, color: "#4876EC", r: this.rows - 2, c: 1 }, this),
            new Snake({ id: 1, color: "#F94848", r: 1, c: this.cols - 2 }, this)
        ];
    }

    // 检查双方是否连通，判断连通性使用 Floyd-fill 算法
    check_connectivity(g, sx, sy, tx, ty) { // 参数依次为：地图，起点坐标，终点坐标
        if (sx === tx && sy === ty) {
            return true;
        }

        g[sx][sy] = true;

        let dx = [-1, 0, 1, 0],
            dy = [0, 1, 0, -1];
        for (let i = 0; i < 4; i++) {
            let x = sx + dx[i],
                y = sy + dy[i];
            if (!g[x][y] && this.check_connectivity(g, x, y, tx, ty)) {
                return true;
            }
        }

        return false;
    }

    create_walls() {
        // 初始化障碍物，无障碍物为 false，反之为 true
        const g = [];
        for (let r = 0; r < this.rows; r++) {
            g[r] = [];
            for (let c = 0; c < this.cols; c++) {
                g[r][c] = false;
            }
        }

        // 给四周加上障碍物
        for (let r = 0; r < this.rows; r++) {
            g[r][0] = g[r][this.cols - 1] = true;
        }

        for (let c = 0; c < this.cols; c++) {
            g[0][c] = g[this.rows - 1][c] = true;
        }

        // 创建随机障碍物
        for (let i = 0; i < this.inner_walls_count / 2; i++) {
            for (let j = 0; j < 1000; j++) { // 随机 1000 次找到还没有放障碍物的格子
                let r = parseInt(Math.random() * this.rows); // random 返回 [0, 1) 之间的浮点数，乘上 rows 再取整，可以得到 [0, rows - 1] 之间的任意一个值
                let c = parseInt(Math.random() * this.cols);
                if (g[r][c] || g[this.rows - 1 - r][this.cols - 1 - c]) { // 中心对称
                    continue;
                }
                if ((r === this.rows - 2 && c === 1) || (c === this.cols - 2 && r === 1)) { // 使左下角和右上角没有障碍物吗，因为这两个格子是两方的起点
                    continue;
                }

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = true; // 关于左斜线对称放障碍物
                break;
            }
        }

        const copy_g = JSON.parse(JSON.stringify(g)); // 把当前的状态复制下来，防止判断连通性的时候被破坏掉
        if (!this.check_connectivity(copy_g, this.rows - 2, 1, 1, this.cols - 2)) {
            return false;
        }

        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }

        return true;
    }

    add_listening_events() { // 给 canvas 绑定一个获取用户操作信息的事件
        this.ctx.canvas.focus(); // 聚焦 canvas

        const [snake0, snake1] = this.snakes;
        this.ctx.canvas.addEventListener("keydown", e => {
            if (e.key === 'w') {
                snake0.set_direction(0);
            } else if (e.key === 'd') {
                snake0.set_direction(1);
            } else if (e.key === 's') {
                snake0.set_direction(2);
            } else if (e.key === 'a') {
                snake0.set_direction(3);
            } else if (e.key === 'ArrowUp') {
                snake1.set_direction(0);
            } else if (e.key === 'ArrowRight') {
                snake1.set_direction(1);
            } else if (e.key === 'ArrowDown') {
                snake1.set_direction(2);
            } else if (e.key === 'ArrowLeft') {
                snake1.set_direction(3);
            }
        });
    }

    start() {
        for (let i = 0; i < 1000; i++) {
            if (this.create_walls()) {
                break;
            }
        }

        this.add_listening_events();
    }

    update_size() { // 更新长度
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    check_ready() { // 判断双方是否都准备好下一步的操作
        for (const snake of this.snakes) {
            if (snake.status !== "idle") {
                return false;
            }
            if (snake.direction === -1) {
                return false;
            }
        }
        return true;
    }

    next_step() { // 让两条蛇进入下一回合
        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    check_valid(cell) { // 检测目标位置是否合法：没有撞到两条蛇的身体和障碍物
        for (const wall of this.walls) { // 判断障碍物
            if (cell.r === wall.r && cell.c === wall.c) {
                return false;
            }
        }

        for (const snake of this.snakes) { // 判断两条蛇的身体
            let k = snake.cells.length;
            if (!snake.check_tail_increasing()) { // 当蛇的长度不会增加，蛇尾会前进的时候，不用判断蛇尾
                k--;
            }

            for (let i = 0; i < k; i++) {
                if (snake.cells[i].r === cell.r && snake.cells[i].c === cell.c) {
                    return false;
                }
            }
        }

        return true;
    }

    update() {
        this.update_size(); // 每一帧都更新一下长度
        if (this.check_ready()) {
            this.next_step();
        }
        this.render(); // 每一帧都渲染一次
    }

    render() { // 渲染的辅助函数
        const color_even = "#AAD751";
        const color_odd = "#A2D149";
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if ((r + c) % 2 === 0) {
                    this.ctx.fillStyle = color_even;
                } else {
                    this.ctx.fillStyle = color_odd;
                }
                this.ctx.fillRect(c * this.L, r * this.L, this.L, this.L); // 参数依次为：左上角的 x 坐标，左上角的 y 坐标，矩形的宽，矩形的高
            }
        }
    }
}
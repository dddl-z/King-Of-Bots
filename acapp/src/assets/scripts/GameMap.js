import { AcGameObject } from "./AcGameObject";
import { Snake } from "./Snake";
import { Wall } from "./Wall";

export class GameMap extends AcGameObject {
    constructor(ctx, parent, store) { // ctx 是画布，前端游戏优先在画布里画，parent 是画布的父元素，用来动态修改画布的长宽
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.store = store;
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

    create_walls() {
        const g = this.store.state.pk.gamemap;

        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }
    }

    add_listening_events() { // 给 canvas 绑定一个获取用户操作信息的事件
        if (this.store.state.record.is_record) { // 判断是录像还是对战
            let k = 0;
            const a_steps = this.store.state.record.a_steps;
            const b_steps = this.store.state.record.b_steps;
            const loser = this.store.state.record.record_loser;
            const [snake0, snake1] = this.snakes;
            const interval_id = setInterval(() => { // 延迟函数
                if (k >= a_steps.length - 1) { // 录像对局结束
                    if (loser === "all" || loser === "A") {
                        snake0.status = "die";
                    }
                    if (loser === "all" || loser === "B") {
                        snake1.status = "die";
                    }
                    clearInterval(interval_id);
                } else {
                    snake0.set_direction(parseInt(a_steps[k]));
                    snake1.set_direction(parseInt(b_steps[k]));
                }
                k++;
            }, 300);
        } else {
            this.ctx.canvas.focus(); // 聚焦 canvas

            this.ctx.canvas.addEventListener("keydown", e => {
                let d = -1;
                if (e.key === 'w') {
                    d = 0;
                } else if (e.key === 'd') {
                    d = 1;
                } else if (e.key === 's') {
                    d = 2;
                } else if (e.key === 'a') {
                    d = 3;
                }

                if (d >= 0) {
                    this.store.state.pk.socket.send(JSON.stringify({
                        event: "move",
                        direction: d,
                    }));
                }
            });
        }


    }

    start() {
        this.create_walls();

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
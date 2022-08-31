import { Cell } from "./Cell";
import { AcGameObject } from "@/assets/scripts/AcGameObject";

export class Snake extends AcGameObject {
    constructor(info, gamemap) {
        super();

        this.id = info.id; // 区分每条蛇
        this.color = info.color;
        this.gamemap = gamemap; // 传入地图的目的是：调用一些函数以及获取一些参数（每个格子的长度）

        this.cells = [new Cell(info.r, info.c)]; // 存放蛇的身体，cells[0] 存放蛇头
        this.next_cell = null; // 下一步的目标位置

        this.speed = 5; // 蛇每秒钟走 5 个格子
        this.direction = -1; // 蛇的下一步指令，-1 表示没有指令，0，1，2，3 表示上右下左
        this.status = "idle"; // 蛇的状态，idle 表示静止，move 表示正在移动，die 表示死亡

        this.dr = [-1, 0, 1, 0]; // 四个方向行的偏移量
        this.dc = [0, 1, 0, -1]; // 四个方向列的偏移量

        this.step = 0; // 表示回合数
        this.eps = 1e-2; // 允许的误差

        this.eye_direction = 0;
        if (this.id === 1) { // 左下角的蛇眼初始朝上，右上角的蛇眼初始朝下
            this.eye_direction = 2;
        }

        this.eye_dx = [ // 蛇眼在不同方向上的 x 偏移量
            [-1, 1],
            [1, 1],
            [1, -1],
            [-1, -1],
        ];
        this.eye_dy = [ // 蛇眼在不同方向上的 y 偏移量
            [-1, -1],
            [-1, 1],
            [1, 1],
            [-1, 1],
        ];
    }

    start() {

    }

    set_direction(d) { // 统一的接口来设置方向
        this.direction = d;
    }

    check_tail_increasing() { // 检测当前回合，蛇的长度是否增加
        if (this.step <= 10) {
            return true;
        }
        if (this.step % 3 === 1) {
            return true;
        }

        return false;
    }

    next_step() { // 将蛇的状态更新为走下一步
        const d = this.direction;
        this.next_cell = new Cell(this.cells[0].r + this.dr[d], this.cells[0].c + this.dc[d]); // 下一步的位置
        this.eye_direction = d;
        this.direction = -1; // 清空操作
        this.status = "move";
        this.step++;

        const k = this.cells.length;
        for (let i = k; i > 0; i--) {
            this.cells[i] = JSON.parse(JSON.stringify(this.cells[i - 1])); // 防止出现重复的问题，先转换为 JSON，然后再解析出来，这样就会创建一个新的对象
        }
    }

    update_move() { // 蛇的移动
        const dx = this.next_cell.x - this.cells[0].x;
        const dy = this.next_cell.y - this.cells[0].y;
        const distance = Math.sqrt(dx * dx + dy * dy); // 到目标点的距离

        if (distance < this.eps) { // 走到了目标点，完成了这一步
            this.cells[0] = this.next_cell; // 添加一个新蛇头
            this.next_cell = null; // 将这一步的目标点置为 null，等待下一步指令
            this.status = "idle"; // 走完了这一步操作，停下来

            if (!this.check_tail_increasing()) { // 如果不变长，蛇尾移到目的点，原来的蛇尾需要砍掉
                this.cells.pop();
            }
        } else {
            const move_distance = this.speed * this.timedelta / 1000; // 每两帧之间走的距离，速度乘以时间，timedelta 的单位是毫秒，需要 / 1000
            this.cells[0].x += move_distance * dx / distance; // x 的偏移量
            this.cells[0].y += move_distance * dy / distance; // y 的偏移量

            if (!this.check_tail_increasing()) {
                const k = this.cells.length;
                const tail = this.cells[k - 1];
                const tail_target = this.cells[k - 2];
                const tail_dx = tail_target.x - tail.x;
                const tail_dy = tail_target.y - tail.y;
                tail.x += move_distance * tail_dx / distance;
                tail.y += move_distance * tail_dy / distance;
            }
        }
    }

    update() { // 每一帧执行一次
        if (this.status === 'move') {
            this.update_move();
        }

        this.render();
    }

    render() {
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;

        ctx.fillStyle = this.color;
        if (this.status === "die") {
            ctx.fillStyle = "white";
        }

        for (const cell of this.cells) { // 枚举蛇的身体里的每一个格子
            ctx.beginPath(); // 画圆，开启一个路径
            ctx.arc(cell.x * L, cell.y * L, L / 2 * 0.8, 0, Math.PI * 2); // 画一个圆弧，参数依次为：圆的中心坐标 x，圆的中心坐标 y，圆的半径，画圆弧的起始角度，画圆弧的终止角度
            ctx.fill();
        }

        for (let i = 1; i < this.cells.length; i++) {
            const a = this.cells[i - 1];
            const b = this.cells[i];
            if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps) { // 两个圆重合
                continue;
            }
            if (Math.abs(a.x - b.x) < this.eps) { // 竖直方向
                ctx.fillRect((a.x - 0.4) * L, Math.min(a.y, b.y) * L, L * 0.8, Math.abs(a.y - b.y) * L);
            }
            if (Math.abs(a.y - b.y) < this.eps) { // 水平方向
                ctx.fillRect(Math.min(a.x, b.x) * L, (a.y - 0.4) * L, Math.abs(a.x - b.x) * L, L * 0.8);
            }
        }

        ctx.fillStyle = "black";
        for (let i = 0; i < 2; i++) {
            const eye_x = (this.cells[0].x + this.eye_dx[this.eye_direction][i] * 0.15) * L; // * L 变成绝对距离，0.15 为圆心到蛇眼偏的距离
            const eye_y = (this.cells[0].y + this.eye_dy[this.eye_direction][i] * 0.15) * L;
            ctx.beginPath();
            ctx.arc(eye_x, eye_y, L * 0.05, 0, Math.PI * 2);
            ctx.fill();
        }
    }
}
import { AcGameObject } from "./AcGameObject";
import { Wall } from "./Wall";

export class GameMap extends AcGameObject {
    constructor(ctx, parent) { // ctx 是画布，前端游戏优先在画布里画，parent 是画布的父元素，用来动态修改画布的长宽
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.L = 0; // L 表示一个单位的长度

        this.rows = 13;
        this.cols = 13;

        this.inner_walls_count = 20;
        this.walls = []; // 存储所有的墙
    }

    // 检查双方是否连通，判断连通性使用 Floyd-fill 算法
    check_connectivity(g, sx, sy, tx, ty) { // 参数依次为：地图，起点坐标，终点坐标
        if (sx == tx && sy == ty) {
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
                if (g[r][c] || g[c][r]) { // 条件是因为要对称放障碍物
                    continue;
                }
                if ((r == this.rows - 2 && c == 1) || (c == this.cols - 2 && r == 1)) { // 使左下角和右上角没有障碍物吗，因为这两个格子是两方的起点
                    continue;
                }

                g[r][c] = g[c][r] = true; // 关于左斜线对称放障碍物
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

    start() {
        for (let i = 0; i < 1000; i++) {
            if (this.create_walls()) {
                break;
            }
        }
    }

    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    update() {
        this.update_size(); // 每一帧都更新一下长度
        this.render(); // 每一帧都渲染一次
    }

    render() { // 渲染的辅助函数
        const color_even = "#AAD751";
        const color_odd = "#A2D149";
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if ((r + c) % 2 == 0) {
                    this.ctx.fillStyle = color_even;
                } else {
                    this.ctx.fillStyle = color_odd;
                }
                this.ctx.fillRect(c * this.L, r * this.L, this.L, this.L);
            }
        }
    }
}
const AC_GAME_OBJECTS = [];

export class AcGameObject {
    constructor() {
        AC_GAME_OBJECTS.push(this);
        this.timedelta = 0; // 这一帧执行的时刻和上一帧执行的时刻的间隔（物体移动的速度）
        this.has_called_start = false; // 记录有没有执行过 start 函数
    }

    start() { // 只执行一次

    }

    update() { // 每一帧执行一次，除了第一帧之外

    }

    on_destroy() { // 删除之前执行

    }

    destroy() { // 删除操作
        this.on_destroy();

        for (let i in AC_GAME_OBJECTS) {
            const obj = AC_GAME_OBJECTS[i];
            if (obj === this) {
                AC_GAME_OBJECTS.splice(i);
                break;
            }
        }
    }

}


let last_timestamp; // 上一次执行的时刻
const step = timestamp => { // 每一帧都执行一遍，传入参数 timestamp 为当前执行的时刻
    for (let obj of AC_GAME_OBJECTS) {
        if (!obj.has_called_start) {
            obj.has_called_start = true;
            obj.start();
        } else {
            obj.timedelta = timestamp - last_timestamp;
            obj.update();
        }
    }

    last_timestamp = timestamp;
    requestAnimationFrame(step)
}

requestAnimationFrame(step)
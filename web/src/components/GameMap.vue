<template>
    <div ref="parent" class="gamemap">
        <canvas ref="canvas" tabindex="0"></canvas>
    </div>
</template>

<script>
import { GameMap } from '@/assets/scripts/GameMap.js'
import { ref, onMounted } from 'vue' // ref 引用 canvas，onMounted 当组件挂载完之后需要执行哪些操作
import { useStore } from 'vuex';


export default {
    setup() {
        const store = useStore();
        let parent = ref(null);
        let canvas = ref(null);

        onMounted(() => {
           store.commit(
                "updateGameObject",
                new GameMap(canvas.value.getContext('2d'), parent.value, store)
            );
        });

        return {
            parent,
            canvas
        }
    }
}
 

</script>

<style scoped>
div.gamemap {
    width: 100%;
    height: 100%;
    display: flex; /* flex 可以同时实现水平居中和竖直居中 */
    justify-content: center;
    align-items: center;
}

</style>
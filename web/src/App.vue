<template>
  <NavBar />
  <router-view/>
</template>

<script>
import NavBar from '@/components/NavBar.vue'
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap/dist/js/bootstrap"
/* import $ from 'jquery' */


export default {
  components: {
    NavBar
  },
  setup() {
    /* $.ajax({
      url: "http://localhost:3000/user/account/token/",
      type: "post",
      data: {
        username: 'yxc',
        password: 'pwyxc',
      },
      success(resp) {
        console.log(resp);
      },
      error(resp) {
        console.log(resp);
      }
    });

    $.ajax({
      url: "http://localhost:3000/user/account/info/",
      type: "get",
      headers: {
        // 报文，授权
        Authorization: "Bearer " + "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIxMGZkMmJiYzc5NzE0M2RjYjljZmQ4YjI0N2NlZmY0MyIsInN1YiI6IjIiLCJpc3MiOiJzZyIsImlhdCI6MTY1ODkzNTM3OCwiZXhwIjoxNjYwMTQ0OTc4fQ.qCGfbMv761mlnGr66jTw3s7Q3NjandTDXaGQSk7Z1_c"
      },
      success(resp) {
        console.log(resp);
      },
      error(resp) {
        console.log(resp);
      }
    });

    $.ajax({
      url: "http://localhost:3000/user/account/register/",
      type: "post",
      data: {
        username: "Test",
        password: "pwdl",
        confirmedPassword: "pwdl",
      },
      success(resp) { // resp 就是后端函数返回的 map
        console.log(resp);
      },
      error(resp) {
        console.log(resp);
      }
    }) */
  }
}
</script>

<style>
body {
  background-image: url("@/assets/images/background.png");
  background-size: cover;
}
</style>

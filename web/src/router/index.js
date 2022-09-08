import { createRouter, createWebHistory } from 'vue-router'
import PkIndexView from '@/views/pk/PkIndexView'
import RecordIndexView from '@/views/record/RecordIndexView'
import RecordContentView from '@/views/record/RecordContentView'
import RanklistIndexView from '@/views/ranklist/RanklistIndexView'
import UserBotIndexView from '@/views/user/bot/UserBotIndexView'
import NotFound from '@/views/error/NotFound'
import UserAccountLoginView from '@/views/user/account/UserAccountLoginView'
import UserAccountRegisterView from '@/views/user/account/UserAccountRegisterView'
import UserAccountAcWingWebReceiveCodeView from '@/views/user/account/UserAccountAcWingWebReceiveCodeView'
import store from '@/store'

const routes = [{
        path: "/",
        name: "home",
        redirect: "/pk/",
        meta: {
            requestAuth: true,
        }
    },
    {
        path: "/pk/",
        name: "pk_index",
        component: PkIndexView,
        meta: {
            requestAuth: true,
        }
    },
    {
        path: "/record/",
        name: "record_index",
        component: RecordIndexView,
        meta: {
            requestAuth: true,
        }
    },
    {
        path: "/record/:recordId/",
        name: "record_content",
        component: RecordContentView,
        meta: {
            requestAuth: true,
        }
    },
    {
        path: "/ranklist/",
        name: "ranklist_index",
        component: RanklistIndexView,
        meta: {
            requestAuth: true,
        }
    },
    {
        path: "/user/bot/",
        name: "user_bot_index",
        component: UserBotIndexView,
        meta: {
            requestAuth: true,
        }
    },
    {
        path: "/user/account/login/",
        name: "user_account_login",
        component: UserAccountLoginView,
        meta: {
            requestAuth: false,
        }
    },
    {
        path: "/user/account/register/",
        name: "user_account_register",
        component: UserAccountRegisterView,
        meta: {
            requestAuth: false,
        }
    },
    {
        path: "/user/account/acwing/web/receive_code/",
        name: "user_account_acwing_web_receive_code",
        component: UserAccountAcWingWebReceiveCodeView,
        meta: {
            requestAuth: false,
        }
    },
    {
        path: "/404/",
        name: "404",
        component: NotFound,
        meta: {
            requestAuth: false,
        }
    },
    {
        path: "/:catchAll(.*)",
        redirect: "/404/", // 重定向
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => { // 通过router进入某个页面之前调用这个函数，to表示跳转到某个页面，from表示从哪个页面跳转过去的，next表示要不要将页面执行下一步操作
    if (to.meta.requestAuth && !store.state.user.is_login) { // 需要登录就跳转到登录页面
        next({ name: "user_account_login" });
    } else { // 不需要登录就跳转到默认页面即可
        next();
    }
})

export default router
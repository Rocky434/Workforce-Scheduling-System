import { createRouter, createWebHistory } from "vue-router";
import LoginView from "./views/LoginView.vue";
import ScheduleView from "./views/ScheduleView.vue";
import DashboardView from "./views/DashboardView.vue";
import { useAuthStore } from "./stores/auth";
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/schedule" },
    { path: "/login", component: LoginView, meta: { title: "登入" } },
    {
      path: "/schedule",
      component: ScheduleView,
      meta: { title: "我的班表", role: "EMPLOYEE" },
    },
    {
      path: "/dashboard",
      component: DashboardView,
      meta: { title: "班表總覽", role: "OWNER" },
    },
  ],
});
router.beforeEach(async (to) => {
  const auth = useAuthStore();
  await auth.initialize();
  const user = auth.user;
  if (to.path != "/login" && !user) return "/login";
  if (to.path === "/login" && user)
    return user.role === "OWNER" ? "/dashboard" : "/schedule";
  if (to.meta.role && user?.role !== to.meta.role)
    return user?.role === "OWNER" ? "/dashboard" : "/schedule";
});
export default router;

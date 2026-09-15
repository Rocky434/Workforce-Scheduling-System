<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api } from "../api";
import type { Dashboard, DashboardDay } from "../types";
const next = new Date();
next.setMonth(next.getMonth() + 1);
const month = ref(
    `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, "0")}`,
  ),
  dashboard = ref<Dashboard | null>(null),
  loading = ref(false),
  error = ref("");
const days = computed(() => {
  if (!dashboard.value) return [];
  const daysInMonth = new Date(
    Number(month.value.slice(0, 4)),
    Number(month.value.slice(5, 7)),
    0,
  ).getDate();
  return Array.from({ length: daysInMonth }, (_, dayIndex) => dayIndex + 1);
});
function names(day: number) {
  const date = `${month.value}-${String(day).padStart(2, "0")}`;
  return (
    dashboard.value?.assignments
      .filter((assignment) => assignment.date === date)
      .map((assignment) => assignment.displayName) ?? []
  );
}
function dayInfo(day: number) {
  const date = `${month.value}-${String(day).padStart(2, "0")}`;
  return dashboard.value?.days.find((calendarDay) => calendarDay.date === date);
}
function dayLabel(day: DashboardDay | undefined) {
  if (!day) return "";
  if (day.dayType === "HOLIDAY") {
    if (day.holidayName?.includes("補假")) return day.holidayName;
    if (day.schedulable) {
      return day.holidayName ? `特定節日 · ${day.holidayName}` : "特定節日";
    }
    return day.holidayName ? `國定假日 · ${day.holidayName}` : "假日";
  }
  return day.dayType === "WEEKEND" ? "週末假日" : "";
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    dashboard.value = (
      await api.get<Dashboard>("/owner/dashboard", { params: { month: month.value } })
    ).data;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
<template>
  <div class="dashboard-tools">
    <label>查看月份<input v-model="month" type="month" @change="load" /></label
    ><button class="secondary" @click="load">重新整理</button>
  </div>
  <p v-if="error" class="error">{{ error }}</p>
  <div v-if="loading" class="panel">載入統計中…</div>
  <template v-else-if="dashboard"
    ><section class="stats-grid">
      <article>
        <span>員工人數</span><strong>{{ dashboard.employees.length }}</strong>
      </article>
      <article>
        <span>本月排班人次</span><strong>{{ dashboard.assignments.length }}</strong>
      </article>
      <article>
        <span>已完成 6 天</span
        ><strong>{{
          dashboard.employees.filter((employee) => employee.monthlyDays >= 6).length
        }}</strong>
      </article>
    </section>
    <section class="panel">
      <h2>員工排班統計</h2>
      <table>
        <thead>
          <tr>
            <th>排名</th>
            <th>員工</th>
            <th>本月天數</th>
            <th>本年天數</th>
            <th>進度</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(employee, rank) in dashboard.employees" :key="employee.employeeId">
            <td>{{ rank + 1 }}</td>
            <td>
              <strong>{{ employee.displayName }}</strong>
            </td>
            <td>{{ employee.monthlyDays }}</td>
            <td>{{ employee.yearlyDays }}</td>
            <td>
              <span :class="['badge', employee.monthlyDays >= 6 ? 'ok' : 'pending']">{{
                employee.monthlyDays >= 6
                  ? "已達標"
                  : "尚差 " + (6 - employee.monthlyDays) + " 天"
              }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
    <section class="panel">
      <h2>每日班表</h2>
      <div class="roster">
        <article
          v-for="d in days"
          :key="d"
          :class="{
            'roster-holiday': dayInfo(d)?.dayType === 'HOLIDAY',
            'roster-weekend': dayInfo(d)?.dayType === 'WEEKEND',
            'roster-schedulable': dayInfo(d)?.schedulable === true,
            'roster-unschedulable':
              dayInfo(d) && dayInfo(d)?.schedulable === false,
          }"
          :aria-label="
            dayInfo(d)?.schedulable === true ? '可排班' : '不可排班'
          "
        >
          <b>{{ d }}</b
          ><span>{{
            ["日", "一", "二", "三", "四", "五", "六"][
              new Date(
                `${month}-${String(d).padStart(2, "0")}T00:00:00`,
              ).getDay()
            ]
          }}</span>
          <span v-if="dayLabel(dayInfo(d))" class="roster-day-label">{{
            dayLabel(dayInfo(d))
          }}</span>
          <p v-for="n in names(d)" :key="n">{{ n }}</p>
          <em v-if="!names(d).length">尚無人員排班</em>
        </article>
      </div>
    </section></template
  >
</template>

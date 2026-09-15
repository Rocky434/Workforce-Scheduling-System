<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api } from "../api";
import type { MonthView, CalendarDay } from "../types";
const data = ref<MonthView | null>(null),
  selected = ref(new Set<string>()),
  initialSelected = ref(new Set<string>()),
  loading = ref(true),
  saving = ref(false),
  message = ref(""),
  error = ref("");
const weekdays = ["日", "一", "二", "三", "四", "五", "六"];
const monthLabel = computed(() =>
  data.value ? `${data.value.month.replace("-", " 年 ")} 月` : "",
);
const cells = computed(() =>
  data.value
    ? [
        ...Array(new Date(`${data.value.month}-01T00:00:00`).getDay()).fill(
          null,
        ),
        ...data.value.days,
      ]
    : [],
);
function applyMonthView(view: MonthView) {
  const selectedDates = view.days
    .filter((day) => day.selected)
    .map((day) => day.date);
  data.value = view;
  selected.value = new Set(selectedDates);
  initialSelected.value = new Set(selectedDates);
}
function assignedCount(day: CalendarDay) {
  const wasSelected = initialSelected.value.has(day.date);
  const isSelected = selected.value.has(day.date);
  if (wasSelected && !isSelected) return day.assignedCount - 1;
  if (!wasSelected && isSelected) return day.assignedCount + 1;
  return day.assignedCount;
}
async function load() {
  loading.value = true;
  try {
    const r = await api.get("/schedules/me");
    applyMonthView(r.data);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function toggle(day: CalendarDay) {
  if (
    !day.schedulable ||
    (!selected.value.has(day.date) &&
      assignedCount(day) >= data.value!.dailyCapacity)
  )
    return;
  const copy = new Set(selected.value);
  copy.has(day.date) ? copy.delete(day.date) : copy.add(day.date);
  selected.value = copy;
}
async function save() {
  if (!data.value) return;
  saving.value = true;
  error.value = "";
  message.value = "";
  try {
    applyMonthView(
      (
        await api.put("/schedules/me", {
          month: data.value.month,
          dates: [...selected.value],
        })
      ).data,
    );
    message.value = "班表已成功儲存";
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    saving.value = false;
  }
}
onMounted(load);
</script>
<template>
  <div v-if="loading" class="panel">載入班表中…</div>
  <template v-else-if="data"
    ><section class="summary-row">
      <div>
        <span>排班月份</span><strong>{{ monthLabel }}</strong>
      </div>
      <div>
        <span>已選天數</span
        ><strong :class="{ warn: selected.size < data.minimumDays }"
          >{{ selected.size }} 天</strong
        >
      </div>
      <div>
        <span>排班規則</span
        ><strong>{{ data.minimumDays }}–{{ data.maximumDays }} 天</strong>
      </div>
      <div>
        <span>每日名額</span><strong>{{ data.dailyCapacity }} 人</strong>
      </div>
    </section>
    <section class="panel calendar-panel">
      <div class="calendar-head">
        <div>
          <h2>{{ monthLabel }}</h2>
          <p>點選可排班日期，再一次儲存整月班表。</p>
        </div>
        <div class="legend">
          <span><i class="available"></i>可排</span
          ><span><i class="mine"></i>已選</span
          ><span><i class="full"></i>額滿／休假</span>
        </div>
      </div>
      <div class="weekdays">
        <span v-for="w in weekdays" :key="w">{{ w }}</span>
      </div>
      <div class="calendar">
        <div
          v-for="(day, i) in cells"
          :key="i"
          class="day"
          :class="{
            blank: !day,
            selected: day && selected.has(day.date),
            disabled: day && !day.schedulable,
            'schedulable-holiday':
              day && day.dayType === 'HOLIDAY' && day.schedulable,
            full:
              day &&
              assignedCount(day) >= data.dailyCapacity &&
              !selected.has(day.date),
          }"
          @click="day && toggle(day)"
        >
          <template v-if="day"
            ><b>{{ Number(day.date.slice(-2)) }}</b
            ><span v-if="day.holidayName" class="holiday">{{
              day.holidayName
            }}</span
            ><span v-else-if="day.dayType === 'WEEKEND'">週末</span
            ><span v-else>{{
              selected.has(day.date)
                ? "✓ 已選"
                : `${assignedCount(day)}/${data.dailyCapacity} 人`
            }}</span></template
          >
        </div>
      </div>
    </section>
    <div class="savebar">
      <div>
        <strong>已選 {{ selected.size }} 天</strong
        ><span
          >最少 {{ data.minimumDays }} 天，最多 {{ data.maximumDays }} 天</span
        ><span v-if="message" class="success">{{ message }}</span
        ><span v-if="error" class="error">{{ error }}</span>
      </div>
      <button
        class="primary"
        :disabled="
          saving ||
          selected.size < data.minimumDays ||
          selected.size > data.maximumDays
        "
        @click="save"
      >
        {{ saving ? "儲存中…" : "儲存班表" }}
      </button>
    </div></template
  >
  <div v-else class="panel error">{{ error }}</div>
</template>

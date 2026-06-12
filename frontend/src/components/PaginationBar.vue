<script setup>
import { computed } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

const props = defineProps({
  page: { type: Number, required: true },
  size: { type: Number, required: true },
  total: { type: Number, required: true },
  pages: { type: Number, required: true },
  sizeOptions: { type: Array, default: () => [10, 20, 50] }
})

const emit = defineEmits(['change'])

const currentPage = computed(() => Math.max(props.page, 1))
const totalPages = computed(() => Math.max(props.pages, 1))
const start = computed(() => (props.total === 0 ? 0 : (currentPage.value - 1) * props.size + 1))
const end = computed(() => Math.min(currentPage.value * props.size, props.total))
const pageSizeOptions = computed(() => {
  const options = props.sizeOptions
    .map((item) => Number(item))
    .filter((item) => Number.isFinite(item) && item > 0)
  return options.length > 0 ? options : [10, 20, 50]
})

function changePage(page) {
  const next = Math.min(Math.max(page, 1), totalPages.value)
  if (next !== currentPage.value) {
    emit('change', { page: next, size: props.size })
  }
}

function changeSize(event) {
  emit('change', { page: 1, size: Number(event.target.value) })
}
</script>

<template>
  <div class="pagination">
    <div class="pagination-info">
      <span>共 {{ total }} 条</span>
      <span>{{ start }}-{{ end }}</span>
    </div>

    <div class="pagination-controls">
      <select class="select compact" :value="size" @change="changeSize">
        <option v-for="option in pageSizeOptions" :key="option" :value="option">{{ option }} 条/页</option>
      </select>

      <button class="btn icon" :disabled="currentPage <= 1" title="上一页" @click="changePage(currentPage - 1)">
        <ChevronLeft :size="17" />
      </button>
      <span class="page-index">{{ currentPage }} / {{ totalPages }}</span>
      <button class="btn icon" :disabled="currentPage >= totalPages" title="下一页" @click="changePage(currentPage + 1)">
        <ChevronRight :size="17" />
      </button>
    </div>
  </div>
</template>

<template>
  <div class="main-wrapper">

    <div class="page-header">
      <h2>Search results</h2>
      <span class="page-subtitle">&ldquo;{{ store.search }}&rdquo;</span>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner style="width: 2rem; height: 2rem" strokeWidth="6" />
    </div>

    <div v-else-if="groupKeys.length === 0" class="empty-state">
      <i class="pi pi-search"></i>
      No results found
    </div>

    <div v-else class="result-list">
      <div v-for="title in groupKeys" :key="title" class="page-card">
        <div class="page-card-header">
          <i class="pi pi-file-edit section-icon"></i>
          <span class="result-title">{{ title }}</span>
          <Tag :value="matchLabel(results[title])" severity="secondary" class="result-count" />
          <Button
            icon="pi pi-arrow-right"
            text
            size="small"
            severity="secondary"
            @click="clickedTranscript(results[title][0].id, results[title])"
            v-tooltip.top="'Open transcript'"
            class="result-open-btn"
          />
        </div>
        <div class="page-content">
          <Tag v-if="hasTitleMatch(results[title])" value="Title match" severity="info" class="title-match-tag" />
          <div v-if="contentPages(results[title]).length" class="page-refs">
            <a
              v-for="p in contentPages(results[title])"
              :key="p"
              href="#"
              @click.prevent="clickedTranscript(results[title][0].id, results[title])"
              class="page-ref-link"
            >
              <Tag :value="`p. ${p + 1}`" severity="secondary" />
            </a>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from "vue";
import { authFetch } from "@/requests";
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

import { useRouter } from 'vue-router'
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)

interface DtoSearchResult {
  id: string
  title: string
  srType: 'title' | 'content'
  pageNumber: number
}

type SearchResult = Record<string, DtoSearchResult[]>
const results = ref<SearchResult>({});

const groupKeys = computed(() => Object.keys(results.value))

function hasTitleMatch(items: DtoSearchResult[]): boolean {
  return items.some(i => i.srType === 'title')
}

function contentPages(items: DtoSearchResult[]): number[] {
  const pages = items.filter(i => i.srType === 'content').map(i => i.pageNumber)
  return [...new Set(pages)].sort((a, b) => a - b)
}

function matchLabel(items: DtoSearchResult[]): string {
  return `${items.length} match${items.length > 1 ? 'es' : ''}`
}

const request = async() => {
  loading.value = true
  try {
    store.setLoading(true)
    const response = await authFetch("search", {
      method: "POST",
      headers: {
        "Content-Type": "text/plain",
      },
      body: store.search
    });

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }
    results.value = await response.json()
  } catch (err: any) {
    console.error(err.message)
    error.value = err.message || "Something went wrong.";
  } finally {
    store.setLoading(false)
    loading.value = false;
  }
}

function clickedTranscript(fileId: string, items: DtoSearchResult[]) {
  const pages = items.filter(i => i.srType === 'content').map(i => i.pageNumber)
  store.setSRPages(pages)
  router.push({ name: 'transcriptSearchResult', params: { fileId, pageNumber: pages[0] ?? 0 } })
}

onMounted(() => {
  request()
});

</script>

<style scoped>
.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  margin: 0;
}

.page-subtitle {
  font-size: 0.875rem;
  color: var(--p-surface-500);
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 4rem 0;
}

.empty-state {
  color: var(--p-surface-400);
  font-size: 0.875rem;
  font-style: italic;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.result-list {
  display: flex;
  flex-direction: column;
}

.page-card {
  background-color: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  border-radius: 0.5rem;
  margin-bottom: 1rem;
  overflow: hidden;
}

.page-card-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 1rem;
  background-color: var(--p-surface-50);
  border-bottom: 1px solid var(--p-surface-200);
}

.section-icon {
  color: var(--p-primary-500);
  font-size: 0.875rem;
}

.result-title {
  font-size: 0.875rem;
  font-weight: 500;
}

.result-count {
  margin-left: 0.25rem;
}

.result-open-btn {
  margin-left: auto;
}

.page-content {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.title-match-tag {
  align-self: flex-start;
}

.page-refs {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.page-ref-link {
  text-decoration: none;
}
</style>
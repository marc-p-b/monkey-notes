<template>
  <div class="main-wrapper">

    <div class="page-header">
      <h2>Named Entities</h2>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner style="width: 2rem; height: 2rem" strokeWidth="6" />
    </div>

    <div v-else-if="verbKeys.length === 0" class="empty-state">
      <i class="pi pi-info-circle"></i>
      No named entities found
    </div>

    <div v-else class="page-card">
      <Tabs :value="verbKeys[0]">
        <TabList>
          <Tab v-for="verb in verbKeys" :key="verb" :value="verb">
            <i :class="['pi', verbIcon(verb), 'tab-icon']"></i>
            {{ verbLabel(verb) }}
            <Tag :value="String(Object.keys(neMap[verb]).length)" severity="secondary" class="tab-count" />
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel v-for="verb in verbKeys" :key="verb" :value="verb">
            <div v-if="Object.keys(neMap[verb]).length === 0" class="empty-state">No entries</div>
            <div v-else class="entity-grid">
              <div v-for="(dtos, value) in neMap[verb]" :key="value" class="entity-group">
                <span class="entity-value">{{ value }}</span>
                <div class="entity-refs">
                  <a
                    v-for="dto in dtos"
                    :key="dto.uuid"
                    href="#"
                    @click.prevent="goToTranscript(dto.fileId, dto.pageNumber)"
                    class="entity-ref-link"
                  >
                    <Tag severity="secondary">
                      <i class="pi pi-file-edit"></i> {{ dto.fileName }} &middot; p.{{ dto.pageNumber + 1 }}
                    </Tag>
                  </a>
                </div>
              </div>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>

  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from "vue";
import { authFetch } from "@/requests";
import { useRouter } from 'vue-router'
const router = useRouter()

interface DtoNamedEntity {
  uuid: string
  verb: string
  value: string
  fileId: string
  fileName: string
  pageNumber: number
  start: number
  end: number
}

type NamedEntityMap = Record<string, Record<string, DtoNamedEntity[]>>;
const neMap = ref<NamedEntityMap>({});

const loading = ref(true)
const error = ref<string | null>(null)

const VERB_ORDER = ['tag', 'person', 'email']

const VERB_LABELS: Record<string, string> = {
  tag: 'Tags',
  person: 'People',
  email: 'Emails',
}

const VERB_ICONS: Record<string, string> = {
  tag: 'pi-tag',
  person: 'pi-user',
  email: 'pi-at',
}

const verbKeys = computed(() =>
  Object.keys(neMap.value).sort((a, b) => VERB_ORDER.indexOf(a) - VERB_ORDER.indexOf(b))
)

function verbLabel(verb: string): string {
  return VERB_LABELS[verb] || verb
}

function verbIcon(verb: string): string {
  return VERB_ICONS[verb] || 'pi-bookmark'
}

function goToTranscript(fileId: string, pageNumber: number) {
  router.push({ name: 'transcript', params: { fileId }, hash: '#pageNumber' + pageNumber })
}

async function fetchVerbs() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("ne/verbs");
    if (!response.ok) throw new Error("Network response was not ok");
    neMap.value = await response.json()
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load named entities.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchVerbs()
});

</script>

<style scoped>
.page-header {
  margin-bottom: 1.5rem;
}

.page-header h2 {
  margin: 0;
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

.page-card {
  background-color: var(--p-surface-0);
  border: 1px solid var(--p-surface-200);
  border-radius: 0.5rem;
  overflow: hidden;
}

.tab-icon {
  margin-right: 0.4rem;
}

.tab-count {
  margin-left: 0.4rem;
}

.entity-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.entity-group {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.entity-value {
  font-size: 0.875rem;
  font-weight: 500;
  min-width: 7rem;
  color: var(--p-surface-700);
}

.entity-refs {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.entity-ref-link {
  text-decoration: none;
  cursor: pointer;
}
</style>
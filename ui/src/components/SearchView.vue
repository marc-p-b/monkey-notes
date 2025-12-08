<template>
  <div class="home-wrapper">

    <h2>Search results : {{  store.search  }}</h2>
    <ul>
      <li v-for="(a, b) in results">
        {{b}} :
        <ul>
          <li v-for="r in a">
            <a href="#" @click.prevent="clickedTranscript(r.id, a)">page {{r.pageNumber + 1}}</a>
          </li>
        </ul>
      </li>
    </ul>

  </div>
</template>

<script lang="ts" setup>
import {ref, onMounted, defineProps} from "vue";
import {authFetch} from "@/requests";
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

import { useRouter } from 'vue-router'
const router = useRouter()

const loading = ref(true)
const error = ref<string | null>(null)

interface DtoResults {
  id: string
  title: string
  pageNumber: number
}

type SearchResult = Record<DtoResults, DtoResults[]>
const results = ref<SearchResult>({});

const request = async() => {

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
    store.setLoading(false)
    results.value = await response.json()
  } catch (err: any) {
    console.log(err.message)
    error.value = err.message || "Something went wrong.";
  } finally {
    loading.value = false;
  }

}

function clickedTranscript(fileId : string, results : DtoResults[]) {
  let pages = []
  results.forEach(result => {
    pages.push(result.pageNumber)
  });

  const pageNumber = 1
  store.setSRPages(pages)
  router.push({ name: 'transcriptSearchResult', params: { fileId, pageNumber } })
}

onMounted(() => {
  request()
});

</script>

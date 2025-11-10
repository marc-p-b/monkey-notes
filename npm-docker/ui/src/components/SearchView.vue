<template>
  <div class="home-wrapper">

    <h2>Search results : {{  store.search  }}</h2>
    <ul>
      <li v-for="result in results">
        {{result}}
      </li>
    </ul>

  </div>
</template>

<script lang="ts" setup>
import {ref, onMounted, defineProps} from "vue";
import {authFetch} from "@/requests";
import { useUiStore } from '@/composables/store.js'
const store = useUiStore()

const loading = ref(true)
const error = ref<string | null>(null)

interface DtoResults {
  id: string
  title: string
}

const results = ref<DtoResults[]>([])

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
    //console.log(response.json())
    results.value = await response.json()
  } catch (err: any) {
    console.log(err.message)
    error.value = err.message || "Something went wrong.";
  } finally {
    loading.value = false;
  }

}


onMounted(() => {
  request()
});

</script>

<template>
  <div class="p-6">
    <h2 class="text-2xl font-bold mb-4">Transcripts</h2>

    <div v-if="loading" class="text-gray-500">Loading...</div>
    <div v-else-if="error" class="text-red-500">{{ error }}</div>

    <ul v-else class="space-y-2">
      <li
        v-for="transcript in transcripts"
        :key="transcript.fileId"
        class="p-3 bg-white rounded shadow hover:bg-gray-50 transition"
      >
        {{ transcript.name }}
      </li>
    </ul>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";

// Define the minimal DTO for TypeScript
interface DtoTranscript {
  fileId: string;
  name: string;
}

// State
const transcripts = ref<DtoTranscript[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

async function authFetch(url, options = {}) {
  const token = localStorage.getItem("token");

  const headers = {
    ...(options.headers || {}),
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  };

  const response = await fetch(url, { ...options, headers });

  if (response.status === 401) {
    // optional: handle expired/invalid token
    localStorage.removeItem("token");
    window.location.href = "/login";
  }

  console.log(response.json());

  return response;
}

const response = await authFetch("http://localhost:8080/test/recent");


// // Fetch data using default fetch
// async function fetchTranscripts() {
//   loading.value = true;
//   error.value = null;
//   try {
//     const response = await fetch("http://localhost:8080/test/recent", { ...init, headers });
//     if (!response.ok) throw new Error("Network response was not ok");
//     const data: DtoTranscript[] = await response.json();
//     //transcripts.value = data;
//     console.log(data);
//   } catch (err: any) {
//     console.error(err);
//     error.value = "Failed to load transcripts.";
//   } finally {
//     loading.value = false;
//   }
// }

// Load on mount
onMounted(() => {
  fetchTranscripts();
});
</script>

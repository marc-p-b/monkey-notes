<template>
  <div class="home-wrapper">

    <h2>Named Entities</h2>

    <TabView>
      <TabPanel
          v-for="(mapValues, verb) in neMap"
          :key="verb"
          :header="`${verb} (${Object.keys(mapValues).length})`"
      >
        <p>
          {{}}
          <ul>
            <li v-for="(dtos, value) in mapValues">
              {{value}} :
              <ul>
                <li v-for="dto in dtos">
                  {{dto.fileName}} <a :href="`transcript/${dto.fileId}`"><i class="pi pi-link"/></a>
                </li>
              </ul>
            </li>
          </ul>
        </p>
      </TabPanel>
    </TabView>


  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from "vue";
import {authFetch} from "@/requests";

interface NamedEntity {
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

async function fetchVerbs() {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("ne/verbs");
    if (!response.ok) throw new Error("Network response was not ok");
    neMap.value = await response.json()
    //console.log(neMap.value)

    const c = Object.keys(neMap.value).length
    console.log(c)
  } catch (err: any) {
    console.error(err);
    error.value = "Failed to load processes.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchVerbs()
});

</script>

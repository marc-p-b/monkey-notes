<template>
  <div class="home-wrapper">

    <h2>Named Entities</h2>

    <ul>
      <li v-for="(mapValues, verb) in neMap">
        {{verb}} :
        <ul>
          <li v-for="(dtos, value) in mapValues">
            {{value}} :
            <ul>
              <li v-for="dto in dtos">
                {{dto.uuid}}
              </li>
            </ul>
          </li>
        </ul>
      </li>
    </ul>


    <TabView>
      <TabPanel
          v-for="(mapValues, verb) in neMap"
          :key="verb"
          :header="`${verb} ()`"
      >
        <p>

          <ul>
            <li v-for="(dtos, value) in mapValues">
              {{value}} :
              <ul>
                <li v-for="dto in dtos">
                  {{dto.uuid}}
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
    console.log(neMap.value)
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

<template>

  <p v-html="text"></p>
  <a :href="page.imageUrl">page {{page.pageNumber + 1}} source</a> -
  <a href="#" @click.prevent="updatePage(page)">update</a>

</template>

<script lang="ts" setup>
import {ref, defineProps, onMounted} from "vue";
import {authFetch} from "@/requests";
// import { useRouter } from 'vue-router'
// const router = useRouter()

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

interface Page {
  fileId: string
  username: string
  pageNumber: number
  transcript: string
  transcriptHtml: string
  transcriptTook: number
  tokensPrompt: number
  tokensResponse: number
  version: number
  aiModel: string
  imageUrl: string
  completed: boolean
  listNamedEntities: NamedEntity[]
}

function replaceSubstring(str, start, end, replacement) {
  return str.slice(0, start) + replacement + str.slice(end);
}

async function updatePage(page) {
  loading.value = true;
  error.value = null;
  try {
    const response = await authFetch("transcript/update/" + page.fileId + '/' + page.pageNumber);
    if (!response.ok) throw new Error("Network response was not ok");

    console.log(response)

  } catch (err: any) {
    console.error(err);
    error.value = "Failed to update transcript page.";
  } finally {
    loading.value = false;
  }
}

const props = defineProps<{ page: Page }>();
const text = ref()

const loading = ref(true)
const error = ref<string | null>(null)

let transcript = props.page.transcript;

let lFix = 0;
props.page.listNamedEntities.forEach(ne => {
  let repl = "";
  if(ne.verb == 'h2') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h2>" + ne.value + "</h2>");
  } else if(ne.verb == 'h3') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h3>" + ne.value + "</h3>");
  } else if(ne.verb == 'h4') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h4>" + ne.value + "</h4>");
  } else if(ne.verb == 'h5') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h5>" + ne.value + "</h5>");
  } else if(ne.verb == 'h6') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h5>" + ne.value + "</h5>");
  } else {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "|" + ne.verb + ":" + ne.value + "|");
  }
  lFix += transcript.length - repl.length;
  transcript = repl;

});
transcript = transcript.replaceAll("\n", "<br/>");

text.value = transcript


</script>

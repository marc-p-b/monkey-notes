<template>


  <p v-html="text"></p>
  page {{page.pageNumber}}

</template>

<script lang="ts" setup>
import {ref, defineProps} from "vue";

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

const props = defineProps<{ page: Page }>();

const text = ref()

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

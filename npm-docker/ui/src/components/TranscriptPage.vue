<template>
  <span :id="'pageNumber' + page.pageNumber" />
  <p v-if="editMode===false" v-html="text" @click.prevent="switchEdit()"></p>
  <div v-else class="edit-container">


    <div class="flex-row">
      <div class="left">
      <Textarea
          v-model="text2"
          auto-resize
          class="w-full"
      />
      </div>
      <div class="right">
        <img
            :src="page.imageUrl"
            alt="preview"
            class="preview-img"
        />
      </div>

    </div>

    <!-- Buttons below -->
    <div class="buttons">
      <Button @click.prevent="save" label="save" />
      <Button @click.prevent="closeEdit" label="close" />
    </div>
  </div>
  <a :href="page.imageUrl">page {{page.pageNumber + 1}} source</a> -
  <a href="#" @click.prevent="updatePage(page)">update</a>
  <span v-if="page.deltas === 1"> - {{page.deltas}} delta</span>
  <span v-else-if="page.deltas > 1"> - {{page.deltas}} deltas</span>
</template>

<style>

.edit-container {
  display: flex;
  flex-direction: column;  /* stack flex-row and buttons */
  gap: 1rem;
}

.flex-row {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.left {
  flex: 1;                /* textarea takes remaining space */
}

.right {
  flex: 0 0 auto;         /* image keeps natural width */
}

.preview-img {
  width: 80%;       /* 40% of flex row or any fixed px */
  height: auto;
}

.buttons {
  display: flex;
  gap: 0.5rem;            /* space between buttons */
}

</style>

<script lang="ts" setup>
import {ref, defineProps, onMounted} from "vue";
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

interface Page {
  fileId: string
  username: string
  pageNumber: number
  transcript: string
  //transcriptHtml: string
  transcriptTook: number
  tokensPrompt: number
  tokensResponse: number
  version: number
  aiModel: string
  imageUrl: string
  completed: boolean
  listNamedEntities: NamedEntity[]
  cols: number
  rows: number
  deltas: number
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

const switchEdit = async () => {
  editMode.value = true
}
const closeEdit = async () => {
  editMode.value = false
}

const save = async () => {
  const fileId = props.page.fileId
  const pageNumber = props.page.pageNumber
  editMode.value = false;

  try {
    const response = await authFetch("transcript/edit/" + fileId + "/" + pageNumber, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: text2.value,
    });

    if (!response.ok) {
      throw new Error(`Server error: ${response.status}`);
    }
  } catch (err: any) {
    error.value = err.message || "Something went wrong.";
  } finally {
    loading.value = false;
  }
}

const props = defineProps<{ page: Page }>();
const text = ref()
const text2 = ref()

const loading = ref(true)
const error = ref<string | null>(null)

const editMode = ref(false)

let transcript = props.page.transcript;
text2.value = transcript

let lFix = 0;
props.page.listNamedEntities.forEach(ne => {
  let repl = "";
  if(ne.verb == 'h2') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h2 id='"+ne.uuid+"'>" + ne.value + "</h2>");
  } else if(ne.verb == 'h3') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h3 id='"+ne.uuid+"'>" + ne.value + "</h3>");
  } else if(ne.verb == 'h4') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h4 id='"+ne.uuid+"'>" + ne.value + "</h4>");
  } else if(ne.verb == 'h5') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h5 id='"+ne.uuid+"'>" + ne.value + "</h5>");
  } else if(ne.verb == 'h6') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<h6 id='"+ne.uuid+"'>" + ne.value + "</h6>");
  } else if(ne.verb == 'tag') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<span id='"+ne.uuid+"'>" + ne.value + " <i class='pi pi-tag'></i></span>");
  } else if(ne.verb == 'person') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<span id='"+ne.uuid+"'>" + ne.value + " <i class='pi pi-user'></i></span>");
  } else if(ne.verb == 'email') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<span id='"+ne.uuid+"'>" + ne.value + " <i class='pi pi-envelope'></i></span>");
  } else if(ne.verb == 'link') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<span id='"+ne.uuid+"'>" + ne.value + " <i class='pi pi-link'></i></span>");
  } else if (ne.verb == 'dateUs' || ne.verb == 'dateIntl') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<span id='"+ne.uuid+"'>" + ne.value + " <i class='pi pi-calendar'></i></span>");
  } else if(ne.verb == 'checked') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<input id='"+ne.uuid+"' type='checkbox' checked /><label for='"+ne.uuid+"'>"+ne.value+"</label>");
  } else if(ne.verb == 'unchecked') {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "<input id='"+ne.uuid+"' type='checkbox' /><label for='"+ne.uuid+"'>"+ne.value+"</label>");
  } else {
    repl = replaceSubstring(transcript, ne.start - lFix, ne.end - lFix, "|" + ne.verb + ":" + ne.value + "|");
  }
  lFix += transcript.length - repl.length;
  transcript = repl;

});
transcript = transcript.replaceAll("\n", "<br/>");

text.value = transcript



</script>

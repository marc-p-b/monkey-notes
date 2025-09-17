



<template>

  <form action="" @submit.prevent="addTask">
    <fieldset role="group">
      <input type="text" v-model="taskName" placeholder="new task"/>
      <button :disabled="taskName.length===0">add</button>
      </fieldset>
    <button @click="orderList">order</button>
    <button @click="delCompleted">del done</button>
    <label>
      <input type="checkbox" v-model="hideCompleted"/>hide completed
    </label>
  </form>

  <p v-if="taskList.length == 0">no tasks</p>

  <ul v-if="taskList.length > 0">
    <li v-for="task in orderedList()" :key="task">
      <!--input type="checkbox" value="{{task.done}}" @click="checkTask(task)"/-->
      <input type="checkbox" value="{{task.done}}" v-model="task.done" />
      <span :class="{doneTask : task.done}">{{task.name}}</span>
    </li>
  </ul>
</template>

<script setup>
import {ref} from "vue"

const taskName = ref('')
const taskList = ref([])
const hideCompleted = ref(false)

const addTask = () => {
  if(taskName.value.length == 0) {
    return
  }
  taskList.value.push({
    name : taskName.value,
    done : false
  })
  taskName.value=''

}

const orderedList = () => {
  const list = taskList.value.toSorted((a, b) => a.done > b.done ? 1 : -1)
  if(hideCompleted.value === true) {
    return list.filter(t => t.done === false)
  }
  return list
}

const delCompleted = () => {
  taskList.value = taskList.value.filter(t=>!t.done)
}

</script>


<style>

.doneTask {
  text-decoration-line: line-through;
}

</style>
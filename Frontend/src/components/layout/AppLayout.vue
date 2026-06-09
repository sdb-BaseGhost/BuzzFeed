<script setup>
import LeftSidebar from './LeftSidebar.vue'
import RightSidebar from './RightSidebar.vue'
</script>

<template>
  <div class="flex justify-center h-screen mx-auto max-w-[1280px]">
    <LeftSidebar class="w-[60px] lg:w-[275px] flex-shrink-0" />
    <main id="main-scroll" class="w-full max-w-[600px] border-x border-border-custom h-screen overflow-y-auto">
      <router-view />
    </main>
    <RightSidebar class="w-[350px] flex-shrink-0 hidden lg:block ml-7" />
  </div>
</template>

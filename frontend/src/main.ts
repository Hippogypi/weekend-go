import { createApp } from 'vue';

import App from './App.vue';
import { router } from './router';
import { sessionStore, weekendGoApi } from './services';
import './styles/base.css';

async function bootstrap(): Promise<void> {
  await sessionStore.restoreSession(weekendGoApi);

  createApp(App).use(router).mount('#app');
}

bootstrap();

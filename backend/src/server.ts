import { app } from './app.js';
import { env } from './config/env.js';

app.listen(env.PORT, () => {
  console.log(`[BUZTRACK API] Running in ${env.NODE_ENV} mode on port ${env.PORT}`);
});

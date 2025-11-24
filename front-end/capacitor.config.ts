import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.toastlab.pos',
  appName: 'Toast Lab App',
  webDir: 'build',
  server: {
    url: 'https://toastlab.duckdns.org/user',
    cleartext: true,
  },
};

export default config;

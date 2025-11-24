// vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import path from 'path'

export default defineConfig(({ command, mode }) => {
  const baseConfig: any = {
    plugins: [react()],
    resolve: {
      extensions: ['.js', '.jsx', '.ts', '.tsx', '.json'],
      alias: { '@': path.resolve(__dirname, './src') },
    },
    build: { target: 'esnext', outDir: 'build' },
    server: {
      port: 3000,
      strictPort: true,
      open: '/login',
      proxy: {
        // 프론트는 오직 가맹점 서버로만 프록시
        '/api': {
          target: 'http://localhost:8082',   // ✅ '/user' 제거
          changeOrigin: true,
          // rewrite 불필요 (경로 보존)
        },
        // ⚠️ '/admin' 프록시 만들지 마세요 (8081로 새는 원인)
      },
    },
  };

  if (command === 'build' && mode !== 'android') {
     // 웹 배포용 (Spring Boot static 폴더로 갈 때)
     baseConfig.base = '/user/';
  } else if (mode === 'android') {
     // 앱 배포용 (Capacitor 내부용) -> 절대경로 '/' 또는 상대경로 './'
     baseConfig.base = './'; 
  }

  return baseConfig;
});

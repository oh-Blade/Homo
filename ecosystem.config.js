module.exports = {
  apps: [{
    name: 'journey-notes',
    script: 'server.js',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'development',
      PORT: 1234
    },
    env_production: {
      NODE_ENV: 'production',
      PORT: 1234
    },
    error_file: './logs/error.log',
    out_file: './logs/out.log',
    log_file: './logs/combined.log',
    time: true,
    min_uptime: '10s',
    max_restarts: 10,
    node_args: '--max-old-space-size=1024',
    kill_timeout: 5000,
    listen_timeout: 3000,
    env_file: '.env'
  }]
};

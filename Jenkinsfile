pipeline {
    agent any

    environment {
        COMPOSE_FILE = "docker-compose.user.yml"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Deploy') {
            steps {
                sh """
                docker compose -f ${COMPOSE_FILE} down || true
                docker compose -f ${COMPOSE_FILE} up -d --build
                """
            }
        }
    }
}

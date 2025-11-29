// Jenkinsfile for the User Project

pipeline {
    // Run on any available agent
    agent any

    tools {
        // Use the 'docker' tool configured in Jenkins Global Tool Configuration
        dockerTool 'docker'
    }

    // Environment variables used throughout the pipeline
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKERHUB_USERNAME    = 'loopwhile' // As per DevOps.md
        USER_BACKEND_IMAGE    = "${DOCKERHUB_USERNAME}/ict05-final-user-backend"
        USER_PDF_IMAGE        = "${DOCKERHUB_USERNAME}/ict05-final-user-pdf"
        USER_FRONTEND_IMAGE   = "${DOCKERHUB_USERNAME}/ict05-final-user-frontend"
    }

    stages {
        // Stage 1: Checkout source code from Git
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                git branch: 'main', url: 'https://github.com/loopwhile/ict05_final_user-kw.git'
            }
        }

        // Stage 2: Build and push the Spring Boot backend image
        stage('Build & Push User Backend') {
            steps {
                script {
                    echo "Building User Backend Docker image..."
                    // Assumes the Dockerfile for the backend is in the project root
                    def customImage = docker.build(USER_BACKEND_IMAGE, ".")
                    
                    echo "Pushing User Backend image to Docker Hub..."
                    docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS) {
                        customImage.push("latest")
                    }
                }
            }
        }

        // Stage 3: Build and push the Python PDF service image
        stage('Build & Push User PDF Service') {
            steps {
                script {
                    echo "Building User PDF Service Docker image..."
                    // Assumes the Dockerfile for the PDF service is in the 'python-pdf-download' subdirectory
                    def customImage = docker.build(USER_PDF_IMAGE, "python-pdf-download")
                    
                    echo "Pushing User PDF Service image to Docker Hub..."
                    docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS) {
                        customImage.push("latest")
                    }
                }
            }
        }
        
        // Stage 4: Build and push the React frontend image
        stage('Build & Push User Frontend') {
            steps {
                script {
                    echo "Building User Frontend Docker image..."
                    // Assumes the Dockerfile for the frontend is in the 'front-end' subdirectory
                    def customImage = docker.build(USER_FRONTEND_IMAGE, "front-end")
                    
                    echo "Pushing User Frontend image to Docker Hub..."
                    docker.withRegistry('https://registry.hub.docker.com', DOCKERHUB_CREDENTIALS) {
                        customImage.push("latest")
                    }
                }
            }
        }

        // Stage 5: Trigger the deployment on the EC2 server
        stage('Deploy to EC2') {
            steps {
                echo "Executing deployment script on EC2 for 'user' services..."
                sh '/home/ubuntu/deploy/deploy.sh user'
            }
        }
    }

    // Post-build actions
    post {
        always {
            echo 'User pipeline finished.'
        }
    }
}
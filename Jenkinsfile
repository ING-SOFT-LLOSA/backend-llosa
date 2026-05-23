pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            agent {
                docker {
                    image 'maven:3.9.8-eclipse-temurin-21-alpine'
                    reuseNode true
                }
            }
            steps {
                sh '''
                    mvn clean verify
                    mvn spring-boot:run
                '''
            }
        }

        stage('SonarQube Analysis') {
            environment {
                scannerHome = tool 'SonarScanner'
            }
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=llosa-backend \
                            -Dsonar.projectName="Llosa Edificaciones Backend" \
                            -Dsonar.projectVersion=1.0.0
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Deploy (Docker Compose)') {
            steps {
                withCredentials([
                    string(credentialsId: 'KEY1', variable: 'KEY1'),
                    string(credentialsId: 'SPRING_PROFILES_ACTIVE', variable: 'SPRING_PROFILES_ACTIVE')
                ]) {
                    sh '''
                        docker compose down
                        docker compose up -d --build backend
                    '''
                }
            }
        }
    }
}

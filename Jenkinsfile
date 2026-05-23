pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
        stage('Checkout Repo') {
            steps {
                checkout scm
            }
        }
        
        stage('Test (En Contenedor Java)') {
            agent {
                docker {
                    image 'openjdk:21-jdk-slim'
                    reuseNode true 
                }
            }
            steps {
                sh '''
                    mvn clean test
                '''
            }
        }
        
        stage('SonarQube Analysis') {
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
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy (Docker Compose)') {
            steps {
                withCredentials([string(credentialsId: 'KEY1', variable: 'KEY1')]) {
                    sh '''
                        docker compose down
                        docker compose up -d --build
                    '''
                }
            }
        }
    }
}
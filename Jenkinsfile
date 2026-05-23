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
                    image 'openjdk:25-ea-21-jdk-slim'
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
            environment {
                scannerHome = tool 'SonarScanner'
            }
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    sh "${scannerHome}/bin/sonar-scanner"
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
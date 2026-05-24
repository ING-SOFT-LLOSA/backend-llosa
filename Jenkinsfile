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
                    mvn clean package -DskipTests -Dmaven.repo.local=.m2/repository
                '''
            }
        }

        stage('SonarQube Analysis') {
            agent {
                docker {
                    image 'maven:3.9.8-eclipse-temurin-21-alpine'
                    reuseNode true
                }
            }
            environment {
                scannerHome = tool 'SonarScanner'
            }
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        export SONAR_USER_HOME="${WORKSPACE}/.sonar"
                        mkdir -p "${SONAR_USER_HOME}"
                        ${scannerHome}/bin/sonar-scanner
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
                    string(credentialsId: 'FIREBASE_API_KEY_LLOSA', variable: 'FIREBASE_API_KEY_LLOSA'),
                    string(credentialsId: 'DOMINIO_CORPORATIVO_LLOSA', variable: 'DOMINIO_CORPORATIVO_LLOSA'),
                    string(credentialsId: 'SHOW_SQL_LLOSA', variable: 'SHOW_SQL_LLOSA'),
                    string(credentialsId: 'FIREBASE_CREDENTIALS_PATH_LLOSA', variable: 'FIREBASE_CREDENTIALS_PATH_LLOSA'),
                    string(credentialsId: 'SPRING_FLYWAY_SCHEMAS_LLOSA', variable: 'SPRING_FLYWAY_SCHEMAS_LLOSA')
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
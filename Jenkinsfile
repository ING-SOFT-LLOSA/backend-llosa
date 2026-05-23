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
                    mvn spring:boot run
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
                        export COREPACK_HOME="$WORKSPACE/.corepack"
                        export SONAR_TOKEN="${SONAR_AUTH_TOKEN:-$SONAR_TOKEN}"
                        corepack pnpm --package=sonarqube-scanner@4 dlx sonar-scanner \
                            -Dsonar.host.url="$SONAR_HOST_URL"
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
                    string(credentialsId: 'SPRING_PROFILES_ACTIVE', variable: 'SPRING_PROFILES_ACTIVE'),
                    string(credentialsId: 'FIREBASE_API_KEY', variable: 'FIREBASE_API_KEY'),
                    string(credentialsId: 'DOMINIO_CORPORATIVO', variable: 'DOMINIO_CORPORATIVO'),
                    string(credentialsId: 'SHOW_SQL', variable: 'SHOW_SQL'),
                    string(credentialsId: 'FIREBASE_CREDENTIALS_PATH', variable: 'FIREBASE_CREDENTIALS_PATH')
                ]) {
                    sh '''
                        docker compose down
                        docker compose up -d --build backend-llosa
                    '''
                }
            }
        }
    }
}
                ]) {
                    sh '''
                        docker compose down
                        docker compose up -d --build backend-llosa
                    '''
                }
            }
        }
    }
}
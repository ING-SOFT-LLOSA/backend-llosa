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
            when {
                branch 'test'
            }
            agent {
                docker {
                    image 'maven:3.9.8-eclipse-temurin-21-alpine'
                    reuseNode true
                }
            }
            steps {
                sh '''
                    mvn clean package -Dmaven.test.skip=true -Dmaven.repo.local=.m2/repository
                '''
            }
        }

        stage('SonarQube Analysis') {
            when {
                branch 'test'
            }
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
            when {
                branch 'test'
            }
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Deploy Dev') {
            when {
                branch 'dev'
            }
            steps {
                withCredentials([
                    file(credentialsId: 'LLOSA_SECRETS_BACKEND_DEV', variable: 'ENV_FILE'),
                    file(credentialsId: 'FIREBASE_SERVICE_ACCOUNT_LLOSA', variable: 'FIREBASE_SA_FILE')
                ]) {
                    sh '''
                        STAGE=dev
                        mkdir -p ./secrets
                        cp "$FIREBASE_SA_FILE" ./secrets/firebase-service-account.json
                        chmod 644 ./secrets/firebase-service-account.json

                        rm -f .env
                        cp "$ENV_FILE" .env
                        echo "=== .env inyectado ==="
                        cat .env
                        echo "======================="

                        docker compose -p llosa_dev down --remove-orphans
                        docker compose -p llosa_dev up --build --no-start
                        docker cp ./secrets/firebase-service-account.json llosa_backend_${STAGE}:/app/secrets/firebase-service-account.json
                        docker compose -p llosa_dev start
                    '''
                }
            }
        }

        stage('Deploy Test') {
            when {
                branch 'test'
            }
            steps {
                withCredentials([
                    file(credentialsId: 'LLOSA_SECRETS_BACKEND_TEST', variable: 'ENV_FILE'),
                    file(credentialsId: 'FIREBASE_SERVICE_ACCOUNT_LLOSA', variable: 'FIREBASE_SA_FILE')
                ]) {
                    sh '''
                        STAGE=test
                        mkdir -p ./secrets
                        cp "$FIREBASE_SA_FILE" ./secrets/firebase-service-account.json
                        chmod 644 ./secrets/firebase-service-account.json

                        rm -f .env
                        cp "$ENV_FILE" .env
                        echo "=== .env inyectado ==="
                        cat .env
                        echo "======================="

                        docker compose -p llosa_test down --remove-orphans
                        docker compose -p llosa_test up --build --no-start
                        docker cp ./secrets/firebase-service-account.json llosa_backend_${STAGE}:/app/secrets/firebase-service-account.json
                        docker compose -p llosa_test start
                    '''
                }
            }
        }

        stage('Verify Deployment Dev') {
            when {
                branch 'dev'
            }
            steps {
                sh '''
                    set +e
                    CONTAINER=llosa_backend_dev

                    echo "Esperando hasta 120s a que el healthcheck reporte healthy..."
                    for i in $(seq 1 15); do
                        HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' "$CONTAINER" 2>/dev/null)
                        RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)
                        RESTARTS=$(docker inspect -f '{{.RestartCount}}' "$CONTAINER" 2>/dev/null)
                        echo "  intento $i/15 -> Running=$RUNNING Health=$HEALTH Restarts=$RESTARTS"
                        if [ "$RUNNING" != "true" ]; then break; fi
                        if [ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "n/a" ]; then break; fi
                        sleep 5
                    done

                    echo ""
                    echo "=================================================="
                    echo "  docker compose ps"
                    echo "=================================================="
                    docker compose -p llosa_dev ps

                    echo ""
                    echo "=================================================="
                    echo "  Estado del contenedor ($CONTAINER)"
                    echo "=================================================="
                    docker inspect "$CONTAINER" \
                        --format 'Status: {{.State.Status}} | Running: {{.State.Running}} | ExitCode: {{.State.ExitCode}} | Restarts: {{.RestartCount}} | Health: {{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}'

                    echo ""
                    echo "=================================================="
                    echo "  Últimas 150 líneas de logs"
                    echo "=================================================="
                    docker compose -p llosa_dev logs --tail=150 --no-color

                    echo ""
                    echo "=================================================="
                    echo "  Verificación final"
                    echo "=================================================="
                    RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)
                    HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' "$CONTAINER" 2>/dev/null)
                    RESTARTS=$(docker inspect -f '{{.RestartCount}}' "$CONTAINER" 2>/dev/null)

                    if [ "$RUNNING" != "true" ]; then
                        echo "ERROR: el contenedor $CONTAINER no está corriendo."
                        exit 1
                    fi
                    if [ "$RESTARTS" -gt 0 ] 2>/dev/null; then
                        echo "ERROR: el contenedor $CONTAINER se reinició $RESTARTS veces (crash loop)."
                        exit 1
                    fi
                    if [ "$HEALTH" != "healthy" ] && [ "$HEALTH" != "n/a" ]; then
                        echo "ERROR: el contenedor $CONTAINER no llegó a healthy (Health=$HEALTH)."
                        exit 1
                    fi
                    echo "OK: $CONTAINER está corriendo (Health=$HEALTH, Restarts=$RESTARTS)."
                '''
            }
        }

        stage('Verify Deployment Test') {
            when {
                branch 'test'
            }
            steps {
                sh '''
                    set +e
                    CONTAINER=llosa_backend_test

                    echo "Esperando hasta 120s a que el healthcheck reporte healthy..."
                    for i in $(seq 1 15); do
                        HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' "$CONTAINER" 2>/dev/null)
                        RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)
                        RESTARTS=$(docker inspect -f '{{.RestartCount}}' "$CONTAINER" 2>/dev/null)
                        echo "  intento $i/15 -> Running=$RUNNING Health=$HEALTH Restarts=$RESTARTS"
                        if [ "$RUNNING" != "true" ]; then break; fi
                        if [ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "n/a" ]; then break; fi
                        sleep 5
                    done

                    echo ""
                    echo "=================================================="
                    echo "  docker compose ps"
                    echo "=================================================="
                    docker compose -p llosa_test ps

                    echo ""
                    echo "=================================================="
                    echo "  Estado del contenedor ($CONTAINER)"
                    echo "=================================================="
                    docker inspect "$CONTAINER" \
                        --format 'Status: {{.State.Status}} | Running: {{.State.Running}} | ExitCode: {{.State.ExitCode}} | Restarts: {{.RestartCount}} | Health: {{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}'

                    echo ""
                    echo "=================================================="
                    echo "  Últimas 150 líneas de logs"
                    echo "=================================================="
                    docker compose -p llosa_test logs --tail=150 --no-color

                    echo ""
                    echo "=================================================="
                    echo "  Verificación final"
                    echo "=================================================="
                    RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)
                    HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' "$CONTAINER" 2>/dev/null)
                    RESTARTS=$(docker inspect -f '{{.RestartCount}}' "$CONTAINER" 2>/dev/null)

                    if [ "$RUNNING" != "true" ]; then
                        echo "ERROR: el contenedor $CONTAINER no está corriendo."
                        exit 1
                    fi
                    if [ "$RESTARTS" -gt 0 ] 2>/dev/null; then
                        echo "ERROR: el contenedor $CONTAINER se reinició $RESTARTS veces (crash loop)."
                        exit 1
                    fi
                    if [ "$HEALTH" != "healthy" ] && [ "$HEALTH" != "n/a" ]; then
                        echo "ERROR: el contenedor $CONTAINER no llegó a healthy (Health=$HEALTH)."
                        exit 1
                    fi
                    echo "OK: $CONTAINER está corriendo (Health=$HEALTH, Restarts=$RESTARTS)."
                '''
            }
        }
    }
}
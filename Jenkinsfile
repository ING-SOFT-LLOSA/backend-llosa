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
                    string(credentialsId: 'FIREBASE_API_KEY_LLOSA', variable: 'FIREBASE_API_KEY'),
                    string(credentialsId: 'DOMINIO_CORPORATIVO_LLOSA', variable: 'DOMINIO_CORPORATIVO'),
                    string(credentialsId: 'SHOW_SQL_LLOSA', variable: 'SHOW_SQL'),
                    string(credentialsId: 'FIREBASE_CREDENTIALS_PATH_LLOSA', variable: 'FIREBASE_CREDENTIALS_PATH')
                ]) {
                    sh '''
                        docker compose down
                        docker compose up -d --build backend
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                withCredentials([
                    string(credentialsId: 'FIREBASE_API_KEY_LLOSA',          variable: 'FIREBASE_API_KEY'),
                    string(credentialsId: 'DOMINIO_CORPORATIVO_LLOSA',       variable: 'DOMINIO_CORPORATIVO'),
                    string(credentialsId: 'SHOW_SQL_LLOSA',                  variable: 'SHOW_SQL'),
                    string(credentialsId: 'SPRING_FLYWAY_SCHEMAS_LLOSA',     variable: 'SPRING_FLYWAY_SCHEMAS'),
                    string(credentialsId: 'DB_URL_LLOSA',                    variable: 'DB_URL'),
                    string(credentialsId: 'DB_USERNAME_LLOSA',               variable: 'DB_USERNAME'),
                    string(credentialsId: 'DB_PASSWORD_LLOSA',               variable: 'DB_PASSWORD'),
                    file(credentialsId:   'FIREBASE_SERVICE_ACCOUNT_LLOSA',  variable: 'FIREBASE_SA_FILE')
                ]) {
                    sh '''
                        mkdir -p ./secrets
                        cp "$FIREBASE_SA_FILE" ./secrets/firebase-service-account.json
                        chmod 644 ./secrets/firebase-service-account.json

                        docker compose down
                        docker compose up --build --no-start backend
                        docker cp ./secrets/firebase-service-account.json llosa_backend:/app/secrets/firebase-service-account.json
                        docker compose start backend
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                withCredentials([
                    string(credentialsId: 'FIREBASE_API_KEY_LLOSA',          variable: 'FIREBASE_API_KEY'),
                    string(credentialsId: 'DOMINIO_CORPORATIVO_LLOSA',       variable: 'DOMINIO_CORPORATIVO'),
                    string(credentialsId: 'SHOW_SQL_LLOSA',                  variable: 'SHOW_SQL'),
                    string(credentialsId: 'SPRING_FLYWAY_SCHEMAS_LLOSA',     variable: 'SPRING_FLYWAY_SCHEMAS'),
                    string(credentialsId: 'DB_URL_LLOSA',                    variable: 'DB_URL'),
                    string(credentialsId: 'DB_USERNAME_LLOSA',               variable: 'DB_USERNAME'),
                    string(credentialsId: 'DB_PASSWORD_LLOSA',               variable: 'DB_PASSWORD')
                ]) {
                    sh '''
                        set +e
                        CONTAINER=llosa_backend

                        echo "Esperando hasta 120s a que el healthcheck reporte healthy..."
                        for i in $(seq 1 24); do
                            HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' "$CONTAINER" 2>/dev/null)
                            RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)
                            RESTARTS=$(docker inspect -f '{{.RestartCount}}' "$CONTAINER" 2>/dev/null)
                            echo "  intento $i/24 -> Running=$RUNNING Health=$HEALTH Restarts=$RESTARTS"
                            if [ "$RUNNING" != "true" ]; then break; fi
                            if [ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "n/a" ]; then break; fi
                            sleep 5
                        done

                        echo ""
                        echo "=================================================="
                        echo "  docker compose ps"
                        echo "=================================================="
                        docker compose ps

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
                        docker compose logs --tail=150 --no-color

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
}
pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timestamps()
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
                    set -e

                    echo "======================================"
                    echo "Build & Test Stage"
                    echo "======================================"

                    # PASO 1: Clean
                    echo "PASO 1: Limpiando compilacion anterior..."
                    mvn clean -Dmaven.repo.local=.m2/repository -q

                    # PASO 2: Compile
                    echo "PASO 2: Compilando codigo fuente..."
                    mvn compile -Dmaven.repo.local=.m2/repository -DskipTests

                    # PASO 3: Test + JaCoCo Report
                    echo "PASO 3: Ejecutando tests con reporte JaCoCo..."
                    mvn test jacoco:report -Dmaven.repo.local=.m2/repository

                    # PASO 4: Package (sin re-ejecutar tests)
                    echo "PASO 4: Empaquetando JAR..."
                    mvn package -DskipTests -Dmaven.repo.local=.m2/repository -q

                    # PASO 5: Validar JAR
                    echo "PASO 5: Validando artefacto..."
                    if [ -f "target/backend-0.0.1-SNAPSHOT.jar" ]; then
                        echo "OK: JAR creado"
                    else
                        echo "ERROR: JAR no encontrado"
                        exit 1
                    fi

                    echo "======================================"
                    echo "Build & Test completado"
                    echo "======================================"
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    publishHTML(target: [
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Report'
                    ])
                }
            }
        }

        stage('SonarQube Analysis') {
            agent {
                docker {
                    image 'maven:3.9.8-eclipse-temurin-21-alpine'
                    reuseNode true
                }
            }
            steps {
                echo "======================================"
                echo "SonarQube Analysis"
                echo "======================================"
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        mvn sonar:sonar \
                            -Dmaven.repo.local=.m2/repository \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -DskipTests
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo "======================================"
                echo "Quality Gate"
                echo "======================================"
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'FIREBASE_API_KEY_LLOSA', variable: 'FIREBASE_API_KEY'),
                    string(credentialsId: 'DOMINIO_CORPORATIVO_LLOSA', variable: 'DOMINIO_CORPORATIVO'),
                    string(credentialsId: 'SHOW_SQL_LLOSA', variable: 'SHOW_SQL'),
                    string(credentialsId: 'SPRING_FLYWAY_SCHEMAS_LLOSA', variable: 'SPRING_FLYWAY_SCHEMAS'),
                    string(credentialsId: 'DB_URL_LLOSA', variable: 'DB_URL'),
                    string(credentialsId: 'DB_USERNAME_LLOSA', variable: 'DB_USERNAME'),
                    string(credentialsId: 'DB_PASSWORD_LLOSA', variable: 'DB_PASSWORD'),
                    file(credentialsId: 'FIREBASE_SERVICE_ACCOUNT_LLOSA', variable: 'FIREBASE_SA_FILE')
                ]) {
                    sh '''
                        mkdir -p ./secrets
                        cp "$FIREBASE_SA_FILE" ./secrets/firebase-service-account.json
                        docker compose down --remove-orphans 2>/dev/null || true
                        docker compose up --build -d backend
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    set +e
                    CONTAINER=llosa_backend

                    echo "Esperando que contenedor este healthy..."
                    for i in $(seq 1 15); do
                        HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}n/a{{end}}' "$CONTAINER" 2>/dev/null)
                        RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)

                        if [ "$RUNNING" = "true" ] && ([ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "n/a" ]); then
                            echo "OK: Contenedor healthy"
                            exit 0
                        fi

                        echo "Intento $i/15 - Running: $RUNNING, Health: $HEALTH"
                        sleep 5
                    done

                    echo "ERROR: Contenedor no llego a healthy"
                    docker compose logs
                    exit 1
                '''
            }
        }
    }
}

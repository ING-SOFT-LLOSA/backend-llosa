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
                    set -e

                    # Mostrar información del entorno
                    echo "======================================"
                    echo "Build & Test Stage"
                    echo "======================================"

                    # PASO 1: Limpiar compilación anterior (no caché de Maven)
                    echo "PASO 1: Limpiando compilación anterior..."
                    mvn clean -Dmaven.repo.local=.m2/repository -q

                    # PASO 2: Compilar código fuente
                    echo "PASO 2: Compilando código fuente..."
                    mvn compile -Dmaven.repo.local=.m2/repository -DskipTests

                    # PASO 3: Ejecutar tests e generar reportes JaCoCo para cobertura
                    echo "PASO 3: Ejecutando tests con reporte de cobertura JaCoCo..."
                    mvn test jacoco:report -Dmaven.repo.local=.m2/repository

                    # PASO 4: Empaquetar JAR (sin re-ejecutar tests)
                    echo "PASO 4: Empaquetando JAR (sin re-ejecutar tests)..."
                    mvn package -DskipTests -Dmaven.repo.local=.m2/repository -q

                    # PASO 5: Verificar que JAR fue creado
                    echo "PASO 5: Verificando artefacto..."
                    if [ -f "target/backend-0.0.1-SNAPSHOT.jar" ]; then
                        echo "✓ JAR creado exitosamente: $(ls -lh target/backend-0.0.1-SNAPSHOT.jar)"
                    else
                        echo "✗ Error: JAR no encontrado"
                        exit 1
                    fi

                    echo ""
                    echo "======================================"
                    echo "Build & Test completado exitosamente"
                    echo "======================================"
                '''
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: false,
                          skipPublishingChecks: false,
                          stdioRetentionCount: 100

                    publishHTML([
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Coverage Report',
                        keepAll: true,
                        allowMissing: false
                    ])
                }
                success {
                    echo "✓ Build & Test exitoso"
                }
                failure {
                    echo "✗ Build & Test falló"
                    sh '''
                        echo "Últimas 50 líneas de output:"
                        tail -50 ${WORKSPACE}/build.log || true
                    '''
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
            environment {
                scannerHome = tool 'SonarScanner'
            }
            steps {
                script {
                    echo "======================================"
                    echo "SonarQube Analysis Stage"
                    echo "======================================"

                    // NOTA: Los reportes JaCoCo ya fueron generados en Build & Test stage
                    // No re-ejecutamos tests aquí para optimizar tiempo de build

                    // PASO 1: Sonar análisis con Maven (alternativa a sonar-scanner CLI)
                    echo "PASO 1: Ejecutando análisis SonarQube con Maven..."
                    withSonarQubeEnv('SonarQube-Server') {
                        sh '''
                            mvn sonar:sonar \
                                -Dmaven.repo.local=.m2/repository \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                -DskipTests
                        '''
                    }

                    // PASO 2: Esperar resultado del Quality Gate
                    echo "PASO 2: Esperando Quality Gate..."
                }
            }
            post {
                success {
                    echo "✓ SonarQube Analysis completado"
                }
                failure {
                    echo "✗ SonarQube Analysis falló"
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    echo "======================================"
                    echo "Quality Gate Validation"
                    echo "======================================"
                    echo "Esperando resultado del Quality Gate (máximo 1 hora)..."

                    timeout(time: 1, unit: 'HOURS') {
                        def qualityGate = waitForQualityGate abortPipeline: true

                        if (qualityGate.status == 'OK') {
                            echo "✓ Quality Gate PASSED"
                            echo "  Coverage: ≥ 80%"
                            echo "  Duplicated Lines: ≤ 2%"
                        } else {
                            echo "✗ Quality Gate FAILED"
                            echo "  Status: ${qualityGate.status}"
                            currentBuild.result = 'UNSTABLE'
                            error("Quality Gate falló. Revisa SonarQube en: http://sonarqube:9000")
                        }
                    }
                }
            }
            post {
                always {
                    echo "Quality Gate completado"
                }
            }
        }

        stage('Deploy (Docker Compose)') {
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

                        docker compose down --remove-orphans
                        docker rm -f llosa_backend llosa_db 2>/dev/null || true
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
                    string(credentialsId: 'DB_PASSWORD_LLOSA',               variable: 'DB_PASSWORD'),
                    file(credentialsId:   'FIREBASE_SERVICE_ACCOUNT_LLOSA',  variable: 'FIREBASE_SA_FILE')
                ]) {
                    sh '''
                        set +e
                        CONTAINER=llosa_backend

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

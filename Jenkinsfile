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
            steps {
                sh '''
                    set -e

                    echo "======================================"
                    echo "Build & Test Stage"
                    echo "======================================"

                    # PASO 1: Compile
                    echo "PASO 1: Compilando codigo fuente..."
                    ./mvnw compile -DskipTests

                    # PASO 2: Test + JaCoCo Report (SOLO UNITARIOS - sin Testcontainers)
                    echo "PASO 2: Ejecutando TESTS UNITARIOS (sin Testcontainers)..."
                    ./mvnw test jacoco:report \\
                        -Dtest="!*IntegrationTest,!*E2ETest" \\
                        -DexcludedGroups="integration"

                    # PASO 3: Package (sin re-ejecutar tests)
                    echo "PASO 3: Empaquetando JAR..."
                    ./mvnw package -DskipTests -q

                    # PASO 4: Validar JAR
                    echo "PASO 4: Validando artefacto..."
                    if [ -f "target/backend-0.0.1-SNAPSHOT.jar" ]; then
                        SIZE=$(ls -lh target/backend-0.0.1-SNAPSHOT.jar | awk '{print $5}')
                        echo "OK: JAR creado ($SIZE)"
                    else
                        echo "ERROR: JAR no encontrado"
                        exit 1
                    fi

                    # PASO 5: Verificar reportes JaCoCo
                    echo "PASO 5: Verificando reportes..."
                    if [ -f "target/site/jacoco/index.html" ]; then
                        echo "OK: Reporte JaCoCo generado"
                    else
                        echo "ADVERTENCIA: Reporte JaCoCo no encontrado"
                    fi

                    echo ""
                    echo "======================================"
                    echo "Build & Test completado exitosamente"
                    echo "======================================"
                '''
            }
            post {
                always {
                    // Publicar JUnit results
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

                    // Publicar JaCoCo solo si existe
                    script {
                        if (fileExists('target/site/jacoco/index.html')) {
                            publishHTML(target: [
                                reportDir: 'target/site/jacoco',
                                reportFiles: 'index.html',
                                reportName: 'JaCoCo Coverage Report',
                                keepAll: true
                            ])
                        } else {
                            echo "Advertencia: No se pudo publicar JaCoCo report"
                        }
                    }
                }
                success {
                    echo "Build & Test exitoso"
                }
                failure {
                    echo "Build & Test falló"
                    sh 'echo "Directorio target:" && ls -la target/ || true'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo "======================================"
                echo "SonarQube Analysis"
                echo "======================================"
                withSonarQubeEnv('SonarQube-Server') {
                    sh '''
                        ./mvnw sonar:sonar \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                        -DskipTests \
                        -Djacoco.skip=true
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
                        export FIREBASE_API_KEY DOMINIO_CORPORATIVO SHOW_SQL DB_URL DB_USERNAME DB_PASSWORD SPRING_FLYWAY_SCHEMAS

                        mkdir -p ./secrets
                        cp "$FIREBASE_SA_FILE" ./secrets/firebase-service-account.json
                        chmod 644 ./secrets/firebase-service-account.json

                        docker compose down --remove-orphans
                        docker rm -f llosa_backend llosa_db 2>/dev/null || true
                        docker compose up --build --force-recreate --no-start backend
                        docker cp ./secrets/firebase-service-account.json llosa_backend:/app/secrets/firebase-service-account.json
                        docker compose start backend
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                // Las credenciales son necesarias para que `docker compose` resuelva
                // las variables del compose; sin ellas salen en blanco y los logs/ps
                // se evalúan contra un compose vacío.
                withCredentials([
                    string(credentialsId: 'FIREBASE_API_KEY_LLOSA', variable: 'FIREBASE_API_KEY'),
                    string(credentialsId: 'DOMINIO_CORPORATIVO_LLOSA', variable: 'DOMINIO_CORPORATIVO'),
                    string(credentialsId: 'SHOW_SQL_LLOSA', variable: 'SHOW_SQL'),
                    string(credentialsId: 'SPRING_FLYWAY_SCHEMAS_LLOSA', variable: 'SPRING_FLYWAY_SCHEMAS'),
                    string(credentialsId: 'DB_URL_LLOSA', variable: 'DB_URL'),
                    string(credentialsId: 'DB_USERNAME_LLOSA', variable: 'DB_USERNAME'),
                    string(credentialsId: 'DB_PASSWORD_LLOSA', variable: 'DB_PASSWORD')
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
                            # Fallar rápido ante un crash loop en vez de esperar los 120s completos.
                            if [ "$RESTARTS" -gt 0 ] 2>/dev/null; then
                                echo "DETECTADO: el contenedor se está reiniciando (Restarts=$RESTARTS). Abortando espera."
                                break
                            fi
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
                            echo "CAUSA PROBABLE: revisa los logs de arriba. Si ves 'Connection to localhost:5432 refused',"
                            echo "la credencial DB_URL_LLOSA apunta a localhost; dentro del contenedor debe apuntar al host/servicio real de Postgres."
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

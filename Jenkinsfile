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
                    mvn compile -DskipTests

                    # PASO 2: Test + JaCoCo Report (SOLO UNITARIOS - sin Testcontainers)
                    echo "PASO 2: Ejecutando TESTS UNITARIOS (sin Testcontainers)..."
                    mvn test jacoco:report \\
                        -Dtest="!*IntegrationTest,!*E2ETest" \\
                        -DexcludedGroups="integration"

                    # PASO 3: Package (sin re-ejecutar tests)
                    echo "PASO 3: Empaquetando JAR..."
                    mvn package -DskipTests -q

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
                    echo "✓ Build & Test exitoso"
                }
                failure {
                    echo "✗ Build & Test falló"
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
                        mvn sonar:sonar \
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

pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhubpwd')
        SLACK_CREDENTIALS = credentials('b3ee302b-e782-4d8e-ba83-7fa591d43205')
        SONARQUBE_CREDENTIALS = credentials('pipeline_Stoken') // SonarQube token credentials
        SONARQUBE_SERVER = 'http://localhost:9000' // Replace with your SonarQube server URL
        ANCHORE_URL = 'http://192.168.1.6:8228' // Replace with your Anchore Engine URL
        ANCHORE_USERNAME = 'admin' // Anchore username
        ANCHORE_PASSWORD = 'foobar' // Anchore password
        IMAGE_TAG = 'pramila188/testhello:87'
        IMAGE_DIGEST = 'sha256:92828defa39509daf3c43a57b47364a79ef9bf383f80664b5d434e4b376f13f2' // Replace with actual digest
    }

    parameters {
        string(name: 'JAVA_REPO', defaultValue: 'https://github.com/pramilasawant/helloword1.git', description: 'Java Application Repository')
        string(name: 'DOCKERHUB_USERNAME', defaultValue: 'pramila188', description: 'DockerHub Username')
        string(name: 'JAVA_IMAGE_NAME', defaultValue: 'testhello', description: 'Java Docker Image Name')
        string(name: 'JAVA_NAMESPACE', defaultValue: 'test', description: 'Kubernetes Namespace for Java Application')
    }

    stages {
        stage('Clone Repository') {
            steps {
                git url: params.JAVA_REPO, branch: 'main'
            }
        }

        stage('Build and Push Docker Image') {
            steps {
                dir('testhello') {
                    sh 'mvn clean install'
                    script {
                        def image = docker.build("${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}:${currentBuild.number}")
                        docker.withRegistry('', 'dockerhubpwd') {
                            image.push()
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('testhello') {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.projectKey=testhello \
                                -Dsonar.host.url=${SONARQUBE_SERVER} \
                                -Dsonar.login=${SONARQUBE_CREDENTIALS}
                        '''
                    }
                }
            }
        }

        stage('Get Approval') {
            steps {
                script {
                    input message: 'Do you approve this deployment?', ok: 'Yes, deploy'
                }
            }
        }

        stage('Install yq') {
            steps {
                sh '''
                    wget https://github.com/mikefarah/yq/releases/download/v4.6.1/yq_linux_amd64 -O "${WORKSPACE}/yq"
                    chmod +x "${WORKSPACE}/yq"
                    export PATH="${WORKSPACE}:$PATH"
                '''
            }
        }

        stage('Build and Package Java Helm Chart') {
            steps {
                dir('testhello') {
                    sh '''
                        "${WORKSPACE}/yq" e -i '.image.tag = "latest"' ./myspringbootchart/values.yaml
                        helm template ./myspringbootchart
                        helm lint ./myspringbootchart
                        helm package ./myspringbootchart --version "1.0.0"
                    '''
                }
            }
        }

        stage('Scan Image with Anchore') {
            steps {
                script {
                    sh """
                        curl -X POST -u '${ANCHORE_USERNAME}:${ANCHORE_PASSWORD}' -H 'Content-Type: application/json' -d '{"tag": "${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}:${currentBuild.number}"}' ${ANCHORE_URL}/v1/images
                    """
                    sleep(time: 30, unit: 'SECONDS')
                }
            }
        }

        stage('Check Analysis Status') {
            steps {
                script {
                    def status = sh(
                        script: "curl -s -u ${ANCHORE_USERNAME}:${ANCHORE_PASSWORD} ${ANCHORE_URL}/v1/images/${IMAGE_DIGEST} | jq '.analysis_status'",
                        returnStdout: true
                    ).trim()
                    echo "Analysis Status: ${status}"

                    if (status != '"analyzed"') {
                        error "Image analysis is not complete. Please check Anchore for details."
                    }
                }
            }
        }

        stage('Get Scan Results') {
            steps {
                script {
                    def scanReport = sh(
                        script: "curl -s -u ${ANCHORE_USERNAME}:${ANCHORE_PASSWORD} ${ANCHORE_URL}/v1/images/${IMAGE_DIGEST}/content",
                        returnStdout: true
                    ).trim()
                    echo "Scan Results: ${scanReport}"
                }
            }
        }

        stage('Deploy Java Application to Kubernetes') {
            steps {
                script {
                    kubernetesDeploy(
                        configs: 'Build and Deploy Java and Python Applications',
                        kubeconfigId: 'kubeconfig1pwd'
                    )
                }
            }
        }
    }

    post {
        always {
            // Add Slack notification and email notifications here as in the previous code
        }
        failure {
            // Add failure email notification here as in the previous code
        }
        success {
            // Add success email notification here as in the previous code
        }
    }
}

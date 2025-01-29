pipeline {
    agent any

    environment {
        GITHUB_USERNAME = 'verovec'
        GITHUB_TOKEN = credentials('github-token-id')
        IMAGE_NAME = 'ghcr.io/verovec/svp-application'
        NEW_TAG = 'latest'
        COMPOSE_FILE = 'docker-compose.yml'
    }

    stages {
        stage('SSH Connect & Prepare') {
            steps {
                sshagent(['remote-ssh-key-id']) {
                    sh '''
                    ssh -o StrictHostKeyChecking=no root@10.89.155.31 <<EOF
                    echo "Connected to remote server"
                    cd /home/root
                    EOF
                    '''
                }
            }
        }

        stage('Authenticate to GitHub Registry') {
            steps {
                sh '''
                echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USERNAME --password-stdin
                '''
            }
        }

        stage('Pull Docker Image') {
            steps {
                sh '''
                docker pull $IMAGE_NAME:$NEW_TAG
                '''
            }
        }

        stage('Update Docker Compose File') {
            steps {
                script {
                    def composeFile = readFile("$COMPOSE_FILE")
                    def updatedComposeFile = composeFile.replaceAll(/image:.*$/, "image: $IMAGE_NAME:$NEW_TAG")
                    writeFile file: "$COMPOSE_FILE", text: updatedComposeFile
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                sh '''
                docker-compose pull
                docker-compose up -d --no-deps --build
                docker image prune -f
                '''
            }
        }
    }

    post {
        success {
            echo 'Deployment succeeded!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}

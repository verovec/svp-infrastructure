pipeline {
    agent any

    environment {
        GITHUB_USERNAME = 'verovec'
        IMAGE_NAME = 'ghcr.io/verovec/svp-application'
        NEW_TAG = 'latest'
        COMPOSE_FILE = 'docker-compose.yml'
        SERVERS = "10.89.155.31 10.89.155.32"
        REMOTE_USER = "root"
    }

    stages {
        stage('Deploy Sequentially') {
            steps {
                script {
                    def serverList = SERVERS.split(" ")

                    withCredentials([string(credentialsId: 'github-token-id', variable: 'GITHUB_TOKEN')]) {
                        for (server in serverList) {
                            echo "Deploying to ${server}..."

                            sh """
                            ssh -o StrictHostKeyChecking=no $REMOTE_USER@$server '
                            cd /home/root
                            
                            # Authenticate securely to GitHub Registry
                            docker login ghcr.io -u "$GITHUB_USERNAME" --password-stdin <<< "$GITHUB_TOKEN"
                            
                            # Pull the latest image directly on the remote server
                            docker pull $IMAGE_NAME:$NEW_TAG
                            
                            # Update the docker-compose file dynamically
                            sed -i "s|image:.*|image: $IMAGE_NAME:$NEW_TAG|" $COMPOSE_FILE
                            
                            # Deploy the updated service with no downtime
                            docker-compose up -d --no-deps --build
                            
                            # Optional: Clean up unused Docker images
                            docker image prune -f
                            
                            echo "Deployment completed on ${server}"
                            '
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment to all servers succeeded!'
        }
        failure {
            echo 'Deployment failed on one or more servers!'
        }
    }
}
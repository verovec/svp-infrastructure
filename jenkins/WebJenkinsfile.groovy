pipeline {
    agent any

    environment {
        GITHUB_USERNAME = 'verovec'
        GITHUB_TOKEN = credentials('github-token-id')
        IMAGE_NAME = 'ghcr.io/verovec/your_image_name'
        NEW_TAG = 'latest'
        COMPOSE_FILE = 'docker-compose.yml'

        SERVERS = "10.89.155.31 10.89.155.32"
        REMOTE_USER = "root"
    }

    stages {
        // Deploy to each server sequentially
        stage('Deploy Sequentially') {
            steps {
                script {
                    def serverList = SERVERS.split(" ")

                    for (server in serverList) {
                        echo "Deploying to ${server}..."
                        sshagent(['remote-ssh-key-id']) {
                            sh """
                            ssh $REMOTE_USER@$server <<EOF
                            echo "Connected to ${server}"
                            cd /home/root

                            # Authenticate to GitHub Registry (on remote server)
                            echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USERNAME --password-stdin

                            # Pull the latest image directly on the remote server
                            docker pull $IMAGE_NAME:$NEW_TAG

                            # Update the docker-compose file dynamically
                            sed -i 's|image:.*|image: $IMAGE_NAME:$NEW_TAG|' $COMPOSE_FILE

                            # Deploy the updated service with no downtime
                            cd /path/to/project
                            docker-compose up -d --no-deps --build

                            # Optional: Clean up unused Docker images
                            docker image prune -f

                            echo "Deployment completed on ${server}"
                            EOF
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

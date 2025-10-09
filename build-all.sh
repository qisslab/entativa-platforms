#!/bin/bash

# Entativa Platforms - Complete Build and Setup Script
# This script builds all platforms, generates gRPC code, and sets up infrastructure
# 
# @author Neo Qiss
# @status Production-ready deployment script

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
PLATFORMS=("sonet-backend" "gala-backend" "pika-backend" "playpods-backend" "entativa-id")
PROTO_DIR="$PROJECT_ROOT/shared-proto"
GENERATED_DIR="$PROJECT_ROOT/generated"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo -e "${PURPLE}============================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}============================================${NC}"
}

# Check prerequisites
check_prerequisites() {
    log_header "Checking Prerequisites"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}')
    log_info "Java version: $JAVA_VERSION"
    
    # Check Gradle
    if ! command -v gradle &> /dev/null; then
        log_error "Gradle is not installed or not in PATH"
        exit 1
    fi
    
    GRADLE_VERSION=$(gradle -version | grep "Gradle" | awk '{print $2}')
    log_info "Gradle version: $GRADLE_VERSION"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    DOCKER_VERSION=$(docker --version | awk '{print $3}' | sed 's/,//')
    log_info "Docker version: $DOCKER_VERSION"
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
    
    COMPOSE_VERSION=$(docker-compose --version | awk '{print $3}' | sed 's/,//')
    log_info "Docker Compose version: $COMPOSE_VERSION"
    
    # Check Protocol Buffer Compiler
    if ! command -v protoc &> /dev/null; then
        log_warning "protoc is not installed, attempting to install..."
        install_protoc
    fi
    
    PROTOC_VERSION=$(protoc --version | awk '{print $2}')
    log_info "Protocol Buffer Compiler version: $PROTOC_VERSION"
    
    log_success "All prerequisites checked successfully!"
}

# Install Protocol Buffer Compiler
install_protoc() {
    log_info "Installing Protocol Buffer Compiler..."
    
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        sudo apt-get update
        sudo apt-get install -y protobuf-compiler
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        brew install protobuf
    else
        log_error "Unsupported OS for automatic protoc installation"
        exit 1
    fi
    
    log_success "Protocol Buffer Compiler installed successfully!"
}

# Generate gRPC code
generate_grpc_code() {
    log_header "Generating gRPC Code"
    
    # Create generated directory
    mkdir -p "$GENERATED_DIR"
    
    # Generate for each platform
    for platform in "${PLATFORMS[@]}"; do
        log_info "Generating gRPC code for $platform..."
        
        PLATFORM_GENERATED_DIR="$PROJECT_ROOT/$platform/src/main/kotlin/com/entativa/grpc/generated"
        mkdir -p "$PLATFORM_GENERATED_DIR"
        
        # Generate Kotlin gRPC code
        protoc \
            --proto_path="$PROTO_DIR" \
            --java_out="$PROJECT_ROOT/$platform/src/main/java" \
            --grpc-java_out="$PROJECT_ROOT/$platform/src/main/java" \
            --kotlin_out="$PROJECT_ROOT/$platform/src/main/kotlin" \
            --plugin=protoc-gen-grpc-java=/usr/local/bin/protoc-gen-grpc-java \
            "$PROTO_DIR/entativa.proto"
        
        log_success "gRPC code generated for $platform"
    done
    
    # Generate for shared library
    log_info "Generating gRPC code for shared library..."
    SHARED_GENERATED_DIR="$PROJECT_ROOT/shared/src/main/kotlin/com/entativa/grpc/generated"
    mkdir -p "$SHARED_GENERATED_DIR"
    
    protoc \
        --proto_path="$PROTO_DIR" \
        --java_out="$PROJECT_ROOT/shared/src/main/java" \
        --grpc-java_out="$PROJECT_ROOT/shared/src/main/java" \
        --kotlin_out="$PROJECT_ROOT/shared/src/main/kotlin" \
        "$PROTO_DIR/entativa.proto"
    
    log_success "All gRPC code generated successfully!"
}

# Build shared library
build_shared_library() {
    log_header "Building Shared Library"
    
    cd "$PROJECT_ROOT/shared"
    
    log_info "Cleaning shared library..."
    ./gradlew clean
    
    log_info "Building shared library..."
    ./gradlew build publishToMavenLocal
    
    log_success "Shared library built and published to local Maven repository!"
    
    cd "$PROJECT_ROOT"
}

# Build platform services
build_platform_services() {
    log_header "Building Platform Services"
    
    for platform in "${PLATFORMS[@]}"; do
        if [ -d "$PROJECT_ROOT/$platform" ]; then
            log_info "Building $platform..."
            
            cd "$PROJECT_ROOT/$platform"
            
            # Clean and build
            ./gradlew clean build
            
            # Build Docker image
            log_info "Building Docker image for $platform..."
            docker build -t "entativa/$platform:latest" .
            
            log_success "$platform built successfully!"
            
            cd "$PROJECT_ROOT"
        else
            log_warning "Directory for $platform not found, skipping..."
        fi
    done
    
    log_success "All platform services built successfully!"
}

# Setup infrastructure
setup_infrastructure() {
    log_header "Setting Up Infrastructure"
    
    log_info "Starting infrastructure services..."
    
    # Start infrastructure using docker-compose
    docker-compose -f docker-compose.infrastructure.yml up -d
    
    # Wait for services to be ready
    log_info "Waiting for infrastructure services to be ready..."
    sleep 30
    
    # Check service health
    check_infrastructure_health
    
    log_success "Infrastructure setup completed!"
}

# Check infrastructure health
check_infrastructure_health() {
    log_info "Checking infrastructure health..."
    
    # Check PostgreSQL
    if docker-compose -f docker-compose.infrastructure.yml exec -T postgres pg_isready -U entativa_user; then
        log_success "PostgreSQL is ready"
    else
        log_error "PostgreSQL is not ready"
    fi
    
    # Check Redis
    if docker-compose -f docker-compose.infrastructure.yml exec -T redis redis-cli ping | grep -q "PONG"; then
        log_success "Redis is ready"
    else
        log_error "Redis is not ready"
    fi
    
    # Check MongoDB
    if docker-compose -f docker-compose.infrastructure.yml exec -T mongodb mongosh --eval "db.runCommand('ping')" | grep -q "ok"; then
        log_success "MongoDB is ready"
    else
        log_error "MongoDB is not ready"
    fi
    
    # Check Elasticsearch
    if curl -s http://localhost:9200/_health | grep -q "yellow\|green"; then
        log_success "Elasticsearch is ready"
    else
        log_error "Elasticsearch is not ready"
    fi
}

# Initialize databases
initialize_databases() {
    log_header "Initializing Databases"
    
    log_info "Running database initialization scripts..."
    
    # Initialize PostgreSQL databases
    docker-compose -f docker-compose.infrastructure.yml exec -T postgres psql -U entativa_user -d postgres -f /docker-entrypoint-initdb.d/init-databases.sql
    
    # Initialize MongoDB collections
    docker-compose -f docker-compose.infrastructure.yml exec -T mongodb mongosh entativa_analytics /docker-entrypoint-initdb.d/init-analytics.js
    
    # Initialize Cassandra keyspace and tables
    docker-compose -f docker-compose.infrastructure.yml exec -T cassandra cqlsh -f /docker-entrypoint-initdb.d/init-schema.cql
    
    log_success "Databases initialized successfully!"
}

# Run tests
run_tests() {
    log_header "Running Tests"
    
    # Test shared library
    log_info "Testing shared library..."
    cd "$PROJECT_ROOT/shared"
    ./gradlew test
    cd "$PROJECT_ROOT"
    
    # Test platform services
    for platform in "${PLATFORMS[@]}"; do
        if [ -d "$PROJECT_ROOT/$platform" ]; then
            log_info "Testing $platform..."
            cd "$PROJECT_ROOT/$platform"
            ./gradlew test
            cd "$PROJECT_ROOT"
        fi
    done
    
    log_success "All tests completed successfully!"
}

# Deploy services
deploy_services() {
    log_header "Deploying Services"
    
    log_info "Starting all platform services..."
    
    for platform in "${PLATFORMS[@]}"; do
        if [ -d "$PROJECT_ROOT/$platform" ]; then
            log_info "Deploying $platform..."
            
            # Get platform-specific port
            case $platform in
                "sonet-backend")
                    PORT=50051
                    ;;
                "gala-backend")
                    PORT=50052
                    ;;
                "pika-backend")
                    PORT=50053
                    ;;
                "playpods-backend")
                    PORT=50054
                    ;;
                "entativa-id")
                    PORT=50055
                    ;;
                *)
                    PORT=50051
                    ;;
            esac
            
            # Run the service
            docker run -d \
                --name "$platform" \
                --network entativa-network \
                -p "$PORT:$PORT" \
                -e PLATFORM="$(echo $platform | cut -d'-' -f1)" \
                -e GRPC_PORT="$PORT" \
                -e POSTGRES_URL="jdbc:postgresql://postgres:5432/entativa_master" \
                -e POSTGRES_USER="entativa_user" \
                -e POSTGRES_PASSWORD="entativa_password" \
                -e REDIS_HOST="redis" \
                -e MONGODB_CONNECTION_STRING="mongodb://entativa_user:entativa_password@mongodb:27017/entativa_analytics" \
                -e CASSANDRA_CONTACT_POINTS="cassandra" \
                "entativa/$platform:latest"
            
            log_success "$platform deployed on port $PORT"
        fi
    done
    
    log_success "All services deployed successfully!"
}

# Show status
show_status() {
    log_header "Service Status"
    
    log_info "Infrastructure Services:"
    docker-compose -f docker-compose.infrastructure.yml ps
    
    echo
    log_info "Platform Services:"
    for platform in "${PLATFORMS[@]}"; do
        if docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -q "$platform"; then
            docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "$platform"
        else
            log_warning "$platform is not running"
        fi
    done
}

# Clean up
cleanup() {
    log_header "Cleaning Up"
    
    log_info "Stopping and removing containers..."
    
    # Stop platform services
    for platform in "${PLATFORMS[@]}"; do
        if docker ps -q -f name="$platform" | grep -q .; then
            docker stop "$platform"
            docker rm "$platform"
        fi
    done
    
    # Stop infrastructure
    docker-compose -f docker-compose.infrastructure.yml down -v
    
    log_success "Cleanup completed!"
}

# Main function
main() {
    log_header "Entativa Platforms - Build and Deployment Script"
    
    case "${1:-all}" in
        "check")
            check_prerequisites
            ;;
        "grpc")
            generate_grpc_code
            ;;
        "shared")
            build_shared_library
            ;;
        "build")
            check_prerequisites
            generate_grpc_code
            build_shared_library
            build_platform_services
            ;;
        "infrastructure")
            setup_infrastructure
            initialize_databases
            ;;
        "test")
            run_tests
            ;;
        "deploy")
            deploy_services
            ;;
        "status")
            show_status
            ;;
        "clean")
            cleanup
            ;;
        "all")
            check_prerequisites
            generate_grpc_code
            build_shared_library
            build_platform_services
            setup_infrastructure
            initialize_databases
            run_tests
            deploy_services
            show_status
            ;;
        "help"|"-h"|"--help")
            echo "Usage: $0 [command]"
            echo
            echo "Commands:"
            echo "  check          - Check prerequisites"
            echo "  grpc          - Generate gRPC code"
            echo "  shared        - Build shared library"
            echo "  build         - Build all services"
            echo "  infrastructure - Setup infrastructure"
            echo "  test          - Run tests"
            echo "  deploy        - Deploy services"
            echo "  status        - Show service status"
            echo "  clean         - Clean up containers"
            echo "  all           - Run complete build and deployment (default)"
            echo "  help          - Show this help message"
            ;;
        *)
            log_error "Unknown command: $1"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
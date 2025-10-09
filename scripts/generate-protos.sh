#!/bin/bash
# Entativa Platforms - gRPC Proto Generation Script
# Generates all proto files for all platforms

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Entativa Platforms - gRPC Proto Generation${NC}"
echo -e "${YELLOW}Generating proto files for all platforms...${NC}"

# Base directories
PROTO_DIR="shared-proto"
KOTLIN_OUT_BASE="src/main/kotlin"
JAVA_OUT_BASE="src/main/java"
PYTHON_OUT_BASE="src/main/python"

# Platform directories
PLATFORMS=("sonet-backend" "gala-backend" "pika-backend" "playpods-backend" "entativa-id" "entativa-gateway")

# Check if protoc is installed
if ! command -v protoc &> /dev/null; then
    echo -e "${RED}❌ protoc is not installed. Please install Protocol Buffers compiler.${NC}"
    exit 1
fi

# Check if grpc_kotlin_plugin is available
if ! command -v protoc-gen-grpc-kotlin &> /dev/null; then
    echo -e "${YELLOW}⚠️  grpc-kotlin plugin not found. Installing...${NC}"
    # You may need to install this separately
fi

# Function to generate proto for a platform
generate_proto_for_platform() {
    local platform=$1
    echo -e "${BLUE}📦 Generating protos for ${platform}...${NC}"
    
    # Create output directories
    mkdir -p "${platform}/${KOTLIN_OUT_BASE}/com/entativa/proto"
    mkdir -p "${platform}/${JAVA_OUT_BASE}/com/entativa/proto"
    
    # Generate Kotlin/Java files
    protoc \
        --proto_path=${PROTO_DIR} \
        --java_out=${platform}/${JAVA_OUT_BASE} \
        --kotlin_out=${platform}/${KOTLIN_OUT_BASE} \
        --grpc-java_out=${platform}/${JAVA_OUT_BASE} \
        --grpc-kotlin_out=${platform}/${KOTLIN_OUT_BASE} \
        ${PROTO_DIR}/*.proto
    
    echo -e "${GREEN}✅ Proto generation completed for ${platform}${NC}"
}

# Function to generate Python protos for algorithm engine
generate_python_protos() {
    echo -e "${BLUE}🐍 Generating Python protos for algorithm engine...${NC}"
    
    mkdir -p "entativa-algorithm/${PYTHON_OUT_BASE}/entativa_proto"
    
    # Generate Python files
    python -m grpc_tools.protoc \
        --proto_path=${PROTO_DIR} \
        --python_out=entativa-algorithm/${PYTHON_OUT_BASE}/entativa_proto \
        --grpc_python_out=entativa-algorithm/${PYTHON_OUT_BASE}/entativa_proto \
        ${PROTO_DIR}/*.proto
    
    # Create __init__.py files
    touch "entativa-algorithm/${PYTHON_OUT_BASE}/entativa_proto/__init__.py"
    
    echo -e "${GREEN}✅ Python proto generation completed${NC}"
}

# Clean existing generated files
echo -e "${YELLOW}🧹 Cleaning existing generated files...${NC}"
for platform in "${PLATFORMS[@]}"; do
    if [ -d "${platform}" ]; then
        find "${platform}" -name "*_pb2.py" -delete 2>/dev/null || true
        find "${platform}" -name "*_pb2_grpc.py" -delete 2>/dev/null || true
        find "${platform}" -path "*/com/entativa/proto/*" -name "*.java" -delete 2>/dev/null || true
        find "${platform}" -path "*/com/entativa/proto/*" -name "*.kt" -delete 2>/dev/null || true
    fi
done

# Generate protos for each platform
for platform in "${PLATFORMS[@]}"; do
    if [ -d "${platform}" ]; then
        generate_proto_for_platform "${platform}"
    else
        echo -e "${YELLOW}⚠️  Platform directory ${platform} not found, skipping...${NC}"
    fi
done

# Generate Python protos
if [ -d "entativa-algorithm" ]; then
    generate_python_protos
fi

# Create build info file
cat > shared-proto/build-info.txt << EOF
Generated at: $(date)
Platforms: ${PLATFORMS[*]}
Proto files: $(ls ${PROTO_DIR}/*.proto | wc -l)
Protoc version: $(protoc --version)
EOF

echo -e "${GREEN}🎉 All proto files generated successfully!${NC}"
echo -e "${BLUE}📄 Build info saved to shared-proto/build-info.txt${NC}"
echo -e "${YELLOW}💡 Next steps:${NC}"
echo -e "   1. Build your platform services"
echo -e "   2. Start the infrastructure: docker-compose -f docker-compose.infrastructure.yml up -d"
echo -e "   3. Run your services"
#!/usr/bin/env bash
# ==============================================================================
# Minecraft Server & LogToSplunk Plugin Installation and Configuration Script
# ==============================================================================
set -euo pipefail

# --- Color Constants for Premium UX ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================================${NC}"
echo -e "${CYAN}        Minecraft & LogToSplunk Plugin Automated Setup Script        ${NC}"
echo -e "${BLUE}======================================================================${NC}"

# --- Configuration & Paths ---
MC_SERVER_DIR="${MC_SERVER_DIR:-/home/splunk/minecraft-server}"
MC_VERSION="1.21.1"
PAPER_BUILD="133"
WORKSPACE_DIR="/home/splunk/appDev/minecraft-app"
JAVA_HOME_PATH="/usr/lib/jvm/java-25-openjdk-amd64"
SPLUNK_HOME="/opt/splunk"

# --- 1. Validate Java ---
echo -e "\n${CYAN}[1/8] Verifying Java Installation...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed or not in PATH.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java is available: $(java -version 2>&1 | head -n 1)${NC}"

# --- 2. Build LogToSplunk Plugin ---
echo -e "\n${CYAN}[2/8] Compiling LogToSplunk Plugin...${NC}"
if [ -d "${WORKSPACE_DIR}/mvn-bin" ]; then
    echo -e "${YELLOW}Using local Maven installation at ${WORKSPACE_DIR}/mvn-bin${NC}"
    JAVA_HOME="${JAVA_HOME_PATH}" "${WORKSPACE_DIR}/mvn-bin/bin/mvn" -f "${WORKSPACE_DIR}/pom.xml" clean package
else
    echo -e "${RED}Error: Local Maven bin folder not found at ${WORKSPACE_DIR}/mvn-bin. Please make sure Maven is installed.${NC}"
    exit 1
fi

PLUGIN_JAR="${WORKSPACE_DIR}/logtosplunk-plugin/target/logtosplunk-plugin-1.0-SNAPSHOT.jar"
if [ ! -f "${PLUGIN_JAR}" ]; then
    echo -e "${RED}Error: Failed to build plugin jar at ${PLUGIN_JAR}${NC}"
    exit 1
fi
echo -e "${GREEN}✓ LogToSplunk Plugin compiled successfully.${NC}"

# --- 3. Setup Minecraft Server Directory ---
echo -e "\n${CYAN}[3/8] Setting up Minecraft server directory at ${MC_SERVER_DIR}...${NC}"
mkdir -p "${MC_SERVER_DIR}"
mkdir -p "${MC_SERVER_DIR}/plugins"
mkdir -p "${MC_SERVER_DIR}/config"
echo -e "${GREEN}✓ Folders created.${NC}"

# --- 4. Download Minecraft Server Jar (Paper 1.21.1) ---
echo -e "\n${CYAN}[4/8] Downloading Paper ${MC_VERSION} (Build ${PAPER_BUILD}) Server Jar...${NC}"
SERVER_JAR="${MC_SERVER_DIR}/server.jar"
if [ -f "${SERVER_JAR}" ]; then
    echo -e "${YELLOW}Server jar already exists. Skipping download.${NC}"
else
    DOWNLOAD_URL="https://api.papermc.io/v2/projects/paper/versions/${MC_VERSION}/builds/${PAPER_BUILD}/downloads/paper-${MC_VERSION}-${PAPER_BUILD}.jar"
    echo -e "${BLUE}Downloading from: ${DOWNLOAD_URL}${NC}"
    wget -O "${SERVER_JAR}" "${DOWNLOAD_URL}"
fi
echo -e "${GREEN}✓ Minecraft Server jar downloaded successfully.${NC}"

# --- 5. Accept Minecraft EULA ---
echo -e "\n${CYAN}[5/8] Accepting Minecraft EULA...${NC}"
echo "eula=true" > "${MC_SERVER_DIR}/eula.txt"
echo -e "${GREEN}✓ EULA accepted (eula=true written to ${MC_SERVER_DIR}/eula.txt).${NC}"

# --- 6. Configure Splunk HTTP Event Collector (HEC) ---
echo -e "\n${CYAN}[6/8] Configuring Splunk HEC Inputs...${NC}"
HEC_CONF="${SPLUNK_HOME}/etc/apps/splunk_httpinput/local/inputs.conf"
HEC_TOKEN=""

# Generate HEC token if not already existing
if [ -f "${HEC_CONF}" ] && grep -q "\[http://minecraft\]" "${HEC_CONF}"; then
    echo -e "${YELLOW}Stanza [http://minecraft] already exists in Splunk inputs.conf.${NC}"
    # Extract existing token
    HEC_TOKEN=$(grep -A 5 "\[http://minecraft\]" "${HEC_CONF}" | grep "token =" | head -n 1 | awk -F'= ' '{print $2}' | tr -d '[:space:]')
    echo -e "${GREEN}✓ Using existing HEC token: ${HEC_TOKEN}${NC}"
else
    # Generate new UUID for HEC Token
    HEC_TOKEN=$(python3 -c "import uuid; print(uuid.uuid4())")
    echo -e "${YELLOW}Configuring new Splunk HEC token: ${HEC_TOKEN}${NC}"
    
    mkdir -p "$(dirname "${HEC_CONF}")"
    cat >> "${HEC_CONF}" <<EOF

[http://minecraft]
disabled = 0
token = ${HEC_TOKEN}
index = minecraft
indexes = minecraft
sourcetype = minecraft:json
EOF
    echo -e "${GREEN}✓ Added HEC configuration to ${HEC_CONF}.${NC}"
    
    # Restart Splunk to pick up HEC changes
    echo -e "${YELLOW}Restarting Splunk to apply new HTTP Input configs...${NC}"
    "${SPLUNK_HOME}/bin/splunk" restart
    echo -e "${GREEN}✓ Splunk restarted successfully.${NC}"
fi

# --- 7. Configure Minecraft Splunk Plugin Properties ---
echo -e "\n${CYAN}[7/8] Configuring LogToSplunk Plugin properties...${NC}"
PROPERTIES_FILE="${MC_SERVER_DIR}/config/splunk.properties"
cat > "${PROPERTIES_FILE}" <<EOF
splunk.craft.connection.host=127.0.0.1
splunk.craft.connection.port=8088
splunk.craft.token=${HEC_TOKEN}
splunk.craft.enable.consolelog=true
EOF
echo -e "${GREEN}✓ splunk.properties generated at ${PROPERTIES_FILE}.${NC}"

# Setup server.properties for local testing (disable online auth to avoid login errors)
SERVER_PROPS_FILE="${MC_SERVER_DIR}/server.properties"
if [ ! -f "${SERVER_PROPS_FILE}" ]; then
    cat > "${SERVER_PROPS_FILE}" <<EOF
online-mode=false
server-port=25565
query.port=25565
motd=Splunk Minecraft Server
EOF
else
    # If file exists, ensure online-mode is false
    if sed --version >/dev/null 2>&1; then
        sed -i 's/online-mode=true/online-mode=false/g' "${SERVER_PROPS_FILE}"
        if ! grep -q "online-mode=" "${SERVER_PROPS_FILE}"; then
            echo "online-mode=false" >> "${SERVER_PROPS_FILE}"
        fi
    fi
fi
echo -e "${GREEN}✓ server.properties configured (online-mode=false).${NC}"

# --- 8. Install LogToSplunk Plugin Jar ---
echo -e "\n${CYAN}[8/8] Deploying LogToSplunk Plugin jar to Server...${NC}"
cp "${PLUGIN_JAR}" "${MC_SERVER_DIR}/plugins/logtosplunk-plugin.jar"
echo -e "${GREEN}✓ Plugin jar copied to ${MC_SERVER_DIR}/plugins/logtosplunk-plugin.jar.${NC}"

echo -e "\n${GREEN}======================================================================${NC}"
echo -e "${GREEN}✓ Minecraft Server and LogToSplunk Plugin installed & configured!     ${NC}"
echo -e "${GREEN}======================================================================${NC}"
echo -e "Server Directory:  ${MC_SERVER_DIR}"
echo -e "HEC Token:         ${HEC_TOKEN}"
echo -e "Run the server with: ./run_minecraft.sh"
echo -e "${GREEN}======================================================================${NC}"

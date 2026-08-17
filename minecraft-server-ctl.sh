#!/usr/bin/env bash
# =============================================================================
# minecraft-server-ctl.sh -- start/stop/restart/status for the LogToSplunk
# Minecraft server, run inside a detached tmux session so it survives logout.
#
# Usage:
#   ./minecraft-server-ctl.sh start
#   ./minecraft-server-ctl.sh stop
#   ./minecraft-server-ctl.sh restart
#   ./minecraft-server-ctl.sh status
#   ./minecraft-server-ctl.sh console      # attach to the live server console (Ctrl-b d to detach)
#
# Configuration (override via env vars):
#   MC_SERVER_DIR   Directory containing the server launch script. Default:
#                   /home/splunk/.local/share/atlauncher/servers/SplunkCraft
#   MC_LAUNCH_CMD   Command to launch the server, run from MC_SERVER_DIR. Default: ./LaunchServer.sh
#   MC_TMUX_SESSION tmux session name. Default: splunkcraft
#   MC_SERVER_PORT  Port to check for "server is accepting connections" in `status`. Default: 25565
#   MC_STOP_TIMEOUT Seconds to wait for a graceful shutdown before giving up. Default: 60
#
# Why tmux, and why this script exists:
#   Running the server directly ties it to your terminal session; tmux keeps it running after
#   you disconnect. But a naive `tmux send-keys stop` + blindly sending another keystroke
#   afterwards is dangerous: once the server process exits, the pane sits at a shell prompt
#   ("Press any key to close..."). Sending one more Enter at that point can consume that
#   prompt and close the pane -- and if it's the only pane in the only window, that kills the
#   entire tmux session (server socket and all), not just the Minecraft process. This script
#   never sends a second keystroke after `stop`; it polls for the java process to actually
#   exit, then explicitly kills the tmux session.
# =============================================================================

set -euo pipefail

MC_SERVER_DIR="${MC_SERVER_DIR:-/home/splunk/.local/share/atlauncher/servers/SplunkCraft}"
MC_LAUNCH_CMD="${MC_LAUNCH_CMD:-./LaunchServer.sh}"
MC_TMUX_SESSION="${MC_TMUX_SESSION:-splunkcraft}"
MC_SERVER_PORT="${MC_SERVER_PORT:-25565}"
MC_STOP_TIMEOUT="${MC_STOP_TIMEOUT:-60}"

log() { echo "[minecraft-server-ctl] $*"; }

session_exists() {
    tmux has-session -t "${MC_TMUX_SESSION}" 2>/dev/null
}

server_pid() {
    # Finds the java process whose cwd is the server directory. Empty output if not running.
    for pid in $(pgrep -x java 2>/dev/null || true); do
        if [ "$(readlink -f "/proc/${pid}/cwd" 2>/dev/null)" = "$(readlink -f "${MC_SERVER_DIR}")" ]; then
            echo "${pid}"
            return 0
        fi
    done
    return 1
}

port_listening() {
    ss -ltn 2>/dev/null | awk '{print $4}' | grep -q ":${MC_SERVER_PORT}$"
}

do_status() {
    if session_exists; then
        log "tmux session '${MC_TMUX_SESSION}': running"
    else
        log "tmux session '${MC_TMUX_SESSION}': not running"
    fi
    if pid=$(server_pid); then
        log "server process: running (pid ${pid})"
    else
        log "server process: not running"
    fi
    if port_listening; then
        log "port ${MC_SERVER_PORT}: accepting connections"
    else
        log "port ${MC_SERVER_PORT}: not listening"
    fi
}

do_start() {
    if session_exists; then
        log "already running (tmux session '${MC_TMUX_SESSION}' exists). Use 'restart' to bounce it."
        exit 1
    fi
    if [ ! -d "${MC_SERVER_DIR}" ]; then
        log "ERROR: MC_SERVER_DIR '${MC_SERVER_DIR}' does not exist."
        exit 1
    fi
    log "starting server in tmux session '${MC_TMUX_SESSION}' (${MC_SERVER_DIR})..."
    tmux new-session -d -s "${MC_TMUX_SESSION}" -c "${MC_SERVER_DIR}"
    tmux send-keys -t "${MC_TMUX_SESSION}" "${MC_LAUNCH_CMD}" Enter
    log "launched. Use '$0 status' to check it came up, or '$0 console' to watch it."
}

do_stop() {
    if ! session_exists; then
        log "no tmux session '${MC_TMUX_SESSION}' found; nothing to stop."
        return 0
    fi
    if pid=$(server_pid); then
        log "sending 'stop' to the server console..."
        tmux send-keys -t "${MC_TMUX_SESSION}" "stop" Enter
        waited=0
        while kill -0 "${pid}" 2>/dev/null; do
            if [ "${waited}" -ge "${MC_STOP_TIMEOUT}" ]; then
                log "WARNING: server still running after ${MC_STOP_TIMEOUT}s; leaving tmux session up for inspection."
                exit 1
            fi
            sleep 2
            waited=$((waited + 2))
        done
        log "server process exited cleanly after ${waited}s."
    else
        log "tmux session exists but no server process found; probably already stopped."
    fi
    # Deliberately do NOT send any further keystrokes into the pane here (see header comment) --
    # kill the session directly instead of interacting with its now-idle shell prompt.
    tmux kill-session -t "${MC_TMUX_SESSION}" 2>/dev/null || true
    log "tmux session '${MC_TMUX_SESSION}' closed."
}

do_restart() {
    do_stop
    sleep 2
    do_start
}

do_console() {
    if ! session_exists; then
        log "no tmux session '${MC_TMUX_SESSION}' found. Start it first with '$0 start'."
        exit 1
    fi
    log "attaching to '${MC_TMUX_SESSION}' -- press Ctrl-b then d to detach without stopping the server."
    tmux attach -t "${MC_TMUX_SESSION}"
}

case "${1:-}" in
    start)   do_start ;;
    stop)    do_stop ;;
    restart) do_restart ;;
    status)  do_status ;;
    console) do_console ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|console}" >&2
        exit 1
        ;;
esac

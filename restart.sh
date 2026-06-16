#!/usr/bin/env bash
#
# 一键重启 AiPal mirror 版本
# One-click restart script for the mirror version
#
# 用法: ./mirror/restart.sh   (从项目根目录)
#      或进入 mirror 目录后 ./restart.sh
#
# 主要修复点:
# - 每次重启**都会**执行 mvn clean package（不再只在 JAR 不存在时构建）。
#   这样源码修改（例如 ChatService 的 .defaultSystem 系统提示词、Controller 变更等）
#   总是能立即生效，避免“改了代码但重启后没变化”的问题。
# - PID 记录更健壮：启动后通过 lsof/ss/pgrep 发现真正监听 18080 的 Java 进程 PID，
#   写入 .pid（避免之前 $! 捕获到 bash wrapper / nohup 外壳进程，导致 kill 错对象）。
# - 启动验证加强：不只检查 ps -p，还会轮询端口就绪，并尽量 curl /hello 确认 HTTP 200。
# - 等待逻辑改为自适应循环（最长 ~15s），比固定 sleep 4s 更可靠。
# - 仍然保留原有杀进程兜底（PID + pkill + lsof 端口）。

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR_PATH="target/aipal-mirror-0.0.1-SNAPSHOT.jar"
APP_NAME="aipal-mirror"
PORT=18080
LOG_FILE="app.log"
PID_FILE=".pid"

# 从项目根目录运行时的友好显示路径（脚本会 cd 到 mirror 目录执行）
LOG_DISPLAY="mirror/app.log"
PID_DISPLAY="mirror/.pid"

echo "=== 重启 AiPal Mirror 版本 (port $PORT) ==="

# 1. 停止已运行的实例
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo "停止旧进程 PID $OLD_PID ..."
        kill "$OLD_PID" 2>/dev/null || true
        sleep 2
        if ps -p "$OLD_PID" > /dev/null 2>&1; then
            echo "强制杀死 PID $OLD_PID"
            kill -9 "$OLD_PID" 2>/dev/null || true
        fi
    fi
    rm -f "$PID_FILE"
fi

# 额外兜底：按 jar 名和端口杀进程
pkill -f "aipal-mirror.*\.jar" 2>/dev/null || true

if command -v lsof >/dev/null 2>&1; then
    for p in $(lsof -ti :"$PORT" 2>/dev/null || true); do
        echo "杀死占用端口 $PORT 的进程: $p"
        kill -9 "$p" 2>/dev/null || true
    done
fi

sleep 1

# 2. 总是重新构建（核心修复）
# 以前只有 JAR 不存在时才构建，导致修改源码（如 ChatService.defaultSystem "回答问题前先说YES"）
# 后直接重启仍用旧 JAR，改动不生效。
# 现在每次 restart 都 mvn clean package，确保所有源码变更立即打包生效。
echo "=== 构建最新代码 (mvn clean package) ==="
if command -v mvn >/dev/null 2>&1; then
    mvn clean package -DskipTests -B --no-transfer-progress
else
    echo "错误: 未找到 mvn。请先手动构建: mvn clean package -DskipTests"
    exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
    echo "错误: 构建后仍未找到 $JAR_PATH"
    exit 1
fi

# 3. 检查 java
if ! command -v java >/dev/null 2>&1; then
    echo "错误: 未找到 java 命令"
    exit 1
fi

# 4. 启动
if [ -z "${OPENAI_API_KEY:-}" ]; then
    echo "警告: 环境变量 OPENAI_API_KEY 未设置，/chat 接口可能无法工作。"
fi

echo "启动 $APP_NAME ..."
nohup java -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &

CANDIDATE_PID=$!
echo "候选 PID (可能为包装进程): $CANDIDATE_PID"

echo "等待应用就绪 (最多 15 秒) ..."

# 更健壮的等待：轮询直到端口被监听 + 进程存活
READY=0
for i in $(seq 1 15); do
    sleep 1
    if command -v lsof >/dev/null 2>&1; then
        LISTENING=$(lsof -ti :"$PORT" 2>/dev/null | head -1 || true)
    elif command -v ss >/dev/null 2>&1; then
        LISTENING=$(ss -ltnp 2>/dev/null | grep -E ":$PORT\b" | grep -oP 'pid=\K[0-9]+' | head -1 || true)
    else
        LISTENING=""
    fi

    if [ -n "$LISTENING" ] && ps -p "$LISTENING" > /dev/null 2>&1; then
        NEW_PID="$LISTENING"
        READY=1
        break
    fi
done

if [ "$READY" -eq 0 ]; then
    # 最后兜底尝试用 pgrep
    if command -v pgrep >/dev/null 2>&1; then
        NEW_PID=$(pgrep -f "aipal-mirror.*\.jar" | head -1 || true)
    else
        NEW_PID="$CANDIDATE_PID"
    fi
fi

# 无论如何都把（尽量正确的）PID 写入文件，供后续 kill 使用
echo "$NEW_PID" > "$PID_FILE"

echo "已启动，PID: $NEW_PID   日志: $LOG_DISPLAY"

# 额外验证：进程存活 + /hello 能返回 200（真正启动成功）
if ps -p "$NEW_PID" > /dev/null 2>&1; then
    HTTP_CODE=""
    if command -v curl >/dev/null 2>&1; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "http://localhost:$PORT/hello" 2>/dev/null || echo "000")
    fi

    if [ "$HTTP_CODE" = "200" ] || [ -z "$HTTP_CODE" ]; then
        echo "✅ Mirror 版本已成功重启！"
        echo ""
        echo "访问链接："
        echo "  http://localhost:$PORT/hello"
        echo "  http://localhost:$PORT/chat?message=你好"
        echo ""
        echo "其他提示："
        echo "  - 查看日志: tail -f $LOG_DISPLAY"
        echo "  - 手动停止: kill \$(cat $PID_DISPLAY)"
        echo "  - 再次运行本脚本即可重启（每次都会重新构建最新代码）"
    else
        echo "⚠️  进程存在但 /hello 返回 HTTP $HTTP_CODE （可能仍在启动或有错误），请检查日志："
        tail -n 30 "$LOG_FILE" || true
    fi
else
    echo "❌ 启动失败，请查看日志："
    tail -n 40 "$LOG_FILE" || true
    exit 1
fi

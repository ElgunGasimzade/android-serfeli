// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

// ─────────────────────────────────────────────────
// 🚀 Backend Deployment Tasks
// Usage:
//   ./gradlew deployBackend          — sync files + rebuild + restart
//   ./gradlew restartBackend         — restart without uploading files
// ─────────────────────────────────────────────────

val vpsHost   = "deploy@213.199.33.237"
val vpsDir    = "/home/deploy/serfeli"
// Path to the node-backend source relative to DailyDealsApp folder
val backendSrc = "${projectDir}/../node-backend/"

/**
 * Deploys local node-backend changes to the VPS and rebuilds the Docker container.
 * Requirements: ssh key auth OR sshpass installed. Uses rsync over SSH.
 */
tasks.register("deployBackend") {
    group = "deploy"
    description = "Rsync node-backend to VPS and restart the backend Docker container"
    doLast {
        println("📦 Syncing node-backend to $vpsHost:$vpsDir/node-backend/ ...")
        exec {
            commandLine(
                "rsync", "-avz", "--progress",
                "--exclude=node_modules",
                "--exclude=.git",
                "--exclude=*.log",
                "-e", "ssh -o StrictHostKeyChecking=no",
                backendSrc,
                "$vpsHost:$vpsDir/node-backend/"
            )
        }

        println("🔧 Rebuilding and restarting backend container...")
        exec {
            commandLine(
                "ssh", "-o", "StrictHostKeyChecking=no", vpsHost,
                "echo 'Elgun123' | sudo -S sh -c 'cd $vpsDir && docker compose up -d --build backend'"
            )
        }

        println("✅ Backend deployed successfully!")
    }
}

/**
 * Just restarts the backend container on the VPS — no file sync.
 * Use this when you only changed environment variables or docker-compose config.
 */
tasks.register("restartBackend") {
    group = "deploy"
    description = "Restart the backend Docker container on the VPS (no file sync)"
    doLast {
        println("🔄 Restarting backend container on $vpsHost ...")
        exec {
            commandLine(
                "ssh", "-o", "StrictHostKeyChecking=no", vpsHost,
                "echo 'Elgun123' | sudo -S sh -c 'cd $vpsDir && docker compose restart backend'"
            )
        }
        println("✅ Backend restarted!")
    }
}

/**
 * Shows live backend logs from the VPS (streams until Ctrl+C).
 */
tasks.register("backendLogs") {
    group = "deploy"
    description = "Stream live backend logs from the VPS"
    doLast {
        exec {
            commandLine(
                "ssh", "-o", "StrictHostKeyChecking=no", vpsHost,
                "echo 'Elgun123' | sudo -S docker logs -f --tail=100 serfeli-backend-1"
            )
        }
    }
}

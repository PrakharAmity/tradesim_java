#!/usr/bin/env python3
"""Execution Engine metadata for the TradeSim challenge."""
runtime_config = {
    "runtime_type": "java-maven",
    "resources": {"cpu": 2, "memory_mb": 2048, "disk_mb": 4096, "timeout_seconds": 300},
}
test_command = "bash tests/run_tests.sh"
preview_port = 3000
if __name__ == "__main__":
    import json
    print(json.dumps({"runtime_config": runtime_config, "test_command": test_command, "preview_port": preview_port}))

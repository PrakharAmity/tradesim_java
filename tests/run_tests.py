import subprocess
import sys
import os

project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
os.chdir(project_root)

result = subprocess.run(
    ["java", "-cp", "target/tradesim.jar", "com.tradesim.TestRunner"],
    capture_output=True,
    text=True
)

if result.stdout:
    print(result.stdout, end="")
if result.stderr:
    print(result.stderr, file=sys.stderr, end="")

sys.exit(result.returncode)

#!/usr/bin/env bash
set -u
cd "$(dirname "$0")/.."
if ! command -v mvn &> /dev/null; then
    exec java -cp target/tradesim.jar com.tradesim.TestRunner
fi
mkdir -p target/surefire-reports
mvn -q test 1>&2
build_status=$?
BUILD_STATUS="$build_status" python3 - <<'PY'
import glob, json, os, sys, xml.etree.ElementTree as ET
names = ["test_single_trade_optimal","test_unlimited_trades_profit","test_limited_transaction_dp","test_fee_aware_strategy_profit","test_cooldown_reenter_validity","test_policy_engine_state_isolation"]
found = {}
for path in glob.glob("target/surefire-reports/TEST-*.xml"):
    try:
        root=ET.parse(path).getroot()
        for case in root.iter("testcase"):
            name=case.attrib.get("name","")
            if name in names:
                problem=case.find("failure")
                if problem is None: problem=case.find("error")
                found[name]={"Status":"failed" if problem is not None else "passed","Execution time":str(round(float(case.attrib.get("time","0"))*1000))+"ms"}
                if problem is not None: found[name]["Error"]=(problem.attrib.get("message") or (problem.text or "Assertion failed")).splitlines()[0]
    except (ET.ParseError, OSError): pass
for name in names:
    found.setdefault(name,{"Status":"failed","Execution time":"0ms","Error":"No JUnit result found; Maven test execution may have failed"})
passed=sum(1 for name in names if found[name]["Status"]=="passed")
failed=len(names)-passed
found.update({"Passed":passed,"Failed":failed,"Total bugs":6,"Total Execution time":str(sum(int(found[name]["Execution time"][:-2]) for name in names))+"ms"})
print(json.dumps(found,indent=2))
sys.exit(1 if failed or int(os.environ.get("BUILD_STATUS","0")) else 0)
PY

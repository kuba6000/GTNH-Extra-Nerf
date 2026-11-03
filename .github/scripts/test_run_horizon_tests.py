import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


LAUNCHER = Path(__file__).with_name("run_horizon_tests.py")


class HorizonLauncherTests(unittest.TestCase):
    def test_existing_success_report_cannot_be_reused_as_a_new_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, "launch-result.json").write_text(json.dumps({"exitCode": 0}), encoding="utf-8")
            result = subprocess.run([sys.executable, str(LAUNCHER), "--report-dir", directory],
                                    capture_output=True, text=True, timeout=10)
            self.assertEqual(result.returncode, 2)
            self.assertIn("Report directory must be new", result.stderr)

    def test_nonpositive_deadline_is_rejected_before_framework_preparation(self):
        result = subprocess.run([sys.executable, str(LAUNCHER), "--timeout-seconds", "0"],
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(result.returncode, 2)
        self.assertIn("must be positive", result.stderr)


if __name__ == "__main__":
    unittest.main()

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import tv_performance as perf


class PerformanceProfileTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.backup = Path(self.folder.name) / "state.json"
        self.scales = dict(zip(perf.SCALES, ["1.0", "0.5", None]))
        self.original = self.scales.copy()
        self.compile_fails = False
        self.mutations = []
        self.runner = self.enterContext(patch("tv_performance.adb", side_effect=self.adb))

    def adb(self, serial, *args):
        if args[:2] == ("shell", "getprop"):
            return {"ro.build.fingerprint": "test/fingerprint", "ro.build.version.sdk": "30"}[args[2]]
        if args[:3] == ("shell", "pm", "path"):
            return "package:/data/app/test/base.apk"
        if args[:3] == ("shell", "settings", "get"):
            return self.scales[args[4]] or "null"
        if args[:2] == ("shell", "settings"):
            # All mutations must follow a durable backup of the original values.
            self.assertEqual(self.original, json.loads(self.backup.read_text())["scales"])
            self.mutations.append(args)
            self.scales[args[4]] = args[5] if args[2] == "put" else None
            return ""
        if args[:4] == ("shell", "cmd", "package", "compile"):
            self.assertEqual(perf.APP, args[-1])
            self.assertNotIn("-a", args)
            if self.compile_fails:
                raise subprocess.CalledProcessError(1, "adb")
            return "Success"
        raise AssertionError("Comando inatteso: " + repr(args))

    def test_apply_backs_up_before_changes_and_compiles_only_our_app(self):
        perf.apply("test-device", self.backup)
        self.assertEqual(dict.fromkeys(perf.SCALES, "0"), self.scales)
        self.assertEqual(self.original, json.loads(self.backup.read_text())["scales"])
        if os.name != "nt":
            self.assertEqual(0o600, self.backup.stat().st_mode & 0o777)
        self.runner.assert_any_call("test-device", "shell", "cmd", "package", "compile",
                                    "-m", "speed", "-f", perf.APP)

    def test_compile_failure_restores_all_settings_including_absent_value(self):
        self.compile_fails = True
        with self.assertRaises(subprocess.CalledProcessError):
            perf.apply("test-device", self.backup)
        self.assertEqual(self.original, self.scales)
        self.assertIn(("shell", "settings", "delete", "global", perf.SCALES[2]), self.mutations)

    def test_existing_backup_is_not_overwritten_and_no_settings_change(self):
        self.backup.write_text("Existing backup")
        with self.assertRaises(FileExistsError):
            perf.apply("test-device", self.backup)
        self.assertEqual("Existing backup", self.backup.read_text())
        self.assertEqual([], self.mutations)

    def test_profile_refuses_another_android_version_before_any_mutation(self):
        self.runner.side_effect = lambda *args: "33" if args[-1] == "ro.build.version.sdk" else "test/fingerprint"
        with self.assertRaises(ValueError):
            perf.apply("test-device", self.backup)
        self.assertFalse(self.backup.exists())
        self.assertEqual([], self.mutations)

    def test_restore_returns_exact_values(self):
        perf.apply("test-device", self.backup)
        perf.restore("test-device", self.backup)
        self.assertEqual(self.original, self.scales)

    def test_restore_on_another_transport_does_not_change_settings(self):
        perf.apply("test-device", self.backup)
        self.mutations.clear()
        with self.assertRaises(ValueError):
            perf.restore("another-device", self.backup)
        self.assertEqual([], self.mutations)

    def test_untrusted_backup_cannot_introduce_shell_commands_or_other_keys(self):
        for scales in [{**self.original, perf.SCALES[0]: "0; reboot"},
                       {**self.original, "adb_enabled": "0"}]:
            with self.subTest(scales=scales):
                self.backup.write_text(json.dumps({"version": 1, "scales": scales}))
                with self.assertRaises(ValueError):
                    perf.restore("test-device", self.backup)
        self.runner.assert_not_called()

    def test_inspection_never_mutates_or_launches_apps(self):
        self.runner.side_effect = lambda *args: "null" if "settings" in args else "Read-only sample"
        perf.inspect("test-device")
        for call in self.runner.call_args_list:
            self.assertNotIn("put", call.args)
            self.assertNotIn("delete", call.args)
            self.assertNotIn("compile", call.args)
            self.assertNotIn("am", call.args)


if __name__ == "__main__":
    unittest.main()

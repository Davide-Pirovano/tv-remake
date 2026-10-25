import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import tv_cleanup as cleanup


class CleanupTest(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        self.backup = Path(folder.name) / "backup.json"
        self.original = dict(zip(list(cleanup.PACKAGES)[:5], range(5)))
        self.states = self.original.copy()
        self.device = {"transport": "tv", "fingerprint": "Changhong/DVB92/redwood:11/test", "sdk": "30"}
        self.enterContext(patch.object(cleanup, "identity", side_effect=lambda serial: {**self.device, "transport": serial}))
        self.runner = self.enterContext(patch.object(cleanup, "adb", side_effect=self.adb))
        self.mutations = []
        self.fail_once = False

    def adb(self, serial, *args):
        if args == ("shell", "am", "get-current-user"):
            return "0"
        if args[:4] == ("shell", "pm", "list", "packages"):
            return "\n".join("package:"+p for p in self.states)
        if args[:3] == ("shell", "dumpsys", "package"):
            return f"  User 0: installed=true hidden=false enabled={self.states[args[3]]}\n  User 10: enabled=1"
        if args[:2] == ("shell", "pm") and args[2] in cleanup.COMMANDS:
            self.assertEqual(self.original, json.loads(self.backup.read_text())["states"])
            self.assertIn(args[-1], cleanup.PACKAGES)
            self.mutations.append(args)
            if self.fail_once:
                self.fail_once = False
                raise RuntimeError("Simulated failure")
            self.states[args[-1]] = cleanup.COMMANDS.index(args[2])
            return ""
        self.fail("Unexpected command: " + repr(args))

    def test_apply_disables_only_enabled_apps_and_restore_preserves_all_five_states(self):
        cleanup.apply("tv", self.backup)
        self.assertEqual([3, 3, 2, 3, 4], list(self.states.values()))
        self.assertEqual(2, len(self.mutations))
        cleanup.restore("tv", self.backup)
        self.assertEqual(self.original, self.states)

    def test_failed_mutation_rolls_back(self):
        self.fail_once = True
        with self.assertRaises(RuntimeError):
            cleanup.apply("tv", self.backup)
        self.assertEqual(self.original, self.states)

    def test_backup_cannot_be_overwritten(self):
        self.backup.write_text("existing")
        with self.assertRaises(FileExistsError):
            cleanup.apply("tv", self.backup)
        self.assertEqual([], self.mutations)

    def test_restore_rejects_another_transport(self):
        cleanup.apply("tv", self.backup)
        self.mutations.clear()
        with self.assertRaises(ValueError):
            cleanup.restore("other-tv", self.backup)
        self.assertEqual([], self.mutations)

    def test_backup_cannot_introduce_other_packages_or_shell_syntax(self):
        for states in [{"com.android.vending": 3}, {"x; reboot": 0},
                       {next(iter(cleanup.PACKAGES)): True}, {}, []]:
            self.backup.write_text(json.dumps({"version": 1, "states": states}))
            with self.assertRaises(ValueError):
                cleanup.restore("tv", self.backup)
        self.runner.assert_not_called()

    def test_unknown_firmware_refused_before_changes(self):
        self.device["fingerprint"] = "other-tv"
        with self.assertRaises(ValueError):
            cleanup.apply("tv", self.backup)
        self.assertFalse(self.backup.exists())
        self.assertEqual([], self.mutations)

    def test_inspect_is_read_only(self):
        cleanup.inspect("tv")
        self.assertEqual([], self.mutations)
        self.assertFalse(self.backup.exists())

    def test_protected_features_are_outside_cleanup_scope(self):
        protected = {"it.saba.guesthome", "com.android.vending", "com.google.android.gms",
                     "com.google.android.gsf", "com.google.android.tvlauncher", "com.mediatek.wwtv.tvcenter",
                     "com.google.android.apps.mediashell", "com.homwee.multiscreenshare",
                     "com.homwee.mmp.screenshare", "com.changhong.inface.net.wfd",
                     "com.homwee.synckey", "com.homwee.upgrade", "com.homwee.dmt.upgrade",
                     "com.google.android.youtube.tv", "com.netflix.ninja", "com.spotify.tv.android",
                     "com.amazon.amazonvideo.livingroom", "com.google.android.katniss"}
        self.assertFalse(protected.intersection(cleanup.PACKAGES))


if __name__ == "__main__":
    unittest.main()

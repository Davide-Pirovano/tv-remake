#!/usr/bin/env python3
"""Read performance diagnostics or apply a small Android 11 profile with a settings backup."""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess


APP = "it.saba.guesthome"
SCALES = ("window_animation_scale", "transition_animation_scale", "animator_duration_scale")


def adb(serial, *args):
    result = subprocess.run([os.environ.get("ADB", "adb"), "-s", serial, *args],
                            check=True, capture_output=True, text=True, timeout=90)
    return result.stdout.strip()


def identity(serial):
    return {"transport": serial,
            "fingerprint": adb(serial, "shell", "getprop", "ro.build.fingerprint"),
            "sdk": adb(serial, "shell", "getprop", "ro.build.version.sdk")}


def validate_scales(values):
    if not isinstance(values, dict) or set(values) != set(SCALES):
        raise ValueError("Backup non valido: attese soltanto le tre scale animazione.")
    for value in values.values():
        if value is not None and (not isinstance(value, str)
                                  or re.fullmatch(r"[0-9]+(?:\.[0-9]+)?", value) is None):
            raise ValueError("Scala animazione non valida: nessuna modifica effettuata.")


def read_scales(serial):
    values = {}
    for key in SCALES:
        value = adb(serial, "shell", "settings", "get", "global", key)
        values[key] = None if value == "null" else value
    validate_scales(values)
    return values


def write_private_json(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    with os.fdopen(descriptor, "w") as output:
        json.dump(data, output, indent=2, ensure_ascii=False)
        output.write("\n")
        output.flush()
        os.fsync(output.fileno())


def restore_scales(serial, values):
    validate_scales(values)
    for key in SCALES:
        value = values[key]
        if value is None:
            adb(serial, "shell", "settings", "delete", "global", key)
        else:
            adb(serial, "shell", "settings", "put", "global", key, value)


def apply(serial, backup):
    device = identity(serial)
    if device["sdk"] != "30":
        raise ValueError("Questo profilo è destinato ad Android 11 (SDK 30).")
    if not adb(serial, "shell", "pm", "path", APP).startswith("package:"):
        raise ValueError("Casa · Guest TV non risulta installata.")
    original = read_scales(serial)
    # A durable, exclusive backup must exist before the first device mutation.
    write_private_json(backup, {"version": 1, "device": device, "scales": original,
                                "created": datetime.now(timezone.utc).isoformat()})
    try:
        for key in SCALES:
            adb(serial, "shell", "settings", "put", "global", key, "0")
        output = adb(serial, "shell", "cmd", "package", "compile", "-m", "speed", "-f", APP)
        if "Success" not in output.splitlines():
            raise RuntimeError("Compilazione ART non confermata: " + output)
    except BaseException:
        try:
            restore_scales(serial, original)
        except Exception as error:
            raise RuntimeError("Ripristino automatico incompleto: conserva il backup e usa restore.") from error
        raise


def restore(serial, backup):
    state = json.loads(Path(backup).read_text())
    if not isinstance(state, dict) or state.get("version") != 1:
        raise ValueError("Versione del backup non riconosciuta.")
    validate_scales(state.get("scales"))
    if state.get("device") != identity(serial):
        raise ValueError("Il backup appartiene a un altro collegamento o firmware: ripristino interrotto.")
    restore_scales(serial, state["scales"])


def inspect(serial):
    report = {"device": identity(serial), "scales": read_scales(serial),
              "collected": datetime.now(timezone.utc).isoformat()}
    commands = {
        "ram": ("cat", "/proc/meminfo"),
        "storage": ("df", "/data"),
        "cpu": ("top", "-b", "-n", "1", "-m", "10"),
        "launcher_memory": ("dumpsys", "meminfo", APP),
        "original_home_memory": ("dumpsys", "meminfo", "com.google.android.tvlauncher"),
        "live_tv_memory": ("dumpsys", "meminfo", "com.mediatek.wwtv.tvcenter"),
        "launcher_frames": ("dumpsys", "gfxinfo", APP, "framestats"),
    }
    for key, args in commands.items():
        try:
            report[key] = adb(serial, "shell", *args)
        except (subprocess.SubprocessError, OSError) as error:
            report[key] = {"unavailable": str(error)}
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Dispositivo ADB già collegato")
    actions = parser.add_subparsers(dest="action", required=True)
    diagnostic = actions.add_parser("inspect", help="Leggi CPU, RAM, spazio e frame, senza modifiche")
    diagnostic.add_argument("--output", type=Path)
    for command in ("apply", "restore"):
        sub = actions.add_parser(command)
        sub.add_argument("--backup", type=Path, required=True)
    args = parser.parse_args()
    try:
        if args.action == "inspect":
            report = inspect(args.serial)
            if args.output:
                write_private_json(args.output, report)
                print("Report salvato in", args.output)
            else:
                print(json.dumps(report, indent=2, ensure_ascii=False))
        elif args.action == "apply":
            apply(args.serial, args.backup)
            print("Animazioni di sistema disattivate; compilazione ART del solo launcher completata.")
            print("Backup delle scale:", args.backup)
        else:
            restore(args.serial, args.backup)
            print("Ripristinate le tre scale animazione. La compilazione ART resta gestita da Android.")
    except (ValueError, OSError, RuntimeError, subprocess.SubprocessError) as error:
        parser.exit(1, "Errore: " + str(error) + "\n")


if __name__ == "__main__":
    main()

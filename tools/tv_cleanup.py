#!/usr/bin/env python3
"""Pulizia reversibile delle sole app accessorie del Saba DVB92 Android 11."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess

from tv_performance import adb, identity, write_private_json

# Fixed scope: never infer that an unknown vendor package is disposable.
PACKAGES = {
    "com.google.android.play.games": "Google Play Games",
    "com.google.android.youtube.tvmusic": "YouTube Music",
    "com.google.android.videos": "Google TV / Play Film",
    "com.homwee.alexacontrol": "Alexa Control",
    "com.homwee.store": "Store del produttore",
    "com.homwee.help": "Guida del produttore",
    "com.homwee.aipont": "Portale AI PONT",
    "com.google.android.feedback": "Invio feedback Google",
    "com.google.android.syncadapters.calendar": "Sincronizzazione calendario",
    "android.autoinstalls.config.homwee": "Suggerimenti di installazione partner",
}
COMMANDS = ("default-state", "enable", "disable", "disable-user", "disable-until-used")


def checked_device(serial):
    device = identity(serial)
    if device["sdk"] != "30" or not device["fingerprint"].startswith("Changhong/DVB92/redwood:11/"):
        raise ValueError("Profilo destinato esclusivamente al Saba DVB92/redwood Android 11.")
    if adb(serial, "shell", "am", "get-current-user") != "0":
        raise ValueError("Atteso il profilo Android 0 del televisore.")
    return device


def state(serial, package):
    output = adb(serial, "shell", "dumpsys", "package", package)
    match = re.search(r"^\s*User 0:([^\n]*)", output, re.MULTILINE)
    value = re.search(r"\benabled=([0-4])\b", match[1]) if match else None
    if value is None:
        raise ValueError("Stato non riconosciuto per " + package + ": nessuna modifica a questa app.")
    return int(value[1])


def inspect(serial):
    device = checked_device(serial)
    installed = set(adb(serial, "shell", "pm", "list", "packages", "--user", "0").splitlines())
    states = {package: state(serial, package) for package in PACKAGES if "package:" + package in installed}
    return {"version": 1, "device": device, "states": states,
            "created": datetime.now(timezone.utc).isoformat()}


def validate(states):
    if (not isinstance(states, dict) or not states or not set(states).issubset(PACKAGES)
            or any(type(value) is not int or value not in range(5) for value in states.values())):
        raise ValueError("Backup non valido o contenente app fuori dal profilo autorizzato.")


def set_state(serial, package, value):
    adb(serial, "shell", "pm", COMMANDS[value], "--user", "0", package)
    if state(serial, package) != value:
        raise RuntimeError("Android non conferma il cambio di stato di " + package)


def restore_states(serial, states):
    validate(states)
    failed = []
    for package, value in states.items():
        try:
            set_state(serial, package, value)
        except (ValueError, OSError, RuntimeError, subprocess.SubprocessError):
            failed.append(package)
    if failed:
        raise RuntimeError("Ripristino incompleto; conserva il backup e riprova: " + ", ".join(failed))


def apply(serial, backup):
    original = inspect(serial)
    validate(original["states"])
    write_private_json(backup, original)  # Durable, exclusive backup before any change.
    try:
        for package, value in original["states"].items():
            if value in (0, 1):  # Already-disabled apps retain their exact original state.
                set_state(serial, package, 3)
    except BaseException:
        restore_states(serial, original["states"])
        raise
    return original


def restore(serial, backup):
    original = json.loads(Path(backup).read_text())
    if not isinstance(original, dict) or original.get("version") != 1:
        raise ValueError("Formato backup non riconosciuto.")
    validate(original.get("states"))
    if original.get("device") != checked_device(serial):
        raise ValueError("Backup di un altro collegamento o firmware: nessuna modifica.")
    restore_states(serial, original["states"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Collegamento ADB già autorizzato")
    actions = parser.add_subparsers(dest="action", required=True)
    actions.add_parser("inspect", help="Mostra il piano senza modificare il TV")
    for action in ("apply", "restore"):
        actions.add_parser(action).add_argument("--backup", type=Path, required=True)
    args = parser.parse_args()
    try:
        if args.action == "inspect":
            report = inspect(args.serial)
            for package, value in report["states"].items():
                print(f"{PACKAGES[package]}: {'da disattivare' if value in (0, 1) else 'già disattivata'} ({package})")
        elif args.action == "apply":
            report = apply(args.serial, args.backup)
            print("Profilo applicato; app accessorie presenti:", len(report["states"]))
            print("Nessuna app o dato cancellato. Backup:", args.backup)
        else:
            restore(args.serial, args.backup)
            print("Ripristinati gli stati originali delle app del profilo.")
    except (ValueError, OSError, RuntimeError, subprocess.SubprocessError) as error:
        parser.exit(1, "Errore: " + str(error) + "\n")


if __name__ == "__main__":
    main()
